package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;

/**
 * Created by jpfiset on 3/14/16.
 */
public abstract class ServiceBasedActivity extends AppCompatActivity {

    private ServiceConnection mCouchbaseConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchbaseLiteService.CouchDbBinder binder = (CouchbaseLiteService.CouchDbBinder) service;
            mCouchbaseService = binder.getService();
            mCouchbaseBound = true;
            couchbaseServiceReporting(mCouchbaseService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCouchbaseBound = false;
        }
    };
    private CouchbaseLiteService mCouchbaseService;
    private boolean mCouchbaseBound = false;

    public abstract String getTag();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(getTag(), "Activity Created");

        // Bind to CouchDbService
        Intent intent = new Intent(this, CouchbaseLiteService.class);
        bindService(intent, mCouchbaseConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        // Unbind from the service
        if (mCouchbaseBound) {
            unbindService(mCouchbaseConnection);
            mCouchbaseBound = false;
        }

        super.onDestroy();
    }

    public boolean isServiceBound(){
        return mCouchbaseBound;
    }

    public CouchbaseLiteService getCouchbaseService(){
        return mCouchbaseService;
    }

    /**
     * Subclasses should re-implement this method to become aware of the
     * service when it becomes available
     * @param service
     */
    public void couchbaseServiceReporting(CouchbaseLiteService service){
    }
}
