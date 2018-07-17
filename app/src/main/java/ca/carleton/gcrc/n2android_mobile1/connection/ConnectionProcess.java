package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;
import android.util.Log;

import java.util.UUID;

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
            ConnectionInfoDb infoDb = mgr.getConnectionsDb();

            connection.checkRemoteSite();

            createLocalDatabases(info);

            info.setDeviceId(UUID.randomUUID().toString());

            infoDb.createConnectionInfo(info);

            ConnectionSyncProcess sync = new ConnectionSyncProcess(service, connection);
            ConnectionSyncResult syncResult = sync.synchronize();

        } catch(Exception e) {
            throw new Exception("Error while adding a connection",e);
        }
    }

    public void deleteConnection(String connId) throws Exception {
        try {
            CouchbaseManager mgr = service.getCouchbaseManager();
            ConnectionInfoDb connDb = mgr.getConnectionsDb();

            ConnectionInfo info = connDb.getConnectionInfo(connId);

            String localDocsDbName = null;
            String localTrackingDbName = null;
            if( null != info ){
                localDocsDbName = info.getLocalDocumentDbName();
                localTrackingDbName = info.getLocalTrackingDbName();
            }

            if( null != localDocsDbName ){
                mgr.deleteDatabase(localDocsDbName);
            }

            if( null != localTrackingDbName ){
                mgr.deleteDatabase(localTrackingDbName);
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
            String trackingDbName = "tracking_"+count;
            if( false == mgr.databaseExists(docDbName)
                && false == mgr.databaseExists(trackingDbName) ){

                // Create doc db
                boolean docDbCreated = false;
                try {
                    mgr.createDatabase(docDbName);
                    docDbCreated = true;

                } catch(Exception e) {
                    Log.e(TAG, "Error creating database: "+docDbName, e);
                }

                // Create tracking db
                boolean trackingDbCreated = false;
                if( docDbCreated ){
                    try {
                        mgr.createDatabase(trackingDbName);
                        trackingDbCreated = true;

                    } catch(Exception e) {
                        Log.e(TAG, "Error creating database: "+trackingDbName, e);
                    }
                }

                if( trackingDbCreated ){
                    created = true;
                    connectionInfo.setLocalDocumentDbName(docDbName);
                    connectionInfo.setLocalTrackingDbName(trackingDbName);

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
