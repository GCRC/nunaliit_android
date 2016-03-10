package ca.carleton.gcrc.n2android_mobile1;

/**
 * Created by jpfiset on 3/10/16.
 */
public class ConnectionInfo {
    private String name;
    private String url;
    private String user;
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String toString(){
        if( null == name ){
            return "?";
        }

        return name;
    }
}
