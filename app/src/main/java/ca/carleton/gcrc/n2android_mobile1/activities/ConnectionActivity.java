package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionManagementService;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionActivity extends AppCompatActivity {

    protected String TAG = this.getClass().getSimpleName();

    private OnClickListener btnSynchronizeListener = new OnClickListener() {
        @Override
        public void onClick(View v){
            synchronizeConnection();
        }
    };

    private OnClickListener btnDeleteListener = new OnClickListener() {
        @Override
        public void onClick(View v){
            deleteConnection();
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveBroadcast(intent);
        }
    };

    private String connectionId = null;
    private ConnectionInfo currentConnection = null;

    public String getTag() {
        return TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve connection id
        {
            Intent intent = getIntent();
            if (null != intent) {
                connectionId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            }
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.RESULT_GET_CONNECTION_INFO)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.ERROR_GET_CONNECTION_INFO)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.RESULT_DELETE_CONNECTION)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.ERROR_DELETE_CONNECTION)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.RESULT_SYNC)
        );

        setContentView(R.layout.activity_connection);

        // Synchronize button
        {
            View view = findViewById(R.id.button_sync);
            if( view instanceof Button ){
                Button button = (Button)view;
                button.setOnClickListener(btnSynchronizeListener);
            } else {
                view.setVisibility(View.INVISIBLE);
            }
        }

        // Delete button
        {
            View view = findViewById(R.id.button_delete);
            if( view instanceof Button ){
                Button button = (Button)view;
                button.setOnClickListener(btnDeleteListener);
            } else {
                view.setVisibility(View.INVISIBLE);
            }
        }

        // Request
        {
            Intent intent = new Intent(this,ConnectionManagementService.class);
            intent.setAction(ConnectionManagementService.ACTION_GET_CONNECTION_INFO);
            intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connectionId);
            startService(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        refreshDisplay();
    }

    public void refreshDisplay(){
        if( null != currentConnection ){
            // Name
            {
                View view = findViewById(R.id.connection_name_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(currentConnection.getName());
                }
            }

            // URL
            {
                View view = findViewById(R.id.connection_url_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(currentConnection.getUrl());
                }
            }

            // User
            {
                View view = findViewById(R.id.connection_user_value);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(currentConnection.getUser());
                }
            }

            // Local db name
            {
                View view = findViewById(R.id.connection_local_db_name);
                if( null != view && view instanceof TextView ){
                    TextView textView = (TextView)view;
                    textView.setText(currentConnection.getLocalDocumentDbName());
                }
            }

            // Synchronize Button
            {
                View view = findViewById(R.id.button_sync);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.VISIBLE);
                }
            }

            // Delete Button
            {
                View view = findViewById(R.id.button_delete);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.VISIBLE);
                }
            }

        } else {
            // Connection information not yet available

            // Synchronize Button
            {
                View view = findViewById(R.id.button_sync);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.INVISIBLE);
                }
            }

            // Delete Button
            {
                View view = findViewById(R.id.button_delete);
                if( null != view && view instanceof Button ){
                    Button button = (Button)view;
                    button.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void synchronizeConnection() {
        Intent syncIntent = new Intent(this, ConnectionManagementService.class);
        syncIntent.setAction(ConnectionManagementService.ACTION_SYNC);
        Log.v(TAG, "action:" + syncIntent.getAction() + Nunaliit.threadId());
        syncIntent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connectionId);
        startService(syncIntent);
    }

    private void deleteConnection() {
        Intent intent = new Intent(this, ConnectionManagementService.class);
        intent.setAction(ConnectionManagementService.ACTION_DELETE_CONNECTION);
        Log.v(TAG, "action:" + intent.getAction() + Nunaliit.threadId());
        intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connectionId);
        startService(intent);
    }

    protected void receiveBroadcast(Intent intent){
        Log.v(TAG,"Received broadcast :"+intent.getAction()+ Nunaliit.threadId());

        if( ConnectionManagementService.RESULT_GET_CONNECTION_INFO.equals(intent.getAction()) ) {
            Parcelable parcelable = intent.getParcelableExtra(Nunaliit.EXTRA_CONNECTION_INFO);
            if (parcelable instanceof ConnectionInfo) {
                ConnectionInfo connInfo = (ConnectionInfo) parcelable;
                if (connInfo.getId().equals(connectionId)) {
                    currentConnection = connInfo;
                    refreshDisplay();
                }
            }

        } else if( ConnectionManagementService.RESULT_DELETE_CONNECTION.equals(intent.getAction()) ){
                String connId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
                if( null != connId && connId.equals(connectionId) ) {
                    finish();
                }

        } else {
            Log.w(TAG, "Ignoring received intent :" + intent.getAction() + Nunaliit.threadId());
        }
    }
}
