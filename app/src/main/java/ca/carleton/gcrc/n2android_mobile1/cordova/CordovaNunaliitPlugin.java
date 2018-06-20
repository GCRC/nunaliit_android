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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

/**
 * Created by jpfiset on 3/14/16.
 */
public class CordovaNunaliitPlugin extends CordovaPlugin {

    final protected String TAG = this.getClass().getSimpleName();

    private CordovaInterface cordovaInterface = null;
    private PluginActions actions = null;
    private static String sEventCallback = null;
    private static CordovaWebView sWebView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        this.cordovaInterface = cordova;
        Log.v(TAG,"Cordova Interface: "+cordova.getClass().getSimpleName());

        this.actions = new PluginActions(cordovaInterface, webView.getContext());

        sWebView = this.webView;

        Activity activity = cordova.getActivity();
        if( null != activity ){
            Log.v(TAG, "Activity: " + activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sEventCallback = null;
        sWebView = null;
    }

    @Override
    public void pluginInitialize() {
        Log.i(TAG, "Plugin initialized. Service name: " + getServiceName());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Action: " + action);


        ExecutorService threadPool = this.cordovaInterface.getThreadPool();

        if( "echo".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    String message = getArguments().getString(0);
                    getActions().echo(message, getCallbackContext());
                }
            });
            return true;

        } else if( "getConnectionInfo".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    getActions().getConnectionInfo(getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseGetDatabaseInfo".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    getActions().couchbaseGetDatabaseInfo(getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseGetDocumentRevision".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    String docId = getArguments().getString(0);
                    getActions().couchbaseGetDocumentRevision(docId, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseCreateDocument".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    JSONObject doc = getArguments().getJSONObject(0);
                    getActions().couchbaseCreateDocument(doc, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseUpdateDocument".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    JSONObject doc = getArguments().getJSONObject(0);
                    getActions().couchbaseUpdateDocument(doc, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseDeleteDocument".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    JSONObject doc = getArguments().getJSONObject(0);
                    getActions().couchbaseDeleteDocument(doc, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseGetDocument".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    String docId = getArguments().getString(0);
                    getActions().couchbaseGetDocument(docId, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseGetDocuments".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    List<String> docIds = new Vector<String>();
                    JSONArray jsonDocIds = getArguments().getJSONArray(0);
                    for (int i = 0, e = jsonDocIds.length(); i < e; ++i) {
                        Object objId = jsonDocIds.get(i);
                        if (objId instanceof String) {
                            String docId = (String) objId;
                            docIds.add(docId);
                        } else {
                            String className = "" + objId;
                            if (null != objId) {
                                className = objId.getClass().getName();
                            }
                            Log.w(TAG, "couchbaseGetDocuments: invalid docId: " + className);
                        }
                    }
                    getActions().couchbaseGetDocuments(docIds, getCallbackContext());
                }
            });
            return true;

        } else if( "couchbaseGetAllDocuments".equals(action) ) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    getActions().couchbaseGetAllDocuments(getCallbackContext());
                }
            });
            return true;

        } else if ("couchbaseGetAllDocumentIds".equals(action)) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    getActions().couchbaseGetAllDocumentIds(getCallbackContext());
                }
            });
            return true;

        } else if ("couchbasePerformQuery".equals(action)) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    String designName = getArguments().getString(0);
                    JSONObject jsonQuery = getArguments().getJSONObject(1);
                    getActions().couchbasePerformQuery(designName, jsonQuery, getCallbackContext());
                }
            });
            return true;
        } else if ("registerCallback".equals(action)) {
            threadPool.execute(new PluginRunnable(actions, args, callbackContext) {
                @Override
                public void pluginRun() throws Exception {
                    sEventCallback = getArguments().getString(0);
                    Log.d(TAG, "Register callback was called for: " + sEventCallback);
                    getCallbackContext().success(sEventCallback);
                }
            });
            return true;
        }

        return false;  // Returning false results in a "MethodNotFound" error.
    }

    /**
    * Use the Cordova bridge to invoke the callback.
    */
    public static void javascriptEventCallback(String name) {
        if (sEventCallback != null && !sEventCallback.isEmpty() && sWebView != null) {
            String snippet = "javascript:" + sEventCallback + "(" + name + ")";
            sWebView.sendJavascript(snippet);
        }
    }
}
