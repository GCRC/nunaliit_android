package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Serializable;

import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionManagementService;

/**
 * Created by jpfiset on 3/10/16.
 */
public class AddConnectionActivity extends AppCompatActivity {

    protected String TAG = this.getClass().getName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveBroadcast(intent);
        }
    };

    private OnClickListener btnCreateListener = new OnClickListener() {
        @Override
        public void onClick(View v){
            createConnection();
        }
    };

    public String getTag() {
        return TAG;
    }

    public TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            EditText connectionTextView = findViewById(R.id.connectionName);
            EditText urlTextView = findViewById(R.id.url);
            EditText userNameTextView = findViewById(R.id.userName);
            EditText passwordTextView = findViewById(R.id.userPassword);

            boolean enabled = connectionTextView.getText().length() != 0 && urlTextView.getText().length() != 0 && userNameTextView.getText().length() != 0 &&
                    passwordTextView.getText().length() != 0;

            Button button = findViewById(R.id.button_create);
            button.setEnabled(enabled);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG,"onCreate"+Nunaliit.threadId());

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.RESULT_ADD_CONNECTION)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.ERROR_ADD_CONNECTION)
        );

        setContentView(R.layout.activity_add_connection);

        // Create button
        {
            View view = findViewById(R.id.button_create);
            if( view instanceof Button){
                Button button = (Button)view;
                button.setOnClickListener(btnCreateListener);
                button.setVisibility(View.VISIBLE);
                button.setEnabled(false);
            }
        }

        EditText connectionTextView = findViewById(R.id.connectionName);
        EditText urlTextView = findViewById(R.id.url);
        EditText userNameTextView = findViewById(R.id.userName);
        EditText passwordTextView = findViewById(R.id.userPassword);

        connectionTextView.addTextChangedListener(textWatcher);
        urlTextView.addTextChangedListener(textWatcher);
        userNameTextView.addTextChangedListener(textWatcher);
        passwordTextView.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "onDestroy" + Nunaliit.threadId());
    }

    public void createConnection(){
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

        Log.v(TAG, "Connection " + connectionName + "/" + url + "/" + userName + "/" + userPassword);

        try {
            ConnectionInfo info = new ConnectionInfo();
            info.setName(connectionName);
            info.setUrl(url);
            info.setUser(userName);
            info.setPassword(userPassword);

            Intent intent = new Intent(this, ConnectionManagementService.class);
            intent.setAction(ConnectionManagementService.ACTION_ADD_CONNECTION);
            intent.putExtra(Nunaliit.EXTRA_CONNECTION_INFO,info);
            startService(intent);

        } catch(Exception e) {
            errorOnConnection(e);
        }
    }

    public void connectionCreated(ConnectionInfo info){
        try {
            Toast
                .makeText(
                        getApplicationContext(),
                        getResources().getString(R.string.connection_created),
                        Toast.LENGTH_LONG
                )
                .show();

            finish();

        } catch(Exception e) {
            errorOnConnection(e);
        }
    }

    public void errorOnConnection(Throwable e){
        Toast
            .makeText(
                getApplicationContext(),
                getResources().getString(R.string.error_creating_connection),
                Toast.LENGTH_LONG
            )
            .show();

        Log.e(TAG, "Error while creating connection", e);
    }

    protected void receiveBroadcast(Intent intent){
        Log.v(TAG, "Received broadcast :" + intent.getAction() + Nunaliit.threadId());

        if( ConnectionManagementService.RESULT_ADD_CONNECTION.equals(intent.getAction()) ) {
            ConnectionInfo connInfo = null;
            Parcelable parcelable = intent.getParcelableExtra(Nunaliit.EXTRA_CONNECTION_INFO);
            if (parcelable instanceof ConnectionInfo) {
                connInfo = (ConnectionInfo) parcelable;
            }
            connectionCreated(connInfo);

        } else if( ConnectionManagementService.ERROR_ADD_CONNECTION.equals(intent.getAction()) ){
            Throwable e = null;
            Serializable ser = intent.getSerializableExtra(Nunaliit.EXTRA_ERROR);
            if( ser instanceof Throwable ){
                e = (Throwable)ser;
            }
            errorOnConnection(e);

        } else {
            Log.w(TAG, "Ignoring received intent :" + intent.getAction() + Nunaliit.threadId());
        }
    }
}
