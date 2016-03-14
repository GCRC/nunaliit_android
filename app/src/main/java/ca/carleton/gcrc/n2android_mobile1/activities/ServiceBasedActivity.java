package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import ca.carleton.gcrc.n2android_mobile1.CouchDbService;

/**
 * Created by jpfiset on 3/14/16.
 */
public class ServiceBasedActivity extends AppCompatActivity {

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchDbService.CouchDbBinder binder = (CouchDbService.CouchDbBinder) service;
            mService = binder.getService();
            mBound = true;
            serviceReporting(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private CouchDbService mService;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to CouchDbService
        Intent intent = new Intent(this, CouchDbService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    public boolean isServiceBound(){
        return mBound;
    }

    public CouchDbService getService(){
        return mService;
    }

    /**
     * Subclasses should re-implement this method to become aware of the
     * service when it becomes available
     * @param service
     */
    public void serviceReporting(CouchDbService service){
    }
}
