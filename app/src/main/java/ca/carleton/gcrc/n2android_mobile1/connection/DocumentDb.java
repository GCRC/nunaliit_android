package ca.carleton.gcrc.n2android_mobile1.connection;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;

import java.util.HashMap;
import java.util.Map;

import ca.carleton.gcrc.n2android_mobile1.couchbase.Couchbase;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseDb;
import ca.carleton.gcrc.n2android_mobile1.couchbase.CouchbaseView;

/**
 * Created by jpfiset on 3/25/16.
 */
public class DocumentDb extends CouchbaseDb {

    public static final CouchbaseView viewInfo = new CouchbaseView(){
        @Override
        public String getName() { return "info"; }

        @Override
        public String getVersion() { return "1"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Map<String,Object> value = new HashMap<String,Object>();

                String id = null;
                {
                    id = Couchbase.optString(document, "_id");
                    value.put("id", id);
                }

                {
                    String rev = Couchbase.optString(document, "_rev");
                    value.put("rev", rev);
                }

                {
                    String schemaName = Couchbase.optString(document, "nunaliit_schema");
                    if( null != schemaName ){
                        value.put("schema", schemaName);
                    }
                }

                {
                    Map<String, Object> created = Couchbase.optMap(document, "nunaliit_created");
                    if( null != created ){
                        Number time = Couchbase.optNumber(created, "time");
                        if( null != time ){
                            value.put("createdTime",time.longValue());
                        }
                    }
                }

                {
                    Map<String, Object> lastUpdated = Couchbase.optMap(document, "nunaliit_last_updated");
                    if( null != lastUpdated ){
                        Number time = Couchbase.optNumber(lastUpdated, "time");
                        if( null != time ){
                            value.put("updatedTime", time.longValue());
                        }
                    }
                }

                Boolean deleted = Couchbase.optBoolean(document, "nunaliit_mobile_deleted", false);
                if (!deleted) {
                    emitter.emit(id, value);
                }
            }
        };
    };

    public static final CouchbaseView viewNunaliitSchema = new CouchbaseView(){
        @Override
        public String getName() { return "nunaliit-schema"; }

        @Override
        public String getVersion() { return "2"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object schema = document.get("nunaliit_schema");
                if (null != schema
                 && schema instanceof String ) {
                    emitter.emit(schema, null);
                }
            }
        };
    };

    public static final CouchbaseView viewSchemas = new CouchbaseView(){
        @Override
        public String getName() { return "schemas"; }

        @Override
        public String getVersion() { return "2"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object type = document.get("nunaliit_type");
                if( "schema".equals(type) ) {
                    Object nameObj = document.get("name");
                    if (null != nameObj && nameObj instanceof String) {
                        String name = (String) nameObj;
                        emitter.emit(name, null);
                    }
                }
            }
        };
    };

    public static final CouchbaseView viewSchemasRoot = new CouchbaseView(){
        @Override
        public String getName() { return "schemas-root"; }

        @Override
        public String getVersion() { return "2"; }

        @Override
        public Mapper getMapper() {
            return mapper;
        }

        private Mapper mapper = new Mapper(){
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object type = document.get("nunaliit_type");
                Object isRoot = document.get("isRootSchema");
                if( "schema".equals(type)
                    && null != isRoot
                    && isRoot instanceof Boolean
                    && ((Boolean)isRoot) ) {
                    Object nameObj = document.get("name");
                    if (null != nameObj && nameObj instanceof String) {
                        String name = (String) nameObj;
                        emitter.emit(name, null);
                    }
                }
            }
        };
    };

    public DocumentDb(Database database) throws Exception {
        super(database);

        installView(viewInfo);
        installView(viewNunaliitSchema);
        installView(viewSchemas);
        installView(viewSchemasRoot);
    }
}
