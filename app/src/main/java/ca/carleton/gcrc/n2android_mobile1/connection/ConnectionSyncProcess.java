package ca.carleton.gcrc.n2android_mobile1.connection;

import android.util.Log;

import org.json.JSONObject;

import java.util.List;
import java.util.Vector;

import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchDesignDocument;
import ca.carleton.gcrc.couch.client.CouchQuery;
import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;

/**
 * Created by jpfiset on 3/21/16.
 */
public class ConnectionSyncProcess {

    protected final String TAG = this.getClass().getSimpleName();

    private Connection connection;
    private CouchbaseLiteService service;
    private CouchDb couchDb;
    private CouchDesignDocument atlasDesign;

    public ConnectionSyncProcess(CouchbaseLiteService service, Connection connection) throws Exception {
        this.service = service;
        this.connection = connection;

        couchDb = connection.getRemoteCouchDb();
        atlasDesign = couchDb.getDesignDocument("atlas");
    }

    public void synchronize() throws Exception {
        try {
            List<JSONObject> docs = getRemoteSkeletonDocs();
            for(JSONObject doc : docs){
                updateLocalSkeletonDocument(doc);
            }

            Log.v(TAG, "Number of skeleton docs: "+ docs.size());

        } catch(Exception e) {
            throw new Exception("Error while synchronizing connection",e);
        }
    }

    public List<String> getRemoteSkeletonDocIds() throws Exception {
        CouchQuery query = new CouchQuery();
        query.setViewName("skeleton-docs");
        query.setReduce(false);
        query.setIncludeDocs(false);

        CouchQueryResults results = atlasDesign.performQuery(query);

        List<String> docIds = new Vector<String>();
        List<JSONObject> rows = results.getRows();
        for(JSONObject row : rows){
            String docId = row.getString("id");
            docIds.add(docId);
        }
        return docIds;
    }

    public List<JSONObject> getRemoteSkeletonDocs() throws Exception {
        CouchQuery query = new CouchQuery();
        query.setViewName("skeleton-docs");
        query.setReduce(false);
        query.setIncludeDocs(true);

        CouchQueryResults results = atlasDesign.performQuery(query);

        List<JSONObject> docs = new Vector<JSONObject>();
        List<JSONObject> rows = results.getRows();
        for(JSONObject row : rows){
            JSONObject doc = row.getJSONObject("doc");
            docs.add(doc);
        }
        return docs;
    }

    public void updateLocalSkeletonDocument(JSONObject doc) throws Exception {
        try {
            String remoteRev = doc.optString("_rev",null);
            if( null != remoteRev ){
                doc.remove("_rev");
            }

            CouchbaseDb localDb = connection.getLocalDocumentDb();
            String docId = doc.getString("_id");
            if( localDb.documentExists(docId) ) {
                JSONObject existingDoc = localDb.getDocument(docId);
                String existingRev = existingDoc.optString("_rev",null);
                if( null != existingRev ){
                    doc.put("_rev",existingRev);
                }
                localDb.updateDocument(doc);
            } else {
                localDb.createDocument(doc);
            }

        } catch(Exception e) {
            throw new Exception("Unable to update local skeleton documents",e);
        }
    }
}
