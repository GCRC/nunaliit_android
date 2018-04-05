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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.Serializable;
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

    public String getTag() {
        return TAG;
    }

    private List<ConnectionInfo> displayedConnections = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveBroadcast(intent);
        }
    };

    public TextWatcher userInputTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            setConnectionButtonState();
        }
    };

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
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.RESULT_ADD_CONNECTION)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.ERROR_ADD_CONNECTION)
        );

        // Start CouchDb service (and keep it up indefinitely)
        {
            Intent intent = new Intent(this, CouchbaseLiteService.class);
            startService(intent);
        }

        setContentView(R.layout.activity_first_user);

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

    public void setUpListView() {
        ListView lv = findViewById(R.id.connnections);
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

    protected void setUpFirstUserView() {
        // Create button
        {
            Button view = findViewById(R.id.button_create);
            View spinner = findViewById(R.id.spinner);
            Button button = (Button)view;
            button.setOnClickListener(btnCreateListener);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(false);

            spinner.setVisibility(View.GONE);
        }

        EditText urlTextView = findViewById(R.id.url);
        EditText userNameTextView = findViewById(R.id.userName);
        EditText passwordTextView = findViewById(R.id.userPassword);

        urlTextView.addTextChangedListener(userInputTextWatcher);
        userNameTextView.addTextChangedListener(userInputTextWatcher);
        passwordTextView.addTextChangedListener(userInputTextWatcher);

        setConnectionButtonState();
    }

    protected void setConnectionButtonState() {
        EditText urlTextView = findViewById(R.id.url);
        EditText userNameTextView = findViewById(R.id.userName);
        EditText passwordTextView = findViewById(R.id.userPassword);

        boolean enabled = urlTextView.getText().length() != 0 && userNameTextView.getText().length() != 0 &&
                passwordTextView.getText().length() != 0;

        Button button = findViewById(R.id.button_create);
        button.setEnabled(enabled);
    }

    public void startConnectionActivity(ConnectionInfo connInfo){
        Intent intent = new Intent(this, EmbeddedCordovaActivity.class);

        intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connInfo.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }

    public void startConnectionListActivity(){
        Intent intent = new Intent(this, ConnectionListActivity.class);
        startActivity(intent);
    }

    private View.OnClickListener btnCreateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v){
            createConnection();
        }
    };

    public void createConnection(){

        findViewById(R.id.button_create).setVisibility(View.GONE);
        findViewById(R.id.spinner).setVisibility(View.VISIBLE);

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

        Log.v(TAG, "Connection " + "/" + url + "/" + userName + "/" + userPassword);

        try {
            ConnectionInfo info = new ConnectionInfo();
            info.setName("Atlas Name");
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

            startConnectionActivity(info);

            findViewById(R.id.button_create).setVisibility(View.VISIBLE);
            findViewById(R.id.spinner).setVisibility(View.GONE);

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


        findViewById(R.id.button_create).setVisibility(View.VISIBLE);
        findViewById(R.id.spinner).setVisibility(View.GONE);

        Log.e(TAG, "Error while creating connection", e);
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

            if (displayedConnections.size() == 0) {
                setContentView(R.layout.activity_first_user);
                setUpFirstUserView();
            } else {
                setContentView(R.layout.activity_main);
                setUpListView();
                drawList();
            }
        } else if( ConnectionManagementService.RESULT_ADD_CONNECTION.equals(intent.getAction()) ) {
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
