package service.interfaces;

public class DownloadTaskPogress extends TaskProgress {
    private String downloaded;
    private String downloading;

    public String getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(String downloaded) {
        this.downloaded = downloaded;
    }

    public String getDownloading() {
        return downloading;
    }

    public void setDownloading(String downloading) {
        this.downloading = downloading;
    }
}
