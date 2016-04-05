package ca.carleton.gcrc.n2android_mobile1.cordova;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;

/**
 * Created by jpfiset on 4/5/16.
 */
public abstract class PluginRunnable implements Runnable {

    private String TAG = PluginRunnable.class.getSimpleName();

    private PluginActions actions;
    private JSONArray args;
    private CallbackContext callbackContext;

    public PluginRunnable(PluginActions actions, JSONArray args, CallbackContext callbackContext){
        this.actions = actions;
        this.args = args;
        this.callbackContext = callbackContext;
    }

    public PluginActions getActions(){
        return actions;
    }

    public JSONArray getArguments(){
        return args;
    }

    public CallbackContext getCallbackContext(){
        return callbackContext;
    }

    @Override
    final public void run() {
        try {
            pluginRun();
        } catch(Exception e) {
            Log.e(TAG, "Error while executing plugin callback", e);
        }
    }

    abstract public void pluginRun() throws Exception;
}
