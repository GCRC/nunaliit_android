package ca.carleton.gcrc.n2android_mobile1.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import ca.carleton.gcrc.n2android_mobile1.connection.AddConnectionThread;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.R;

/**
 * Created by jpfiset on 3/10/16.
 */
public class AddConnectionActivity extends ServiceBasedActivity {

    protected String TAG = this.getClass().getName();

    private AddConnectionThread addConnectionThread = null;
    private Handler handler = null;

    public String getTag() {
        return TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_connection);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message inputMessage) {
                int code = inputMessage.what;

                Log.i(TAG,"Message received: "+code);

                switch(code){
                    case AddConnectionThread.CONNECTION_CREATED:
                        ConnectionInfo info = null;
                        if( null != inputMessage.obj && inputMessage.obj instanceof ConnectionInfo ){
                            info = (ConnectionInfo)inputMessage.obj;
                        }
                        connectionCreated(info);
                        break;

                    case AddConnectionThread.CONNECTION_CREATION_ERROR:
                        Throwable e = null;
                        if( null != inputMessage.obj && inputMessage.obj instanceof Throwable ){
                            e = (Throwable)inputMessage.obj;
                        }
                        errorOnConnection(e);
                        break;

                    default:
                        Log.e(TAG, "Unrecognized message: "+code);
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        if( null != addConnectionThread ){
            addConnectionThread.stopTesting();
            addConnectionThread = null;
        }

        super.onDestroy();
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

            if( null != addConnectionThread ){
                addConnectionThread.stopTesting();
            }
            addConnectionThread = new AddConnectionThread(getCouchbaseService(), info, handler);
            addConnectionThread.start();

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
}
