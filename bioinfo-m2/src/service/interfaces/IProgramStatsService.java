package service.interfaces;

import java.time.ZonedDateTime;
import java.util.Observer;

public interface IProgramStatsService {
    void resetAcquisitionTime();
    void addDate(ZonedDateTime date);
    int getRemainingRequests();
    void setRemainingRequests(int number);
    void addObserver(Observer o);
    void endAcquisitionTimeEstimation();
}
