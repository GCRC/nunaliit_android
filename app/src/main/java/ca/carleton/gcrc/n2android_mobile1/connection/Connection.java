package ca.carleton.gcrc.n2android_mobile1.connection;

import android.util.Log;

import org.json.JSONObject;

import java.net.URL;

import ca.carleton.gcrc.couch.client.CouchClient;
import ca.carleton.gcrc.couch.client.CouchContext;
import ca.carleton.gcrc.couch.client.CouchDb;
import ca.carleton.gcrc.couch.client.CouchFactory;
import ca.carleton.gcrc.couch.client.CouchServerVersion;
import ca.carleton.gcrc.couch.client.impl.ConnectionUtils;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;

/**
 * Created by jpfiset on 3/21/16.
 */
public class Connection {

    protected final String TAG = this.getClass().getSimpleName();

    private CouchbaseManager manager;
    private ConnectionInfo info;
    private String remoteDbName = null;

    public Connection(CouchbaseManager manager, ConnectionInfo info){
        this.info = info;
        this.manager = manager;
    }

    public ConnectionInfo getConnectionInfo(){
        return info;
    }

    public CouchClient getRemoteCouchClient() throws Exception {
        try {
            URL url = new URL(info.getUrl());
            URL serverUrl = new URL(url, "server/");

            CouchFactory factory = new CouchFactory();
            CouchContext context = factory.getContext(info.getUser(),info.getPassword().toCharArray());
            CouchClient client = factory.getClient(context, serverUrl);

            return client;

        } catch(Exception e) {
            throw new Exception("Unable to create remote client",e);
        }
    }

    public CouchDb getRemoteCouchDb() throws Exception {
        try {
            CouchClient client = getRemoteCouchClient();
            String dbName = getDbName();

            CouchDb db = client.getDatabase(dbName);

            return db;

        } catch(Exception e) {
            throw new Exception("Unable to create remote database",e);
        }
    }

    public void checkRemoteSite() throws Exception {
        CouchClient client = getRemoteCouchClient();

        CouchServerVersion version = client.getVersion();

        int major = version.getMajor();
        int minor = version.getMinor();

        Log.i(TAG,"Database version major: "+major+" minor: "+minor);

        String dbName = getDbName();
        Log.i(TAG,"Remote Database Name: "+dbName);
    }

    public String getDbName() throws Exception {
        if( null == remoteDbName ){
            URL url = new URL(info.getUrl());
            URL dbUrl = new URL(url, "db");

            CouchClient client = getRemoteCouchClient();
            Object response = ConnectionUtils.getJsonResource(client.getContext(), dbUrl);

            if( null == response ){
                throw new Exception("Unable to find database object");

            } else if( response instanceof JSONObject ) {
                JSONObject result = (JSONObject)response;
                String dbName = result.optString("db_name");

                if( null == dbName ){
                    throw new Exception("Database resource does not report name");
                }
                remoteDbName = dbName;

            } else {
                throw new Exception("Unexpected resource for database: "+response.getClass().getSimpleName());
            }
        }

        return remoteDbName;
    }

    public CouchbaseDb getLocalDocumentDb() throws Exception {
        String localDbName = info.getLocalDocumentDbName();
        CouchbaseDb db = manager.getDatabase(localDbName);
        return db;
    }
}
