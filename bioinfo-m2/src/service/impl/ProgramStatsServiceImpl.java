package service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import service.interfaces.IProgramStatsService;
import service.interfaces.ProgramStat;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProgramStatsServiceImpl extends Observable implements IProgramStatsService {
    private final ListeningExecutorService executorService;
    private AtomicInteger remainingRequests = new AtomicInteger(0);
    private AtomicLong averageMilliseconds = new AtomicLong(0);
    private AtomicLong numberOfRequests = new AtomicLong(0);
    private ZonedDateTime lastDate = null;
    private ListenableFuture currentFuture = null;


    @Inject
    public ProgramStatsServiceImpl(@Named("ProgramStatsExecutor") ListeningExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void resetAcquisitionTime() {

    }

    @Override
    public void addDate(ZonedDateTime date) {
        currentFuture = executorService.submit(() -> {
            if (currentFuture != null && !currentFuture.isCancelled()) {
                if (lastDate != null) {
                    long currentNumberOfRequests = numberOfRequests.incrementAndGet();
                    long currentAverageMilliseconds = averageMilliseconds.get();
                    long nextNumberOfRequests = currentNumberOfRequests + 1;

                    Duration difference = Duration.between(lastDate, date);

                    long newAverageMilliseconds = ((currentAverageMilliseconds * currentNumberOfRequests) + difference.toMillis()) / nextNumberOfRequests;

                    if (newAverageMilliseconds < 0) {
                        newAverageMilliseconds = 0;
                    }

                    averageMilliseconds.set(newAverageMilliseconds);
                } else {
                    averageMilliseconds.set(0);
                    numberOfRequests.incrementAndGet();
                }

                lastDate = ZonedDateTime.now();

                ProgramStat programStat = new ProgramStat();
                programStat.setTimeRemaining(averageMilliseconds.get() * remainingRequests.get());
                setChanged();
                notifyObservers(programStat);
            } else {
                endAcquisitionTimeEstimation();
            }
        });
    }

    @Override
    public int getRemainingRequests() {
        return remainingRequests.get();
    }

    @Override
    public void setRemainingRequests(int number) {
        remainingRequests.set(number);
    }

    @Override
    public void endAcquisitionTimeEstimation() {
        if (currentFuture != null) {
            currentFuture.cancel(true);
        }
        lastDate = null;
        averageMilliseconds.set(0);
        numberOfRequests.set(0);
        remainingRequests.set(0);
    }

}
