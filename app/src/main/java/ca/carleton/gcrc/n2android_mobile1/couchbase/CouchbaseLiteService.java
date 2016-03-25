package ca.carleton.gcrc.n2android_mobile1.couchbase;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;

/**
 * Created by jpfiset on 3/10/16.
 */
public class CouchbaseLiteService extends Service {
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class CouchDbBinder extends Binder {
        public CouchbaseLiteService getService() {
            return CouchbaseLiteService.this;
        }
    }

    public static String TAG = "NunaliitMobileDatabase";

    // constants
    public static final String DATABASE_NAME = "connections";
    public static final String VIEW_CONNECTIONS = "connections-by-id";

    // couchdb internals
    protected static Manager manager;
    private Database database;
    private LiveQuery liveQuery;

    // Binder given to clients
    private final IBinder mBinder = new CouchDbBinder();

    public CouchbaseLiteService(){
        Log.v(TAG, "Constructor"+ Nunaliit.threadId());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate" + Nunaliit.threadId());

        try {
            startCouchDb();

            Log.i(TAG, "CouchDB Initialized");

        } catch(Exception e) {
            Toast
                    .makeText(getApplicationContext(), "Error Initializing CouchDB, see logs for details", Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "Error initializing CouchDB", e);
        }
    }

    @Override
    public void onDestroy() {

        Log.v(TAG, "onDestroy" + Nunaliit.threadId());

        if(manager != null) {
            manager.close();
            manager = null;

            Log.i(TAG, "CouchDB Finalized");
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind" + Nunaliit.threadId());

        return mBinder;
    }

    protected void startCouchDb() throws Exception {

        Manager.enableLogging(TAG, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG,
            com.couchbase.lite.util.Log.VERBOSE
        );
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG_SYNC_ASYNC_TASK,
            com.couchbase.lite.util.Log.VERBOSE
        );
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG_SYNC,
            com.couchbase.lite.util.Log.VERBOSE
        );
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG_QUERY,
            com.couchbase.lite.util.Log.VERBOSE
        );
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG_VIEW,
            com.couchbase.lite.util.Log.VERBOSE
        );
        Manager.enableLogging(
            com.couchbase.lite.util.Log.TAG_DATABASE,
            com.couchbase.lite.util.Log.VERBOSE
        );

        manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);

        // install a view definition needed by the application
        DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        database = manager.openDatabase(DATABASE_NAME, options);

//        Document doc = database.getDocument("testDoc");
//        SavedRevision currentRevision = doc.getCurrentRevision();
//        if( null == currentRevision ){
//            Log.i(TAG, "testDoc does not exist");
//            Map<String,Object> props = new HashMap<String,Object>();
//            props.put("nunaliit_test","allo");
//            doc.putProperties(props);
//        } else {
//            Log.i(TAG, "testDoc revision: "+currentRevision.getProperties().get("_rev"));
//        }

        // View: connnections-by-label
        com.couchbase.lite.View connectionsView = database.getView(VIEW_CONNECTIONS);
        connectionsView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object connInfoObj = document.get("mobile_connection");
                if( null != connInfoObj && connInfoObj instanceof Map ){
                    Object idObj = document.get("_id");
                    if( null != idObj && idObj instanceof String ){
                        String id = (String)idObj;
                        emitter.emit(id,null);
                    }
                }
            }
        }, "1.0");
//
//        initItemListAdapter();
//
//        startLiveQuery(viewItemsByDate);
//
//        startSync();

    }

    public boolean databaseExists(String dbName) {
        boolean exists = false;

        try {
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(false);
            Database db = manager.openDatabase(dbName, options);
            if( null != db ){
                exists = db.exists();
                db.close();
            }

        } catch(Exception e) {
            Log.e(TAG, "Error checking if database exists: "+dbName, e);
        }
        return exists;
    }

    public CouchbaseDb getConnectionsDb(){
        return new CouchbaseDb(database);
    }

    public void createLocalDatabase(ConnectionInfo connectionInfo){
        int count = 0;
        boolean created = false;
        while( !created && count < 100 ){
            ++count;
            String name = "docs_"+count;
            if( false == databaseExists(name) ){
                try {
                    DatabaseOptions options = new DatabaseOptions();
                    options.setCreate(true);
                    Database db = manager.openDatabase(name, options);
                    if( false == db.exists() ){
                        throw new Exception("Unable to create database: "+name);
                    }
                    db.close();

                    created = true;
                    connectionInfo.setLocalDocumentDbName(name);
                    Log.i(TAG, "Created local database: "+name);

                } catch(Exception e) {
                    Log.e(TAG, "Error creating database: "+name, e);
                }
            }
        }
    }

    public void deleteDb(String dbName) throws Exception {
        try {
            Database db = manager.getDatabase(dbName);
            db.delete();

        } catch(Exception e) {
            throw new Exception("Error while deleting database "+dbName,e);
        }
    }

    public void addConnectionInfo(ConnectionInfo connectionInfo) throws Exception {

        try {
            Document doc = database.createDocument();

            Map<String, Object> connInfo = new HashMap<String, Object>();
            connInfo.put("name", connectionInfo.getName());
            connInfo.put("url", connectionInfo.getUrl());
            connInfo.put("user", connectionInfo.getUser());
            connInfo.put("password", connectionInfo.getPassword());
            connInfo.put("localDocsDb", connectionInfo.getLocalDocumentDbName());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("mobile_connection", connInfo);
            doc.putProperties(props);

        } catch(Exception e) {
            throw new Exception("Unable to save connection information",e);
        }
    }

    public ConnectionInfo getConnectionInfo(String id) throws Exception {

        try {
            Document doc = database.getDocument(id);
            Map<String,Object> props = doc.getProperties();
            ConnectionInfo info = getConnectionInfoFromProps(props);

            return info;

        } catch(Exception e) {
            throw new Exception("Unable to load connection information for id: "+id,e);
        }
    }

    public List<ConnectionInfo> getConnectionInfos() throws Exception {

        try {
            List<ConnectionInfo> infos = new Vector<ConnectionInfo>();

            Query query = database.getView(VIEW_CONNECTIONS).createQuery();

            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();

                Document connInfoDoc = row.getDocument();
                Map<String,Object> props = connInfoDoc.getProperties();
                ConnectionInfo info = getConnectionInfoFromProps(props);

                if( null != info ) {
                    infos.add(info);
                }
            }

            return infos;

        } catch(Exception e) {
            throw new Exception("Unable to load connection information documents",e);
        }
    }

    private ConnectionInfo getConnectionInfoFromProps(Map<String,Object> props) {
        ConnectionInfo info = null;

        Object connInfoObj = props.get("mobile_connection");
        if (null != connInfoObj && connInfoObj instanceof Map) {
            Map<String, Object> connInfo = (Map<String, Object>) connInfoObj;

            info = new ConnectionInfo();

            // id
            {
                Object idObj = props.get("_id");
                if (idObj != null && idObj instanceof String) {
                    String id = (String) idObj;
                    info.setId(id);
                }
            }

            // name
            {
                Object nameObj = connInfo.get("name");
                if (nameObj != null && nameObj instanceof String) {
                    String name = (String) nameObj;
                    info.setName(name);
                }
            }

            // url
            {
                Object urlObj = connInfo.get("url");
                if (urlObj != null && urlObj instanceof String) {
                    String url = (String) urlObj;
                    info.setUrl(url);
                }
            }

            // user
            {
                Object userObj = connInfo.get("user");
                if (userObj != null && userObj instanceof String) {
                    String user = (String) userObj;
                    info.setUser(user);
                }
            }

            // password
            {
                Object passwordObj = connInfo.get("password");
                if (passwordObj != null && passwordObj instanceof String) {
                    String password = (String) passwordObj;
                    info.setPassword(password);
                }
            }

            // local docs db
            {
                Object localDocsDbObj = connInfo.get("localDocsDb");
                if( localDocsDbObj != null && localDocsDbObj instanceof String ){
                    String localDocsDb = (String)localDocsDbObj;
                    info.setLocalDocumentDbName(localDocsDb);
                }
            }
        }

        return info;
    }
}
