package ca.carleton.gcrc.n2android_mobile1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    public static String TAG = "NunaliitMobile";

    // constants
    public static final String DATABASE_NAME = "connections";
    public static final String designDocName = "local";

    // couchdb internals
    protected static Manager manager;
    private Database database;
    private LiveQuery liveQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            startCouchDb();
        } catch(Exception e) {
            Toast
                .makeText(getApplicationContext(), "Error Initializing CouchDB, see logs for details", Toast.LENGTH_LONG)
                .show();
            Log.e(TAG, "Error initializing CouchDB", e);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    protected void onDestroy() {
        if(manager != null) {
            manager.close();
            manager = null;
        }
        super.onDestroy();
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

        Document doc = database.getDocument("testDoc");
        SavedRevision currentRevision = doc.getCurrentRevision();
        if( null == currentRevision ){
            Log.i(TAG, "testDoc does not exist");
            Map<String,Object> props = new HashMap<String,Object>();
            props.put("nunaliit_test","allo");
            doc.putProperties(props);
        } else {
            Log.i(TAG, "testDoc revision: "+currentRevision.getProperties().get("_rev"));
        }
//        com.couchbase.lite.View viewItemsByDate = database.getView(String.format("%s/%s", designDocName, byDateViewName));
//        viewItemsByDate.setMap(new Mapper() {
//            @Override
//            public void map(Map<String, Object> document, Emitter emitter) {
//                Object createdAt = document.get("created_at");
//                if (createdAt != null) {
//                    emitter.emit(createdAt.toString(), null);
//                }
//            }
//        }, "1.0");
//
//        initItemListAdapter();
//
//        startLiveQuery(viewItemsByDate);
//
//        startSync();

    }

    public void startActivity2(View view){
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }
}
