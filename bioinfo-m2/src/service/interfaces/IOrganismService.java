package service.interfaces;

import com.google.common.util.concurrent.ListenableFuture;

import model.Gene;
import model.Kingdom;
import model.Organism;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IOrganismService {
    Organism createOrganism(String name, String bioProject, String group, String subGroup, Date updateDate, List<Tuple<String, String>> geneIds, String kingdomId);

    DateFormat getUpdateDateFormat();

    ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism);
    
    ListenableFuture<Organism> processOrganismWithoutGene(Map<String,Gene> genes, Kingdom kingdom, Organism organism);

    ListenableFuture<Organism> processOrganism(Kingdom kingdom, Organism organism, HashMap<String, Gene> plasmidGenesMap);
}
