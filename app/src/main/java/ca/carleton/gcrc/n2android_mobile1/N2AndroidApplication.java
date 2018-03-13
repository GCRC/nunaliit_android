package ca.carleton.gcrc.n2android_mobile1;

import android.app.Application;
import android.webkit.WebView;

/**
 * Created by mrb on 2018-03-13.
 */

public class N2AndroidApplication extends Application {

    @Override public void onCreate() {
        super.onCreate();
        WebView.setWebContentsDebuggingEnabled(true);
    }

}
