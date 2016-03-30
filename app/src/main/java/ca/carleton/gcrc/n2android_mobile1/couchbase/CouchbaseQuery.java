package ca.carleton.gcrc.n2android_mobile1.couchbase;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by jpfiset on 3/24/16.
 */
public class CouchbaseQuery {
    private String viewName = null;
    private Object startKey = null;
    private Object endKey = null;
    private JSONArray keys = null;
    private Integer limit = null;
    private boolean includeDocs = false;
    private boolean reduce = false;

    public String getViewName() {
        return viewName;
    }
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Object getStartKey() {
        return startKey;
    }
    public void setStartKey(Object startKey) {
        this.startKey = startKey;
    }

    public Object getEndKey() {
        return endKey;
    }
    public void setEndKey(Object endKey) {
        this.endKey = endKey;
    }

    public JSONArray getKeys() {
        return keys;
    }
    public void setKeys(JSONArray keys) {
        this.keys = keys;
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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
