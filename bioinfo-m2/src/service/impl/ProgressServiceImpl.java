package service.impl;

import java.util.Observable;
import java.util.Observer;

import service.interfaces.ApiStatus;
import service.interfaces.DownloadTaskPogress;
import service.interfaces.IProgressService;
import service.interfaces.TaskProgress;

public class ProgressServiceImpl extends Observable implements IProgressService {
    private TaskProgress currentProgress = new TaskProgress();
    private DownloadTaskPogress currentDownloadProgress = new DownloadTaskPogress();
    private ApiStatus currentApiStatus = new ApiStatus();

    @Override
    public TaskProgress getCurrentProgress() {
        return currentProgress;
    }

    @Override
    public DownloadTaskPogress getCurrentDownloadProgress() { return currentDownloadProgress; }

    @Override
    public ApiStatus getCurrentApiStatus() { return currentApiStatus; };

    @Override
    public void invalidateProgress() {
        setChanged();
        notifyObservers(currentProgress);
    }

    @Override
    public void invalidateDownloadProgress() {
        setChanged();
        notifyObservers(currentDownloadProgress);
    }

    @Override
    public void invalidateApiStatus() {
        setChanged();
        notifyObservers(currentApiStatus);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
    }
}
