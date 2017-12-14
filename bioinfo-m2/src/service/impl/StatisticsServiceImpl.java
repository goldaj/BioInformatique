package service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import model.*;
import service.interfaces.IFileService;
import service.interfaces.IStatisticsService;

import java.util.*;

public class StatisticsServiceImpl implements IStatisticsService {
    private final ListeningExecutorService executorService;
    private final IFileService fileService;

    @Inject
    public StatisticsServiceImpl(ListeningExecutorService listeningExecutorService, IFileService fileService) {
        this.executorService = listeningExecutorService;
        this.fileService = fileService;
    }

    private <T extends NucleotidesHolder> ListenableFuture<T> computeTrinucleotidesProbabilities(final T holder) {
        return executorService.submit(() -> {
            if (holder.getTotalTrinucleotide() >  0) {
                Set<String> keys = holder.getTrinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) holder.getTotalTrinucleotide();
                double result;
                for(String key: keys){
                    tmp1 = (double) holder.getTrinuStatPhase0().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu0(holder.getTotalProbaTrinu0() + result);
                    holder.getTrinuProbaPhase0().put(key, result);

                    tmp1 = (double) holder.getTrinuStatPhase1().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu1(holder.getTotalProbaTrinu1() + result);
                    holder.getTrinuProbaPhase1().put(key, result);

                    tmp1 = (double) holder.getTrinuStatPhase2().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaTrinu2(holder.getTotalProbaTrinu2() + result);
                    holder.getTrinuProbaPhase2().put(key, result);
                }
            }
            return holder;
        });
    }

    private <T extends NucleotidesHolder> ListenableFuture<T> computeDinucleotideProbabilities(final T holder) {
        return executorService.submit(() -> {
            if (holder.getTotalDinucleotide() > 0) {
                Set<String> keys = holder.getDinuStatPhase0().keySet();
                double tmp1, tmp2 = (double) holder.getTotalDinucleotide();
                double result;
                for(String key: keys){
                    tmp1 = (double) holder.getDinuStatPhase0().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaDinu0(holder.getTotalProbaDinu0() + result);
                    holder.getDinuProbaPhase0().put(key, result);

                    tmp1 = (double) holder.getDinuStatPhase1().get(key);
                    result = (tmp1 / tmp2) * 100.0;
                    holder.setTotalProbaDinu1(holder.getTotalProbaDinu1() + result);
                    holder.getDinuProbaPhase1().put(key, result);
                }
            }
            return holder;
        });
    }

    @Override
    public ListenableFuture<Gene> computeStatistics(Kingdom kingdom, Organism organism, Gene gene) {
        ListenableFuture<Gene> dinuFuture = computeDinucleotideProbabilities(gene);
        return Futures.transformAsync(dinuFuture, this::computeTrinucleotidesProbabilities, executorService);
    }

    @Override
    public ListenableFuture<Sum> computeSum(Kingdom kingdom, Organism organism, Sum sum, Gene gene) {
        return executorService.submit(() -> {
            sum.setDinuStatPhase0(addHashMaps(sum.getDinuStatPhase0(), gene.getDinuStatPhase0()));
            sum.setDinuStatPhase1(addHashMaps(sum.getDinuStatPhase1(), gene.getDinuStatPhase1()));

            sum.setTrinuStatPhase0(addHashMaps(sum.getTrinuStatPhase0(), gene.getTrinuStatPhase0()));
            sum.setTrinuStatPhase1(addHashMaps(sum.getTrinuStatPhase1(), gene.getTrinuStatPhase1()));
            sum.setTrinuStatPhase2(addHashMaps(sum.getTrinuStatPhase2(), gene.getTrinuStatPhase2()));
            
            sum.setTrinuPrefPhase0(addHashMaps(sum.getTrinuPrefPhase0(), gene.getTrinuPrefPhase0()));
            sum.setTrinuPrefPhase1(addHashMaps(sum.getTrinuPrefPhase1(), gene.getTrinuPrefPhase1()));
            sum.setTrinuPrefPhase2(addHashMaps(sum.getTrinuPrefPhase2(), gene.getTrinuPrefPhase2()));
            
            sum.setTotalPrefTrinu0(sum.getTotalPrefTrinu0() + gene.getTotalPrefTrinu0());
            sum.setTotalPrefTrinu1(sum.getTotalPrefTrinu1() + gene.getTotalPrefTrinu1());
            sum.setTotalPrefTrinu2(sum.getTotalPrefTrinu2() + gene.getTotalPrefTrinu2());

            sum.setTotalDinucleotide(sum.getTotalDinucleotide() + gene.getTotalDinucleotide());
            sum.setTotalTrinucleotide(sum.getTotalTrinucleotide() + gene.getTotalTrinucleotide());
            sum.setTotalUnprocessedCds(sum.getTotalUnprocessedCds() + gene.getTotalUnprocessedCds());
            sum.setTotalCds(sum.getTotalCds() + gene.getTotalCds());

            return sum;
        });
    }

    @Override
    public ListenableFuture<Sum> computeProbabilitiesFromSum(Organism organism, Sum organismSum) {
        return executorService.submit(() -> {
            computeTrinucleotidesProbabilities(organismSum).get();
            computeDinucleotideProbabilities(organismSum).get();

            return organismSum;
        });
    }

    private LinkedHashMap<String, Integer> addHashMaps(HashMap<String, Integer> hashMap1, HashMap<String, Integer> hashMap2) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        result.putAll(hashMap1);
        result.forEach((key, value) -> {
            Integer toAdd = hashMap2.get(key);
            if (toAdd != null) {
                result.put(key, value + toAdd);
            }
        });
        return result;
    }
}
