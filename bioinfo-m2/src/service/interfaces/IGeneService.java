package service.interfaces;

import java.util.Date;

import com.google.common.util.concurrent.ListenableFuture;

import model.Gene;
import model.Kingdom;
import model.Organism;

public interface IGeneService {
	Gene createGene(final String name, final String type, final String path, final int totalDinucleotides, final int totalTrinucleotides);
	
    ListenableFuture<Gene> processGene(Kingdom kingdom, Organism organism, Tuple<String, String> geneId);
}
