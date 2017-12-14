package service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import model.*;
import service.exception.EmptyFileException;
import service.interfaces.IConfigService;
import service.interfaces.IKingdomService;
import service.interfaces.IOrganismService;
import service.interfaces.IParseService;
import service.interfaces.Tuple;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseServiceImpl implements IParseService {
    private final IOrganismService organismService;
    private final ListeningExecutorService executorService;
    private final IConfigService configService;
    private final IKingdomService kingdomService;

    private Date parseDateColumn(String column) throws ParseException {
        if (column == null || column.equals("-")) return null;
        String[] parts = column.trim().split("/");
        if (parts.length != 3) return null;
        String pattern;
        if (parts[0].length() == 4) {
            pattern = "yyyy/mm/dd";
        } else {
            pattern = "dd/mm/yyyy";
        }
        DateFormat dateFormat = new SimpleDateFormat(pattern, Locale.FRENCH);
        return dateFormat.parse(column);
    }

    @Inject
    public ParseServiceImpl(IOrganismService organismService, ListeningExecutorService listeningExecutorService, IConfigService configService , IKingdomService kingdomService) {
        this.organismService = organismService;
        this.executorService = listeningExecutorService;
        this.configService = configService;
        this.kingdomService = kingdomService;
    }

    private static boolean checkLocator(String locator) {
        if (locator == null) {
            return false;
        }
        String[] indexes = locator.split("\\.\\.");
        return indexes.length == 2 && Integer.parseInt(indexes[0]) < Integer.parseInt(indexes[1]);
    }

    private static boolean checkSequence(String sequence) {
        return sequence != null
                && (sequence.length() > 0)
                && (sequence.length() % 3 == 0)
                && CodingSequence.InitCodon.contains(sequence.substring(0, 3))
                && CodingSequence.StopCodon.contains(sequence.substring(sequence.length() - 3))
                && !Pattern.compile(CodingSequence.REGEX_ATGC).matcher(sequence).find();
    }

    /**
     * Extracts the sequences from a given input stream for the given gene.
     */
    @Override
    public ListenableFuture<List<String>> extractSequences(final InputStream inputStream, Gene gene) {
        return executorService.submit(() -> {
            List<String> sequences = new ArrayList<String>();
            String line;
            boolean running;

            System.out.println("Extracting : " + gene.getName());

            //Gene
            FileWriter fwGene = null;
            BufferedWriter bwGene = null;
            String zipGenePath = configService.getProperty("gene");
            //Genome
            FileWriter fwGenome = null;
            BufferedWriter bwGenome = null;
            String zipGenomePath = configService.getProperty("genome");

            String[] explodePath = (gene.getPath() != null) ? gene.getPath().split("/"): null;

            if(kingdomService.getGenesCkBIsSelected() && gene.getPath() != null){
                for(int i = 2; i < explodePath.length; i++){
                    zipGenePath += explodePath[i] + "/";
                }

                fwGene = new FileWriter(new File(zipGenePath + gene.getName() + ".txt"));
                bwGene =  new BufferedWriter(fwGene);
            }
            
            if(kingdomService.getGenomesCkBIsSelected() && gene.getPath() != null){
                for(int i = 2; i < explodePath.length; i++){
                	zipGenomePath += explodePath[i] + "/";
                }

                fwGenome = new FileWriter(new File(zipGenomePath + gene.getName() + ".txt"));
                bwGenome =  new BufferedWriter(fwGenome);
            }

            boolean fileIsEmpty = true;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    fileIsEmpty = false;
                }
                if(kingdomService.getGenesCkBIsSelected() && gene.getPath() != null){
                    bwGene.write(line);
                    bwGene.newLine();
                }

                if(kingdomService.getGenomesCkBIsSelected() && gene.getPath() != null){
                    bwGenome.write(line);
                    bwGenome.newLine();
                }

                if (line.startsWith(CodingSequence.START_CDS_INFO)) {
                    Pattern p = Pattern.compile(CodingSequence.REGEX_COMPLETE);
                    Matcher m = p.matcher(line);

                    if (m.find()) {
                        String s = m.group();
                        p = Pattern.compile(CodingSequence.REGEX_LOCATOR);
                        m = p.matcher(s);
                        boolean locators_ok = true;
                        while (m.find() && locators_ok) {
                            locators_ok = checkLocator(m.group());
                        }
                        if (locators_ok) {
                            String sequence = "", line2 = null;

                            running = true;
                            while (running) {
                                reader.mark(1);
                                int temp = reader.read();
                                if (temp == -1) {
                                    running = false;
                                } else {
                                    reader.reset();
                                    if (temp == CodingSequence.START_CDS_INFO.charAt(0)) {
                                        running = false;
                                    } else {
                                        try {
                                            line2 = reader.readLine();

                                            if(kingdomService.getGenesCkBIsSelected()){
                                                bwGene.write(line2);
                                                bwGene.newLine();
                                            }

                                            if(kingdomService.getGenomesCkBIsSelected()){
                                                bwGenome.write(line2);
                                                bwGenome.newLine();
                                            }

                                        } catch (SocketException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            running = false;
                                        }
                                        if (line2 == null) {
                                            running = false;
                                        } else {
                                            sequence += line2;
                                        }
                                    }
                                }
                            }

                            if (checkSequence(sequence)) {
                                gene.setTotalCds(gene.getTotalCds() + 1);

                                sequences.add(sequence);
                            } else {
                                gene.setTotalCds(gene.getTotalCds() + 1);
                                gene.setTotalUnprocessedCds(gene.getTotalUnprocessedCds() + 1);
                            }
                        }
                    }
                }
            }

            inputStream.close();

            // If the file was empty, we throw an exception.
            if (fileIsEmpty) {
                throw new EmptyFileException();
            }

            if(kingdomService.getGenesCkBIsSelected()){
                bwGene.close();
            }

            if(kingdomService.getGenomesCkBIsSelected()){
                bwGenome.close();
            }

            return sequences;
        });
    }

    /**
     * Parses the organisms in the given input stream.
     */
    @Override
    public ListenableFuture<List<Organism>> extractOrganismList(final InputStream inputStream, final String kingdomId) {
        return executorService.submit(() -> {
            List<Organism> organisms = new ArrayList<Organism>();
            String sep = "\t";

            String line;


            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            try {
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                bufferedReader.readLine();
                while ((line = bufferedReader.readLine()) != null) {
                    String[] data = line.split(sep);
                    if (data.length == 0) {
                        break;
                    }

                    String name;
                    ProkaryoteGroup prokaryoteGroup = null;
                    String group;
                    String subGroup;
                    String bioProject;
                    Date updateDate = null;
                    List<Tuple<String, String>> geneIds;

                    if (Kingdom.Eukaryota.equals(kingdomId)) {
                        name = data[0];
                        bioProject = data[3];
                        group = data[4];
                        subGroup = data[5];
                        geneIds = extractGeneIds(data[9]);
                        updateDate = parseDateColumn(data[15]);
                    } else if (Kingdom.Plasmids.equals(kingdomId)){
                        name = data[0];
                        bioProject = null;
                        group = data[2];
                        subGroup = data[3];
                        geneIds = extractGeneIds(data[5]);
                        updateDate = parseDateColumn(data[16]);
                    } else if (Kingdom.Prokaryotes.equals(kingdomId) || Kingdom.Bacteria.equals(kingdomId) || Kingdom.Archaea.equals(kingdomId)) {
                        name = data[0];
                        bioProject = data[4];
                        try {
                            prokaryoteGroup = ProkaryoteGroup.buildByName(data[5]);
                        } catch (Exception e) {
                            prokaryoteGroup = null;
                        }
                        group = data[5];
                        subGroup = data[6];
                        geneIds = extractGeneIds(data[10]);
                        updateDate = parseDateColumn(data[16]);
                    } else if (Kingdom.Viruses.equals(kingdomId)) {
                        name = data[0];
                        bioProject = data[1];
                        group = data[2];
                        subGroup = data[3];
                        geneIds = extractGeneIds(data[7]);
                        updateDate = parseDateColumn(data[11]);
                    }
                    //TODO: check that parsing
                    else if (Kingdom.Organelles.equals(kingdomId)) {
                        name = data[0];
                        bioProject = data[1];
                        group = data[2];
                        subGroup = data[3];
                        geneIds = extractGeneIds(data[4]);
                        updateDate = parseDateColumn(data[14]);
                    }
                    else if (Kingdom.Viroids.equals(kingdomId)) {
                        name = data[0];
                        bioProject = data[1];
                        group = data[2];
                        subGroup = data[3];
                        geneIds = extractGeneIds(data[7]);
                        updateDate = parseDateColumn(data[11]);
                    }
                    else {
                        name = null;
                        bioProject = null;
                        group = null;
                        subGroup = null;
                        geneIds = null;
                    }

                    Organism organism = organismService.createOrganism(name, bioProject, group, subGroup, updateDate, geneIds, kingdomId);

                    //TODO: check if bacteria & archaea need to be put in prokaryotes here
                    if (Kingdom.Prokaryotes.equals(kingdomId)) {
                        organism.setProkaryoteGroup(prokaryoteGroup);
                    }

                    organisms.add(organism);
                    /*
                    if (data.length > updatedDateIndex) {
                        if (data[updatedDateIndex].compareTo("-") == 0) {
                            updatedDate = new Date();
                        } else {
                            try {
                                updatedDate = format.parse(data[updatedDateIndex]);
                            } catch (ParseException e) {
                                System.err.println(e.getDownloaded());
                                e.printStackTrace();
                            }
                        }
                    }
                    */
                }
                inputStream.close();
                inputStreamReader.close();
                bufferedReader.close();
            } catch (Exception e){
                e.printStackTrace();
            }

            return organisms.stream().filter(organism -> organism.getGeneIds() != null && organism.getGeneIds().size() > 0 && ((!Kingdom.Prokaryotes.equals(kingdomId)) || (Kingdom.Prokaryotes.equals(kingdomId) && organism.getProkaryoteGroup() != null))).collect(Collectors.toList());
        });

    }

    /**
     * Extracts the geneIds from the given entry in the organism list file. It returns a tuple containing the gene name, and it's type (chromosome, mitochondrion...)
     */
    private List<Tuple<String, String>> extractGeneIds(String segmentsColumn){
        String[] tmpSegments;
        List<Tuple<String, String>> segments;
        if (segmentsColumn.compareTo("-") == 0) {
            return null;
        } else {
            tmpSegments = segmentsColumn.split(";");
            segments = new ArrayList<>(tmpSegments.length);
            if (tmpSegments.length > 0) {
                Tuple<String, String> tuple;
                for (String segment : tmpSegments) {
                    String[] segmentParts = segment.split(":");
                    if (segmentParts.length == 2) {
                        tuple = new Tuple<>(segmentParts[1].trim().split("/")[0], segmentParts[0].trim().split(" ")[0]);
                    } else {
                        tuple = new Tuple<>(segmentParts[0].trim().split("/")[0], null);
                    }

                    if ((tuple.getT1() != null && tuple.getT2() != null && tuple.getT2().trim().toLowerCase().equals("chromosome") && tuple.getT1().trim().toLowerCase().startsWith("nc"))
                            || (tuple.getT1() != null && tuple.getT2() != null && !tuple.getT2().trim().toLowerCase().equals("chromosome"))
                            || (tuple.getT1() != null && tuple.getT2() == null)) {
                        segments.add(tuple);
                    }
                }
            }
            return segments;
        }
		/*
        String[] res;
        if(line.compareTo("-") != 0)
        {
            res = line.split(";");
            if(res.length>0)
            {
                for(int i=0;i<res.length;i++)
                {
                    String segment = res[i];
                    String[] segmentParts = segment.split(":");
                    if (segmentParts.length == 2) {
                        segment = segmentParts[1];
                    }
                    res[i] = segment.split("/")[0];
                }
            }
            else
            {
                res = null;
            }
        }
        else{
            res = null;
        }
        return res;
		 */
    }
}
