package ca.carleton.gcrc.n2android_mobile1.couchbase;

import com.couchbase.lite.Mapper;

/**
 * Created by jpfiset on 3/25/16.
 */
public interface CouchbaseView {
    String getName();
    String getVersion();
    Mapper getMapper();
}
