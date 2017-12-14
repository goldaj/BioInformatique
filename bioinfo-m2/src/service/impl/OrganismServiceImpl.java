package service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import model.Gene;
import model.Kingdom;
import model.Organism;
import model.Sum;
import service.interfaces.*;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

public class OrganismServiceImpl extends NucleotideHolderService implements IOrganismService {
    private final IGeneService geneService;
    private final ListeningExecutorService executorService;
    private final IProgressService progressService;
    private final IFileService fileService;
    private final IProgramStatsService programStatsService;
    private final IStatisticsService statisticsService;

    @Inject
    public OrganismServiceImpl(IGeneService geneService, ListeningExecutorService listeningExecutorService, IProgressService progressService, IFileService fileService, IProgramStatsService programStatsService, IStatisticsService statisticsService) {
        this.geneService = geneService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.fileService = fileService;
        this.programStatsService = programStatsService;
        this.statisticsService = statisticsService;
    }

    @Override
    public Organism createOrganism(final String name, final String bioProject, final String group, final String subGroup, final Date updateDate, final List<Tuple<String, String>> geneIds, final String kingdomId) {
        return new Organism(name, bioProject, group, subGroup, updateDate, geneIds, kingdomId);
    }

    @Override
    public DateFormat getUpdateDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    
    @Override
    public ListenableFuture<Organism> processOrganismWithoutGene(Map<String,Gene> genes, Kingdom kingdom, Organism organism) {
        return executorService.submit(() -> {
        	HashMap<String, Sum> organismSums = new HashMap<>();
            XSSFWorkbook workbook = fileService.createWorkbook();
            
            for(String key : genes.keySet())
            {
            	Gene gene=genes.get(key);
            	if (gene != null) {
                    String type = gene.getType();
                    if (type == null) {
                        type = "replicon";
                    }
                    organismSums.putIfAbsent(type, createSum(gene.getType(), organism.getPath(), 0, 0));
                    Sum sum = organismSums.get(gene.getType());
                    statisticsService.computeSum(kingdom, organism, sum, gene).get();
                }
            }
            
            for (Map.Entry<String, Sum> organismSumEntry: organismSums.entrySet()) {
                organismSumEntry.setValue(statisticsService.computeProbabilitiesFromSum(organism, organismSumEntry.getValue()).get());
            }
            fileService.fillWorkbookSum(organism, organismSums, workbook);
            
            fileService.writeWorkbook(workbook, organism.getPath(), organism.getName());

            System.out.println(organism.getName());

            return organism;
        });
    }

    public ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism) {
        return processOrganism(kingdom, organism, null);
    }

    @Override
    public ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism, HashMap<String, Gene> plasmidGenesMap) {
        return executorService.submit(() -> {
            HashMap<String, Sum> organismSums = new HashMap<>();
            List<Tuple<String, String>> geneIds = organism.getGeneIds();
            List<ListenableFuture<Gene>> geneFutures = new ArrayList<>();
            for (Tuple<String, String> geneId: geneIds) {
            	if(geneId.getT1().startsWith("NC_")) {
	                if (Kingdom.Prokaryotes.equals(kingdom) && plasmidGenesMap != null) {
	                    geneFutures.add(executorService.submit(() -> plasmidGenesMap.get(geneId.getT1())));
	                } else {
	                    geneFutures.add(geneService.processGene(kingdom, organism, geneId));
	                }
            	}
            }
            List<Gene> genes = Futures.successfulAsList(geneFutures).get();

            XSSFWorkbook workbook = fileService.createWorkbook();
            for (Gene gene: genes) {
                if (gene != null) {
                    String type = gene.getType();
                    if (type == null) {
                        type = "replicon";
                    }
                    organismSums.putIfAbsent(type, createSum(gene.getType(), organism.getPath(), 0, 0));
                    Sum sum = organismSums.get(gene.getType());
                    statisticsService.computeSum(kingdom, organism, sum, gene).get();
                    fileService.fillWorkbook(organism, gene, workbook);
                }
            }

            for (Map.Entry<String, Sum> organismSumEntry: organismSums.entrySet()) {
                organismSumEntry.setValue(statisticsService.computeProbabilitiesFromSum(organism, organismSumEntry.getValue()).get());
            }
            fileService.fillWorkbookSum(organism, organismSums, workbook);

            fileService.writeWorkbook(workbook, organism.getPath(), organism.getName());

            programStatsService.addDate(ZonedDateTime.now());
            programStatsService.setRemainingRequests(programStatsService.getRemainingRequests());
            progressService.getCurrentProgress().getProgress().incrementAndGet();
            progressService.invalidateProgress();

            System.out.print(organism.getName());

            return organism;
        });
    }

    private Sum createSum(final String type, final String path, final int totalDinucleotides, final int totalTrinucleotides) {

        Sum sum = new Sum(type, path, totalDinucleotides, totalTrinucleotides);

        sum.setTrinuStatPhase0(initLinkedHashMap());
        sum.setTrinuStatPhase1(initLinkedHashMap());
        sum.setTrinuStatPhase2(initLinkedHashMap());

        sum.setTrinuProbaPhase0(initLinkedHashMapProba());
        sum.setTrinuProbaPhase1(initLinkedHashMapProba());
        sum.setTrinuProbaPhase2(initLinkedHashMapProba());

        sum.setTrinuPrefPhase0(initLinkedHashMap());
        sum.setTrinuPrefPhase2(initLinkedHashMap());
        sum.setTrinuPrefPhase1(initLinkedHashMap());

        sum.setDinuStatPhase0(initLinkedHashMapDinucleo());
        sum.setDinuStatPhase1(initLinkedHashMapDinucleo());

        sum.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
        sum.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());

        return sum;
    }
}
