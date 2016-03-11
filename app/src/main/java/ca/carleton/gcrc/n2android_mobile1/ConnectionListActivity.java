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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionListActivity extends AppCompatActivity {

    protected String TAG = this.getClass().getSimpleName();

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchDbService.CouchDbBinder binder = (CouchDbService.CouchDbBinder) service;
            mService = binder.getService();
            mBound = true;
            drawList();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private CouchDbService mService;
    private boolean mBound = false;
    private List<ConnectionInfo> displayedConnections = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_connections);

        ListView lv = (ListView)findViewById(R.id.connnections);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ConnectionInfo selectedConnection = null;
                if( null != displayedConnections && position < displayedConnections.size()){
                    selectedConnection = displayedConnections.get(position);
                }

                if( null != selectedConnection ){
                    startConnectionActivity(selectedConnection);

                    Toast
                        .makeText(
                            getApplicationContext(),
                            "You selected : " + selectedConnection.toString(),
                            Toast.LENGTH_SHORT
                        )
                        .show();
                }
            }
        });

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

        drawList();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void startAddConnectionActivity(View view){
        Intent intent = new Intent(this, AddConnectionActivity.class);
        startActivity(intent);
    }

    public void drawList() {
        try {
            if( null != mService ){
                List<ConnectionInfo> connectionInfos = mService.getConnections();
                ListView listView = (ListView)findViewById(R.id.connnections);

                String[] stringArray = new String[connectionInfos.size()];
                for(int i=0,e=connectionInfos.size(); i<e; ++i){
                    ConnectionInfo connectionInfo = connectionInfos.get(i);
                    stringArray[i] = connectionInfo.toString();
                }

                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, R.layout.list_connection_item, stringArray);
                listView.setAdapter(modeAdapter);

                displayedConnections = connectionInfos;
            }

        } catch(Exception e) {
            Log.e(TAG, "Error obtaining connection list", e);
        }
    }

    public void startConnectionActivity(ConnectionInfo connInfo){
        Intent intent = new Intent(this, ConnectionActivity.class);

        intent.putExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID, connInfo.getId());

        startActivity(intent);
    }
}
