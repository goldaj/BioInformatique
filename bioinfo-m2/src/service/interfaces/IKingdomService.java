package service.interfaces;

import com.google.common.util.concurrent.ListenableFuture;

import model.Kingdom;

import java.util.List;
import java.util.Map;

public interface IKingdomService {

    String generateKingdomGeneListUrl(Kingdom kingdom);

    ListenableFuture<List<Kingdom>> createKingdomTrees(List<Kingdom> kingdoms, String bioProject);

    void interrupt();

    boolean getShouldInterrupt();
    
    void setGenesCkBIsSelected(Boolean isSelected);
    Boolean getGenesCkBIsSelected();
    void setGenomesCkBIsSelected(Boolean isSelected);
    Boolean getGenomesCkBIsSelected();
    public boolean getCreatingExcelParents();
    public void createParents(Kingdom kingdom, Map<Integer, List<String>> map, String folderPath, String folderName, int level, int max);
}
