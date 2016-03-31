package ca.carleton.gcrc.n2android_mobile1.connection;

import android.util.Log;

import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchDesignDocument;
import ca.carleton.gcrc.couch.client.CouchQuery;
import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
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

            List<JSONObject> remoteDocs = getRemoteSkeletonDocs();

            Log.v(TAG, "Synchronization received "+remoteDocs.size()+" skeleton document(s)");

            int updatedCount = 0;
            for(JSONObject doc : remoteDocs){
                boolean updated = updateLocalSkeletonDocument(doc);
                if( updated ){
                    ++updatedCount;
                }
            }
            Log.i(TAG, "Synchronization updated " + updatedCount + " documents");

            Log.v(TAG, "Synchronization complete");

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

    public Collection<JSONObject> getRemoteDocuments(List<String> docIds) throws Exception {
        try {
            Collection<JSONObject> docs = couchDb.getDocuments(docIds);
            return docs;
        } catch(Exception e) {
            throw new Exception("Error while downloading remote documents",e);
        }
    }

    public boolean updateLocalSkeletonDocument(JSONObject doc) throws Exception {
        try {
            boolean updated = false;

            String docId = doc.getString("_id");
            String remoteRev = doc.optString("_rev", null);

            Revision revisionRecord = trackingDb.getRevisionFromDocId(docId);
            if( null == revisionRecord ){
                revisionRecord = new Revision();
            }
            revisionRecord.setDocId(docId);

            if( false == remoteRev.equals(revisionRecord.getRemoteRevision()) ){
                CouchbaseDocInfo info = null;
                if( documentDb.documentExists(docId) ) {
                    JSONObject existingDoc = documentDb.getDocument(docId);
                    String existingRev = existingDoc.optString("_rev",null);
                    if( null != existingRev ){
                        doc.put("_rev",existingRev);
                    }
                    info = documentDb.updateDocument(doc);
                } else {
                    // When creating a document, no revision should be set
                    if( null != remoteRev ){
                        doc.remove("_rev");
                    }

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
}
