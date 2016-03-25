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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionManagementService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    protected String TAG = this.getClass().getSimpleName();

    private List<ConnectionInfo> displayedConnections = null;

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

        Log.v(TAG, "onCreate" + Nunaliit.threadId());

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(
            broadcastReceiver,
            new IntentFilter(ConnectionManagementService.RESULT_GET_CONNECTION_INFOS)
        );
        lbm.registerReceiver(
            broadcastReceiver,
            new IntentFilter(ConnectionManagementService.ERROR_GET_CONNECTION_INFOS)
        );

        // Start CouchDb service (and keep it up indefinitely)
        {
            Intent intent = new Intent(this, CouchbaseLiteService.class);
            startService(intent);
        }

        setContentView(R.layout.activity_main);

        ListView lv = (ListView)findViewById(R.id.connnections);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            ConnectionInfo selectedConnection = null;
            if (null != displayedConnections && position < displayedConnections.size()) {
                selectedConnection = displayedConnections.get(position);
            }

            if (null != selectedConnection) {
                startConnectionActivity(selectedConnection);
            }
            }
        });

        // Request for list of connection infos
        {
            Intent intent = new Intent(this, ConnectionManagementService.class);
            intent.setAction(ConnectionManagementService.ACTION_GET_CONNECTION_INFOS);
            startService(intent);
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "onDestroy" + Nunaliit.threadId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_manage_connections:
                startConnectionListActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.v(TAG, "onStart" + Nunaliit.threadId());

        drawList();
    }

    public void drawList() {
        try {
            if( null != displayedConnections ){

                ListView listView = (ListView)findViewById(R.id.connnections);

                String[] stringArray = new String[displayedConnections.size()];
                for(int i=0,e=displayedConnections.size(); i<e; ++i){
                    ConnectionInfo connectionInfo = displayedConnections.get(i);
                    stringArray[i] = connectionInfo.toString();
                }

                ArrayAdapter<String> modeAdapter =
                        new ArrayAdapter<String>(this, R.layout.list_connection_item, stringArray);
                listView.setAdapter(modeAdapter);
            }

        } catch(Exception e) {
            Log.e(TAG, "Error displaying connection list", e);
        }
    }

    public void startConnectionActivity(ConnectionInfo connInfo){
        Intent intent = new Intent(this, EmbeddedCordovaActivity.class);

        intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connInfo.getId());

        startActivity(intent);
    }

    public void startConnectionListActivity(){
        Intent intent = new Intent(this, ConnectionListActivity.class);
        startActivity(intent);
    }

    protected void receiveBroadcast(Intent intent){
        Log.v(TAG, "Received broadcast :" + intent.getAction() + Nunaliit.threadId());

        if( ConnectionManagementService.RESULT_GET_CONNECTION_INFOS.equals(intent.getAction()) ){
            ArrayList<Parcelable> parcelables = intent.getParcelableArrayListExtra(Nunaliit.EXTRA_CONNECTION_INFOS);
            List<ConnectionInfo> connectionInfos = new Vector<ConnectionInfo>();
            for(Parcelable parcelable : parcelables){
                if( parcelable instanceof ConnectionInfo ){
                    ConnectionInfo connInfo = (ConnectionInfo)parcelable;
                    connectionInfos.add(connInfo);
                }
            }
            displayedConnections = connectionInfos;
            drawList();

        } else {
            Log.w(TAG, "Ignoring received intent :" + intent.getAction() + Nunaliit.threadId());
        }
    }
}
