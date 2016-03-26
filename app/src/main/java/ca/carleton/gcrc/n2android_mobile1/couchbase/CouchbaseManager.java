package ca.carleton.gcrc.n2android_mobile1.couchbase;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.util.HashMap;
import java.util.Map;

import ca.carleton.gcrc.n2android_mobile1.Nunaliit;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfoDb;

/**
 * Created by jpfiset on 3/25/16.
 */
public class CouchbaseManager {
    protected final String TAG = this.getClass().getSimpleName();

    // constants
    public static final String DATABASE_NAME = "connections";
    public static final String VIEW_CONNECTIONS = "connections-by-id";

    // couchdb internals
    private Manager manager;
    private Database connDb;
    private Map<String,Database> databasesByName = new HashMap<String,Database>();


    public void startCouchbase(Context context) throws Exception {

        Log.v(TAG,"startCouchbase" + Nunaliit.threadId());

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

        manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);

        // install a view definition needed by the application
        DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        connDb = manager.openDatabase(DATABASE_NAME, options);
    }

    public void stopCouchbase() {

        Log.v(TAG,"stopCouchbase" + Nunaliit.threadId());

        if( null != connDb ){
            connDb.close();
            connDb = null;
        }

        for(String dbName : databasesByName.keySet()){
            Database db = databasesByName.get(dbName);
            db.close();
            databasesByName.remove(dbName);
        }

        if( manager != null ) {
            manager.close();
            manager = null;

            Log.i(TAG, "CouchDB Finalized");
        }
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
            Log.e(TAG, "Error checking if database exists: " + dbName, e);
        }
        return exists;
    }

    public Database getDatabase(String dbName) throws Exception {
        try {
            Database db = databasesByName.get(dbName);
            if (null == db) {
                DatabaseOptions options = new DatabaseOptions();
                options.setCreate(false);
                db = manager.openDatabase(dbName, options);
            }
            return db;

        } catch(Exception e) {
            throw new Exception("Unable to get database "+dbName,e);
        }
    }

    public ConnectionInfoDb getConnectionsDb() throws Exception {
        return new ConnectionInfoDb(connDb);
    }

    public void createDatabase(String dbName) throws Exception {
        try {
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            Database db = manager.openDatabase(dbName, options);
            if( false == db.exists() ){
                throw new Exception("Database does not exist after creation");
            }
            db.close();

            Log.i(TAG, "Created local database: " + dbName);

        } catch(Exception e) {
            throw new Exception("Error while creating database "+dbName,e);
        }
    }

    public void deleteDatabase(String dbName) throws Exception {
        try {
            Database db = manager.getDatabase(dbName);
            db.delete();
            db.close();

            databasesByName.remove(dbName);

        } catch(Exception e) {
            throw new Exception("Error while deleting database "+dbName,e);
        }
    }
}
