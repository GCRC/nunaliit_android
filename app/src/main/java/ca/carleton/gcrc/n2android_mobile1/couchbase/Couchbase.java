package ca.carleton.gcrc.n2android_mobile1.couchbase;

import java.util.Map;

/**
 * Created by jpfiset on 3/30/16.
 */
public class Couchbase {
    static public Object opt(Map<String,Object> map, String key){
        Object value = null;

        if( null != map ){
            if( map.containsKey(key) ){
                value = map.get(key);
            }
        }

        return value;
    }

    static public String optString(Map<String,Object> map, String key){
        String value = null;

        Object obj = opt(map, key);
        if( null != obj && obj instanceof String ){
            value = (String) obj;
        }

        return value;
    }

    static public Number optNumber(Map<String,Object> map, String key){
        Number value = null;

        Object obj = opt(map, key);
        if( null != obj && obj instanceof Number ){
            value = (Number) obj;
        }

        return value;
    }

    static public Map<String,Object> optMap(Map<String,Object> map, String key){
        Map<String,Object> value = null;

        Object obj = opt(map, key);
        if( null != obj && obj instanceof Map ){
            value = (Map<String,Object>) obj;
        }

        return value;
    }

    static public Boolean optBoolean(Map<String, Object> map, String key) {
        Boolean value = null;

        Object obj = opt(map, key);
        if( null != obj && obj instanceof Boolean ){
            value = (Boolean) obj;
        }

        return value;
    }
}
