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
            Object value = props.get(key);

            if( value == null ) {
                obj.put(key, JSONObject.NULL);

            } else if( value instanceof String ){
                String str = (String)value;
                obj.put(key,str);

            } else if( value instanceof Number ){
                Number num = (Number)value;
                obj.put(key,num);

            } else if( value instanceof Boolean ){
                Boolean num = (Boolean)value;
                obj.put(key,num);

            } else if( value instanceof Map ){
                Map<String,Object> p = (Map<String,Object>)value;
                JSONObject jsonValue = jsonObjectFromProperties(p);
                obj.put(key,jsonValue);

            } else if( value instanceof List ){
                List<Object> p = (List<Object>)value;
                JSONArray jsonArray = jsonArrayFromProperties(p);
                obj.put(key,jsonArray);

            } else {
                String name = "" + value;
                if( null != value ){
                    name = value.getClass().getName();
                }
                throw new Exception("jsonObjectFromProperties() can not handle "+name);
            }
        }

        return obj;
    };

    static public JSONArray jsonArrayFromProperties(List<Object> props) throws Exception {
        JSONArray array = new JSONArray();

        for(Object value : props) {

            if( value == null ) {
                array.put(JSONObject.NULL);

            } else if( value instanceof String ){
                String str = (String)value;
                array.put(str);

            } else if( value instanceof Number ){
                Number num = (Number)value;
                array.put(num);

            } else if( value instanceof Boolean ){
                Boolean num = (Boolean)value;
                array.put(num);

            } else if( value instanceof Map ){
                Map<String,Object> p = (Map<String,Object>)value;
                JSONObject jsonValue = jsonObjectFromProperties(p);
                array.put(jsonValue);

            } else if( value instanceof List ){
                List<Object> p = (List<Object>)value;
                JSONArray jsonArray = jsonArrayFromProperties(p);
                array.put(jsonArray);

            } else {
                String name = "" + value;
                if( null != value ){
                    name = value.getClass().getName();
                }
                throw new Exception("jsonArrayFromProperties() can not handle "+name);
            }
        }

        return array;
    };

    static public Map<String,Object> propertiesFromJsonObject(JSONObject jsonObject) throws Exception {
        Map<String,Object> props = new HashMap<String,Object>();

        Iterator<String> keysIt = jsonObject.keys();
        while( keysIt.hasNext() ) {
            String key = keysIt.next();
            Object obj = jsonObject.get(key);

            if( obj == JSONObject.NULL ) {
                props.put(key, null);

            } else if( obj instanceof String ){
                String str = (String)obj;
                props.put(key, str);

            } else if( obj instanceof Number ){
                Number num = (Number)obj;
                props.put(key, num);

            } else if( obj instanceof Boolean ){
                Boolean flag = (Boolean)obj;
                props.put(key, flag);

            } else if( obj instanceof JSONObject ){
                JSONObject json = (JSONObject)obj;
                Map<String,Object> p = propertiesFromJsonObject(json);
                props.put(key, p);

            } else if( obj instanceof JSONArray ){
                JSONArray json = (JSONArray)obj;
                List<Object> p = propertiesFromJsonArray(json);
                props.put(key, p);

            } else {
                String name = "" + obj;
                if( null != obj ){
                    name = obj.getClass().getName();
                }
                throw new Exception("propertiesFromJsonObject() can not handle "+name);
            }
        }

        return props;
    };

    static public List<Object> propertiesFromJsonArray(JSONArray jsonArray) throws Exception {
        List<Object> props = new Vector<Object>();

        for(int i=0,e=jsonArray.length(); i<e; ++i) {
            Object obj = jsonArray.get(i);

            if( obj == JSONObject.NULL ) {
                props.add(null);

            } else if( obj instanceof String ){
                String str = (String)obj;
                props.add(str);

            } else if( obj instanceof Number ){
                Number num = (Number)obj;
                props.add(num);

            } else if( obj instanceof Boolean ){
                Boolean flag = (Boolean)obj;
                props.add(flag);

            } else if( obj instanceof JSONObject ){
                JSONObject json = (JSONObject)obj;
                Map<String,Object> p = propertiesFromJsonObject(json);
                props.add(p);

            } else if( obj instanceof JSONArray ){
                JSONArray json = (JSONArray)obj;
                List<Object> p = propertiesFromJsonArray(json);
                props.add(p);

            } else {
                String name = "" + obj;
                if( null != obj ){
                    name = obj.getClass().getName();
                }
                throw new Exception("propertiesFromJsonArray() can not handle "+name);
            }
        }

        return props;
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

            CouchbaseQueryResults results = new CouchbaseQueryResults();

            Query dbQuery = database.getView(viewName).createQuery();

            QueryEnumerator resultEnum = dbQuery.run();
            for (Iterator<QueryRow> it = resultEnum; it.hasNext(); ) {
                QueryRow dbRow = it.next();

                JSONObject row = new JSONObject();

                Document doc = dbRow.getDocument();

                row.put("id",doc.getId());
                row.put("rev", doc.getCurrentRevisionId());

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
