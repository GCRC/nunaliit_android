package ca.carleton.gcrc.n2android_mobile1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * Created by jpfiset on 3/10/16.
 */
public class AddConnectionActivity extends AppCompatActivity {

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
                EditText editText = (EditText)editTextView;
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

        Log.i(TAG,"Connection "+connectionName+"/"+url+"/"+userName+"/"+userPassword);
    }
}
