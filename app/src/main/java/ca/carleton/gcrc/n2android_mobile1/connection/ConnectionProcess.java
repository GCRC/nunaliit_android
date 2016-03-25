package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;

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
        Connection connection = new Connection(info);
        try {
            CouchbaseManager mgr = service.getCouchbaseManager();
            CouchbaseDb connectionsDb = mgr.getConnectionsDb();
            ConnectionInfoDb infoDb = new ConnectionInfoDb(connectionsDb);

            connection.checkRemoteSite();

            mgr.createLocalDatabase(info);

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
}
