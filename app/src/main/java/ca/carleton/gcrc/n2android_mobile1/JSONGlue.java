package ca.carleton.gcrc.n2android_mobile1;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nunaliit.org.json.JSONObject;

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

    public static nunaliit.org.json.JSONObject convertJSONObjectFromAndroidToUpstream(org.json.JSONObject from) {
        return new nunaliit.org.json.JSONObject(from.toString());
    }

    public static org.json.JSONArray convertJSONArrayFromUpstreamToAndroid(nunaliit.org.json.JSONArray from) {
        try {
            return new org.json.JSONArray(from.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<org.json.JSONObject> convertJSONObjectCollectionFromUpstreamToAndroid(Collection<JSONObject> from) {
        final List<org.json.JSONObject> androidObjects = new ArrayList<>(from.size());

        for (nunaliit.org.json.JSONObject upstreamObject : from) {
            androidObjects.add(convertJSONObjectFromUpstreamToAndroid(upstreamObject));
        }

        return Collections.unmodifiableList(androidObjects);
    }
}
