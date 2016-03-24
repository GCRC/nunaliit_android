package ca.carleton.gcrc.n2android_mobile1;

/**
 * Created by jpfiset on 3/11/16.
 */
public class NunaliitMobileConstants {

    public static String threadId() {
        return "(thr-"+Thread.currentThread().getId()+")";
    }

    public static final String EXTRA_ERROR = "ca.carleton.gcrc.EXTRA_ERROR";
    public static final String EXTRA_CONNECTION_ID = "ca.carleton.gcrc.EXTRA_CONNECTION_ID";
    public static final String EXTRA_CONNECTION_INFO = "ca.carleton.gcrc.EXTRA_CONNECTION_INFO";
    public static final String EXTRA_CONNECTION_INFOS = "ca.carleton.gcrc.EXTRA_CONNECTION_INFOS";
}
