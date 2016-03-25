package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;

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
            connection.checkRemoteSite();

            service.createLocalDatabase(info);

            service.addConnectionInfo(info);

            ConnectionSyncProcess sync = new ConnectionSyncProcess(service, connection);

        } catch(Exception e) {
            throw new Exception("Error while adding a connection",e);
        }
    }

    public void deleteConnection(String connId) throws Exception {
        try {
            CouchbaseDb db = service.getConnectionsDb();
            ConnectionInfoDb connDb = new ConnectionInfoDb(db);

            ConnectionInfo info = connDb.getConnectionInfo(connId);

            String localDbName = null;
            if( null != info ){
                localDbName = info.getLocalDocumentDbName();
            }

            if( null != localDbName ){
                service.deleteDb(localDbName);
            }

            connDb.deleteConnectionInfo(info);

        } catch(Exception e) {
            throw new Exception("Error while deleting connection "+connId,e);
        }
    }
}
