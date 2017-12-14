package service.impl;

import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;

import model.*;
import service.interfaces.*;

import java.io.InputStream;
import java.util.*;

public class GeneServiceImpl extends NucleotideHolderService implements IGeneService {
    private final int MAX_RETRIES = 5; // Retente 5 fois
    private final IStatisticsService statisticsService;
    private final IHttpService httpService;
    private final IParseService parseService;
    private final ListeningExecutorService executorService;
    private final IProgramStatsService programStatsService;
    private final IProgressService progressService;
    private final HashMap<String, Integer> retries = new HashMap<>();

    @Inject
    public GeneServiceImpl(IStatisticsService statisticsService, IHttpService httpService, IParseService parseService, ListeningExecutorService listeningExecutorService, IProgramStatsService programStatsService, IProgressService progressService) {
        this.statisticsService = statisticsService;
        this.httpService = httpService;
        this.parseService = parseService;
        this.executorService = listeningExecutorService;
        this.programStatsService = programStatsService;
        this.progressService = progressService;
    }

    private String generateUrlForGene(String id, Date date) {
    	Calendar c = Calendar.getInstance(); 
    	c.setTime(date);
    	
    	if((c.get(Calendar.MONTH) + 1) > 12) {
    		System.out.println("error in month:\t date:" + date.toString() + ", returns month #" + (c.get(Calendar.MONTH) + 1));
    	}
    	String req = "http://hsbiti.fr:8080/replicons/" + id + "/" + c.get(Calendar.YEAR) + "_" + (c.get(Calendar.MONTH) + 1) + "_" + c.get(Calendar.DAY_OF_MONTH);
    	return req;
        //return "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id="+id+"&rettype=fasta_cds_na&retmode=text";
    }

    public Gene createGene(String name, String type, String path, int totalDinucleotides, int totalTrinucleotides) {
    	if(type!=null)
    	{
    		type = type.trim();
    		if (!type.toLowerCase().equals("chromosome")
                    && !type.toLowerCase().equals("plasmid")
                    && !type.toLowerCase().equals("mitochondrion")
                    && !type.toLowerCase().equals("chloroplast")
                    && !type.toLowerCase().equals("dna")
                    && !type.toLowerCase().equals("unknown")) {
                type = "replicon";
            }
    	}
    	else
    	{
    		type="replicon";
    	}

        Gene gene = new Gene(name, type, path, totalDinucleotides, totalTrinucleotides);

        gene.setTrinuStatPhase0(initLinkedHashMap());
        gene.setTrinuStatPhase1(initLinkedHashMap());
        gene.setTrinuStatPhase2(initLinkedHashMap());

        gene.setTrinuProbaPhase0(initLinkedHashMapProba());
        gene.setTrinuProbaPhase1(initLinkedHashMapProba());
        gene.setTrinuProbaPhase2(initLinkedHashMapProba());

        gene.setTrinuPrefPhase0(initLinkedHashMap());
        gene.setTrinuPrefPhase2(initLinkedHashMap());
        gene.setTrinuPrefPhase1(initLinkedHashMap());

        gene.setDinuStatPhase0(initLinkedHashMapDinucleo());
        gene.setDinuStatPhase1(initLinkedHashMapDinucleo());

        gene.setDinuProbaPhase0(initLinkedHashMapDinucleoProba());
        gene.setDinuProbaPhase1(initLinkedHashMapDinucleoProba());

        return gene;
    }

    @Override
    public ListenableFuture<Gene> processGene(Kingdom kingdom, Organism organism, Tuple<String, String> geneId) {
        ListenableFuture<Gene> processGeneFuture = executorService.submit(() -> {
            Gene gene = createGene(geneId.getT1(), geneId.getT2(), organism.getPath(), 0, 0);

            progressService.getCurrentDownloadProgress().setDownloading(geneId.getT1() + " (" + kingdom.name() + ")");
            progressService.invalidateDownloadProgress();

            String url = generateUrlForGene(geneId.getT1(), organism.getUpdatedDate());
            HttpResponse httpResponse = httpService.get(url).get();

            progressService.getCurrentDownloadProgress().getProgress().incrementAndGet();
            progressService.getCurrentDownloadProgress().setDownloaded(geneId.getT1() + " (" + kingdom.name() + ")");
            progressService.invalidateDownloadProgress();

            InputStream inputStream = httpResponse.getContent();
            List<String> sequences;

            sequences = parseService.extractSequences(inputStream, gene).get();

            gene = extractStatisticsSequenceForDinucleotides(sequences, gene, 0);
            gene = extractStatisticsSequenceForTrinucleotides(sequences, gene, 0);
            gene = statisticsService.computeStatistics(kingdom, organism, gene).get();

            return gene;
        });

        return Futures.catchingAsync(processGeneFuture, Throwable.class, throwable -> {
            retries.putIfAbsent(geneId.getT1(), 0);
            Integer currentRetryCount = retries.get(geneId.getT1());
            if (geneId.getT1().startsWith("NC_") && currentRetryCount < MAX_RETRIES) {
                currentRetryCount++;
                retries.put(geneId.getT1(), currentRetryCount);
                return processGene(kingdom, organism, geneId);
            } else {
                return Futures.immediateFuture(null);
            }
        }, executorService);
    }

    @SuppressWarnings("unchecked")
	private Gene extractStatisticsSequenceForTrinucleotides(final String sequence, final Gene gene) {

            String codon0, codon1, codon2;

            int j = 0;
            
			LinkedHashMap<String, Integer> beforeCount0 = (LinkedHashMap<String, Integer>) gene.getTrinuStatPhase0().clone();
            LinkedHashMap<String, Integer> beforeCount1 = (LinkedHashMap<String, Integer>) gene.getTrinuStatPhase1().clone();
            LinkedHashMap<String, Integer> beforeCount2 = (LinkedHashMap<String, Integer>) gene.getTrinuStatPhase2().clone();

            for (int i = 0; i < sequence.length() - 3; i += 3) {
                codon0 = sequence.substring(i, i + 3);
                codon1 = sequence.substring(i + 1, i + 4);
                codon2 = sequence.substring(i + 2, i + 5);
                gene.getTrinuStatPhase0().put(codon0, gene.getTrinuStatPhase0().get(codon0) + 1);
                gene.getTrinuStatPhase1().put(codon1, gene.getTrinuStatPhase1().get(codon1) + 1);
                gene.getTrinuStatPhase2().put(codon2, gene.getTrinuStatPhase2().get(codon2) + 1);
                j++;
            }
            
            for (String s : beforeCount0.keySet()) {
            	beforeCount0.put(s, gene.getTrinuStatPhase0().get(s) - beforeCount0.get(s));
            	beforeCount1.put(s, gene.getTrinuStatPhase1().get(s) - beforeCount1.get(s));
            	beforeCount2.put(s, gene.getTrinuStatPhase2().get(s) - beforeCount2.get(s));
            	
            	int max = Math.max(beforeCount0.get(s), Math.max(beforeCount1.get(s), beforeCount2.get(s)));
            	
            	if (max == beforeCount0.get(s)) {
                	gene.getTrinuPrefPhase0().put(s, gene.getTrinuPrefPhase0().get(s) + 1);
                	gene.setTotalPrefTrinu0(gene.getTotalPrefTrinu0() + 1);
            	}
            	
            	if (max == beforeCount1.get(s)) {
                	gene.getTrinuPrefPhase1().put(s, gene.getTrinuPrefPhase1().get(s) + 1);
                	gene.setTotalPrefTrinu1(gene.getTotalPrefTrinu1() + 1);
            	}
            	
            	if (max == beforeCount2.get(s)) {
                	gene.getTrinuPrefPhase2().put(s, gene.getTrinuPrefPhase2().get(s) + 1);
                	gene.setTotalPrefTrinu2(gene.getTotalPrefTrinu2() + 1);
            	}
            }

            gene.setTotalTrinucleotide(gene.getTotalTrinucleotide() + j);

            return gene;
    }

    private Gene extractStatisticsSequenceForDinucleotides(final String sequence, final Gene gene) {
            String codon0, codon1;
            int j = 0;

            for (int i = 0; i < sequence.length()-(3+sequence.length()%2)+1; i += 2) {
                codon0 = sequence.substring(i, i + 2);
                codon1 = sequence.substring(i+1, i + 3);
                gene.getDinuStatPhase0().put(codon0, gene.getDinuStatPhase0().get(codon0) + 1);
                gene.getDinuStatPhase1().put(codon1, gene.getDinuStatPhase1().get(codon1) + 1);
                j ++;
            }

            gene.setTotalDinucleotide(gene.getTotalDinucleotide() + j);

            return gene;
    }

    private Gene extractStatisticsSequenceForDinucleotides(List<String> sequences, Gene gene, int index) {
        if (index < sequences.size()) {
            Gene geneTreated = extractStatisticsSequenceForDinucleotides(sequences.get(index), gene);
            return extractStatisticsSequenceForDinucleotides(sequences, geneTreated, index + 1);
        } else {
            return gene;
        }
    }

    private Gene extractStatisticsSequenceForTrinucleotides(List<String> sequences, Gene gene, int index) {
        if (index < sequences.size()) {
            Gene geneTreated = extractStatisticsSequenceForTrinucleotides(sequences.get(index), gene);
            return extractStatisticsSequenceForTrinucleotides(sequences, gene, index + 1);
        }
        return gene;
    }
}
