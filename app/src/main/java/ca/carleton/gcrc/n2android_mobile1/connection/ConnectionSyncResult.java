package ca.carleton.gcrc.n2android_mobile1.connection;

/**
 * Created by thomaseaton on 2018-04-13.
 */

public class ConnectionSyncResult {
    private int filesClientUpdated;
    private int filesRemoteUpdated;

    private int filesFailedClientUpdated;
    private int filesFailedRemoteUpdated;

    private int filesDeletedLocal;
    private int filesDeletedRemote;
    private int filesDeleteRequest;

    public int getFilesClientUpdated() {
        return filesClientUpdated;
    }

    public void setFilesClientUpdated(int filesClientUpdated) {
        this.filesClientUpdated = filesClientUpdated;
    }

    public int getFilesRemoteUpdated() {
        return filesRemoteUpdated;
    }

    public void setFilesRemoteUpdated(int filesRemoteUpdated) {
        this.filesRemoteUpdated = filesRemoteUpdated;
    }

    public int getFilesFailedClientUpdated() {
        return filesFailedClientUpdated;
    }

    public void setFilesFailedClientUpdated(int filesFailedClientUpdated) {
        this.filesFailedClientUpdated = filesFailedClientUpdated;
    }

    public int getFilesFailedRemoteUpdated() {
        return filesFailedRemoteUpdated;
    }

    public void setFilesFailedRemoteUpdated(int filesFailedRemoteUpdated) {
        this.filesFailedRemoteUpdated = filesFailedRemoteUpdated;
    }

    public int getFilesDeletedLocal() {
        return filesDeletedLocal;
    }

    public void setFilesDeletedLocal(int filesDeletedLocal) {
        this.filesDeletedLocal = filesDeletedLocal;
    }

    public int getFilesDeleteRequest() {
        return filesDeleteRequest;
    }

    public void setFilesDeleteRequest(int filesDeleteRequest) {
        this.filesDeleteRequest = filesDeleteRequest;
    }
}
