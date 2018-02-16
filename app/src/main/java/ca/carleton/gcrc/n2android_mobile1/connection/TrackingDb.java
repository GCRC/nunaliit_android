package ca.carleton.gcrc.n2android_mobile1.connection;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;

import nunaliit.org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.couchbase.Couchbase;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseView;

/**
 * Created by jpfiset on 3/25/16.
 */
public class TrackingDb extends CouchbaseDb {

    public static final CouchbaseView viewRevisionById = new CouchbaseView(){
        @Override
        public String getName() { return "revisions-by-id"; }

        @Override
        public String getVersion() { return "1"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object revisionObj = document.get("mobile_revision");
                if (null != revisionObj && revisionObj instanceof Map) {
                    Map<String,Object> revision = (Map<String,Object>)revisionObj;
                    Object idObj = revision.get("docId");
                    if (null != idObj && idObj instanceof String) {
                        String id = (String) idObj;
                        emitter.emit(id, null);
                    }
                }
            }
        };
    };

    static public Revision revisionFromDocument(Document doc){
        Revision revision = new Revision();

        // id
        {
            String id = doc.getId();
            if( null != id ){
                revision.setId(id);
            }
        }

        Map<String,Object> props = doc.getProperties();
        if( null != props ){
            Map<String,Object> rev = Couchbase.optMap(props, "mobile_revision");
            if( null != rev ){
                // docId
                {
                    String docId = Couchbase.optString(rev,"docId");
                    if( null != docId ){
                        revision.setDocId(docId);
                    }
                }

                // remoteRevision
                {
                    String remoteRevision = Couchbase.optString(rev,"remoteRevision");
                    if( null != remoteRevision ){
                        revision.setRemoteRevision(remoteRevision);
                    }
                }

                // localRevision
                {
                    String localRevision = Couchbase.optString(rev,"localRevision");
                    if( null != localRevision ){
                        revision.setLocalRevision(localRevision);
                    }
                }
            }
        }

        return revision;
    }

    static public void updateDocumentWithRevision(Document doc, Revision revision) throws Exception {
        Map<String,Object> props = doc.getProperties();
        if( null == props ){
            props = new HashMap<String,Object>();
        }

        Map<String,Object> rev = Couchbase.optMap(props, "mobile_revision");
        if( null == rev ){
            rev = new HashMap<String,Object>();
            props.put("mobile_revision",rev);
        }

        // docId
        {
            String docId = revision.getDocId();
            rev.put("docId",docId);
        }

        // remoteRevision
        {
            String remoteRevision = revision.getRemoteRevision();
            rev.put("remoteRevision",remoteRevision);
        }

        // localRevision
        {
            String localRevision = revision.getLocalRevision();
            rev.put("localRevision",localRevision);
        }

        // Update happens here
        doc.putProperties(props);
    }

    public TrackingDb(Database database) throws Exception {
        super(database);

        installView(viewRevisionById);
    }

    public Revision getRevisionFromDocId(String docId) throws Exception {
        try {
            Query dbQuery = getCouchbaseDatabase()
                .getView(viewRevisionById.getName())
                .createQuery();
            dbQuery.setStartKey(docId);
            dbQuery.setEndKey(docId);
            dbQuery.setMapOnly(true);

            Revision revision = null;

            QueryEnumerator resultEnum = dbQuery.run();
            for (Iterator<QueryRow> it = resultEnum; it.hasNext(); ) {
                QueryRow dbRow = it.next();

                Document doc = dbRow.getDocument();

                revision = revisionFromDocument(doc);
            }

            return revision;

        } catch(Exception e) {
            throw new Exception("Error while querying for revision from docId",e);
        }
    }

    public List<Revision> getRevisions() throws Exception {
        try {
            Query dbQuery = getCouchbaseDatabase()
                    .getView(viewRevisionById.getName())
                    .createQuery();
            dbQuery.setMapOnly(true);

            List<Revision> revisions = new Vector<Revision>();

            QueryEnumerator resultEnum = dbQuery.run();
            for (Iterator<QueryRow> it = resultEnum; it.hasNext(); ) {
                QueryRow dbRow = it.next();

                Document doc = dbRow.getDocument();

                Revision revision = revisionFromDocument(doc);

                revisions.add(revision);
            }

            return revisions;

        } catch(Exception e) {
            throw new Exception("Error while querying for revisions",e);
        }
    }

    public Revision updateRevision(Revision revision) throws Exception {
        try {
            Database database = getCouchbaseDatabase();

            Document doc = null;
            String id = revision.getId();
            if( null == id ){
                doc = database.createDocument();
            } else {
                doc = database.getDocument(id);
            }

            updateDocumentWithRevision(doc, revision);

            Revision result = revisionFromDocument(doc);

            return result;

        } catch(Exception e) {
            String docId = null;
            if( null != revision ){
                docId = revision.getDocId();
            }
            throw new Exception("Error while updating revision for docId: "+docId,e);
        }
    }
}
