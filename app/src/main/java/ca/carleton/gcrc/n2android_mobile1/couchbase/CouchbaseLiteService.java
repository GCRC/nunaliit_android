package ca.carleton.gcrc.n2android_mobile1.couchbase;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import ca.carleton.gcrc.n2android_mobile1.Nunaliit;

/**
 * Created by jpfiset on 3/10/16.
 */
public class CouchbaseLiteService extends Service {
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class CouchDbBinder extends Binder {
        public CouchbaseLiteService getService() {
            return CouchbaseLiteService.this;
        }
    }

    public static String TAG = "NunaliitMobileDatabase";

    // Binder given to clients
    private final IBinder mBinder = new CouchDbBinder();
    private CouchbaseManager couchbaseManager;

    public CouchbaseLiteService(){
        Log.v(TAG, "Constructor" + Nunaliit.threadId());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate" + Nunaliit.threadId());

        final Context context = getApplicationContext();
        Thread t = new Thread(){
            @Override
            public void run() {
                try {
                    CouchbaseManager mgr = new CouchbaseManager();
                    mgr.startCouchbase(getApplicationContext());
                    setCouchbaseManager(mgr);

                } catch(Exception e) {
                    couchbaseInitFailure(e);
                }
            }
        };
        t.start();
    }

    @Override
    public void onDestroy() {

        Log.v(TAG, "onDestroy" + Nunaliit.threadId());

        if( null != couchbaseManager ){
            couchbaseManager.stopCouchbase();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind" + Nunaliit.threadId());

        return mBinder;
    }

    public CouchbaseManager getCouchbaseManager(){
        synchronized (this){
            boolean interrupted = false;
            while( !interrupted && null == couchbaseManager ){
                try {
                    this.wait();
                } catch(InterruptedException e) {
                    interrupted = true;
                }
            }
        }
        return couchbaseManager;
    }

    protected void setCouchbaseManager(CouchbaseManager mgr){
        synchronized(this){
            couchbaseManager = mgr;
            this.notifyAll();
        }
        Log.i(TAG, "Couchbase Initialized" + Nunaliit.threadId());
    }

    protected void couchbaseInitFailure(Throwable e){
        Log.e(TAG, "Couchbase failed to initialized", e);

        Toast
            .makeText(
                    getApplicationContext(),
                    "Error Initializing Couchbase, see logs for details",
                    Toast.LENGTH_LONG
            )
            .show();
    }
}
