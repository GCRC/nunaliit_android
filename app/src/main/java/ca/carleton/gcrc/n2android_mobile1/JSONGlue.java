package ca.carleton.gcrc.n2android_mobile1;

import org.json.JSONException;

/**
 * Created by mrb on 2018-02-16.
 */

public class JSONGlue {

    public static org.json.JSONObject convertJSONObjectFromUpstreamToAndroid(nunaliit.org.json.JSONObject from) {
        try {
            return new org.json.JSONObject(from.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
