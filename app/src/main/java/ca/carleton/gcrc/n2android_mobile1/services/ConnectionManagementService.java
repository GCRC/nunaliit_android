package ca.carleton.gcrc.n2android_mobile1.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.NunaliitMobileConstants;

/**
 * Created by jpfiset on 3/22/16.
 */
public class ConnectionManagementService extends IntentService {

    public static String ACTION_SYNC = ConnectionManagementService.class.getName()+".SYNC";

    public static String RESULT_SYNC_COMPLETED = ConnectionManagementService.class.getName()+".SYNC_COMPLETED";

    public static String TAG = "NunaliitMobileConnections";

    // Connection to CouchbaseLiteService
    private ServiceConnection mCouchbaseConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchbaseLiteService.CouchDbBinder binder = (CouchbaseLiteService.CouchDbBinder) service;
            reportCouchbaseService(binder.getService());

            Log.v(TAG, "Bound to CouchbaseService"+NunaliitMobileConstants.threadId());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCouchbaseBound = false;

            Log.v(TAG, "Unbound from CouchbaseService"+NunaliitMobileConstants.threadId());
        }
    };
    private CouchbaseLiteService mCouchbaseService;
    private boolean mCouchbaseBound = false;

    public ConnectionManagementService(){
        super(TAG);

        Log.v(TAG, "Created Service"+NunaliitMobileConstants.threadId());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.v(TAG, "onStartCommand: " + intent.getAction()+NunaliitMobileConstants.threadId());

        return IntentService.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Request binding to CouchbaseService"+NunaliitMobileConstants.threadId());

        // Bind to CouchbaseLiteService
        Intent intent = new Intent(this, CouchbaseLiteService.class);
        bindService(intent, mCouchbaseConnection, Context.BIND_AUTO_CREATE);

        try {

        } catch(Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        // Unbind from the service
        if (mCouchbaseBound) {
            Log.v(TAG, "Request unbinding from CouchbaseService"+NunaliitMobileConstants.threadId());

            unbindService(mCouchbaseConnection);
            mCouchbaseBound = false;
        }

        super.onDestroy();
    }

    public CouchbaseLiteService getCouchbaseService() {
        return mCouchbaseService;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "Intent received: "+intent.getAction()+NunaliitMobileConstants.threadId());

        if( ACTION_SYNC.equals(intent.getAction()) ){
            Log.v(TAG, "Action Synchronize Requested: " + intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID) + NunaliitMobileConstants.threadId());
            waitForCouchbaseService();

            Intent result = new Intent(RESULT_SYNC_COMPLETED);
            Log.v(TAG, "Action Synchronize Result: " + result.getAction() + NunaliitMobileConstants.threadId());

            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }

    protected void reportCouchbaseService(CouchbaseLiteService service){
        if( null != service ){
            synchronized (this){
                mCouchbaseBound = true;
                mCouchbaseService = service;
                this.notifyAll();
            }
        }
    }

    private void waitForCouchbaseService(){
        boolean interrupted = false;

        synchronized (this){
            while( !mCouchbaseBound && !interrupted ){
                try {
                    wait();
                } catch(InterruptedException e) {
                    interrupted = true;
                }
            }
        }
    }
}
