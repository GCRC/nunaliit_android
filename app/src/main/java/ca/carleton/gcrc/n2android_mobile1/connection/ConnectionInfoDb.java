package ca.carleton.gcrc.n2android_mobile1.connection;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQuery;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQueryResults;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseView;

/**
 * Created by jpfiset on 3/24/16.
 */
public class ConnectionInfoDb extends CouchbaseDb {

    static public final CouchbaseView viewConnectionsById = new CouchbaseView(){
        @Override
        public String getName() { return "connections-by-id"; }

        @Override
        public String getVersion() { return "1"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object connInfoObj = document.get("mobile_connection");
                if (null != connInfoObj && connInfoObj instanceof Map) {
                    Object idObj = document.get("_id");
                    if (null != idObj && idObj instanceof String) {
                        String id = (String) idObj;
                        emitter.emit(id, null);
                    }
                }
            }
        };
    };

    static public ConnectionInfo connectionInfoFromJson(JSONObject jsonObj) {
        ConnectionInfo info = null;

        JSONObject jsonConnInfo = jsonObj.optJSONObject("mobile_connection");
        if( null != jsonConnInfo ) {
            info = new ConnectionInfo();

            // id
            {
                String id = jsonObj.optString("_id",null);
                if( id != null ) {
                    info.setId(id);
                }
            }

            // name
            {
                String name = jsonConnInfo.optString("name", null);
                if( name != null ) {
                    info.setName(name);
                }
            }

            // url
            {
                String url = jsonConnInfo.optString("url", null);
                if( url != null ) {
                    info.setUrl(url);
                }
            }

            // user
            {
                String user = jsonConnInfo.optString("user", null);
                if( user != null ) {
                    info.setUser(user);
                }
            }

            // password
            {
                String password = jsonConnInfo.optString("password", null);
                if( password != null ) {
                    info.setPassword(password);
                }
            }

            // local docs db
            {
                String localDocsDb = jsonConnInfo.optString("localDocsDb", null);
                if( localDocsDb != null ){
                    info.setLocalDocumentDbName(localDocsDb);
                }
            }

            // local revs db
            {
                String localTrackingDb = jsonConnInfo.optString("localTrackingDb", null);
                if( localTrackingDb != null ){
                    info.setLocalTrackingDbName(localTrackingDb);
                }
            }

            // device id
            {
                String deviceId = jsonConnInfo.optString("deviceId", null);
                if ( deviceId != null ) {
                    info.setDeviceId(deviceId);
                }
            }
        }

        return info;
    }

    static public JSONObject jsonFromConnectionInfo(ConnectionInfo info) throws Exception {
        JSONObject jsonObj = new JSONObject();
        updateJsonFromConnectionInfo(jsonObj,info);
        return jsonObj;
    }

    static public JSONObject updateJsonFromConnectionInfo(JSONObject jsonObj, ConnectionInfo info) throws Exception {

        // id
        {
            String id = info.getId();
            if( null != id ){
                jsonObj.put("_id",id);
            }
        }

        JSONObject jsonConnInfo = jsonObj.optJSONObject("mobile_connection");
        if( null == jsonConnInfo ){
            jsonConnInfo = new JSONObject();
            jsonObj.put("mobile_connection",jsonConnInfo);
        }

        // name
        {
            String name = info.getName();
            jsonConnInfo.put("name", name);
        }

        // url
        {
            String url = info.getUrl();
            jsonConnInfo.put("url", url);
        }

        // user
        {
            String user = info.getUser();
            jsonConnInfo.put("user", user);
        }

        // password
        {
            String password = info.getPassword();
            jsonConnInfo.put("password", password);
        }

        // local docs db
        {
            String localDocsDb = info.getLocalDocumentDbName();
            jsonConnInfo.put("localDocsDb", localDocsDb);
        }

        // local revs db
        {
            String localTrackingDb = info.getLocalTrackingDbName();
            jsonConnInfo.put("localTrackingDb", localTrackingDb);
        }

        {
            String deviceId = info.getDeviceId();
            jsonConnInfo.put("deviceId", deviceId);
        }

        return jsonObj;
    }

    public ConnectionInfoDb(Database database) throws Exception {
        super(database);

        installView(viewConnectionsById);
    }

    public ConnectionInfo createConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonInfo = new JSONObject();
            updateJsonFromConnectionInfo(jsonInfo, info);
            CouchbaseDocInfo docInfo = createDocument(jsonInfo);
            info.setId(docInfo.getId());
            return info;

        } catch(Exception e) {
            throw new Exception("Error while creating connection info document",e);
        }
    }

    public ConnectionInfo getConnectionInfo(String docId) throws Exception {
        try {
            JSONObject jsonInfo = getDocument(docId);
            ConnectionInfo info = connectionInfoFromJson(jsonInfo);
            return info;

        } catch(Exception e) {
            throw new Exception("Error while creating connection info document",e);
        }
    }

    public List<ConnectionInfo> getConnectionInfos() throws Exception {
        try {
            CouchbaseQuery query = new CouchbaseQuery();
            query.setViewName(CouchbaseManager.VIEW_CONNECTIONS);
            query.setIncludeDocs(true);
            CouchbaseQueryResults results = performQuery(query);

            List<ConnectionInfo> connectionInfos = new Vector<ConnectionInfo>();
            for(JSONObject row : results.getRows()) {
                JSONObject doc = row.getJSONObject("doc");
                ConnectionInfo connInfo = connectionInfoFromJson(doc);
                if( null != connInfo ){
                    connectionInfos.add(connInfo);
                }
            }

            return connectionInfos;

        } catch(Exception e) {
            throw new Exception("Error while creating list of connection info documents",e);
        }
    }

    public void updateConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonDoc = getDocument(info.getId());
            updateJsonFromConnectionInfo(jsonDoc, info);
            updateDocument(jsonDoc);

        } catch(Exception e) {
            throw new Exception("Error while updating connection info document",e);
        }
    }

    public void deleteConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonDoc = getDocument(info.getId());
            deleteDocument(jsonDoc);

        } catch(Exception e) {
            throw new Exception("Error while deleting connection info document",e);
        }
    }
}
