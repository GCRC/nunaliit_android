package ca.carleton.gcrc.couch.client;

import java.util.List;

import nunaliit.org.json.JSONObject;

public interface CouchQueryResults {

	JSONObject getFullResults();

	int getTotal();

	int getOffset();

	List<JSONObject> getRows();

	List<JSONObject> getValues();
}
