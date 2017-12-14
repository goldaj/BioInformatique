package Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * Handles file zip
 */
public class ZipUtils
{
	private List<String> fileList;
	private String outputZipFile;
	private String sourceFolder;

	public ZipUtils()
	{
		fileList = new ArrayList<String>();
	}

	public ZipUtils(String source, String output)
	{
		fileList = new ArrayList<String>();
		this.outputZipFile = output;
		this.sourceFolder = source;
	}

	public void zipIt(String zipFile)
	{
		byte[] buffer = new byte[1024];
		String source = "";
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try
		{
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			FileInputStream in = null;

			for (String file : this.fileList)
			{
				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);
				try
				{
					in = new FileInputStream(sourceFolder + "/" + file);
					int len;
					while ((len = in.read(buffer)) > 0)
					{
						zos.write(buffer, 0, len);
					}
				}
				finally
				{
					in.close();   
				} 
			}

			zos.closeEntry();

			// Suppression du dossier temporaire
			FileUtils.forceDelete(new File(sourceFolder));

			System.out.println("Folder successfully compressed");

		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				zos.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void generateFileList(File node)
	{
		// add file only
		if (node.isFile())
		{
			if(node.toString().contains("\\")){
				fileList.add(generateZipEntry(node.toString().replace("\\","/")));
			} else{				
				fileList.add(generateZipEntry(node.toString()));
			}
		}

		if (node.isDirectory())
		{
			String[] subNote = node.list();
			for (String filename : subNote)
			{
				generateFileList(new File(node, filename));
			}
		}
	}

	private String generateZipEntry(String file)
	{
		return file.substring(sourceFolder.length() + 1, file.length());
	}

	public void ExecuteZip(){
		this.generateFileList(new File(this.sourceFolder));
		this.zipIt(this.outputZipFile);
	}

	public String getOutputZipFile() {
		return outputZipFile;
	}

	public void setOutputZipFile(String outputZipFile) {
		this.outputZipFile = outputZipFile;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(String sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public static void cleanSaveFolder(String pathFolder, String[] explodePath){
		if (new File(pathFolder).exists()) {
			try {
				FileUtils.forceDelete(new File(pathFolder));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(explodePath.length > 0){
			if (new File(explodePath[1] + ".zip").exists()) {
				try {
					FileUtils.forceDelete(new File(explodePath[1] + ".zip"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void createGenomeDirectory(File node){

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

	}

	 public static void main(String[] args){
	 	System.out.println("Start of Process");
	 	String zipGene = "./Gene/";
	 	String[] explodeGenePath = zipGene.split("/");
	 	if (new File(zipGene).exists()) {
	 		ZipUtils zip = new ZipUtils(explodeGenePath[1], explodeGenePath[1] + ".zip");
	 		zip.ExecuteZip();
	 	}
	 	System.out.println("End of Process");
	 }

}    