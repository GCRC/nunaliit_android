package ca.carleton.gcrc.n2android_mobile1;

import android.os.Bundle;

import org.apache.cordova.CordovaActivity;

/**
 * Created by jpfiset on 3/9/16.
 */
public class EmbeddedCordovaActivity extends CordovaActivity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }
}
