package ca.carleton.gcrc.n2android_mobile1.connection;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionInfo implements Parcelable {
    public static final Parcelable.Creator<ConnectionInfo> CREATOR =
            new Parcelable.Creator<ConnectionInfo>() {
        public ConnectionInfo createFromParcel(Parcel in) {
            return new ConnectionInfo(in);
        }

        public ConnectionInfo[] newArray(int size) {
            return new ConnectionInfo[size];
        }
    };

    private String id;
    private String name;
    private String url;
    private String user;
    private String password;
    private String localDocumentDbName;
    private String localTrackingDbName;

    public ConnectionInfo(){

    }

    public ConnectionInfo(Parcel in) {
        id = in.readString();
        name = in.readString();
        url = in.readString();
        user = in.readString();
        password = in.readString();
        localDocumentDbName = in.readString();
        localTrackingDbName = in.readString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getLocalDocumentDbName() { return localDocumentDbName; }

    public void setLocalDocumentDbName(String localDocumentDbName) {
        this.localDocumentDbName = localDocumentDbName;
    }

    public String getLocalTrackingDbName() {
        return localTrackingDbName;
    }

    public void setLocalTrackingDbName(String localTrackingDbName) {
        this.localTrackingDbName = localTrackingDbName;
    }

    public String toString(){
        if( null == name ){
            return "?";
        }

        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(user);
        dest.writeString(password);
        dest.writeString(localDocumentDbName);
        dest.writeString(localTrackingDbName);
    }
}
