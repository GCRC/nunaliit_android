package ca.carleton.gcrc.n2android_mobile1.cordova;

import android.app.Activity;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.activities.EmbeddedCordovaActivity;

/**
 * Created by jpfiset on 3/14/16.
 */
public class CordovaNunaliitPlugin extends CordovaPlugin {

    final protected String TAG = this.getClass().getSimpleName();

    private CordovaInterface cordovaInterface = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        this.cordovaInterface = cordova;
        Log.i(TAG,"Cordova Interface: "+cordova.getClass().getSimpleName());

        Activity activity = cordova.getActivity();
        if( null != activity ){
            Log.i(TAG,"Activity: "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void pluginInitialize() {
        Log.i(TAG, "Plugin initialized. Service name: " + getServiceName());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Action: " + action);

        if( "echo".equals(action) ) {
            String message = args.getString(0);
            this.echo(message, callbackContext);
            return true;

        } else if( "getConnectionInfo".equals(action) ) {
            this.getConnectionInfo(callbackContext);
            return true;
        }

        return false;  // Returning false results in a "MethodNotFound" error.
    }

    private void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void getConnectionInfo(CallbackContext callbackContext) {
        ConnectionInfo connInfo = retrieveConnection();

        if( null != connInfo ) {
            try {
                JSONObject result = new JSONObject();
                result.put("name", connInfo.getName());
                result.put("id", connInfo.getId());
                result.put("url", connInfo.getUrl());
                result.put("user", connInfo.getUser());
                callbackContext.success(result);
            } catch(Exception e) {
                callbackContext.error("Error while retrieving connection information: "+e.getMessage());
            }
        } else {
            callbackContext.error("Unable to retrieve connection information");
        }
    }

    public ConnectionInfo retrieveConnection(){
        ConnectionInfo connInfo = null;

        Activity activity = null;
        if( null != cordovaInterface ){
            activity = cordovaInterface.getActivity();
        }

        EmbeddedCordovaActivity cordovaActivity = null;
        if( null != activity && activity instanceof EmbeddedCordovaActivity ){
            cordovaActivity = (EmbeddedCordovaActivity)activity;
        }

        if( null != cordovaActivity ){
            connInfo = cordovaActivity.retrieveConnection();
        }

        return connInfo;
    }

    private CouchbaseLiteService getCouchDbService(){
        CouchbaseLiteService service = null;

        Activity activity = null;
        if( null != cordovaInterface ){
            activity = cordovaInterface.getActivity();
        }

        EmbeddedCordovaActivity cordovaActivity = null;
        if( null != activity && activity instanceof EmbeddedCordovaActivity ){
            cordovaActivity = (EmbeddedCordovaActivity)activity;
        }

        if( null != cordovaActivity ){
            service = cordovaActivity.getCouchDbService();
        }

        return service;
    }
}
