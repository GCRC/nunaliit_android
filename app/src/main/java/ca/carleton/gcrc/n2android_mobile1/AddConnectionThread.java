package ca.carleton.gcrc.n2android_mobile1;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.activities.AddConnectionActivity;
import ca.carleton.gcrc.n2android_mobile1.connection.Connection;

/**
 * Created by jpfiset on 3/21/16.
 */
public class AddConnectionThread extends Thread {

    protected final String TAG = this.getClass().getSimpleName();

    static final public int CONNECTION_CREATED = 0x1;
    static final public int CONNECTION_CREATION_ERROR = 0x2;

    private ConnectionInfo info;
    private Handler handler;
    private CouchDbService service;

    public AddConnectionThread(CouchDbService service, ConnectionInfo info, Handler handler){
        this.service = service;
        this.info = info;
        this.handler = handler;
    }

    public void stopTesting() {
        handler = null;
        this.interrupt();
    }

    @Override
    public void run() {
        Connection connection = new Connection(info);
        try {
            connection.checkRemoteSite();

            service.createLocalDatabase(info);

            service.addConnectionInfo(info);

            if( null != handler ){
                Message message = handler.obtainMessage(CONNECTION_CREATED,info);
                message.sendToTarget();

                Log.i(TAG, "Connection created message sent");
            }

        } catch(Exception e) {
            if( null != handler ){
                Message message = handler.obtainMessage(CONNECTION_CREATION_ERROR,e);
                message.sendToTarget();
            }
        }
    }
}
