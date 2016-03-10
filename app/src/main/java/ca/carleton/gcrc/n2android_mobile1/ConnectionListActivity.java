package ca.carleton.gcrc.n2android_mobile1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_connections);

    }

    public void startAddConnectionActivity(View view){
        Intent intent = new Intent(this, AddConnectionActivity.class);
        startActivity(intent);
    }
}
