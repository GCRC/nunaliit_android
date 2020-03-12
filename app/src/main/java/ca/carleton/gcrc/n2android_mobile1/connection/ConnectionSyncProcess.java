package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchDesignDocument;
import ca.carleton.gcrc.couch.client.CouchQuery;
import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.JSONGlue;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import static com.couchbase.lite.replicator.RemoteRequest.JSON;

/**
 * Created by jpfiset on 3/21/16.
 */
public class ConnectionSyncProcess {
    protected final String TAG = this.getClass().getSimpleName();

    private final Connection connection;
    private final CouchbaseLiteService service;
    private final CouchDb couchDb;
    private final CouchDesignDocument atlasDesign;
    private final DocumentDb localDocumentDb;
    private final TrackingDb trackingDb;

    private final OkHttpClient submissionClient;

    private final ConnectionSyncResult result = new ConnectionSyncResult();

    private Map<String, SubmissionStatus> submissionStatusByDocId;

    private enum SubmissionStatus {
        NotSubmitted,
        WaitingForApproval,
        Completed,
        Declined,
        Deleted,
        Unknown
    }

    public ConnectionSyncProcess(CouchbaseLiteService service, Connection connection) throws Exception {
        this.service = service;
        this.connection = connection;

        couchDb = connection.getRemoteCouchDb();
        atlasDesign = couchDb.getDesignDocument("atlas");

        localDocumentDb = connection.getLocalDocumentDb();
        trackingDb = connection.getLocalTrackingDb();

        submissionClient = new OkHttpClient();
    }

    public ConnectionSyncResult synchronize() throws Exception {
        Log.v(TAG, "Synchronization started");

        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_DOWNLOADING_DOCUMENTS);

        JSONObject submissionStatus = getSubmissionStatus();
        submissionStatusByDocId = createSubmissionStatusMap(submissionStatus);

        List<JSONObject> remoteDocuments = fetchAllRemoteDocuments();

        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_UPDATING_LOCAL_DOCUMENTS);

        /*
            Step 1:

            Check to see if the document exists on the mobile database and in the authoritative database.
            If the document has previously existed in the authoritative database:
            It has been deleted, delete your mobile copy (regardless of edits)
        */

        int deletedOnLocal = 0;
        int deletedSubmitted = 0;

        List<JSONObject> newLocalDocuments = getNewLocalDocuments(remoteDocuments);
        deletedOnLocal += deleteLocalDocumentsPreviouslyDeletedOnRemote(newLocalDocuments);


        /*
            Step 2:

            Delete documents that have been deleted on the local and have not been deleted
            on the remote.
         */

        // Check all locals documents that don't have remote revisions and remove them.
        List<JSONObject> documentsPendingDeletion = getDocumentsPendingDeletion();

        // Deleted Document flow
        for (JSONObject deletedDocument : documentsPendingDeletion) {
            // If the document only exists on mobile, delete it.
            if (!hasBeenCommitedToRemote(deletedDocument)) {
                deleteDocumentOnMobile(deletedDocument);
                deletedOnLocal += 1;
                continue;
            }

            if (getRevisionRecord(deletedDocument).getRemoteRevision() == null) {
                Log.v(TAG, "No need to keep around documents that have not been synced" +
                        "on remote - Delete them on local (they might be re-synced later.");
                deleteDocumentOnMobile(deletedDocument);
                continue;
            }

            if (!hasDeleteBeenSubmitted(deletedDocument)) {
                deletedSubmitted += sendDeleteRequestToRemote(deletedDocument) ? 1 : 0;
            } else {
                SubmissionStatus deletionSubmissionStatus = getSubmissionStatusForDocument(deletedDocument);

                if (deletionSubmissionStatus == SubmissionStatus.Completed) {
                    deleteDocumentOnMobile(deletedDocument);
                    deletedOnLocal += 1;
                } else if (deletionSubmissionStatus == SubmissionStatus.Declined) {
                    // The user deleted their local copy.
                    // Refetch the document from the remote in the next step.
                    deleteDocumentOnMobile(deletedDocument);
                }
            }
        }

        result.setFilesDeletedLocal(deletedOnLocal);
        result.setFilesDeleteRequest(deletedSubmitted);

        /*
            Step 3:

            If the user has any edits to the document (or it is a new document)
            on their mobile database, submit them to the submission database.
        */

        List<JSONObject> prunedNewDocuments = getNewLocalDocuments(remoteDocuments);
        sendNewDocumentsToRemote(prunedNewDocuments);

        sendSyncProgressIntent(ConnectionManagementService.PROGRESS_SYNC_UPDATING_REMOTE_DOCUMENTS);

        /*
            Step 4:

            If the document does not have any edits, is not in the `waiting_for_approval` state,
            update the local document with the remote version.
        */

        updateAllDocumentsIfNeeded(remoteDocuments);

        return result;
    }

    private boolean hasFlagDeleted(JSONObject localDocument) throws Exception {
        return localDocument.optBoolean("nunaliit_mobile_deleted", false);
    }

    private void removeDeletedFlag(JSONObject localDocument) throws Exception {
        localDocument.put("nunaliit_mobile_deleted", false);
        localDocument.put("nunaliit_mobile_delete_submitted", false);
        localDocumentDb.updateDocument(localDocument);
    }

    private boolean hasDeleteBeenSubmitted(JSONObject document) throws Exception {
        return document.optBoolean("nunaliit_mobile_delete_submitted", false);
    }

    private boolean hasBeenCommitedToRemote(JSONObject document) throws Exception {
        return getRevisionRecord(document).getLastCommit() != null;
    }

    private void deleteDocumentOnMobile(JSONObject document) throws Exception {
        localDocumentDb.deleteDocument(document);
    }

    /**
     * Gets all skeleton docs, then all documents for schemas found in skeleton docs.
     *
     * @return
     * @throws Exception
     */
    private List<JSONObject> fetchAllRemoteDocuments() throws Exception {
        // seleton-docs view
        List<JSONObject> remoteDocs = fetchSkeletonDocs();

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

        // Get all docs for those schemas.
        List<JSONObject> subdocsForSchema = fetchDocumentsForSchemas(schemaIdSet);

        for(JSONObject subdoc : subdocsForSchema) {
            String id = subdoc.getString("_id");
            subdocumentMap.put(id, subdoc);

            if (subdoc.has("_attachments")) {
                Log.v(TAG, "SARAH: doc has _attachments: " + subdoc.getString("_attachments"));
                JSONObject attachments = subdoc.getJSONObject("_attachments");
            }

        }

        return new ArrayList<>(subdocumentMap.values());
    }

    private List<JSONObject> fetchDocumentsForSchemas(Collection<String> schemaList) throws Exception {
        Log.v(TAG, "Fetching Subdocuments for schemas started");

        List<JSONObject> subdocuments = getDocsFromView("nunaliit-schema", schemaList);

        Log.v(TAG, "Subdocument Synchronization received "+subdocuments.size()+" subdocument(s)");

        return subdocuments;
    }

    private List<JSONObject> fetchSkeletonDocs() throws Exception {
        return getDocsFromView("skeleton-docs", null);
    }

    private List<JSONObject> getDocsFromView(String view, Collection<String> keys) throws Exception {
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

    private boolean shouldUpdateLocalDocument(JSONObject remoteDoc) throws Exception {
        String docId = remoteDoc.getString("_id");

        JSONObject localDocument = localDocumentDb.getDocument(docId);

        SubmissionStatus documentSubmissionStatus = getSubmissionStatusForDocument(localDocument);

        if (documentSubmissionStatus == SubmissionStatus.WaitingForApproval) {
            return false;
        }

        boolean requiresUpdate = !isChangedOnLocal(localDocument) && (isChangedOnRemote(localDocument, remoteDoc) || documentSubmissionStatus == SubmissionStatus.Declined);

        Log.v(TAG, String.format("Local Document %s requires an update: %b", docId, requiresUpdate));

        return requiresUpdate;
    }

    private boolean shouldUpdateRemoteDocument(JSONObject remoteDoc) throws Exception {
        String docId = remoteDoc.getString("_id");
        JSONObject localDocument = localDocumentDb.getDocument(docId);

        boolean requiresUpdate = isChangedOnLocal(localDocument) && isRequiresSubmission(localDocument);

        Log.v(TAG, String.format("Remote Document %s requires an update: %b", docId, requiresUpdate));

        return requiresUpdate;
    }

    private void updateAllDocumentsIfNeeded(List<JSONObject> remoteDocuments) throws Exception {
        int updatedCount = 0;
        int failedCount = 0;
        for (JSONObject doc : remoteDocuments) {
            try {
                boolean updated = updateDocumentIfNeeded(doc);
                if (updated) {
                    ++updatedCount;
                }

                syncAttachmentsToLocal(doc);
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

    private boolean updateDocumentIfNeeded(JSONObject remoteDoc) throws Exception {
        String docId = remoteDoc.getString("_id");

        JSONObject localDocument = localDocumentDb.getDocument(docId);

        if (hasFlagDeleted(localDocument)) {
            return false;
        }

        if (shouldUpdateLocalDocument(remoteDoc)) {
            updateLocalDocument(remoteDoc);
            return true;
        }

        if (shouldUpdateRemoteDocument(remoteDoc)) {
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

    private Revision getRevisionRecord(JSONObject document) throws Exception {
        return getRevisionRecord(document.getString("_id"));
    }

    private Revision getRevisionRecord(String docId) throws Exception {
        Revision revisionRecord = trackingDb.getRevisionFromDocId(docId);
        if( null == revisionRecord ){
            revisionRecord = new Revision();
        }
        revisionRecord.setDocId(docId);

        return revisionRecord;
    }

    private boolean isChangedOnRemote(JSONObject localDoc, JSONObject remoteDoc) throws Exception {
        if (!Objects.equals(localDoc.getString("_id"), remoteDoc.getString("_id"))) {
            throw new RuntimeException("The two documents must have the same document ID");
        }

        Revision revisionRecord = getRevisionRecord(localDoc);

        return !(remoteDoc.getString("_rev").equals(revisionRecord.getRemoteRevision()));
    }

    private boolean isChangedOnLocal(JSONObject localDocument) throws Exception {
        Revision revisionRecord = getRevisionRecord(localDocument);

        return  localDocument.has("_rev") && !(localDocument.getString("_rev").equals(revisionRecord.getLocalRevision()));
    }

    private boolean isRequiresSubmission(JSONObject localDocument) throws Exception {
        Revision revisionRecord = getRevisionRecord(localDocument);

        String lastCommitVersion = revisionRecord.getLastCommit();
        return lastCommitVersion == null || !lastCommitVersion.equals(localDocument.getString("_rev"));
    }

    private boolean sendDeleteRequestToRemote(JSONObject documentToDelete) throws Exception {
        // You cannot submit a document without a _rev.
        if (getRevisionRecord(documentToDelete).getRemoteRevision() == null) {
            return false;
        }

        String cookie = getNunaliitCookie();

        Response deleteResponse = submissionClient.newCall(createDocumentDeleteRequest(documentToDelete, cookie)).execute();
        String body = deleteResponse.body().string();

        documentToDelete.put("nunaliit_mobile_delete_submitted", true);
        localDocumentDb.updateDocument(documentToDelete);

        Log.v(TAG, body);
        return true;
    }

    private int deleteLocalDocumentsPreviouslyDeletedOnRemote(List<JSONObject> localDocuments) throws Exception {
        int deleted = 0;

        for(JSONObject doc : localDocuments) {
            String docId = doc.getString("_id");

            if (IsDocumentDeletedOnRemote(doc)) {
                Log.d(TAG, "Deleting document: " + docId);
                deleteDocumentOnMobile(doc);

                deleted++;
            }
        }

        Log.v(TAG, String.format("Documents Deleted: %d", deleted));

        return deleted;
    }

    private void sendNewDocumentsToRemote(List<JSONObject> newDocuments) {
        for(JSONObject doc : newDocuments) {
            try {
                if (isRequiresSubmission(doc)) {
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

    private List<JSONObject> getNewLocalDocuments(List<JSONObject> remoteDocuments) {
        try {
            List<JSONObject> localDocuments = localDocumentDb.getAllDocuments();

            HashMap<String, JSONObject> localDocumentsMap = new HashMap<>();

            for (JSONObject doc : localDocuments) {
                localDocumentsMap.put(doc.getString("_id"), doc);
            }

            for (JSONObject doc : remoteDocuments) {
                if (localDocumentsMap.containsKey(doc.getString("_id"))) {
                    localDocumentsMap.remove(doc.getString("_id"));
                }
            }

            List<JSONObject> newLocalDocuments = new ArrayList<>(localDocumentsMap.values());
            List<JSONObject> filteredUndeletedDocuments = new ArrayList<>();

            for(JSONObject doc: newLocalDocuments) {
                if (!hasFlagDeleted(doc)) {
                    filteredUndeletedDocuments.add(doc);
                }
            }

            return filteredUndeletedDocuments;

        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch local documents", e);
        }
    }

    private List<JSONObject> getDocumentsPendingDeletion() throws Exception {
        List<JSONObject> documentsPendingDeletion = new ArrayList<>();
        List<JSONObject> localDocuments = localDocumentDb.getAllDocuments();

        for (JSONObject localDocument: localDocuments) {
            if (hasFlagDeleted(localDocument)) {
                documentsPendingDeletion.add(localDocument);
            }
        }

        return documentsPendingDeletion;
    }

    private void updateLocalDocument(JSONObject doc) throws Exception {
        String docId = doc.getString("_id");
        String remoteRev = doc.optString("_rev", null);

        Revision revisionRecord = getRevisionRecord(doc);

        CouchbaseDocInfo info;
        if( localDocumentDb.documentExists(docId) ) {
            JSONObject existingDoc = localDocumentDb.getDocument(docId);
            String existingRev = existingDoc.optString("_rev",null);
            if( null != existingRev ){
                doc.put("_rev",existingRev);
            }

            // If there is an _attachments, the document will not be updated. //SARAH: what?
            saveAttachments(doc);

            info = localDocumentDb.updateDocument(doc);
        } else {
            // When creating a document, no revision should be set
            doc.remove("_rev");

            // If there is an _attachments, the document will not be created. //SARAH: what?
            saveAttachments(doc);

            info = localDocumentDb.createDocument(doc);
        }

        revisionRecord.setRemoteRevision(remoteRev);
        revisionRecord.setLocalRevision(info.getRev());
        revisionRecord.setLastCommit(info.getRev());
        trackingDb.updateRevision(revisionRecord);
    }

    private void updateRemoteDocument(JSONObject local) throws Exception {

        String docId = local.getString("_id");

        Revision revisionRecord = getRevisionRecord(docId);
        nunaliit.org.json.JSONObject couchDoc = JSONGlue.convertJSONObjectFromAndroidToUpstream(local);

        fetchAttachments(local);

        if (!couchDb.documentExists(couchDoc)) {
            local.remove("_rev");
        } else {
            local.putOpt("_rev", revisionRecord.getRemoteRevision());
        }
        writeDocumentToSubmissionDatabase(local);

        // The document has been committed. Save a reference to its last commit.
        JSONObject localDocument = localDocumentDb.getDocument(docId);
        revisionRecord.setLastCommit(localDocument.getString("_rev"));
        revisionRecord.setLocalRevision(localDocument.getString("_rev"));
        trackingDb.updateRevision(revisionRecord);
    }

    private void saveAttachments(JSONObject doc) throws Exception {
        if (doc.has("_attachments")) {
            JSONObject attachments = doc.getJSONObject("_attachments");
//            doc.remove("_attachments");
            doc.putOpt("nunaliit_authoritative_attachments", attachments);
        }
    }

    /**
     *   "_attachments": {
     *     "VID_20200312_100942_thumb.jpg": {
     *       "content_type": "image/jpeg",
     *       "revpos": 9,
     *       "digest": "md5-jcksLcpfCDC1ZaizFFryHw==",
     *       "length": 5935,
     *       "stub": true
     *     },
     *     ...
     *  }
     * @param remoteDoc
     * @throws Exception
     */
    private void syncAttachmentsToLocal(JSONObject remoteDoc) throws Exception {
        if (remoteDoc.has("_attachments")) {
            JSONObject attachments = remoteDoc.getJSONObject("_attachments");
            String docId = remoteDoc.getString("_id");
            Log.v(TAG, "SARAH: synching attachments for: " + docId);
            Iterator<String> keys = attachments.keys();
            String key;
            JSONObject att;
            ByteArrayOutputStream outputStream;
            while (keys.hasNext()) {
                key = keys.next();
                att = attachments.getJSONObject(key);

                outputStream = new ByteArrayOutputStream(att.getInt("length"));
                couchDb.downloadAttachment(docId, key, outputStream);

                InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                Document localDoc = localDocumentDb.getCouchbaseDocument(docId);
                UnsavedRevision newRev = localDoc.getCurrentRevision().createRevision();
                //SARAH: for now, using key name. JP suggested using digest instead (see #52)
                newRev.setAttachment(key, att.optString("content_type", "unknown"), inputStream);
                newRev.save();

                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
        }
    }

    private void fetchAttachments(JSONObject document) throws Exception {
        if (document.has("nunaliit_authoritative_attachments")) {
            JSONObject attachments = document.getJSONObject("nunaliit_authoritative_attachments");
            document.putOpt("_attachments", attachments);
            document.remove("nunaliit_authoritative_attachments");
        }
    }

    private void writeDocumentToSubmissionDatabase(JSONObject document) throws Exception {
        String cookie = getNunaliitCookie();

        document = removeNullAttachments(document);
        String uploadPath = getNunaliitAttachmentPath(document);
        String uploadId = addNunaliitAttachments(document);

        // Remove the nunaliit_mobile_attachments but keep it on the app. It will be removed
        // when the document is synced down from the server.
        document.remove("nunaliit_mobile_attachments");

        // Delete the deleted flags so it will sync with remote with no conflicts.
        document.remove("nunaliit_mobile_deleted");
        document.remove("nunaliit_mobile_delete_submitted");

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

        return new Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .header("Accept", "application/json")
                .build();
    }

    @NonNull
    private String getNunaliitCookie() throws Exception {
        Response loginResponse = submissionClient.newCall(createAuthRequest()).execute();
        String cookie = loginResponse.header("Set-Cookie");
        cookie += "; NunaliitAuth=" + connection.getConnectionInfo().getUser();
        return cookie;
    }

    private Request createDocumentUploadRequest(JSONObject document, String authCookie) throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        String docString = document.toString();

        // Put the item in the submission database.
        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/submission/submissionDb").addPathSegment(document.getString("_id")).addQueryParameter("deviceId", info.getDeviceId()).build();
        RequestBody body = RequestBody.create(JSON, docString);

        return new Request.Builder()
                .url(url)
                .put(body)
                .header("Cookie", authCookie)
                .build();
    }

    private Request createDocumentDeleteRequest(JSONObject document, String authCookie) throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        // Put the item in the submission database.
        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/submission/submissionDb")
                .addPathSegment(document.getString("_id"))
                .addQueryParameter("deviceId", info.getDeviceId())
                .addQueryParameter("rev", getRevisionRecord(document).getRemoteRevision())
                .build();

        return new Request.Builder()
                .url(url)
                .delete()
                .header("Cookie", authCookie)
                .build();
    }

    private Request createAttachmentRequest(String uploadId, String filepath, String authCookie) throws Exception {
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

        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cookie", authCookie)
                .build();
    }

    private SubmissionStatus getSubmissionStatusForDocument(JSONObject document) throws Exception {
        String docId = document.getString("_id");

        if (submissionStatusByDocId.containsKey(docId)) {
            return submissionStatusByDocId.get(docId);
        } else {
            return SubmissionStatus.NotSubmitted;
        }
    }

    private boolean IsDocumentDeletedOnRemote(JSONObject doc) throws Exception {
        SubmissionStatus submissionStatus = getSubmissionStatusForDocument(doc);

        return submissionStatus != SubmissionStatus.WaitingForApproval && !isRequiresSubmission(doc);
    }

    private JSONObject getSubmissionStatus() throws Exception {
        Response response = submissionClient.newCall(createSubmissionStatusRequest()).execute();
        String body = response.body().string();
        Log.v(TAG, body);

        if (!response.isSuccessful()) {
            throw new RuntimeException("Creating new database document failed: " + body);
        }

        return new JSONObject(body);
    }

    private Request createSubmissionStatusRequest() throws Exception {
        ConnectionInfo info = connection.getConnectionInfo();

        // Put the item in the submission database.
        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/submission/_submission-info-by-device-id").addQueryParameter("key", info.getDeviceId()).build();

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }

    private Map<String, SubmissionStatus> createSubmissionStatusMap(JSONObject submissionStatus) throws Exception {
        Map<String, SubmissionStatus> submissionStatusMap = new HashMap<>();

        JSONArray rows = submissionStatus.getJSONArray("rows");
        for (int i=0;i<rows.length();i++) {
            JSONObject record = rows.getJSONObject(i);
            String docId = record.getJSONObject("value").getString("docId");
            String state = record.getJSONObject("value").optString("state", "");

            SubmissionStatus status = SubmissionStatus.Unknown;

            switch (state) {
                case "waiting_for_approval":
                    status = SubmissionStatus.WaitingForApproval;
                    break;
                case "approved":
                case "complete":
                    status = SubmissionStatus.Completed;
                    break;
                case "denied":
                    status = SubmissionStatus.Declined;
                    break;
            }

            // If any of the documents are WaitingForApproval, return that.
            if (submissionStatusMap.containsKey(docId)) {
                if (submissionStatusMap.get(docId) != SubmissionStatus.WaitingForApproval) {
                    submissionStatusMap.put(docId, status);
                }
            } else {
                submissionStatusMap.put(docId, status);
            }
        }

        return submissionStatusMap;
    }

    private MediaType getMediaType(String filepath) {
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

    private JSONObject removeNullAttachments(JSONObject document) {
        if (document.optString("nunaliit_attachments", "null").equals("null")) {
            document.remove("nunaliit_attachments");
        }

        if (document.optString("nunaliit_mobile_attachments", "null").equals("null")) {
            document.remove("nunaliit_mobile_attachments");
        }

        return document;
    }

    private String getNunaliitAttachmentPath(JSONObject document) {
        return document.optString("nunaliit_mobile_attachments", null);
    }

    private String addNunaliitAttachments(JSONObject document) throws Exception {

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