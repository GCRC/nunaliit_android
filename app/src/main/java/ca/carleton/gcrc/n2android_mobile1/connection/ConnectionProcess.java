package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;

/**
 * Created by jpfiset on 3/21/16.
 */
public class ConnectionProcess {

    protected final String TAG = this.getClass().getSimpleName();

    private Context context;
    private CouchbaseLiteService service;

    public ConnectionProcess(CouchbaseLiteService service, Context context){
        this.service = service;
        this.context = context;
    }

    public void addConnection(ConnectionInfo info) throws Exception {
        Connection connection = new Connection(service.getCouchbaseManager(), info);
        try {
            CouchbaseManager mgr = service.getCouchbaseManager();
            CouchbaseDb connectionsDb = mgr.getConnectionsDb();
            ConnectionInfoDb infoDb = new ConnectionInfoDb(connectionsDb);

            connection.checkRemoteSite();

            createLocalDatabases(info);

            info = infoDb.createConnectionInfo(info);

            ConnectionSyncProcess sync = new ConnectionSyncProcess(service, connection);

        } catch(Exception e) {
            throw new Exception("Error while adding a connection",e);
        }
    }

    public void deleteConnection(String connId) throws Exception {
        try {
            CouchbaseManager mgr = service.getCouchbaseManager();
            CouchbaseDb connectionsDb = mgr.getConnectionsDb();
            ConnectionInfoDb connDb = new ConnectionInfoDb(connectionsDb);

            ConnectionInfo info = connDb.getConnectionInfo(connId);

            String localDbName = null;
            if( null != info ){
                localDbName = info.getLocalDocumentDbName();
            }

            if( null != localDbName ){
                mgr.deleteDatabase(localDbName);
            }

            connDb.deleteConnectionInfo(info);

        } catch(Exception e) {
            throw new Exception("Error while deleting connection "+connId,e);
        }
    }

    private void createLocalDatabases(ConnectionInfo connectionInfo){
        CouchbaseManager mgr = service.getCouchbaseManager();

        int count = 0;
        boolean created = false;
        while( !created && count < 100 ){
            ++count;
            String docDbName = "docs_"+count;
            String revDbName = "revs_"+count;
            if( false == mgr.databaseExists(docDbName)
                && false == mgr.databaseExists(revDbName) ){

                // Create doc db
                boolean docDbCreated = false;
                try {
                    mgr.createDatabase(docDbName);
                    docDbCreated = true;

                } catch(Exception e) {
                    Log.e(TAG, "Error creating database: "+docDbName, e);
                }

                // Create rev db
                boolean revDbCreated = false;
                if( docDbCreated ){
                    try {
                        mgr.createDatabase(revDbName);
                        revDbCreated = true;

                    } catch(Exception e) {
                        Log.e(TAG, "Error creating database: "+revDbName, e);
                    }
                }

                if( revDbCreated ){
                    created = true;
                    connectionInfo.setLocalDocumentDbName(docDbName);
                    connectionInfo.setLocalRevisionDbName(revDbName);

                } else if( docDbCreated ) {
                    try {
                        mgr.deleteDatabase(docDbName);

                    } catch(Exception e) {
                        Log.e(TAG, "Unable to delete database: "+docDbName, e);
                    }
                }
            }
        }
    }
}
