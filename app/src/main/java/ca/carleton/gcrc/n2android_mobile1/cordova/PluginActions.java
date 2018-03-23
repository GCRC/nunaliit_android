package ca.carleton.gcrc.n2android_mobile1.cordova;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ca.carleton.gcrc.n2android_mobile1.activities.EmbeddedCordovaActivity;
import ca.carleton.gcrc.n2android_mobile1.connection.Connection;
import ca.carleton.gcrc.n2android_mobile1.connection.ConnectionInfo;
import ca.carleton.gcrc.n2android_mobile1.connection.DocumentDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDocInfo;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseLiteService;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseManager;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQuery;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseQueryResults;

/**
 * Created by jpfiset on 4/5/16.
 */
public class PluginActions {

    final protected String TAG = this.getClass().getSimpleName();

    private CordovaInterface cordovaInterface = null;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public PluginActions(CordovaInterface cordovaInterface, Context context){
        this.cordovaInterface = cordovaInterface;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    public void getConnectionInfo(CallbackContext callbackContext) {
        ConnectionInfo connInfo = retrieveConnection();

        if( null != connInfo ) {
            try {
                JSONObject result = new JSONObject();
                result.put("name", connInfo.getName());
                result.put("id", connInfo.getId());
                result.put("url", connInfo.getUrl());
                result.put("user", connInfo.getUser());
                callbackContext.success(result);
            } catch(Exception e) {
                callbackContext.error("Error while retrieving connection information: "+e.getMessage());
            }
        } else {
            callbackContext.error("Unable to retrieve connection information");
        }
    }

    public void couchbaseGetDatabaseInfo(CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            JSONObject dbInfo = docDb.getInfo();

            callbackContext.success(dbInfo);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDatabaseInfo(): "+e.getMessage());
        }
    }

    public void couchbaseGetDocumentRevision(String docId, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            String rev = docDb.getDocumentRevision(docId);

            JSONObject result = new JSONObject();
            result.put("rev", rev);
            callbackContext.success(result);
        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDocumentRevision(): "+e.getMessage());
        }
    }

    public void couchbaseCreateDocument(final JSONObject doc, final CallbackContext callbackContext) {
        try {
            final DocumentDb docDb = getDocumentDb();

            try {

                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    Log.v(TAG, location.toString());

                                    try {
                                        double latitude = location.getLatitude();
                                        double longitude = location.getLongitude();

                                        JSONObject locationObject = new JSONObject();
                                        locationObject.put("wkt", "MULTIPOINT((" + longitude + " " + latitude + "))");
                                        locationObject.put("nunaliit_type", "geometry");
                                        doc.put("nunaliit_geom", locationObject);
                                    } catch (JSONException je) {
                                        // If you can't get a location, carry on.
                                    }
                                }

                                createDocument(doc, docDb, callbackContext);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                createDocument(doc, docDb, callbackContext);
                            }
                        });

            } catch (SecurityException se) {
                // Will be thrown if the user declines the location prompt.
                createDocument(doc, docDb, callbackContext);
            }

        } catch (Exception e) {
            callbackContext.error("Error while performing couchbaseCreateDocument(): " + e.getMessage());
        }
    }

    private void createDocument(JSONObject doc, DocumentDb docDb, CallbackContext callbackContext) {
        try {
            CouchbaseDocInfo info = docDb.createDocument(doc);

            final JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());

            callbackContext.success(result);
        } catch (Exception re) {
            callbackContext.error("Error while performing couchbaseCreateDocument(): "+re.getMessage());
        }
    }

    public void couchbaseUpdateDocument(JSONObject doc, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            CouchbaseDocInfo info = docDb.updateDocument(doc);

            JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseCreateDocument(): "+e.getMessage());
        }
    }

    public void couchbaseDeleteDocument(JSONObject doc, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            CouchbaseDocInfo info = docDb.deleteDocument(doc);

            JSONObject result = new JSONObject();
            result.put("id", info.getId());
            result.put("rev", info.getRev());
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseDeleteDocument(): "+e.getMessage());
        }
    }

    public void couchbaseGetDocument(String docId, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            JSONObject doc = docDb.getDocument(docId);

            JSONObject result = new JSONObject();
            result.put("doc", doc);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDocument(): "+e.getMessage());
        }
    }

    public void couchbaseGetDocuments(List<String> docIds, CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            List<JSONObject> docs = docDb.getDocuments(docIds);

            JSONArray jsonDocs = new JSONArray();
            for(JSONObject doc : docs){
                jsonDocs.put(doc);
            }


            JSONObject result = new JSONObject();
            result.put("docs", jsonDocs);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetDocuments(): "+e.getMessage());
        }
    }

    public void couchbaseGetAllDocuments(CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            List<JSONObject> docs = docDb.getAllDocuments();

            JSONArray jsonDocs = new JSONArray();
            for(JSONObject doc : docs){
                jsonDocs.put(doc);
            }


            JSONObject result = new JSONObject();
            result.put("docs", jsonDocs);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetAllDocuments(): "+e.getMessage());
        }
    }

    public void couchbaseGetAllDocumentIds(CallbackContext callbackContext) {
        try {
            DocumentDb docDb = getDocumentDb();

            List<String> docIds = docDb.getAllDocumentIds();

            JSONArray jsonIds = new JSONArray();
            for(String docId : docIds){
                jsonIds.put(docId);
            }

            JSONObject result = new JSONObject();
            result.put("ids", jsonIds);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbaseGetAllDocumentIds(): "+e.getMessage());
        }
    }

    public void couchbasePerformQuery(String designName, JSONObject jsonQuery, CallbackContext callbackContext) {
        try {
            CouchbaseQuery query = new CouchbaseQuery();

            // View name
            {
                String viewName = jsonQuery.getString("viewName");
                query.setViewName(viewName);
            }

            // Start key
            {
                Object startObj = jsonQuery.opt("startkey");
                if( null != startObj ){
                    query.setStartKey(startObj);
                }
            }

            // End key
            {
                Object endObj = jsonQuery.opt("endkey");
                if( null != endObj ){
                    query.setEndKey(endObj);
                }
            }

            // Keys
            {
                JSONArray keys = jsonQuery.optJSONArray("keys");
                if( null != keys ){
                    query.setKeys(keys);
                }
            }

            // Include Docs
            {
                boolean include_docs = jsonQuery.optBoolean("include_docs", false);
                if( include_docs ){
                    query.setIncludeDocs(true);
                }
            }

            // Limit
            {
                if( jsonQuery.has("limit") ) {
                    int limit = jsonQuery.getInt("limit");
                    query.setLimit(limit);
                }
            }

            // Reduce
            {
                boolean reduce = jsonQuery.optBoolean("reduce", false);
                if( reduce ){
                    query.setReduce(true);
                }
            }

            DocumentDb docDb = getDocumentDb();

            CouchbaseQueryResults results = docDb.performQuery(query);

            JSONArray jsonRows = new JSONArray();
            for(JSONObject row : results.getRows()){
                jsonRows.put(row);
            }

            JSONObject result = new JSONObject();
            result.put("rows", jsonRows);
            callbackContext.success(result);

        } catch(Exception e) {
            callbackContext.error("Error while performing couchbasePerformQuery(): "+e.getMessage());
        }
    }

    private DocumentDb getDocumentDb() throws Exception {
        ConnectionInfo connInfo = retrieveConnection();
        if( null == connInfo ){
            throw new Exception("Unable to retrieve connection information");
        }
        CouchbaseLiteService couchbaseService = getCouchDbService();
        if( null == couchbaseService ){
            throw new Exception("Unable to retrieve Couchbase service");
        }
        CouchbaseManager couchbaseManager = couchbaseService.getCouchbaseManager();
        Connection connection = new Connection(couchbaseManager, connInfo);
        DocumentDb docDb = connection.getLocalDocumentDb();
        return docDb;
    }

    public ConnectionInfo retrieveConnection(){
        ConnectionInfo connInfo = null;

        Activity activity = null;
        if( null != cordovaInterface ){
            activity = cordovaInterface.getActivity();
        }

        EmbeddedCordovaActivity cordovaActivity = null;
        if( null != activity && activity instanceof EmbeddedCordovaActivity ){
            cordovaActivity = (EmbeddedCordovaActivity)activity;
        }

        if( null != cordovaActivity ){
            connInfo = cordovaActivity.retrieveConnection();
        }

        return connInfo;
    }

    private CouchbaseLiteService getCouchDbService(){
        CouchbaseLiteService service = null;

        Activity activity = null;
        if( null != cordovaInterface ){
            activity = cordovaInterface.getActivity();
        }

        EmbeddedCordovaActivity cordovaActivity = null;
        if( null != activity && activity instanceof EmbeddedCordovaActivity ){
            cordovaActivity = (EmbeddedCordovaActivity)activity;
        }

        if( null != cordovaActivity ){
            service = cordovaActivity.getCouchDbService();
        }

        return service;
    }
}
