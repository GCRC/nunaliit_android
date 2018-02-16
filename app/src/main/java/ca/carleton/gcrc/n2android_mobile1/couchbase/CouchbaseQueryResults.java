package ca.carleton.gcrc.n2android_mobile1.couchbase;

import nunaliit.org.json.JSONObject;

import java.util.List;
import java.util.Vector;

/**
 * Created by jpfiset on 3/24/16.
 */
public class CouchbaseQueryResults {

    private List<JSONObject> rows = new Vector<JSONObject>();

    public List<JSONObject> getRows() {
        return rows;
    }

    public void addRow(JSONObject row) {
        rows.add(row);
    }
}
