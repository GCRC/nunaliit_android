package ca.carleton.gcrc.n2android_mobile1;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionActivity extends AppCompatActivity {

    protected String TAG = this.getClass().getSimpleName();

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchDbService.CouchDbBinder binder = (CouchDbService.CouchDbBinder) service;
            mService = binder.getService();
            mBound = true;
            refreshDisplay();
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

        setContentView(R.layout.activity_connection);

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

    @Override
    protected void onStart() {
        super.onStart();

        refreshDisplay();
    }

    public void refreshDisplay(){
        String connectionId = null;
        Intent intent = getIntent();
        if( null != intent ){
            connectionId = intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID);
        }

        ConnectionInfo connInfo = null;
        if( null != connectionId && null != mService ){
            try {
                connInfo = mService.getConnection(connectionId);
            } catch (Exception e) {
                Log.e(TAG, "Unable to retrieve connection info", e);
            }
        }

        if( null != connInfo ){
            // Name
            {
                View view = findViewById(R.id.connection_name_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(connInfo.getName());
                }
            }

            // URL
            {
                View view = findViewById(R.id.connection_url_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(connInfo.getUrl());
                }
            }

            // User
            {
                View view = findViewById(R.id.connection_user_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(connInfo.getUser());
                }
            }
        }
    }
}
