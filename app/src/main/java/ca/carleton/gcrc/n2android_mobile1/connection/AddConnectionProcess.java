package ca.carleton.gcrc.n2android_mobile1.connection;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;

/**
 * Created by jpfiset on 3/21/16.
 */
public class AddConnectionProcess {

    protected final String TAG = this.getClass().getSimpleName();

    private ConnectionInfo info;
    private Context context;
    private CouchbaseLiteService service;

    public AddConnectionProcess(CouchbaseLiteService service, ConnectionInfo info, Context context){
        this.service = service;
        this.info = info;
        this.context = context;
    }

    public void addConnection() throws Exception {
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
}
