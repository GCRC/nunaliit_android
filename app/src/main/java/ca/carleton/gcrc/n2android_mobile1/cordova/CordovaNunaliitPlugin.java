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
import org.json.JSONTokener;

import ca.carleton.gcrc.n2android_mobile1.connection.Connection;
import ca.carleton.gcrc.n2android_mobile1.connection.DocumentDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.activities.EmbeddedCordovaActivity;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;

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

        } else if( "couchbaseGetDocumentRevision".equals(action) ) {
            String docId = args.getString(0);
            this.couchbaseGetDocumentRevision(docId, callbackContext);
            return true;

        } else if( "couchbaseCreateDocument".equals(action) ) {
            JSONObject doc = args.getJSONObject(0);
            this.couchbaseCreateDocument(doc, callbackContext);
            return true;

        } else if( "couchbaseUpdateDocument".equals(action) ) {
            JSONObject doc = args.getJSONObject(0);
            this.couchbaseUpdateDocument(doc, callbackContext);
            return true;

        } else if( "couchbaseDeleteDocument".equals(action) ) {
            JSONObject doc = args.getJSONObject(0);
            this.couchbaseDeleteDocument(doc, callbackContext);
            return true;

        } else if( "couchbaseGetDocument".equals(action) ) {
            String docId = args.getString(0);
            this.couchbaseGetDocument(docId, callbackContext);
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

    private void couchbaseGetDocumentRevision(String docId, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            String rev = docDb.getDocumentRevision(docId);

            JSONObject result = new JSONObject();
            result.put("rev", rev);
            callbackContext.success(result);
        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDocumentRevision(): "+e.getMessage());
        }
    }

    private void couchbaseCreateDocument(JSONObject doc, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            CouchbaseDocInfo info = docDb.createDocument(doc);

            JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseCreateDocument(): "+e.getMessage());
        }
    }

    private void couchbaseUpdateDocument(JSONObject doc, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            CouchbaseDocInfo info = docDb.updateDocument(doc);

            JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseCreateDocument(): "+e.getMessage());
        }
    }

    private void couchbaseDeleteDocument(JSONObject doc, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            CouchbaseDocInfo info = docDb.deleteDocument(doc);

            JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseDeleteDocument(): "+e.getMessage());
        }
    }

    private void couchbaseGetDocument(String docId, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            JSONObject doc = docDb.getDocument(docId);

            JSONObject result = new JSONObject();
            result.put("doc", doc);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDocument(): "+e.getMessage());
        }
    }

    private DocumentDb getDocumentDb() throws Exception {
        ConnectionInfo connInfo = retrieveConnection();
        if( null == connInfo ){
            throw new Exception("Unable to retrieve connection information");
        }
        CouchbaseLiteService couchbaseService = getCouchDbService();
        if( null == couchbaseService ){
            throw new Exception("Unable to retrieve Couchbase service");
        }
        CouchbaseManager couchbaseManager = couchbaseService.getCouchbaseManager();
        Connection connection = new Connection(couchbaseManager, connInfo);
        DocumentDb docDb = connection.getLocalDocumentDb();
        return docDb;
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
