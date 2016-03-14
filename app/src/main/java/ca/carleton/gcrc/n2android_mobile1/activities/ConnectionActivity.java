package ca.carleton.gcrc.n2android_mobile1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import ca.carleton.gcrc.n2android_mobile1.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.CouchDbService;
import ca.carleton.gcrc.n2android_mobile1.NunaliitMobileConstants;
import ca.carleton.gcrc.n2android_mobile1.R;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionActivity extends ServiceBasedActivity {

    protected String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connection);
    }

    @Override
    protected void onStart() {
        super.onStart();

        refreshDisplay();
    }

    @Override
    public void serviceReporting(CouchDbService service) {
        refreshDisplay();
    }

    public void refreshDisplay(){
        String connectionId = null;
        Intent intent = getIntent();
        if( null != intent ){
            connectionId = intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID);
        }

        ConnectionInfo connInfo = null;
        CouchDbService service = getService();
        if( null != connectionId && null != service ){
            try {
                connInfo = service.getConnection(connectionId);
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
