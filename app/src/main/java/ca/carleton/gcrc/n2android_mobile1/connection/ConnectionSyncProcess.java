package ca.carleton.gcrc.n2android_mobile1.connection;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchDesignDocument;
import ca.carleton.gcrc.couch.client.CouchQuery;
import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.JSONGlue;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
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

    public ConnectionSyncProcess(CouchbaseLiteService service, Connection connection) throws Exception {
        this.service = service;
        this.connection = connection;

        couchDb = connection.getRemoteCouchDb();
        atlasDesign = couchDb.getDesignDocument("atlas");

        documentDb = connection.getLocalDocumentDb();
        trackingDb = connection.getLocalTrackingDb();
    }

    public void synchronize() throws Exception {
        try {
            Log.v(TAG, "Synchronization started");

            List<JSONObject> remoteDocs = fetchAllDocuments();
            updateLocalDocumentsFromRemote(remoteDocs);

            updateRemoteDocuments(remoteDocs);

        } catch(Exception e) {
            throw new Exception("Error while synchronizing connection",e);
        }
    }

    public List<JSONObject> fetchAllDocuments() throws Exception {
        try {
            List<JSONObject> remoteDocs = getDocsFromView("skeleton-docs");

            Log.v(TAG, "Synchronization received " + remoteDocs.size() + " skeleton document(s)");

            // TODO: Let the user select which documents to sync.
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

        } catch (Exception e) {
            throw new Exception("Error while fetching all documents",e);
        }
    }

    public List<JSONObject> fetchDocumentsForSchemas(Collection<String> schemaList) throws Exception {
        try {
            Log.v(TAG, "Fetching Subdocuments for schemas started");

            List<JSONObject> subdocuments = getDocsFromView("nunaliit-schema", schemaList);

            Log.v(TAG, "Subdocument Synchronization received "+subdocuments.size()+" subdocument(s)");

            return subdocuments;

        } catch(Exception e) {
            throw new Exception("Error while fetching documents from schema",e);
        }
    }

    public void updateLocalDocumentsFromRemote(List<JSONObject> documents) throws Exception {
        try {
            int updatedCount = 0;
            for (JSONObject doc : documents) {
                boolean updated = updateDocumentDatabaseIfNeed(doc);
                if (updated) {
                    ++updatedCount;
                }
            }

            Log.i(TAG, "Synchronization updated " + updatedCount + " documents");

            Log.v(TAG, "Synchronization complete");
        } catch (Exception e) {
            throw new Exception("Error while updating documents",e);
        }
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
        String lastCommitVersion = revisionRecord.getLastCommit();
        boolean isNewCommit = lastCommitVersion == null || !lastCommitVersion.equals(localDocument.getString("_rev"));

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
            updateServerDocument(localDocument);
            return true;
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
            info = documentDb.updateDocument(doc);
        } else {
            // When creating a document, no revision should be set
            doc.remove("_rev");

            // TODO: Find a way to keep the attachments on the document.
            doc.remove("_attachments");

            info = documentDb.createDocument(doc);
        }

        revisionRecord.setRemoteRevision(remoteRev);
        revisionRecord.setLocalRevision(info.getRev());
        trackingDb.updateRevision(revisionRecord);
    }

    public void updateRemoteDocuments(List<JSONObject> remoteDocuments) throws Exception {
        try {
            List<JSONObject> newLocalDocuments = getNewLocalDocuments(remoteDocuments);

            for(JSONObject doc : newLocalDocuments) {
                updateServerDocument(doc);
            }

        } catch (Exception e) {
            throw new Exception("Unable to update remote documents",e);
        }
    }

    public List<JSONObject> getNewLocalDocuments(List<JSONObject> remoteDocuments) throws Exception {
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
            throw new Exception ("Unable to update server documents", e);
        }
    }

    public void updateServerDocument(JSONObject document) throws Exception {
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

        String docString = document.toString();

        // Auth
        // TODO: Move this to a service.
        OkHttpClient client = new OkHttpClient();
        HttpUrl loginUrl = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/server/_session").build();
        FormBody formBody = new FormBody.Builder().add("name", info.getUser()).add("password", info.getPassword()).build();
        Request loginRequest = new Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .header("Accept", "application/json")
                .build();

        Response loginResponse = client.newCall(loginRequest).execute();
        String cookie = loginResponse.header("Set-Cookie");
        cookie += "; NunaliitAuth=" + info.getUser();

        // Put the item in the submission database.
        HttpUrl url = HttpUrl.parse(info.getUrl()).newBuilder().encodedPath("/servlet/submission/submissionDb").addPathSegment(document.getString("_id")).build();
        RequestBody body = RequestBody.create(JSON, docString);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .header("Cookie", cookie)
                .build();

        Response response = client.newCall(request).execute();

        Log.v(TAG, response.body().string());

        if (!response.isSuccessful()) {
            throw new Exception("Creating new database document failed: " + response.body());
        }
    }
}
