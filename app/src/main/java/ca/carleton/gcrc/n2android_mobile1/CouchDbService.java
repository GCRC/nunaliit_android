package ca.carleton.gcrc.n2android_mobile1;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
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
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by jpfiset on 3/10/16.
 */
public class CouchDbService extends Service {
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class CouchDbBinder extends Binder {
        CouchDbService getService() {
            return CouchDbService.this;
        }
    }

    public static String TAG = "NunaliitMobile";

    // constants
    public static final String DATABASE_NAME = "connections";
    public static final String VIEW_CONNECTIONS = "connections-by-id";

    // couchdb internals
    protected static Manager manager;
    private Database database;
    private LiveQuery liveQuery;

    // Binder given to clients
    private final IBinder mBinder = new CouchDbBinder();


    @Override
    public void onCreate() {
        super.onCreate();

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
        if(manager != null) {
            manager.close();
            manager = null;

            Log.i(TAG, "CouchDB Finalized");
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void startCouchDb() throws Exception {

        Manager.enableLogging(TAG, Log.VERBOSE);
        Manager.enableLogging(Log.TAG, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
        Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);

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

    public void addConnection(ConnectionInfo connectionInfo) throws Exception {

        try {
            Document doc = database.createDocument();

            Map<String, Object> connInfo = new HashMap<String, Object>();
            connInfo.put("name", connectionInfo.getName());
            connInfo.put("url", connectionInfo.getUrl());
            connInfo.put("user", connectionInfo.getUser());
            connInfo.put("password", connectionInfo.getPassword());

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("mobile_connection", connInfo);
            doc.putProperties(props);

        } catch(Exception e) {
            throw new Exception("Unable to save connection information",e);
        }
    }

    public List<ConnectionInfo> getConnections() throws Exception {

        try {
            List<ConnectionInfo> infos = new Vector<ConnectionInfo>();

            Query query = database.getView(VIEW_CONNECTIONS).createQuery();

            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();

                Document connInfoDoc = row.getDocument();
                Map<String,Object> props = connInfoDoc.getProperties();
                Object connInfoObj = props.get("mobile_connection");
                if( null != connInfoObj && connInfoObj instanceof Map ){
                    Map<String,Object> connInfo = (Map<String,Object>)connInfoObj;

                    ConnectionInfo info = new ConnectionInfo();

                    // name
                    {
                        Object nameObj = connInfo.get("name");
                        if( nameObj != null && nameObj instanceof String ){
                            String name = (String)nameObj;
                            info.setName( name );
                        }
                    }

                    // url
                    {
                        Object urlObj = connInfo.get("url");
                        if( urlObj != null && urlObj instanceof String ){
                            String url = (String)urlObj;
                            info.setUrl(url);
                        }
                    }

                    // user
                    {
                        Object userObj = connInfo.get("user");
                        if( userObj != null && userObj instanceof String ){
                            String user = (String)userObj;
                            info.setUser(user);
                        }
                    }

                    // password
                    {
                        Object passwordObj = connInfo.get("password");
                        if( passwordObj != null && passwordObj instanceof String ){
                            String password = (String)passwordObj;
                            info.setPassword(password);
                        }
                    }

                    infos.add(info);
                }
            }

            return infos;

        } catch(Exception e) {
            throw new Exception("Unable to load connection information documents",e);
        }
    }
}
