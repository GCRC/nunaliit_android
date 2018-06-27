package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchDesignDocument;
import ca.carleton.gcrc.couch.client.CouchQuery;
import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.JSONGlue;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.couchbase.lite.replicator.RemoteRequest.JSON;

/**
 * Created by jpfiset on 3/21/16.
 */
public class ConnectionSyncProcess {
    protected final String TAG = this.getClass().getSimpleName();

    private Connection connection;
    private CouchbaseLiteService service;
    private CouchDb couchDb;
    private CouchDesignDocument atlasDesign;
    private DocumentDb documentDb;
    private TrackingDb trackingDb;

    private OkHttpClient submissionClient;

    private ConnectionSyncResult result = new ConnectionSyncResult();

    public ConnectionSyncProcess(CouchbaseLiteService service, Connection connection) throws Exception {
        this.service = service;
        this.connection = connection;

        couchDb = connection.getRemoteCouchDb();
        atlasDesign = couchDb.getDesignDocument("atlas");

        documentDb = connection.getLocalDocumentDb();
        trackingDb = connection.getLocalTrackingDb();

        submissionClient = new OkHttpClient();
    }

    public ConnectionSyncResult synchronize() throws Exception {
        Log.v(TAG, "Synchronization started");

        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_DOWNLOADING_DOCUMENTS);

        List<JSONObject> remoteDocs = fetchAllDocuments();
        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_UPDATING_LOCAL_DOCUMENTS);

        updateLocalDocumentsFromRemote(remoteDocs);
        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_UPDATING_REMOTE_DOCUMENTS);

        updateRemoteDocuments(remoteDocs);

        return result;
    }

    public List<JSONObject> fetchAllDocuments() throws Exception {
        List<JSONObject> remoteDocs = getDocsFromView("skeleton-docs");

        Log.v(TAG, "Synchronization received " + remoteDocs.size() + " skeleton document(s)");

        // Update Documents from the Schema
        Log.v(TAG, "Synchronization fetching subdocuments");

        HashMap<String, JSONObject> subdocumentMap = new HashMap<>();
        Set<String> schemaIdSet = new HashSet<>();

        for(JSONObject skeletonDoc : remoteDocs) {
            subdocumentMap.put(skeletonDoc.getString("_id"), skeletonDoc);
        }

        // Get all of the schemas.
        for (JSONObject doc : remoteDocs) {
            if (doc.has("nunaliit_schema") && Objects.equals(doc.getString("nunaliit_schema"), "schema")) {
                schemaIdSet.add(doc.getString("name"));
            }
        }

        List<JSONObject> subdocsForSchema = fetchDocumentsForSchemas(schemaIdSet);

        for(JSONObject subdoc : subdocsForSchema) {
            String id = subdoc.getString("_id");
            subdocumentMap.put(id, subdoc);
        }

        return new ArrayList<>(subdocumentMap.values());
    }

    public List<JSONObject> fetchDocumentsForSchemas(Collection<String> schemaList) throws Exception {
        Log.v(TAG, "Fetching Subdocuments for schemas started");

        List<JSONObject> subdocuments = getDocsFromView("nunaliit-schema", schemaList);

        Log.v(TAG, "Subdocument Synchronization received "+subdocuments.size()+" subdocument(s)");

        return subdocuments;
    }

    public void updateLocalDocumentsFromRemote(List<JSONObject> documents) throws Exception {
        int updatedCount = 0;
        int failedCount = 0;
        for (JSONObject doc : documents) {
            try {
                boolean updated = updateDocumentDatabaseIfNeed(doc);
                if (updated) {
                    ++updatedCount;
                }
            } catch (Exception e) {
                failedCount++;

                Log.d(TAG, "Failure Updating Local Document: " + doc.optString("_id", ""));
                e.printStackTrace();
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        result.setFilesClientUpdated(updatedCount);
        result.setFilesFailedClientUpdated(failedCount);

        Log.i(TAG, "Synchronization updated " + updatedCount + " documents");

        Log.v(TAG, "Synchronization complete");
    }

    public List<JSONObject> getDocsFromView(String view) throws Exception {
        return getDocsFromView(view, null);
    }

    public List<JSONObject> getDocsFromView(String view, Collection<String> keys) throws Exception {
        CouchQuery query = new CouchQuery();
        if (keys != null && !keys.isEmpty()) {
            query.setKeys(keys);
        }
        query.setViewName(view);
        query.setReduce(false);
        query.setIncludeDocs(true);

        CouchQueryResults results = atlasDesign.performQuery(query);

        List<JSONObject> docs = new Vector<JSONObject>();
        List<JSONObject> rows = JSONGlue.convertJSONObjectCollectionFromUpstreamToAndroid(results.getRows());
        for(JSONObject row : rows){
            JSONObject doc = row.getJSONObject("doc");
            docs.add(doc);
        }
        return docs;
    }

    public boolean updateDocumentDatabaseIfNeed(JSONObject remoteDoc) throws Exception {
        String docId = remoteDoc.getString("_id");

        JSONObject localDocument = documentDb.getDocument(docId);
        Revision revisionRecord = getRevisionRecord(docId);

        boolean changedLocally = localDocument.has("_rev") && !(localDocument.getString("_rev").equals(revisionRecord.getLocalRevision()));

        // If the local document has changes.
        if (!changedLocally) {
            Log.v(TAG, docId + " is unchanged locally");
        } else {
            Log.v(TAG, docId + " is changed locally");
        }

        boolean changedRemote = !(remoteDoc.getString("_rev").equals(revisionRecord.getRemoteRevision()));
        boolean isNewCommit = isNewCommit(localDocument, revisionRecord);

        // Check to see if the remote document has changes.
        if (!changedRemote) {
            Log.v(TAG, docId + " is unchanged on remote");
        } else {
            Log.v(TAG, docId + " is changed on remote");
        }

        /*
            We only want to update the local copy with the remote copy IF:

            You have no local changes

            - OR -

            You have committed your local changes and you have no made changes since your previous
            commit.
         */
        boolean isUnchangedLocalVersion = !changedLocally || (changedLocally && !isNewCommit);

        if (changedRemote && isUnchangedLocalVersion) {
            Revision revision = getRevisionRecord(docId);
            updateDocumentDatabase(remoteDoc, revision);
            return true;
        }

        if (changedLocally && isNewCommit) {
            try {
                updateRemoteDocument(localDocument);
                result.setFilesRemoteUpdated(result.getFilesRemoteUpdated() + 1);
                return true;
            } catch (Exception e) {
                result.setFilesFailedRemoteUpdated(result.getFilesFailedRemoteUpdated() + 1);
            }
        }

        return false;
    }

    public Revision getRevisionRecord(String docId) throws Exception {
        Revision revisionRecord = trackingDb.getRevisionFromDocId(docId);
        if( null == revisionRecord ){
            revisionRecord = new Revision();
        }
        revisionRecord.setDocId(docId);

        return revisionRecord;
    }

    private boolean isNewCommit(JSONObject localDocument, Revision revisionRecord) throws JSONException {
        String lastCommitVersion = revisionRecord.getLastCommit();
        return lastCommitVersion == null || !lastCommitVersion.equals(localDocument.getString("_rev"));
    }

    public void updateDocumentDatabase(JSONObject doc, Revision revisionRecord) throws Exception {
        String docId = doc.getString("_id");
        String remoteRev = doc.optString("_rev", null);

        CouchbaseDocInfo info;
        if( documentDb.documentExists(docId) ) {
            JSONObject existingDoc = documentDb.getDocument(docId);
            String existingRev = existingDoc.optString("_rev",null);
            if( null != existingRev ){
                doc.put("_rev",existingRev);
            }

            // If there is an _attachments, the document will not be updated.
            doc.remove("_attachments");
            info = documentDb.updateDocument(doc);
        } else {
            // When creating a document, no revision should be set
            doc.remove("_rev");

            // If there is an _attachments, the document will not be created.
            doc.remove("_attachments");

            info = documentDb.createDocument(doc);
        }

        revisionRecord.setRemoteRevision(remoteRev);
        revisionRecord.setLocalRevision(info.getRev());
        trackingDb.updateRevision(revisionRecord);
    }

    public void updateRemoteDocuments(List<JSONObject> remoteDocuments) {
        List<JSONObject> newLocalDocuments = getNewLocalDocuments(remoteDocuments);

        for(JSONObject doc : newLocalDocuments) {
            try {
                Revision revisionRecord = getRevisionRecord(doc.optString("_id", ""));

                if (isNewCommit(doc, revisionRecord)) {
                    updateRemoteDocument(doc);
                    result.setFilesRemoteUpdated(result.getFilesRemoteUpdated() + 1);
                }
            } catch (Exception e) {
                result.setFilesFailedRemoteUpdated(result.getFilesFailedRemoteUpdated() + 1);

                Log.d(TAG, "Failure Updating Remote Document: " + doc.optString("_id", ""));
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    public List<JSONObject> getNewLocalDocuments(List<JSONObject> remoteDocuments) {
        try {
            List<JSONObject> localDocuments = documentDb.getAllDocuments();

            HashMap<String, JSONObject> localDocumentsMap = new HashMap<String, JSONObject>();

            for (JSONObject doc : localDocuments) {
                localDocumentsMap.put (doc.getString("_id"), doc);
            }

            for (JSONObject doc : remoteDocuments) {
                if (localDocumentsMap.containsKey(doc.getString("_id"))) {
                    localDocumentsMap.remove(doc.getString("_id"));
                }
            }

            return new ArrayList<>(localDocumentsMap.values());

        } catch (Exception e) {
            throw new RuntimeException ("Unable to fetch local documents", e);
        }
    }

    public void updateRemoteDocument(JSONObject document) throws Exception {

        String docId = document.getString("_id");
        Revision revisionRecord = getRevisionRecord(docId);
        nunaliit.org.json.JSONObject couchDoc = JSONGlue.convertJSONObjectFromAndroidToUpstream(document);

        if (!couchDb.documentExists(couchDoc)) {
            document.remove("_rev");
        } else {
            document.putOpt("_rev", revisionRecord.getRemoteRevision());
        }
        writeDocumentToSubmissionDatabase(document);

        // The document has been committed. Save a reference to its last commit.
        JSONObject localDocument = documentDb.getDocument(docId);
        revisionRecord.setLastCommit(localDocument.getString("_rev"));
        trackingDb.updateRevision(revisionRecord);
    }

    public void writeDocumentToSubmissionDatabase(JSONObject document) throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        Response loginResponse = submissionClient.newCall(createAuthRequest()).execute();
        String cookie = loginResponse.header("Set-Cookie");
        cookie += "; NunaliitAuth=" + info.getUser();

        document = removeNullAttachments(document);
        String uploadPath = getNunaliitAttachmentPath(document);
        String uploadId = addNunaliitAttachments(document);

        // Remove the nunaliit_mobile_attachments but keep it on the app. It will be removed
        // when the document is synced down from the server.
        document.remove("nunaliit_mobile_attachments");

        Response response = submissionClient.newCall(createDocumentUploadRequest(document, cookie)).execute();
        Log.v(TAG, response.body().string());

        if (!response.isSuccessful()) {
            throw new RuntimeException("Creating new database document failed: " + response.body());
        }

        if (response.isSuccessful() && uploadPath != null && uploadId != null) {
            if (uploadPath.equals("null")) return;
            Response imageResponse = submissionClient.newCall(createAttachmentRequest(uploadId, uploadPath, cookie)).execute();
            Log.v(TAG, imageResponse.body().string());

            if (!imageResponse.isSuccessful()) {
                throw new RuntimeException("Creating new database image failed: " + response.body());
            }
        }
    }

    private Request createAuthRequest() throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        // Auth
        HttpUrl loginUrl = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/server/_session").build();
        FormBody formBody = new FormBody.Builder().add("name", info.getUser()).add("password", info.getPassword()).build();
        Request loginRequest = new Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .header("Accept", "application/json")
                .build();

        return loginRequest;
    }

    public Request createDocumentUploadRequest(JSONObject document, String authCookie) throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        String docString = document.toString();

        // Put the item in the submission database.
        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/submission/submissionDb").addPathSegment(document.getString("_id")).addQueryParameter("deviceId", info.getDeviceId()).build();
        RequestBody body = RequestBody.create(JSON, docString);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .header("Cookie", authCookie)
                .build();

        return request;
    }

    public Request createAttachmentRequest(String uploadId, String filepath, String authCookie) throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        MediaType mediaType = getMediaType(filepath);
        String progressId = UUID.randomUUID().toString();
        File file = new File(filepath);

        String uploadIdHeader = "form-data; name=\"uploadId\"";
        String progressHeader = "form-data; name=\"progressId\"";
        String fileHeader = "form-data; name=\"media\"; filename=" + file.getName();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(
                        Headers.of("Content-Disposition", uploadIdHeader),
                        RequestBody.create(null, uploadId)
                )
                .addPart(
                        Headers.of("Content-Disposition", progressHeader),
                        RequestBody.create(null, progressId)
                )
                .addPart(
                        Headers.of("Content-Disposition", fileHeader),
                        RequestBody.create(mediaType, file)
                ).build();

        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/upload/put").build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cookie", authCookie)
                .build();

        return request;
    }

    public MediaType getMediaType(String filepath) {
        try {
            // Some of the documents are tagged with "null", once those documents are fixed, this
            // can be removed.
            if (filepath == null && filepath.equals("null")) return null;

            String extension = MimeTypeMap.getFileExtensionFromUrl(filepath);
            return MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
        } catch (Exception e) {
            Log.e(TAG, "Could not get media type for filepath: " + filepath);
            return null;
        }
    }

    public JSONObject removeNullAttachments(JSONObject document) {
        if (document.optString("nunaliit_attachments", null).equals("null")) {
            document.remove("nunaliit_attachments");
        }

        if (document.optString("nunaliit_mobile_attachments", null).equals("null")) {
            document.remove("nunaliit_mobile_attachments");
        }

        return document;
    }

    public String getNunaliitAttachmentPath(JSONObject document) {
        return document.optString("nunaliit_mobile_attachments", null);
    }

    public String addNunaliitAttachments(JSONObject document) throws Exception {

        String path = document.optString("nunaliit_mobile_attachments", null);
        if (path != null && !path.equals("null")) {
            String uploadId = UUID.randomUUID().toString();

            // Attachment Documents
            JSONObject nunaliitAttachments = new JSONObject();
            nunaliitAttachments.put("nunaliit_type", "attachment_descriptions");

            JSONObject files = new JSONObject();
            JSONObject media = new JSONObject();
            JSONObject data = new JSONObject();

            media.put("attachmentName", "media");
            media.put("status", "waiting for upload");
            media.put("uploadId", uploadId);
            media.put("data", data);

            files.put("media", media);

            nunaliitAttachments.put("files", files);

            document.put("nunaliit_attachments", nunaliitAttachments);

            return uploadId;
        }

        return null;
    }

    private void sendSyncProgressIntent(int progressState) {
        Intent progress = new Intent(ConnectionManagementService.PROGRESS_SYNC);
        Log.v(TAG, "Result: " + progress.getAction() + Nunaliit.threadId());
        progress.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connection.getConnectionInfo().getId());

        progress.putExtra(Nunaliit.EXTRA_SYNC_PROGRESS_STATE, progressState);

        LocalBroadcastManager.getInstance(service).sendBroadcast(progress);
    }
}