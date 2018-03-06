package ca.carleton.gcrc.n2android_mobile1.couchbase;

import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.QueryRow;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;

/**
 * Created by jpfiset on 3/24/16.
 */
public class CouchbaseDb {

    static public JSONObject jsonObjectFromDocument(Document doc) throws Exception {
        JSONObject obj = null;
        Map<String,Object> props = doc.getProperties();
        if( null != props ){
            obj = jsonObjectFromProperties(props);
        } else {
            obj = new JSONObject();
        }

        String id = doc.getId();
        if( null != id ){
            obj.put("_id",id);
        }

        String rev = doc.getCurrentRevisionId();
        if( null != rev ){
            obj.put("_rev",rev);
        }

        return obj;
    };

    static public JSONObject jsonObjectFromProperties(Map<String,Object> props) throws Exception {
        JSONObject obj = new JSONObject();

        for(String key : props.keySet()){
            Object couchbaseValue = props.get(key);
            Object jsonValue = jsonValueFromCouchbase(couchbaseValue);
            obj.put(key,jsonValue);
        }

        return obj;
    };

    static public JSONArray jsonArrayFromProperties(List<Object> props) throws Exception {
        JSONArray array = new JSONArray();

        for(Object couchbaseValue : props) {
            Object jsonValue = jsonValueFromCouchbase(couchbaseValue);
            array.put(jsonValue);
        }

        return array;
    };

    static public Object jsonValueFromCouchbase(Object couchbaseValue) throws Exception {
        if( couchbaseValue == null ) {
            return JSONObject.NULL;

        } else if( couchbaseValue instanceof String ){
            String str = (String)couchbaseValue;
            return str;

        } else if( couchbaseValue instanceof Number ){
            Number num = (Number)couchbaseValue;
            return num;

        } else if( couchbaseValue instanceof Boolean ){
            Boolean num = (Boolean)couchbaseValue;
            return num;

        } else if( couchbaseValue instanceof Map ){
            Map<String,Object> p = (Map<String,Object>)couchbaseValue;
            JSONObject jsonValue = jsonObjectFromProperties(p);
            return jsonValue;

        } else if( couchbaseValue instanceof List ){
            List<Object> p = (List<Object>)couchbaseValue;
            JSONArray jsonArray = jsonArrayFromProperties(p);
            return jsonArray;

        } else {
            String name = "" + couchbaseValue;
            if( null != couchbaseValue ){
                name = couchbaseValue.getClass().getName();
            }
            throw new Exception("jsonValueFromCouchbase() can not handle "+name);
        }
    };

    static public Map<String,Object> propertiesFromJsonObject(JSONObject jsonObject) throws Exception {
        Map<String,Object> props = new HashMap<String,Object>();

        Iterator<String> keysIt = jsonObject.keys();
        while( keysIt.hasNext() ) {
            String key = keysIt.next();
            Object jsonValue = jsonObject.get(key);
            Object couchbaseValue = couchbaseValueFromJson(jsonValue);
            props.put(key, couchbaseValue);
        }

        return props;
    };

    static public List<Object> propertiesFromJsonArray(JSONArray jsonArray) throws Exception {
        List<Object> props = new Vector<Object>();

        for(int i=0,e=jsonArray.length(); i<e; ++i) {
            Object jsonValue = jsonArray.get(i);
            Object couchbaseValue = couchbaseValueFromJson(jsonValue);
            props.add(couchbaseValue);
        }

        return props;
    };

    static public Object couchbaseValueFromJson(Object jsonValue) throws Exception {
        if( jsonValue == JSONObject.NULL ) {
            return null;

        } else if( jsonValue instanceof String ){
            String str = (String)jsonValue;
            return str;

        } else if( jsonValue instanceof Number ){
            Number num = (Number)jsonValue;
            return num;

        } else if( jsonValue instanceof Boolean ){
            Boolean flag = (Boolean)jsonValue;
            return flag;

        } else if( jsonValue instanceof JSONObject ){
            JSONObject json = (JSONObject)jsonValue;
            Map<String,Object> p = propertiesFromJsonObject(json);
            return p;

        } else if( jsonValue instanceof JSONArray ){
            JSONArray json = (JSONArray)jsonValue;
            List<Object> p = propertiesFromJsonArray(json);
            return p;

        } else {
            String name = "" + jsonValue;
            if( null != jsonValue ){
                name = jsonValue.getClass().getName();
            }
            throw new Exception("couchbaseValueFromJson() can not handle "+name);
        }
    };

    protected final String TAG = this.getClass().getSimpleName();

    private Database database;

    public CouchbaseDb(Database database){
        this.database = database;
    }

    protected Database getCouchbaseDatabase() {
        return database;
    }

    public JSONObject getInfo() throws Exception {
        try {
            JSONObject jsonInfo = new JSONObject();

            {
                String name = database.getName();
                jsonInfo.put("db_name", name);
            }

            {
                int count = database.getDocumentCount();
                jsonInfo.put("doc_count", count);
            }

            {
                long seqNum = database.getLastSequenceNumber();
                jsonInfo.put("committed_update_seq", seqNum);
            }

            return jsonInfo;

        } catch(Exception e) {
            throw new Exception("Error while creating database information",e);
        }
    }

    public JSONObject getDocument(String docId) throws Exception {
        try {
            Document doc = database.getDocument(docId);
            JSONObject jsonDoc = jsonObjectFromDocument(doc);
            return jsonDoc;

        } catch(Exception e) {
            throw new Exception("Unable to load document with identifier: "+docId,e);
        }
    }

    public List<JSONObject> getDocuments(List<String> docIds) throws Exception {
        try {
            List<JSONObject> docs = new Vector<JSONObject>();
            for(String docId : docIds){
                Document doc = database.getExistingDocument(docId);
                if( null != doc ){
                    JSONObject jsonDoc = jsonObjectFromDocument(doc);
                    if( null != jsonDoc ){
                        docs.add(jsonDoc);
                    }
                }
            }
            return docs;

        } catch(Exception e) {
            throw new Exception("Unable to load documents with identifiers",e);
        }
    }

    public List<JSONObject> getAllDocuments() throws Exception {
        try {
            List<JSONObject> docs = new Vector<JSONObject>();
            Query query = database.createAllDocumentsQuery();
            query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                Document doc = row.getDocument();
                if( null != doc ){
                    JSONObject jsonDoc = jsonObjectFromDocument(doc);
                    if( null != jsonDoc ){
                        docs.add(jsonDoc);
                    }
                }
            }
            return docs;

        } catch(Exception e) {
            throw new Exception("Unable to load documents with identifiers",e);
        }
    }

    public List<String> getAllDocumentIds() throws Exception {
        try {
            List<String> docIds = new Vector<String>();

            Query query = database.createAllDocumentsQuery();
            query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                String docId = row.getSourceDocumentId();
                docIds.add(docId);
            }

            return docIds;

        } catch(Exception e) {
            throw new Exception("Unable to fecth all document ids",e);
        }
    }

    public boolean documentExists(String docId) {
        Document doc = database.getExistingDocument(docId);
        boolean exists = false;
        if( null != doc ){
            exists = true;
        }
        return exists;
    }

    public String getDocumentRevision(String docId) throws Exception {
        try {
            Document doc = database.getDocument(docId);
            return doc.getCurrentRevisionId();

        } catch(Exception e) {
            throw new Exception("Unable to load document with identifier: "+docId,e);
        }
    }

    public CouchbaseDocInfo createDocument(JSONObject jsonDoc) throws Exception {
        String id = null;
        try {
            Document doc = null;
            id = jsonDoc.optString("_id", null);
            if( null != id ){
                doc = database.getDocument(id);
            } else {
                doc = database.createDocument();
            }

            Map<String,Object> props = propertiesFromJsonObject(jsonDoc);

            // Creation happens here
            doc.putProperties(props);

            CouchbaseDocInfo info = new CouchbaseDocInfo();
            info.setId(doc.getId());
            info.setRev(doc.getCurrentRevisionId());
            return info;

        } catch(Exception e) {
            String label = "";
            if( null != id ){
                label = " "+id;
            }
            throw new Exception("Unable to create document"+label,e);
        }
    }

    public CouchbaseDocInfo updateDocument(JSONObject jsonDoc) throws Exception {
        String id = null;
        try {
            id = jsonDoc.getString("_id");
            Document doc = database.getDocument(id);

            Map<String,Object> props = propertiesFromJsonObject(jsonDoc);

            // Update happens here
            doc.putProperties(props);

            CouchbaseDocInfo info = new CouchbaseDocInfo();
            info.setId(doc.getId());
            info.setRev(doc.getCurrentRevisionId());
            return info;

        } catch(Exception e) {
            throw new Exception("Unable to update document "+id,e);
        }
    }

    public CouchbaseDocInfo deleteDocument(JSONObject jsonDoc) throws Exception {
        String id = null;
        try {
            id = jsonDoc.getString("_id");
            String rev = jsonDoc.getString("_rev");

            Document doc = database.getDocument(id);
            String currentRev = doc.getCurrentRevisionId();

            if( currentRev != null && currentRev.equals(rev) ){
                // OK
            } else {
                throw new Exception("Revision must match current on deletion");
            }

            doc.delete();

            CouchbaseDocInfo info = new CouchbaseDocInfo();
            info.setId(id);
            info.setRev(currentRev);
            return info;

        } catch(Exception e) {
            throw new Exception("Unable to delete document"+id,e);
        }
    }

    public void deleteDocument(String docId) throws Exception {
        try {
            Document doc = database.getDocument(docId);
            doc.delete();

        } catch(Exception e) {
            throw new Exception("Unable to delete document"+docId,e);
        }
    }

    public CouchbaseQueryResults performQuery(CouchbaseQuery query) throws Exception {
        String viewName = null;
        try {
            viewName = query.getViewName();
            Query dbQuery = database.getView(viewName).createQuery();

            // Start key
            {
                Object startKey = query.getStartKey();
                if( null != startKey ){
                    Object couchbaseKey = couchbaseValueFromJson(startKey);
                    dbQuery.setStartKey(couchbaseKey);
                }
            }

            // End key
            {
                Object endKey = query.getEndKey();
                if( null != endKey ){
                    Object couchbaseKey = couchbaseValueFromJson(endKey);
                    dbQuery.setEndKey(couchbaseKey);
                }
            }

            // Keys
            {
                JSONArray keys = query.getKeys();
                if( null != keys ){
                    List<Object> couchbaseKeys = new Vector<Object>();
                    for(int i=0,e=keys.length(); i<e; ++i){
                        Object jsonKey = keys.get(i);
                        Object couchbaseKey = couchbaseValueFromJson(jsonKey);
                        couchbaseKeys.add(couchbaseKey);
                    }
                    dbQuery.setKeys(couchbaseKeys);
                }
            }

            // Limit
            {
                Integer limit = query.getLimit();
                if( null != limit ){
                    dbQuery.setLimit(limit);
                }
            }

            // Reduce
            {
                if( query.isReduce() ){
                    dbQuery.setMapOnly(false);
                } else {
                    dbQuery.setMapOnly(true);
                }
            }

            CouchbaseQueryResults results = new CouchbaseQueryResults();
            QueryEnumerator resultEnum = dbQuery.run();
            for (Iterator<QueryRow> it = resultEnum; it.hasNext(); ) {
                QueryRow dbRow = it.next();

                JSONObject row = new JSONObject();

                Document doc = dbRow.getDocument();

                row.put("id",doc.getId());
                row.put("rev", doc.getCurrentRevisionId());

                // Key
                {
                    Object keyObj = dbRow.getKey();
                    Object jsonKey = jsonValueFromCouchbase(keyObj);
                    row.put("key",jsonKey);
                }

                // Value
                {
                    Object valueObj = dbRow.getValue();
                    if( null != valueObj ){
                        Object jsonValue = jsonValueFromCouchbase(valueObj);
                        row.put("value",jsonValue);
                    }
                }

                if( query.getIncludeDocs() ){
                    JSONObject jsonDoc = jsonObjectFromDocument(doc);
                    row.put("doc",jsonDoc);
                }

                results.addRow(row);
            }

            return results;

        } catch(Exception e) {
            throw new Exception("Error during view query "+viewName, e);
        }
    }

    public void installView(CouchbaseView view) throws Exception {
        try {
            String viewName = view.getName();

            com.couchbase.lite.View internalView = database.getView(viewName);

            boolean mustInstall = false;

            if( null == internalView ){
                mustInstall = true;
            } else if( null == internalView.getMapVersion() ){
                mustInstall = true;
            }

            if( mustInstall ){
                internalView.setMap(view.getMapper(), view.getVersion());

                Log.v(TAG, "Installed view: " + view.getName());
            }

        } catch(Exception e) {
            String label = "";
            if( null != view ){
                label = " "+view.getName();
            }
            throw new Exception("Error while installing a view"+label,e);
        }
    }

}
