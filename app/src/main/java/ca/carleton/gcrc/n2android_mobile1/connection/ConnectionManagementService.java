package ca.carleton.gcrc.n2android_mobile1.connection;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.couch.client.CouchQueryResults;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.NunaliitMobileConstants;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQuery;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQueryResults;

/**
 * Created by jpfiset on 3/22/16.
 */
public class ConnectionManagementService extends IntentService {

    public static ConnectionInfo connectionInfoFromJson(JSONObject jsonObject){
        ConnectionInfo info = null;

        JSONObject connInfo = jsonObject.optJSONObject("mobile_connection");
        if( null != connInfo ) {
            info = new ConnectionInfo();

            // id
            {
                String id = jsonObject.optString("_id", null);
                if( id != null ) {
                    info.setId(id);
                }
            }

            // name
            {
                String name = connInfo.optString("name", null);
                if( name != null ) {
                    info.setName(name);
                }
            }

            // url
            {
                String url = connInfo.optString("url", null);
                if( url != null ) {
                    info.setUrl(url);
                }
            }

            // user
            {
                String user = connInfo.optString("user", null);
                if( user != null ) {
                    info.setUser(user);
                }
            }

            // password
            {
                String password = connInfo.optString("password", null);
                if (password != null ) {
                    info.setPassword(password);
                }
            }

            // local docs db
            {
                String localDocsDb = connInfo.optString("localDocsDb", null);
                if( localDocsDb != null ){
                    info.setLocalDocumentDbName(localDocsDb);
                }
            }
        }

        return info;
    }

    public static String ACTION_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS";
    public static String ACTION_SYNC = ConnectionManagementService.class.getName()+".SYNC";

    public static String RESULT_SYNC_COMPLETED = ConnectionManagementService.class.getName()+".SYNC_COMPLETED";
    public static String RESULT_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS_RESULT";
    public static String ERROR_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS_ERROR";

    public static String TAG = "NunaliitMobileConnections";

    // Connection to CouchbaseLiteService
    private ServiceConnection mCouchbaseConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchbaseLiteService.CouchDbBinder binder = (CouchbaseLiteService.CouchDbBinder) service;
            reportCouchbaseService(binder.getService());

            Log.v(TAG, "Bound to CouchbaseService"+NunaliitMobileConstants.threadId());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCouchbaseBound = false;

            Log.v(TAG, "Unbound from CouchbaseService"+NunaliitMobileConstants.threadId());
        }
    };
    private CouchbaseLiteService mCouchbaseService;
    private boolean mCouchbaseBound = false;

    public ConnectionManagementService(){
        super(TAG);

        Log.v(TAG, "Created Service"+NunaliitMobileConstants.threadId());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.v(TAG, "onStartCommand: " + intent.getAction()+NunaliitMobileConstants.threadId());

        return IntentService.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Request binding to CouchbaseService"+NunaliitMobileConstants.threadId());

        // Bind to CouchbaseLiteService
        Intent intent = new Intent(this, CouchbaseLiteService.class);
        bindService(intent, mCouchbaseConnection, Context.BIND_AUTO_CREATE);

        try {

        } catch(Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        // Unbind from the service
        if (mCouchbaseBound) {
            Log.v(TAG, "Request unbinding from CouchbaseService"+NunaliitMobileConstants.threadId());

            unbindService(mCouchbaseConnection);
            mCouchbaseBound = false;
        }

        super.onDestroy();
    }

    public CouchbaseLiteService getCouchbaseService() {
        return mCouchbaseService;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "Intent received: "+intent.getAction()+NunaliitMobileConstants.threadId());

        if( ACTION_GET_CONNECTION_INFOS.equals(intent.getAction()) ){
            getConnectionInfos();

        } else if( ACTION_SYNC.equals(intent.getAction()) ){
            Log.v(TAG, "Action Synchronize Requested: " + intent.getStringExtra(NunaliitMobileConstants.EXTRA_CONNECTION_ID) + NunaliitMobileConstants.threadId());
            waitForCouchbaseService();

            Intent result = new Intent(RESULT_SYNC_COMPLETED);
            Log.v(TAG, "Action Synchronize Result: " + result.getAction() + NunaliitMobileConstants.threadId());

            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }

    protected void reportCouchbaseService(CouchbaseLiteService service){
        if( null != service ){
            synchronized (this){
                mCouchbaseBound = true;
                mCouchbaseService = service;
                this.notifyAll();
            }
        }
    }

    private void waitForCouchbaseService(){
        boolean interrupted = false;

        synchronized (this){
            while( !mCouchbaseBound && !interrupted ){
                try {
                    wait();
                } catch(InterruptedException e) {
                    interrupted = true;
                }
            }
        }
    }

    private void getConnectionInfos(){
        waitForCouchbaseService();

        try {
            CouchbaseDb connectionsDb = mCouchbaseService.getConnectionsDb();

            CouchbaseQuery query = new CouchbaseQuery();
            query.setViewName(CouchbaseLiteService.VIEW_CONNECTIONS);
            query.setIncludeDocs(true);
            CouchbaseQueryResults results = connectionsDb.performQuery(query);

            ArrayList<ConnectionInfo> connectionInfos = new ArrayList<ConnectionInfo>();
            for(JSONObject row : results.getRows()) {
                Log.v(TAG,"Reported row ");
                JSONObject doc = row.getJSONObject("doc");
                Log.v(TAG,"Reported row doc "+doc);
                ConnectionInfo connInfo = connectionInfoFromJson(doc);
                if( null != connInfo ){
                    Log.v(TAG,"Reported conn "+connInfo);
                    connectionInfos.add(connInfo);
                }
            }

            Log.v(TAG,"Number of connection objects: "+connectionInfos.size());

            Intent result = new Intent(RESULT_GET_CONNECTION_INFOS);
            result.putParcelableArrayListExtra(NunaliitMobileConstants.EXTRA_CONNECTION_INFOS,connectionInfos);
            Log.v(TAG, "Action Synchronize Result: " + result.getAction() + NunaliitMobileConstants.threadId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

        } catch(Exception e) {
            Log.e(TAG, "Error while retrieving connection information objects",e);
            Intent result = new Intent(ERROR_GET_CONNECTION_INFOS);
            result.putExtra(NunaliitMobileConstants.EXTRA_ERROR, e.getMessage());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }
}
