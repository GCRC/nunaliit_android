package ca.carleton.gcrc.n2android_mobile1.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.apache.cordova.CordovaActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.R;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfoDb;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionManagementService;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;
import ca.carleton.gcrc.utils.AtlasPictureSingleton;

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

                Menu menu = navigationView.getMenu();
                for (int i=0; i<displayedConnections.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    ConnectionInfo connInfo = displayedConnections.get(i);
                    setAtlasInitialsIcon(item, connInfo);
                }
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
                            appView.getView().setVisibility(View.GONE);
                            findViewById(R.id.sync_progress).setVisibility(View.VISIBLE);
                            drawerLayout.closeDrawers();
                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        } else if (menuItem.getItemId() == 10001) {
                            manageMode = !manageMode;

                            Menu menu = navigationView.getMenu();
                            for (int i=0; i<displayedConnections.size(); i++) {
                                MenuItem item = menu.getItem(i);
                                if (manageMode) {
                                    item.setIcon(R.drawable.ic_trash);
                                } else {
                                    ConnectionInfo connInfo = displayedConnections.get(i);
                                    setAtlasInitialsIcon(item, connInfo);
                                }
                            }
                        } else if (menuItem.getItemId() == 10002) {
                            startAddConnectionActivity();
                        } else if (menuItem.getItemId() < 10000) {
                            ConnectionInfo newConnection = displayedConnections.get(menuItem.getItemId());
                            if (!connectionInfo.getId().equals(newConnection.getId())) {
                                startConnectionActivity(newConnection);
                            } else {
                                drawerLayout.closeDrawers();
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

    public void startConnectionActivity(ConnectionInfo connInfo){
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
}
