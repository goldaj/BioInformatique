package service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.commons.io.FileUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import service.interfaces.IZipService;

public class ZipServiceImpl implements IZipService {

	public final ListeningExecutorService executorService;
	
	@Inject
	public ZipServiceImpl(ListeningExecutorService listeningExecutorService){
		this.executorService = listeningExecutorService;
	}
	
	public ListenableFuture<Boolean> createGenomeDirectory(File node) {
		return this.executorService.submit(() -> {
			File fileGenome = null;
			FileWriter fwGenome = null;
			BufferedWriter bwGenome = null;
			FileReader frGenome = null;
			BufferedReader brGenome = null;
			String line;
			if (node.isFile())
			{
				try{			
					if(node.getName().compareTo(node.getParentFile().getName()+".zip") != 0)
					{
						frGenome = new FileReader(node);
						brGenome =  new BufferedReader(frGenome);

						fileGenome = new File(node.getParent() + File.separator +  node.getParentFile().getName() + ".txt");
						if(fileGenome.exists()){
							fwGenome = new FileWriter(fileGenome,true);
							bwGenome =  new BufferedWriter(fwGenome);
							bwGenome.newLine();
							bwGenome.newLine();
						} else{
							fwGenome = new FileWriter(fileGenome);
							bwGenome =  new BufferedWriter(fwGenome);
						}

						while ((line = brGenome.readLine()) != null) {
							bwGenome.write(line);
							bwGenome.newLine();
						}

						bwGenome.close(); 
						fwGenome.close();
						brGenome.close();
						frGenome.close();

						FileUtils.forceDelete(node);
					}
				} catch (Exception e){
					e.printStackTrace();
					return false;
				}
			}

			if (node.isDirectory())
			{
				String[] subNote = node.list();
				for (String filename : subNote)
				{
					createGenomeDirectory(new File(node, filename));
				}
			}
			
			return true;
		});
	}

	
	
}
