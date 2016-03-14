package ca.carleton.gcrc.n2android_mobile1.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import ca.carleton.gcrc.n2android_mobile1.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.R;

/**
 * Created by jpfiset on 3/10/16.
 */
public class AddConnectionActivity extends ServiceBasedActivity {

    protected String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_connection);
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

            getService().addConnection(info);

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
