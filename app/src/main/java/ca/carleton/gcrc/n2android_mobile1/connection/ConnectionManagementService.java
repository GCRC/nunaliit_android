package ca.carleton.gcrc.n2android_mobile1.connection;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
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

    public static String ACTION_ADD_CONNECTION = ConnectionManagementService.class.getName()+".ADD_CONNECTION";
    public static String ACTION_GET_CONNECTION_INFO = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFO";
    public static String ACTION_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS";
    public static String ACTION_DELETE_CONNECTION = ConnectionManagementService.class.getName()+".DELETE_CONNECTION";
    public static String ACTION_SYNC = ConnectionManagementService.class.getName()+".SYNC";

    public static String RESULT_ADD_CONNECTION = ConnectionManagementService.class.getName()+".ADD_CONNECTION_RESULT";
    public static String ERROR_ADD_CONNECTION = ConnectionManagementService.class.getName()+".ADD_CONNECTION_ERROR";
    public static String RESULT_GET_CONNECTION_INFO = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFO_RESULT";
    public static String ERROR_GET_CONNECTION_INFO = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFO_ERROR";
    public static String RESULT_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS_RESULT";
    public static String ERROR_GET_CONNECTION_INFOS = ConnectionManagementService.class.getName()+".GET_CONNECTION_INFOS_ERROR";
    public static String RESULT_DELETE_CONNECTION = ConnectionManagementService.class.getName()+".DELETE_CONNECTION_RESULT";
    public static String ERROR_DELETE_CONNECTION = ConnectionManagementService.class.getName()+".DELETE_CONNECTION_ERROR";
    public static String RESULT_SYNC = ConnectionManagementService.class.getName()+".SYNC_RESULT";
    public static String ERROR_SYNC = ConnectionManagementService.class.getName()+".SYNC_ERROR";

    public static String TAG = "NunaliitMobileConnections";

    // Connection to CouchbaseLiteService
    private ServiceConnection mCouchbaseConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchbaseLiteService.CouchDbBinder binder = (CouchbaseLiteService.CouchDbBinder) service;
            reportCouchbaseService(binder.getService());

            Log.v(TAG, "Bound to CouchbaseService"+ Nunaliit.threadId());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCouchbaseBound = false;

            Log.v(TAG, "Unbound from CouchbaseService"+ Nunaliit.threadId());
        }
    };
    private CouchbaseLiteService mCouchbaseService;
    private boolean mCouchbaseBound = false;

    public ConnectionManagementService(){
        super(TAG);

        Log.v(TAG, "Constructor"+ Nunaliit.threadId());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.v(TAG, "onStartCommand: " + intent.getAction() + Nunaliit.threadId());

        return IntentService.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate"+ Nunaliit.threadId());

        // Bind to CouchbaseLiteService
        Intent intent = new Intent(this, CouchbaseLiteService.class);
        bindService(intent, mCouchbaseConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy" + Nunaliit.threadId());

        // Unbind from the service
        if (mCouchbaseBound) {
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
        Log.v(TAG, "onHandleIntent: "+intent.getAction()+ Nunaliit.threadId());

        if( ACTION_ADD_CONNECTION.equals(intent.getAction()) ) {
            ConnectionInfo connInfo = null;
            Parcelable parcelable = intent.getParcelableExtra(Nunaliit.EXTRA_CONNECTION_INFO);
            if( parcelable instanceof ConnectionInfo ){
                connInfo = (ConnectionInfo)parcelable;
            }
            addConnection(connInfo);

        } else if( ACTION_GET_CONNECTION_INFOS.equals(intent.getAction()) ){
            getConnectionInfos();

        } else if( ACTION_GET_CONNECTION_INFO.equals(intent.getAction()) ){
            String connId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            getConnectionInfo(connId);

        } else if( ACTION_DELETE_CONNECTION.equals(intent.getAction()) ){
            String connId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            deleteConnection(connId);

        } else if( ACTION_SYNC.equals(intent.getAction()) ){
            String connId = intent.getStringExtra(Nunaliit.EXTRA_CONNECTION_ID);
            synchronizeConnection(connId);
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

    private void addConnection(ConnectionInfo connInfo){
        waitForCouchbaseService();

        try {
            ConnectionProcess connectionProcess = new ConnectionProcess(mCouchbaseService, this);
            connectionProcess.addConnection(connInfo);

            Intent result = new Intent(RESULT_ADD_CONNECTION);
            result.putExtra(Nunaliit.EXTRA_CONNECTION_INFO, connInfo);
            Log.v(TAG, "Result: " + result.getAction() + Nunaliit.threadId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

            getConnectionInfos();

        } catch(Exception e) {
            Log.e(TAG, "Error while adding a connection",e);
            Intent result = new Intent(ERROR_ADD_CONNECTION);
            result.putExtra(Nunaliit.EXTRA_ERROR, e.getMessage());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }

    private void getConnectionInfo(String connId){
        waitForCouchbaseService();

        try {
            CouchbaseDb connectionsDb = mCouchbaseService.getConnectionsDb();

            JSONObject doc = connectionsDb.getDocument(connId);
            ConnectionInfo connInfo = connectionInfoFromJson(doc);

            Intent result = new Intent(RESULT_GET_CONNECTION_INFO);
            result.putExtra(Nunaliit.EXTRA_CONNECTION_INFO, connInfo);
            Log.v(TAG, "Result: " + result.getAction() + Nunaliit.threadId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

        } catch(Exception e) {
            Log.e(TAG, "Error while retrieving connection information "+connId,e);
            Intent result = new Intent(ERROR_GET_CONNECTION_INFO);
            result.putExtra(Nunaliit.EXTRA_ERROR, e);
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
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
                JSONObject doc = row.getJSONObject("doc");
                ConnectionInfo connInfo = connectionInfoFromJson(doc);
                if( null != connInfo ){
                    connectionInfos.add(connInfo);
                }
            }

            Intent result = new Intent(RESULT_GET_CONNECTION_INFOS);
            result.putParcelableArrayListExtra(Nunaliit.EXTRA_CONNECTION_INFOS, connectionInfos);
            Log.v(TAG, "Result: " + result.getAction() + Nunaliit.threadId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

        } catch(Exception e) {
            Log.e(TAG, "Error while retrieving connection information objects",e);
            Intent result = new Intent(ERROR_GET_CONNECTION_INFOS);
            result.putExtra(Nunaliit.EXTRA_ERROR, e);
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }

    private void deleteConnection(String connId){
        waitForCouchbaseService();

        try {
            ConnectionProcess connectionProcess = new ConnectionProcess(mCouchbaseService, this);
            connectionProcess.deleteConnection(connId);

            Intent result = new Intent(RESULT_DELETE_CONNECTION);
            result.putExtra(Nunaliit.EXTRA_CONNECTION_ID, connId);
            Log.v(TAG, "Result: " + result.getAction() + Nunaliit.threadId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

            getConnectionInfos();

        } catch(Exception e) {
            Log.e(TAG, "Error while deleting a connection",e);
            Intent result = new Intent(ERROR_DELETE_CONNECTION);
            result.putExtra(Nunaliit.EXTRA_ERROR, e.getMessage());
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }

    private void synchronizeConnection(String connId){
        waitForCouchbaseService();

        try {
            CouchbaseDb connectionsDb = mCouchbaseService.getConnectionsDb();

            JSONObject doc = connectionsDb.getDocument(connId);
            ConnectionInfo connInfo = connectionInfoFromJson(doc);
            Connection connection = new Connection(connInfo);

            ConnectionSyncProcess syncProcess = new ConnectionSyncProcess(mCouchbaseService, connection);
            syncProcess.synchronize();

            Intent result = new Intent(RESULT_SYNC);
            Log.v(TAG, "Result: " + result.getAction() + Nunaliit.threadId());

            LocalBroadcastManager.getInstance(this).sendBroadcast(result);

        } catch(Exception e) {
            Log.e(TAG, "Error while synchronizing connection "+connId,e);
            Intent result = new Intent(ERROR_SYNC);
            result.putExtra(Nunaliit.EXTRA_ERROR, e);
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
    }
}
