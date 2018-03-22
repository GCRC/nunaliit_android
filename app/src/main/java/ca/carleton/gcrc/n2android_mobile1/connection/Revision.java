package ca.carleton.gcrc.n2android_mobile1.connection;

/**
 * Created by jpfiset on 3/30/16.
 */
public class Revision {

    private String id;
    private String docId;
    private String remoteRevision;
    private String localRevision;
    private String lastCommit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getRemoteRevision() {
        return remoteRevision;
    }

    public void setRemoteRevision(String remoteRevision) {
        this.remoteRevision = remoteRevision;
    }

    public String getLocalRevision() {
        return localRevision;
    }

    public void setLocalRevision(String localRevision) {
        this.localRevision = localRevision;
    }

    public String getLastCommit() { return lastCommit; }

    public void setLastCommit(String lastCommit) { this.lastCommit = lastCommit; }
}
