package service.interfaces;

import java.util.Observer;

public interface IProgressService {
    void invalidateApiStatus();

    void addObserver(Observer o);
    TaskProgress getCurrentProgress();
    DownloadTaskPogress getCurrentDownloadProgress();
    ApiStatus getCurrentApiStatus();
    void invalidateProgress();
    void invalidateDownloadProgress();
}
