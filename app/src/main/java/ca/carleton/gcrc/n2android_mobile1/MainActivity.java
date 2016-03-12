package ca.carleton.gcrc.n2android_mobile1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Start CouchDb service
        Intent intent = new Intent(this, CouchDbService.class);
        startService(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_manage_connections:
                startConnectionListActivity(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startCordova(View view){
        Intent intent = new Intent(this, EmbeddedCordovaActivity.class);
        startActivity(intent);
    }

    public void startActivity2(View view){
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }

    public void startConnectionListActivity(View view){
        Intent intent = new Intent(this, ConnectionListActivity.class);
        startActivity(intent);
    }
}
