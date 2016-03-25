package ca.carleton.gcrc.n2android_mobile1;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by jpfiset on 3/25/16.
 */
public class ServiceSupport {

    static public void createToast(final Context context, final String str, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast
                    .makeText(
                        context.getApplicationContext(),
                        str,
                        duration
                    )
                    .show();
            }
        });
    }
}
