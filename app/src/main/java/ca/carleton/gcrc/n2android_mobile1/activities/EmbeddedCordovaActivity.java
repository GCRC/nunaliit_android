package ca.carleton.gcrc.n2android_mobile1.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.cordova.CordovaActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfoDb;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionManagementService;
import ca.carleton.gcrc.n2android_mobile1.cordova.CordovaNunaliitPlugin;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;
import ca.carleton.gcrc.utils.AtlasPictureSingleton;
import okhttp3.HttpUrl;

/**
 * Created by jpfiset on 3/9/16.
 */
public class EmbeddedCordovaActivity extends CordovaActivity {

    final protected String TAG = this.getClass().getSimpleName();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<ConnectionInfo> displayedConnections = null;

    private boolean manageMode = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchbaseLiteService.CouchDbBinder binder = (CouchbaseLiteService.CouchDbBinder) service;
            mService = binder.getService();
            mBound = true;
            serviceReporting(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private CouchbaseLiteService mService;
    private boolean mBound = false;
    ConnectionInfo connectionInfo = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveBroadcast(intent);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Bind to CouchbaseLiteService
        Intent intent = new Intent(this, CouchbaseLiteService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                EmbeddedCordovaActivity.this.manageMode = false;

                updateMenuItems();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {}
            @Override
            public void onDrawerOpened(View drawerView) {}
            @Override
            public void onDrawerClosed(View drawerView) {}
        });

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        if (menuItem.getItemId() == 10000) {
                            synchronizeConnection(connectionInfo);
                            showProgressBar();
                        } else if (menuItem.getItemId() == 10001) {
                            manageMode = !manageMode;

                            updateMenuItems();
                        } else if (menuItem.getItemId() == 10002) {
                            showAddAtlasDialog();
                        } else if (menuItem.getItemId() < 10000) {
                            final ConnectionInfo newConnection = displayedConnections.get(menuItem.getItemId());

                            if (!manageMode) {
                                if (!connectionInfo.getId().equals(newConnection.getId())) {
                                    startConnectionActivity(newConnection);
                                } else {
                                    drawerLayout.closeDrawers();
                                    return true;
                                }
                            } else {
                                Log.d(TAG, "Delete Atlas");

                                showDeleteAtlasDialog(newConnection);
                            }
                        }

                        return true;
                    }
                });

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
                new IntentFilter(ConnectionManagementService.RESULT_SYNC)
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
                new IntentFilter(ConnectionManagementService.RESULT_ADD_CONNECTION)
        );
        lbm.registerReceiver(
                broadcastReceiver,
                new IntentFilter(ConnectionManagementService.ERROR_ADD_CONNECTION)
        );

        // Request for list of connection infos
        {
            Intent connectionIntent = new Intent(this, ConnectionManagementService.class);
            connectionIntent.setAction(ConnectionManagementService.ACTION_GET_CONNECTION_INFOS);
            startService(connectionIntent);
        }
    }

    @Override
    public void onDestroy() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    @SuppressLint("ResourceType")
    @Override
    protected void createViews() {
        //Why are we setting a constant as the ID? This should be investigated
        appView.getView().setId(100);
        appView.getView().setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(R.layout.activity_cordova);
        ViewGroup layout = findViewById(R.id.content_container);
        layout.addView(appView.getView(), 0);

        retrieveConnection();

        if (preferences.contains("BackgroundColor")) {
            try {
                int backgroundColor = preferences.getInteger("BackgroundColor", Color.BLACK);
                // Background of activity:
                appView.getView().setBackgroundColor(backgroundColor);
            }
            catch (NumberFormatException e){
                e.printStackTrace();
            }
        }

        appView.getView().requestFocusFromTouch();
    }

    public void serviceReporting(CouchbaseLiteService service){
        retrieveConnection();
    }

    public ConnectionInfo retrieveConnection(){
        if( null == connectionInfo ){
            String connectionId = null;
            Intent intent = getIntent();
            if( null != intent ){
                connectionId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            }

            if( null != mService && null != connectionId ){
                try {
                    CouchbaseManager couchbaseMgr = mService.getCouchbaseManager();
                    ConnectionInfoDb infoDb = couchbaseMgr.getConnectionsDb();
                    connectionInfo = infoDb.getConnectionInfo(connectionId);

                } catch(Exception e) {
                    Log.e(TAG,"Unable to retrieve connection info",e);
                }
            }
        }

        configureUI();

        return connectionInfo;
    }

    private void configureUI () {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView atlasNameTextView = findViewById(R.id.atlas_title);
                TextView atlasNameTextUrl = findViewById(R.id.atlas_url);
                Toolbar toolbar = findViewById(R.id.cordova_toolbar);

                if (connectionInfo != null && atlasNameTextView != null && atlasNameTextUrl != null && toolbar != null) {
                    atlasNameTextView.setText(connectionInfo.getName());
                    atlasNameTextUrl.setText(connectionInfo.getUrl());

                    toolbar.setTitle(connectionInfo.getName());
                    toolbar.setTitleTextColor(Color.WHITE);

                    toolbar.setNavigationIcon(R.drawable.ic_menu);
                    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!drawerLayout.isDrawerOpen(navigationView) &&
                                    drawerLayout.getDrawerLockMode(navigationView) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                                drawerLayout.openDrawer(navigationView);
                            } else {
                                drawerLayout.closeDrawer(navigationView);
                            }
                        }
                    });
                }
            }
        });
    }

    private void updateMenuItems() {
        Menu menu = navigationView.getMenu();
        int [] colorIdArray = getResources().getIntArray(R.array.atlas_icon_id_list);

        for (int i=0; i<displayedConnections.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (manageMode) {
                item.setIcon(R.drawable.ic_trash);
            } else {
                item.setChecked(displayedConnections.get(i).getId().equals(connectionInfo.getId()));
                ConnectionInfo connInfo = displayedConnections.get(i);
                setAtlasInitialsIcon(item, connInfo);

                Drawable menuIcon = item.getIcon();
                menuIcon.mutate();
                menuIcon.setColorFilter(colorIdArray[i % colorIdArray.length], PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private void showProgressBar() {
        appView.getView().setVisibility(View.GONE);
        findViewById(R.id.sync_progress).setVisibility(View.VISIBLE);
        drawerLayout.closeDrawers();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void hideProgressBar() {
        appView.getView().setVisibility(View.VISIBLE);
        findViewById(R.id.sync_progress).setVisibility(View.GONE);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public void onCreateDocument(View view) {
        // Call create document through the Cordova bridge
        CordovaNunaliitPlugin.javascriptEventCallback("onCreateDocument");
    }

    public CouchbaseLiteService getCouchDbService(){
        return mService;
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

            retrieveConnection();

        } else if (ConnectionManagementService.RESULT_SYNC.equals(intent.getAction())) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navigationView);
            startConnectionActivity(connectionInfo);
        } else if( ConnectionManagementService.RESULT_DELETE_CONNECTION.equals(intent.getAction()) ){
            String connectionId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            if (connectionId.equals(connectionInfo.getId())) {
                // If there is another connection other than the old connection
                if (displayedConnections.size() > 1) {
                    ConnectionInfo newConnection = displayedConnections.get(0);
                    if (newConnection.getId().equals(connectionId)) {
                        newConnection = displayedConnections.get(1);
                    }
                    startConnectionActivity(newConnection);
                } else {
                    startMainActivity();
                }
            } else {
                startConnectionActivity(connectionInfo);
            }
        } else if (ConnectionManagementService.ERROR_DELETE_CONNECTION.equals(intent.getAction())) {
            hideProgressBar();
        } if (ConnectionManagementService.RESULT_ADD_CONNECTION.equals(intent.getAction()) ) {
            hideProgressBar();

            ConnectionInfo connInfo = null;
            Parcelable parcelable = intent.getParcelableExtra(Nunaliit.EXTRA_CONNECTION_INFO);
            if (parcelable instanceof ConnectionInfo) {
                connInfo = (ConnectionInfo) parcelable;
            }
            connectionCreated(connInfo);

        } else if (ConnectionManagementService.ERROR_ADD_CONNECTION.equals(intent.getAction()) ) {
            hideProgressBar();

            Throwable e = null;
            Serializable ser = intent.getSerializableExtra(Nunaliit.EXTRA_ERROR);
            if (ser instanceof Throwable) {
                e = (Throwable) ser;
            }
            errorOnConnection(e);
        } else {
            Log.w(TAG, "Ignoring received intent :" + intent.getAction() + Nunaliit.threadId());
        }
    }

    private void drawList() {
        if( null != displayedConnections ) {
            for (int i = 0, e = displayedConnections.size(); i < e; ++i) {
                ConnectionInfo connectionInfo = displayedConnections.get(i);
                MenuItem menuItem = navigationView.getMenu().add(Menu.NONE, i, i, connectionInfo.getName());

                ConnectionInfo connInfo = displayedConnections.get(i);
                setAtlasInitialsIcon(menuItem, connInfo);
            }

            MenuItem synchronizeAtlasMenuItem = navigationView.getMenu().add(Menu.NONE, 10000, 10000, "Synchronize Atlas");
            synchronizeAtlasMenuItem.setIcon(R.drawable.ic_synchronize);
            MenuItem manageAtlasMenuItem = navigationView.getMenu().add(Menu.NONE, 10001, 10001, "Manage Atlas");
            manageAtlasMenuItem.setIcon(R.drawable.ic_manage);
            MenuItem addAtlasMenuItem = navigationView.getMenu().add(Menu.NONE, 10002, 10002, "Add Atlas");
        }
    }

    private void synchronizeConnection(ConnectionInfo connInfo) {
        Intent syncIntent = new Intent(this, ConnectionManagementService.class);
        syncIntent.setAction(ConnectionManagementService.ACTION_SYNC);
        Log.v(TAG, "action:" + syncIntent.getAction() + Nunaliit.threadId());
        syncIntent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connInfo.getId());
        startService(syncIntent);
    }

    public void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void deleteConnection(ConnectionInfo connInfo) {
        Intent intent = new Intent(this, ConnectionManagementService.class);
        intent.setAction(ConnectionManagementService.ACTION_DELETE_CONNECTION);
        Log.v(TAG, "action:" + intent.getAction() + Nunaliit.threadId());
        intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connInfo.getId());
        startService(intent);
    }

    public void startConnectionActivity(ConnectionInfo connInfo){
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.atlas_shared_pref), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.atlas_last_used), connInfo.getId());
        editor.apply();

        Intent intent = new Intent(this, EmbeddedCordovaActivity.class);
        intent.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connInfo.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setAtlasInitialsIcon(MenuItem menuItem, ConnectionInfo connInfo) {
        String name = connInfo.getName().length() > 0 ? "" + connInfo.getName().charAt(0) : "";
        Bitmap atlasInitial = AtlasPictureSingleton.getInstance().getUserInitialsBitmap(name, 48, this);

        Drawable drawable = new BitmapDrawable(getResources(), atlasInitial);

        menuItem.setIcon(drawable);
    }

    public void startAddConnectionActivity(){
        Intent intent = new Intent(this, AddConnectionActivity.class);
        startActivity(intent);
    }

    private void showAddAtlasDialog() {
        LayoutInflater inflater = this.getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(EmbeddedCordovaActivity.this);

        builder.setTitle(R.string.add_atlas_title);
        builder.setView(inflater.inflate(R.layout.dialog_add_atlas, null));
        builder.setPositiveButton(R.string.add_atlas_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText urlEditText = ((Dialog)dialogInterface).findViewById(R.id.url);
                EditText userNameEditText = ((Dialog)dialogInterface).findViewById(R.id.userName);
                EditText userPasswordEditText = ((Dialog)dialogInterface).findViewById(R.id.userPassword);

                HttpUrl url = HttpUrl.parse(urlEditText.getText().toString());
                String host = url.host();

                createConnection(
                        host,
                        urlEditText.getText().toString(),
                        userNameEditText.getText().toString(),
                        userPasswordEditText.getText().toString()
                );
            }
        });
        builder.setNegativeButton(R.string.add_atlas_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void showDeleteAtlasDialog(final ConnectionInfo newConnection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(EmbeddedCordovaActivity.this);

        builder.setTitle(R.string.delete_atlas_title);
        builder.setMessage(R.string.delete_atlas_message);
        builder.setPositiveButton(R.string.delete_atlas_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteConnection(newConnection);
                showProgressBar();
            }
        });
        builder.setNegativeButton(R.string.delete_atlas_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
        });

        dialog.show();
    }

    public void createConnection(String connectionName, String url, String userName, String userPassword) {
        Log.v(TAG, "Connection " + connectionName + "/" + url + "/" + userName + "/" + userPassword);

        try {
            showProgressBar();

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
            hideProgressBar();

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
