package service.interfaces;

import com.google.common.util.concurrent.ListenableFuture;

import model.Gene;
import model.Organism;

import java.io.InputStream;
import java.util.List;

public interface IParseService {
    ListenableFuture<List<Organism>> extractOrganismList(InputStream inputStream, String kingdomId);
    ListenableFuture<List<String>> extractSequences(final InputStream inputStream, Gene gene);
}
