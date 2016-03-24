package ca.carleton.gcrc.n2android_mobile1.couchbase;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by jpfiset on 3/24/16.
 */
public class CouchbaseQuery {
    private String viewName = null;
    private String startKey = null;
    private String endKey = null;
    private String keys = null;
    private boolean includeDocs = false;
    private boolean reduce = false;

    public String getViewName() {
        return viewName;
    }
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getStartKey() {
        return startKey;
    }
    public void setStartKey(String startKey) {
        this.startKey = startKey;
    }

    public String getEndKey() {
        return endKey;
    }
    public void setEndKey(String endKey) {
        this.endKey = endKey;
    }

    public String getKeys() {
        return keys;
    }
    public void setKeys(List<String> keys) {
        JSONArray arr = new JSONArray();
        for(String key : keys) {
            arr.put(key);
        }
        this.keys = arr.toString();
    }

    public boolean getIncludeDocs() {
        return includeDocs;
    }
    public void setIncludeDocs(boolean includeDocs) throws Exception {
        this.includeDocs = includeDocs;
    }

    public boolean isReduce() {
        return reduce;
    }
    public void setReduce(boolean reduce) {
        this.reduce = reduce;
    }
}
