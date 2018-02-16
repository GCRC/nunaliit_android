package ca.carleton.gcrc.couch.client;

import nunaliit.org.json.JSONObject;

public interface CouchUserDocContext extends CouchAuthenticationContext {

	JSONObject getUserDoc();
}
