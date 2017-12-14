package service.impl;

import com.google.api.client.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;

import model.Gene;
import model.Kingdom;
import model.Organism;
import service.exception.NothingToProcesssException;
import service.interfaces.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KingdomServiceImpl implements IKingdomService {
    private final int PROCESS_STACK_SIZE = 50;
    private final IStatisticsService statisticsService;
    private final IParseService parseService;
    private final IFileService fileService;
    private final IConfigService configService;
    private final IOrganismService organismService;
    private final IHttpService httpService;
    private final ListeningExecutorService executorService;
    private final IProgressService progressService;
    private final IProgramStatsService programStatsService;
    private final IZipService zipService;
    private final IGeneService geneService;
    private HashMap<Kingdom, Map<String, Date>> updates = new HashMap<>();
    private HashMap<Kingdom, ListenableFuture<List<Organism>>> currentFutures = new HashMap<>();
    private boolean shouldInterrupt = false;
    private Boolean genesCkBIsSelected;
    private Boolean genomesCkBIsSelected;
    private boolean creatingExcelParents = false;
    private HashMap<String, Gene> plasmidGenesMap = new HashMap<>();

    @Inject
    public KingdomServiceImpl(IStatisticsService statisticsService,
                                 IFileService fileService,
                                 IParseService parseService,
                                 IConfigService configService,
                                 IOrganismService organismService,
                                 IHttpService httpService,
                                 ListeningExecutorService listeningExecutorService,
                                 IProgressService progressService,
                                 IProgramStatsService programStatsService,
                                 IZipService zipService,
                                 IGeneService geneService) {
        this.statisticsService=statisticsService;
        this.fileService = fileService;
        this.parseService = parseService;
        this.configService = configService;
        this.organismService = organismService;
        this.httpService = httpService;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
        this.programStatsService = programStatsService;
        this.zipService = zipService;
        this.geneService = geneService;

        this.genomesCkBIsSelected = false;
        this.genesCkBIsSelected = false;
    }

    @Override
    public String generateKingdomGeneListUrl(Kingdom kingdom) {
        try {
            URL url;
            String urlString;
            if(kingdom.getId().equals(Kingdom.Plasmids.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=plasmids&king=All&group=All&subgroup=All&format=";
            } else if(kingdom.getId().equals(Kingdom.Viruses.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50%7C40%7C30%7C20%7C%3B&host=All&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else if(kingdom.getId().equals(Kingdom.Organelles.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50%7C40%7C30%7C20%7C%3B&host=All&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else if(kingdom.getId().equals(Kingdom.Viroids.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50%7C40%7C30%7C20%7C%3B&host=All&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else if (kingdom.getId().equals(Kingdom.Bacteria.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + Kingdom.Prokaryotes.getId() + "&status=50%7C40%7C30%7C20%7C%3B&host=All&group=--%20All%20" + Kingdom.Prokaryotes.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else if (kingdom.getId().equals(Kingdom.Archaea.getId())) {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + Kingdom.Prokaryotes.getId() + "&status=50%7C40%7C30%7C20%7C%3B&host=All&group=--%20All%20" + Kingdom.Prokaryotes.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
            } else if(kingdom.getId().equals(Kingdom.Prokaryotes.getId())) {
                System.out.println("trying to download proka");
                //urlString = null;
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50%7C40%7C30%7C20%7C%3B&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--&format=";
            } else {
                urlString = "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50%7C40%7C30%7C20%7C%3B&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--&format=";
            }
            url = new URL(urlString);
            return url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void interrupt() {
        for (Map.Entry<Kingdom, ListenableFuture<List<Organism>>> currentFuture: currentFutures.entrySet()) {
            currentFuture.getValue().cancel(true);
            currentFuture.getKey().setOrganisms(null);
            shouldInterrupt = true;
            writeUpdateFile(currentFuture.getKey());
        }
    }

    private ListenableFuture<List<Boolean>> createDirectories(final Kingdom kingdom, final InputStream inputStream) {
        String dataDir = configService.getProperty("dataDir");
        String zipGene = configService.getProperty("gene");
        String zipGenome = configService.getProperty("genome");
        Boolean tmpGenesCkBIsSelected = genesCkBIsSelected;
        Boolean tmpGenomesCkBIsSelected = genomesCkBIsSelected;

        return Futures.transform(parseService.extractOrganismList(inputStream, kingdom.getId()), (Function<List<Organism>, List<Boolean>>) organisms -> {
            List < String > paths = new ArrayList<>();

            for (Organism organism : organisms) {
                String path;
                if (kingdom.equals(Kingdom.Prokaryotes)) {
                    path = dataDir
                            + organism.getProkaryoteGroup().getType()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup();
                } else {
                    path = dataDir
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup();
                }
                organism.setPath(path);
                paths.add(path);

                String zipPath = null;

                if(tmpGenesCkBIsSelected){
                    zipPath = zipGene
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup()
                            + "/" + organism.getName();
                    paths.add(zipPath);
                }

                if(tmpGenomesCkBIsSelected){
                    zipPath = zipGenome
                            + kingdom.getLabel()
                            + "/" + organism.getGroup()
                            + "/" + organism.getSubGroup()
                            + "/" + organism.getName();
                    paths.add(zipPath);
                }
            }
            kingdom.setOrganisms(organisms);

            return fileService.createDirectories(paths);

        }, executorService);
    }

    private ListenableFuture<Kingdom> loadUpdateFile(Kingdom kingdom) {
        return executorService.submit(() -> {
            try {
                updates.putIfAbsent(kingdom, fileService.readUpdateFile(kingdom));
            } catch (IOException e) {
                e.printStackTrace();
            }
            updates.putIfAbsent(kingdom, new HashMap<>());
            return kingdom;
        });
    }

    /**
     * This is one of the most important methods.
     * It first loads the update file associated to the kingdom (under "./updates/{kingdomLabel}")
     * then it retrieves it from the eutils API, then reads the response, then creates or check if the directories exist,
     * then checks if an update is needed, then notifies the user interface through the progressService of the number of organisms to process,
     * then processes the organisms, and if the later is a success or a failure, writes the current updates file.
     */
    private ListenableFuture<Kingdom> createKingdomTree(Kingdom kingdom, String bioProject) {
        return executorService.submit(() -> {
            shouldInterrupt = false;
            Kingdom modifiedKingdom = loadUpdateFile(kingdom).get();
            String url = generateKingdomGeneListUrl(kingdom);
            ListenableFuture<HttpResponse> listenableFuture = httpService.get(url);
            HttpResponse httpResponse = listenableFuture.get();
            //HttpResponse httpResponse = httpService.get(url).get();
            InputStream content = httpResponse.getContent();
            List<Boolean> creationResults = createDirectories(kingdom, content).get();

            // Merge plasmids to prokaryote organisms
            if (kingdom.equals(Kingdom.Prokaryotes)) {
                Kingdom plasmidsKingdom = Kingdom.Plasmids;
                //Kingdom modifiedPlasmidsKingdom = loadUpdateFile(plasmidsKingdom).get();
                String plasmidsUrl = generateKingdomGeneListUrl(plasmidsKingdom);
                HttpResponse httpPlasmidResponse = httpService.get(plasmidsUrl).get();
                InputStream plasmidsContent = httpPlasmidResponse.getContent();
                List<Organism> plasmids = parseService.extractOrganismList(plasmidsContent, plasmidsKingdom.getId()).get();
                List<Organism> fullPlasmids = new ArrayList<>();
                HashMap<String, Gene> plasmidGenes = new HashMap<String, Gene>();
                for (Organism plasmid: plasmids) {
                    for (Tuple<String, String> geneId: plasmid.getGeneIds()) {
                        Tuple<String, String> newGeneId = new Tuple<>(geneId.getT1(), "plasmid");
                        plasmid.getGeneIds().remove(geneId);
                        plasmid.getGeneIds().add(newGeneId);
                    }
                    if (!fullPlasmids.contains(plasmid)) {
                        fullPlasmids.add(plasmid);
                    } else {
                        Organism existingPlasmid = fullPlasmids.get(fullPlasmids.indexOf(plasmid));
                        existingPlasmid.getGeneIds().addAll(plasmid.getGeneIds());
                    }
                }

                for (Organism prokaryote: kingdom.getOrganisms()) {
                    for (Organism plasmid: fullPlasmids) {
                        if (prokaryote.getName().startsWith(plasmid.getName())) {
                            prokaryote.getGeneIds().addAll(plasmid.getGeneIds());
                        }
                    }
                }


                List<ListenableFuture<Gene>> geneFutures = new ArrayList<ListenableFuture<Gene>>();
                int j = 0;
                for (int i = 0; i < fullPlasmids.size(); i++) {
                    j++;
                    Organism plasmid = fullPlasmids.get(i);
                    for (Tuple<String, String> geneId: plasmid.getGeneIds()) {
                    	if(geneId.getT1().startsWith("NC_")) {
                    		geneFutures.add(geneService.processGene(plasmidsKingdom, plasmid, geneId));
                    	}
                    }
                    if ((i > 0 && i % PROCESS_STACK_SIZE == 0) || j >= fullPlasmids.size()) {
                        List<Gene> currentlyProcessedGenes = Futures.allAsList(geneFutures).get();
                        for (Gene gene: currentlyProcessedGenes) {
                            plasmidGenes.put(gene.getName(), gene);
                        }
                        geneFutures.clear();
                    }
                }
                plasmidGenesMap = plasmidGenes;
            }

            List<Organism> filteredOrganisms = kingdom.getOrganisms()
                    .stream()
                    .filter(organism -> {
                        if (organism == null) {
                            return false;
                        }
                        Date remoteUpdate = organism.getUpdatedDate();
                        Date localUpdate = updates.get(kingdom).get(organism.getName());
                        // No local update found
                        return (bioProject == null || (organism.getBioProject() != null && bioProject.equals(organism.getBioProject())))
                                && (remoteUpdate == null || localUpdate == null || localUpdate.before(remoteUpdate));
                    })
                    .collect(Collectors.toList());
            kingdom.setOrganisms(filteredOrganisms);

            if (filteredOrganisms.size() == 0) {
                throw new NothingToProcesssException();
            }

            progressService.getCurrentProgress().setStep(TaskProgress.Step.DirectoriesCreationFinished);
            progressService.invalidateProgress();

            TaskProgress progress = progressService.getCurrentProgress();
            progress.setStep(TaskProgress.Step.OrganismProcessing);
            progress.getTotal().addAndGet(kingdom.getOrganisms().size());
            progressService.invalidateProgress();

            programStatsService.setRemainingRequests(programStatsService.getRemainingRequests() + kingdom.getOrganisms().size());


            return processKingdom(modifiedKingdom, 0);
        });
    }

    private Kingdom processKingdom(Kingdom kingdom, int index) throws ExecutionException, InterruptedException {
        if (!shouldInterrupt) {
            int nextIndex = index + PROCESS_STACK_SIZE;
            if (nextIndex > kingdom.getOrganisms().size()) {
                nextIndex = kingdom.getOrganisms().size();
            }
            if (index < kingdom.getOrganisms().size()) {
                List<ListenableFuture<Organism>> organismFutures = new ArrayList<>();
                for (Organism organism : kingdom.getOrganisms().subList(index, nextIndex)) {
                    if (Kingdom.Prokaryotes.equals(kingdom)) {
                        organismFutures.add(organismService.processOrganism(kingdom, organism, plasmidGenesMap));
                    } else {
                        organismFutures.add(organismService.processOrganism(kingdom, organism));
                    }
                }

                ListenableFuture<List<Organism>> currentKingdomFuture = Futures.successfulAsList(organismFutures);
                currentFutures.put(kingdom, currentKingdomFuture);
                List<Organism> organisms = currentKingdomFuture.get();
                for (Organism organism : organisms) {
                    if (organism != null) {
                        updates.get(kingdom).put(organism.getName(), new Date());
                        writeUpdateFile(kingdom);
                    }
                }

                return processKingdom(kingdom, index + PROCESS_STACK_SIZE);
            }
        }
        return kingdom;
    }

    public boolean getCreatingExcelParents()
    {
        return this.creatingExcelParents;
    }

    public void createParents(Kingdom kingdom, Map<Integer, List<String>> map, String folderPath, String folderName, int level, int max)
    {
        File folder;
        File[] childrenFolders;

        boolean good=true;

        if(level==0)
        {
            folder=new File(folderPath+"/"+folderName);
            childrenFolders=folder.listFiles();
            if(childrenFolders != null) {
                for (File childFolder : childrenFolders) {
                    if (childFolder.isDirectory()) {
                        good = false;
                        break;
                    }
                }
            }
        }
        else
        {
            good=true;
            folder=new File(map.get(level-1).get(max));
            childrenFolders=folder.listFiles();
        }

        if(good) {
            Organism org=organismService.createOrganism("Total_"+folder.getName(), "", "", "", new Date(), new ArrayList<Tuple<String,String>>(), kingdom.getId());

            if(level==0) {
                org.setPath(folderPath);
            } else {
                org.setPath(folder.getParent());
            }

            Map<String,Gene> mapGene=new Hashtable<String,Gene>();
            if(childrenFolders == null) {
                childrenFolders = new File[]{};
            }
            for (File excel : childrenFolders) {
                if (excel.isFile()) // only necessary on level >0
                {
                    try {
                        mapGene = fileService.readWorkbooks(mapGene, excel, 1);
                        for (String key : mapGene.keySet()) {
                            try {
                                statisticsService.computeStatistics(kingdom, org, mapGene.get(key)).get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {

                    }
                }
            }
            organismService.processOrganismWithoutGene(mapGene, kingdom, org);
        }
        else
        {
            for(File childFolder : childrenFolders)
            {
                if(childFolder.isDirectory())
                {
                    if(level==0)
                    {
                        if(map.get(max+1)==null)
                        {
                            List<String> liste=new ArrayList<String>();
                            liste.add(folder.getPath());
                            map.put(max+1, liste);
                        }
                        else
                        {
                            List<String> liste=map.get(max+1);
                            if(!liste.contains(folder.getPath()))
                            {
                                liste.add(folder.getPath());
                            }

                        }
                        createParents(kingdom,map,folder.getPath(),childFolder.getName(),0,max+1);
                    }
                    else
                    {
                        createParents(kingdom,map,folder.getPath(),childFolder.getName(),level,max+1);
                    }

                }
            }
        }
    }

    private void writeUpdateFile(Kingdom kingdom) {
        try {
            fileService.writeUpdateFile(kingdom, updates.get(kingdom));
        } catch (IOException e) {
            System.err.println("Unable to write update file for kingdom " + kingdom);
        }
    }

    /**
     * Creates the file trees for the given kingdoms and starts the time estimation.
     * The remaining time is estimated over the 100 last processings.
     */
    public ListenableFuture<List<Kingdom>> createKingdomTrees(final List<Kingdom> kingdoms, String bioProject) {
        programStatsService.resetAcquisitionTime();
        List<ListenableFuture<Kingdom>> acquireFutures = new ArrayList<>();
        for (Kingdom kingdom: kingdoms) {
            if(!kingdom.getId().equals(Kingdom.Prokaryotes.getId())) {
                acquireFutures.add(createKingdomTree(kingdom, bioProject));
            }
        }
        return Futures.transformAsync(Futures.successfulAsList(acquireFutures), processedKingdoms -> {
            //programStatsService.endAcquisitionTimeEstimation();
            // We check if something was processed.
            if (processedKingdoms == null) {
                throw new NothingToProcesssException();
            }
            List<Kingdom> succefullyProcessedKingdoms = kingdoms.stream()
                    .filter(kingdom -> kingdom.getOrganisms().size() > 0)
                    .collect(Collectors.toList());
            if (succefullyProcessedKingdoms.size() == 0) {
                throw new NothingToProcesssException();
            }
            return Futures.immediateFuture(kingdoms);
        }, executorService);
    }

    @Override
    public boolean getShouldInterrupt() {
        return shouldInterrupt;
    }

    public void setGenomesCkBIsSelected(Boolean isSelected){
        this.genomesCkBIsSelected = isSelected;
    }

    public Boolean getGenomesCkBIsSelected(){
        return this.genomesCkBIsSelected;
    }

    public void setGenesCkBIsSelected(Boolean isSelected){
        this.genesCkBIsSelected = isSelected;
    }

    public Boolean getGenesCkBIsSelected(){
        return this.genesCkBIsSelected;
    }
}
