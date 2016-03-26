package ca.carleton.gcrc.n2android_mobile1.connection;

import org.json.JSONObject;

import java.util.Map;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;

/**
 * Created by jpfiset on 3/24/16.
 */
public class ConnectionInfoDb {

    public static ConnectionInfo connectionInfoFromJson(JSONObject jsonObj) {
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
                String localRevsDb = jsonConnInfo.optString("localRevsDb", null);
                if( localRevsDb != null ){
                    info.setLocalRevisionDbName(localRevsDb);
                }
            }
        }

        return info;
    }

    public static JSONObject jsonFromConnectionInfo(ConnectionInfo info) throws Exception {
        JSONObject jsonObj = new JSONObject();

        // id
        {
            String id = info.getId();
            if( null != id ){
                jsonObj.put("_id",id);
            }
        }

        JSONObject jsonConnInfo = new JSONObject();
        jsonObj.put("mobile_connection",jsonConnInfo);

        // name
        {
            String name = info.getName();
            if( name != null ) {
                jsonConnInfo.put("name", name);
            }
        }

        // url
        {
            String url = info.getUrl();
            if( url != null ) {
                jsonConnInfo.put("url", url);
            }
        }

        // user
        {
            String user = info.getUser();
            if( user != null ) {
                jsonConnInfo.put("user", user);
            }
        }

        // password
        {
            String password = info.getPassword();
            if( password != null ) {
                jsonConnInfo.put("password", password);
            }
        }

        // local docs db
        {
            String localDocsDb = info.getLocalDocumentDbName();
            if( localDocsDb != null ){
                jsonConnInfo.put("localDocsDb", localDocsDb);
            }
        }

        // local revs db
        {
            String localRevsDb = info.getLocalRevisionDbName();
            if( localRevsDb != null ){
                jsonConnInfo.put("localRevsDb", localRevsDb);
            }
        }

        return jsonObj;
    }

    private CouchbaseDb connDb;

    public ConnectionInfoDb(CouchbaseDb connDb){
        this.connDb = connDb;
    }

    public ConnectionInfo createConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonInfo = jsonFromConnectionInfo(info);
            JSONObject docInfo = connDb.createDocument(jsonInfo);
            info.setId(docInfo.optString("id"));
            return info;

        } catch(Exception e) {
            throw new Exception("Error while creating connection info document",e);
        }
    }

    public ConnectionInfo getConnectionInfo(String docId) throws Exception {
        try {
            JSONObject jsonInfo = connDb.getDocument(docId);
            ConnectionInfo info = connectionInfoFromJson(jsonInfo);
            return info;

        } catch(Exception e) {
            throw new Exception("Error while creating connection info document",e);
        }
    }

    public void updateConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonInfo = jsonFromConnectionInfo(info);
            connDb.updateDocument(jsonInfo);

        } catch(Exception e) {
            throw new Exception("Error while updating connection info document",e);
        }
    }

    public void deleteConnectionInfo(ConnectionInfo info) throws Exception {
        try {
            JSONObject jsonInfo = jsonFromConnectionInfo(info);
            connDb.deleteDocument(jsonInfo);

        } catch(Exception e) {
            throw new Exception("Error while deleting connection info document",e);
        }
    }
}
