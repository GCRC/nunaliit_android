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
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by jpfiset on 3/10/16.
 */
public class AddConnectionActivity extends AppCompatActivity {

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchDbService.CouchDbBinder binder = (CouchDbService.CouchDbBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private CouchDbService mService;
    private boolean mBound = false;

    protected String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_connection);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to CouchDbService
        Intent intent = new Intent(this, CouchDbService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void createConnection(View view){
        // Connection name
        String connectionName = null;
        {
            View editTextView = findViewById(R.id.connectionName);
            if( editTextView instanceof EditText){
                EditText editText = (EditText)editTextView;
                connectionName = editText.getText().toString();
            }
        }

        // URL
        String url = null;
        {
            View editTextView = findViewById(R.id.url);
            if( editTextView instanceof EditText ){
                EditText editText = (EditText) editTextView;
                url = editText.getText().toString();
            }
        }

        // User Name
        String userName = null;
        {
            View editTextView = findViewById(R.id.userName);
            if( editTextView instanceof EditText ){
                EditText editText = (EditText)editTextView;
                userName = editText.getText().toString();
            }
        }

        // User Password
        String userPassword = null;
        {
            View editTextView = findViewById(R.id.userPassword);
            if( editTextView instanceof EditText ){
                EditText editText = (EditText)editTextView;
                userPassword = editText.getText().toString();
            }
        }

        Log.i(TAG, "Connection " + connectionName + "/" + url + "/" + userName + "/" + userPassword);

        try {
            ConnectionInfo info = new ConnectionInfo();
            info.setName(connectionName);
            info.setUrl(url);
            info.setUser(userName);
            info.setPassword(userPassword);

            mService.addConnection(info);

            Toast
                .makeText(
                    getApplicationContext(),
                    "Connection Created",
                    Toast.LENGTH_LONG
                )
                .show();

            finish();

        } catch(Exception e) {
            Toast
                .makeText(
                        getApplicationContext(),
                        "Error while creating connection",
                        Toast.LENGTH_LONG
                )
                .show();

            Log.e(TAG, "Error while creating connection", e);
        }
    }
}
