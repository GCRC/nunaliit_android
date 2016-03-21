package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.apache.cordova.CordovaActivity;

import ca.carleton.gcrc.n2android_mobile1.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.CouchDbService;
import ca.carleton.gcrc.n2android_mobile1.NunaliitMobileConstants;

/**
 * Created by jpfiset on 3/9/16.
 */
public class EmbeddedCordovaActivity extends CordovaActivity {

    final protected String TAG = this.getClass().getSimpleName();

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
    ConnectionInfo connectionInfo = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Bind to CouchDbService
        Intent intent = new Intent(this, CouchDbService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

    @Override
    public void onDestroy() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    public void serviceReporting(CouchDbService service){
        retrieveConnection();
    }

    public ConnectionInfo retrieveConnection(){
        ConnectionInfo connInfo = connectionInfo;

        if( null == connInfo ){
            String connectionId = null;
            Intent intent = getIntent();
            if( null != intent ){
                connectionId = intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID);
            }

            if( null != mService && null != connectionId ){
                try {
                    connInfo = mService.getConnectionInfo(connectionId);
                } catch(Exception e) {
                    Log.e(TAG,"Unable to retrieve connection info",e);
                }
            }

            connectionInfo = connInfo;
        }

        return connInfo;
    }

    public CouchDbService getCouchDbService(){
        return mService;
    }
}
