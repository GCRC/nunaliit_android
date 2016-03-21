package ca.carleton.gcrc.n2android_mobile1.connection;

import android.util.Log;

import java.net.URL;

import ca.carleton.gcrc.couch.client.CouchClient;
import ca.carleton.gcrc.couch.client.CouchContext;
import ca.carleton.gcrc.couch.client.CouchFactory;
import ca.carleton.gcrc.couch.client.CouchServerVersion;
import ca.carleton.gcrc.n2android_mobile1.ConnectionInfo;

/**
 * Created by jpfiset on 3/21/16.
 */
public class Connection {

    protected final String TAG = this.getClass().getSimpleName();

    private ConnectionInfo info;

    public Connection(ConnectionInfo info){
        this.info = info;
    }

    public void checkRemoteSite() throws Exception {
        URL url = new URL(info.getUrl());
        URL serverUrl = new URL(url,"server");

        CouchFactory factory = new CouchFactory();
        CouchContext context = factory.getContext();
        CouchClient client = factory.getClient(context, serverUrl);

        CouchServerVersion version = client.getVersion();

        int major = version.getMajor();
        int minor = version.getMinor();

        Log.i(TAG,"Database version major: "+major+" minor: "+minor);
    }
}
