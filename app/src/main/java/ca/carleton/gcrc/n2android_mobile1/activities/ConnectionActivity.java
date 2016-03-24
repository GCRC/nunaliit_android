package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.NunaliitMobileConstants;
import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.services.ConnectionManagementService;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionActivity extends ServiceBasedActivity {

    protected String TAG = this.getClass().getSimpleName();

    private OnClickListener btnSynchronizeListener = new OnClickListener() {
        @Override
        public void onClick(View v){
            synchronizeConnection();
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveBroadcast(intent);
        }
    };

    public String getTag() {
        return TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(
            broadcastReceiver,
            new IntentFilter(ConnectionManagementService.RESULT_SYNC_COMPLETED)
        );

        setContentView(R.layout.activity_connection);

        // Synchronize button
        {
            View view = findViewById(R.id.button_sync);
            if( view instanceof Button ){
                Button button = (Button)view;
                button.setOnClickListener(btnSynchronizeListener);
            } else {
                view.setVisibility(0);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        refreshDisplay();
    }

    @Override
    public void couchbaseServiceReporting(CouchbaseLiteService service) {
        refreshDisplay();
    }

    public void refreshDisplay(){
        String connectionId = null;
        Intent intent = getIntent();
        if( null != intent ){
            connectionId = intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID);
        }

        ConnectionInfo connInfo = null;
        CouchbaseLiteService service = getCouchbaseService();
        if( null != connectionId && null != service ){
            try {
                connInfo = service.getConnectionInfo(connectionId);
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

            // Local db name
            {
                View view = findViewById(R.id.connection_local_db_name);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(connInfo.getLocalDocumentDbName());
                }
            }

            // Button
            {
                View view = findViewById(R.id.button_sync);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // Connection information not yet available

            // Button
            {
                View view = findViewById(R.id.button_sync);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void synchronizeConnection() {
        Log.v(TAG, "Synchronization called");

        String connectionId = null;
        {
            Intent intent = getIntent();
            if (null != intent) {
                connectionId = intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID);
            }
        }

        Log.v(TAG,"action:"+ConnectionManagementService.ACTION_SYNC+NunaliitMobileConstants.threadId());

        Intent syncIntent = new Intent(this, ConnectionManagementService.class);
        syncIntent.setAction(ConnectionManagementService.ACTION_SYNC);
        syncIntent.putExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID, connectionId);
        startService(syncIntent);
    }

    protected void receiveBroadcast(Intent intent){
        Log.v(TAG,"Received broadcast :"+intent.getAction()+NunaliitMobileConstants.threadId());
    }
}
