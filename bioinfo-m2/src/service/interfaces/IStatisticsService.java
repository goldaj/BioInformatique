package service.interfaces;

import com.google.common.util.concurrent.ListenableFuture;

import model.Gene;
import model.Kingdom;
import model.Organism;
import model.Sum;

public interface IStatisticsService {

    ListenableFuture<Gene> computeStatistics(Kingdom kingdom, Organism organism, Gene gene);

    ListenableFuture<Sum> computeSum(Kingdom kingdom, Organism organism, Sum sum, Gene gene);

    ListenableFuture<Sum> computeProbabilitiesFromSum(Organism organism, Sum organismSum);

}
