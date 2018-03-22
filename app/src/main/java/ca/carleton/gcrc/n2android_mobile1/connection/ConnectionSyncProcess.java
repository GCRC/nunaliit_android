package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

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
            updateLocalDocuments(remoteDocs);

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

    public void updateLocalDocuments(List<JSONObject> documents) throws Exception {
        try {
            int updatedCount = 0;
            for (JSONObject doc : documents) {
                boolean updated = updateLocalDocument(doc);
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

    public List<String> getRemoteSkeletonDocIds() throws Exception {
        CouchQuery query = new CouchQuery();
        query.setViewName("skeleton-docs");
        query.setReduce(false);
        query.setIncludeDocs(false);

        CouchQueryResults results = atlasDesign.performQuery(query);

        List<String> docIds = new Vector<String>();
        List<JSONObject> rows = JSONGlue.convertJSONObjectCollectionFromUpstreamToAndroid(results.getRows());
        for(JSONObject row : rows){
            String docId = row.getString("id");
            docIds.add(docId);
        }
        return docIds;
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

    public Collection<JSONObject> getRemoteDocuments(List<String> docIds) throws Exception {
        try {
            Collection<JSONObject> docs = JSONGlue.convertJSONObjectCollectionFromUpstreamToAndroid(couchDb.getDocuments(docIds));
            return docs;
        } catch(Exception e) {
            throw new Exception("Error while downloading remote documents",e);
        }
    }

    public boolean updateLocalDocument(JSONObject doc) throws Exception {
        try {
            boolean updated = false;

            String docId = doc.getString("_id");
            String remoteRev = doc.optString("_rev", null);

            Revision revisionRecord = trackingDb.getRevisionFromDocId(docId);
            if( null == revisionRecord ){
                revisionRecord = new Revision();
            }
            revisionRecord.setDocId(docId);

            if(!remoteRev.equals(revisionRecord.getRemoteRevision())){
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

                    info = documentDb.createDocument(doc);
                }

                revisionRecord.setRemoteRevision(remoteRev);
                revisionRecord.setLocalRevision(info.getRev());
                trackingDb.updateRevision(revisionRecord);

                updated = true;
            }

            return updated;

        } catch(Exception e) {
            throw new Exception("Unable to update local skeleton documents",e);
        }
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
        nunaliit.org.json.JSONObject couchDoc = JSONGlue.convertJSONObjectFromAndroidToUpstream(document);
        if (couchDb.documentExists(couchDoc)) {
            couchDb.updateDocument(couchDoc);
        } else {
            writeDocumentToSubmissionDatabase(document);
        }
    }

    public void writeDocumentToSubmissionDatabase(JSONObject document) throws Exception {
        document.remove("_rev");

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
