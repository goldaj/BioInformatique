package service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import model.*;
import service.interfaces.IFileService;
import service.interfaces.IGeneService;
import service.interfaces.IOrganismService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.*;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileServiceImpl implements IFileService {
    private final short NORMAL_COLOR = IndexedColors.WHITE.getIndex();
    private final short PRIMARY_COLOR = IndexedColors.BRIGHT_GREEN.getIndex();
    private final short SECONDARY_COLOR = IndexedColors.LIGHT_GREEN.getIndex();
    private final short PRIMARY_INFO_COLOR = IndexedColors.AQUA.getIndex();
    private final short SECONDARY_INFO_COLOR = IndexedColors.CORAL.getIndex();
    private final IGeneService geneService;
    private final ListeningExecutorService executorService;
    private final IOrganismService organismService;

    @Inject
    public FileServiceImpl(IGeneService geneService, ListeningExecutorService listeningExecutorService, IOrganismService organismService) {
    	this.geneService=geneService;
        this.executorService = listeningExecutorService;
        this.organismService = organismService;
    }

    private DefaultMutableTreeNode buildTreeRoot(DefaultMutableTreeNode root, File file) {
        if (root == null) {
            root = new DefaultMutableTreeNode(file.getName());
        }
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(file.getName());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (file.isDirectory()) {
                        root.add(buildTreeRoot(child, f));
                    }
                }
            }
        } else {
            root.add(child);
        }
        return root;
    }

    private DateFormat getDateFileFormat() {
        return new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
    }

    @Override
    public ListenableFuture<TreeModel> buildTree(String path) {
        return executorService.submit(() -> {
            DefaultMutableTreeNode root = buildTreeRoot(new DefaultMutableTreeNode(), new File(path));
            return new DefaultTreeModel(root);
        });
    }

    @Override
    public XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    @Override
    public XSSFSheet fillWorkbook(Organism organism, Gene gene, final XSSFWorkbook workbook) {
        String sheetName;
        if (!gene.getType().equals("") && !gene.getType().toLowerCase().equals("unknown")) {
            sheetName = gene.getType().substring(0, 1).toUpperCase() + gene.getType().substring(1).toLowerCase() + "_" + gene.getName();
        } else {
            sheetName = "DNA_" + gene.getName();
        }
        XSSFSheet sheet = workbook.createSheet(sheetName);
        sheet = fillFileDinu(gene, workbook, sheet);
        sheet = fillFileTrinu(gene, workbook, sheet);
        sheet = fillFileInfo(organism, gene, sheet);
        return sheet;
    }

    @Override
    public XSSFWorkbook fillWorkbookSum(Organism organism, HashMap<String, Sum> organismSums, final XSSFWorkbook workbook) {
        for (Map.Entry<String, Sum> sum: organismSums.entrySet()) {
            String sheetName;
            if (!sum.getKey().toLowerCase().equals("unknown")) {
                sheetName = "Sum_" + sum.getKey();
            } else {
                sheetName = "Sum_DNA";
            }

            XSSFSheet sheet = workbook.createSheet(sheetName);
            fillInfos(organism, sum.getValue(), workbook, sheet);
            fillFileTrinu(sum.getValue(), workbook, sheet);
            fillFileDinu(sum.getValue(), workbook, sheet);
        }
        return workbook;
    }

    @Override
    public void writeWorkbook(XSSFWorkbook workbook, final String path, final String fileName) throws IOException {
        String filePath = Paths.get(path, sanitizeFileName(fileName) + ".xlsx").toString();
        FileOutputStream stream = new FileOutputStream(filePath);
        workbook.write(stream);
        stream.close();
        workbook.close();
    }

    private File createUpdateFile(Kingdom kingdom) throws IOException {
        String path = Paths.get("updates").toString();
        boolean directoryCreated = new File(path).mkdirs();
        File file = Paths.get(path, kingdom.getLabel() + ".json").toFile();
        file.createNewFile();
        return file;
    }
    
    public Map<String,Gene> readWorkbooks(Map<String,Gene> map, File excel, int retry)
    {
    	XSSFWorkbook workbook=null;
    	try
    	{
			workbook=new XSSFWorkbook(new FileInputStream(excel.getPath()));
			
			XSSFSheet currentSheet;
			XSSFRow row;
			XSSFCell tmpCell;
			
			for(int i=workbook.getNumberOfSheets()-1;i>=0;i--)
			{
				currentSheet=workbook.getSheetAt(i);
				String sheetName=currentSheet.getSheetName();
				if(sheetName.contains("Sum") || sheetName.contains("Total"))
				{
					String[] split=sheetName.split("_");
					String type="";
					if(split.length>1)
					{
						type=split[1];
					}
					
					Gene gene;
					if(map.get(type)!=null)
					{
						gene=map.get(type);
					}
					else
					{
						gene=geneService.createGene("Sum_", type, "", 0, 0);
						map.put(type, gene);
					}
					
					gene.setTotalCds(gene.getTotalCds()+(int) workbook.getSheetAt(i).getRow(2).getCell(1).getNumericCellValue());
					gene.setTotalUnprocessedCds((int)workbook.getSheetAt(i).getRow(3).getCell(1).getNumericCellValue());
					int j = 1;
					Set<String> keys = gene.getTrinuStatPhase0().keySet();
					
					for(String key: keys)
					{
						row=currentSheet.getRow(j);
						tmpCell=row.getCell(1);
						int temp=gene.getTrinuStatPhase0().get(key);
						gene.getTrinuStatPhase0().put(key, temp+(int)tmpCell.getNumericCellValue());
						
						tmpCell=row.getCell(3);
						temp=gene.getTrinuStatPhase1().get(key);
						gene.getTrinuStatPhase1().put(key, temp+(int)tmpCell.getNumericCellValue());
						
						tmpCell=row.getCell(5);
						temp=gene.getTrinuStatPhase2().get(key);
						gene.getTrinuStatPhase2().put(key, temp+(int)tmpCell.getNumericCellValue());
						
						tmpCell=row.getCell(7);
						temp=gene.getTrinuPrefPhase0().get(key);
						gene.getTrinuPrefPhase0().put(key, temp+(int)tmpCell.getNumericCellValue());
						gene.setTotalPrefTrinu0(temp+(int)tmpCell.getNumericCellValue()+gene.getTotalPrefTrinu0());
						
						tmpCell=row.getCell(8);
						temp=gene.getTrinuPrefPhase1().get(key);
						gene.getTrinuPrefPhase1().put(key, temp+(int)tmpCell.getNumericCellValue());
						gene.setTotalPrefTrinu1(temp+(int)tmpCell.getNumericCellValue()+gene.getTotalPrefTrinu1());
						
						tmpCell=row.getCell(9);
						temp=gene.getTrinuPrefPhase2().get(key);
						gene.getTrinuPrefPhase2().put(key, temp+(int)tmpCell.getNumericCellValue());
						gene.setTotalPrefTrinu2(temp+(int)tmpCell.getNumericCellValue()+gene.getTotalPrefTrinu2());
						
						j++;
					}
					
					keys=gene.getDinuStatPhase0().keySet();
					j = 70;
					for(String key : keys)
					{
						row=currentSheet.getRow(j);
						tmpCell=row.getCell(1);
						int temp=gene.getDinuStatPhase0().get(key);
						gene.getDinuStatPhase0().put(key, temp+(int)tmpCell.getNumericCellValue());
						
						tmpCell=row.getCell(3);
						temp=gene.getDinuStatPhase1().get(key);
						gene.getDinuStatPhase1().put(key, temp+(int)tmpCell.getNumericCellValue());
						
						j++;
					}
					gene.setTotalTrinucleotide(gene.getTotalTrinucleotide()+(int)currentSheet.getRow(66).getCell(1).getNumericCellValue());
					gene.setTotalDinucleotide(gene.getTotalDinucleotide()+(int)currentSheet.getRow(87).getCell(1).getNumericCellValue());
				}
				else // optimisation qui fonctionne tant qu'on ne modifie pas l'ordre des sums dans le workbook
				{
					break;
				}
			}
		}
    	catch (Exception e)
    	{
    		System.err.println(retry+" path :"+excel.getPath());
    		if(retry>0)
    		{
    			try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			readWorkbooks(map,excel,retry-1);
    		}
    		else
    		{
    			e.printStackTrace();
    		}
		}
    	finally
    	{
    		try
    		{
				workbook.close();
			}
    		catch (IOException e)
    		{
				e.printStackTrace();
			}
    	};
    	
    	return map;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replace("\\", " ")
                .replace("/", " ")
                .replace(":", " ")
                .replace("*", " ")
                .replace("?", " ")
                .replace("\"", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace("|", " ");
    }

    @Override
    public Map<String, Date> readUpdateFile(Kingdom kingdom) throws IOException {
        File file = createUpdateFile(kingdom);
        FileInputStream stream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        Map<String, String> value = (Map<String, String>) JSONValue.parse(inputStreamReader);
        if (value == null) {
            return new HashMap<>();
        } else {
            Map<String, Date> parsedMap = new HashMap<>();
            for (Map.Entry<String, String> entry: value.entrySet()) {
                DateFormat format = organismService.getUpdateDateFormat();
                try {
                    parsedMap.putIfAbsent(entry.getKey(), format.parse(entry.getValue()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return parsedMap;
        }
    }

    @Override
    public void writeUpdateFile(Kingdom kingdom, Map<String, Date> updates) throws IOException {
        File file = createUpdateFile(kingdom);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        Map<String, String> json = new HashMap<>();
        DateFormat format = organismService.getUpdateDateFormat();
        for (Map.Entry<String, Date> entry: updates.entrySet()) {
            json.putIfAbsent(entry.getKey(), format.format(entry.getValue()));
        }
        JSONObject.writeJSONString(json, outputStreamWriter);
        outputStreamWriter.close();
        fileOutputStream.close();
    }

    //    @Override
//    public Promise<List<File>, Throwable, Object> readDirectory(final String path) {
//        return deferredManager.when(new DeferredCallable<List<File>, Object>() {
//            @Override
//            public List<File> call() throws Exception {
//                List<File> files = null;
//                File file = new File(path);
//                if (file.isDirectory()) {
//                    File[] filesArray = file.listFiles();
//                    if (filesArray != null) {
//                        files = new ArrayList<File>(Arrays.asList(filesArray));
//                    } else {
//                        throw new FileNotFoundException(path);
//                    }
//                } else {
//                    throw new NotDirectoryException(path);
//                }
//                return files;
//            }
//        });
//    }

    @Override
    public List<Boolean> createDirectories(final List<String> paths) {
        List<Boolean> results = new ArrayList<>();
        for (String path: paths) {
            results.add(createDirectory(path));
        }
        return results;
    }

    private Boolean createDirectory(final String path) {
        return new File(path).mkdirs();
    }

    private CellStyle buildCellStyleForProba(XSSFWorkbook workbook) {
        CellStyle probaStyle = workbook.createCellStyle();
        probaStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0.00"));
        return probaStyle;
    }

    private CellStyle buildCellStyleForNumber(XSSFWorkbook workbook) {
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0"));
        return numberStyle;
    }

    private CellStyle buildCellStyleForProba(XSSFWorkbook workbook, short indexedColor) {
        CellStyle probaStyle = buildCellStyleForProba(workbook);
        probaStyle.setFillForegroundColor(indexedColor);
        probaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return probaStyle;
    }

    private CellStyle buildCellStyleForNumber(XSSFWorkbook workbook, short indexedColor) {
        CellStyle numberStyle = buildCellStyleForNumber(workbook);
        numberStyle.setFillForegroundColor(indexedColor);
        numberStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return numberStyle;
    }

    private CellStyle buildCellStyle(XSSFWorkbook workbook, short indexedColor) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillForegroundColor(indexedColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }

    private <T extends NucleotidesHolder> XSSFSheet fillInfos(Organism organism, T holder, XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle secondaryNumberStyle = buildCellStyleForNumber(workbook, SECONDARY_COLOR);
        CellStyle secondaryStyle = buildCellStyle(workbook, SECONDARY_COLOR);
        CellStyle primaryStyle = buildCellStyle(workbook, PRIMARY_COLOR);

        secondaryNumberStyle.setAlignment(HorizontalAlignment.LEFT);
        primaryStyle.setAlignment(HorizontalAlignment.LEFT);
        secondaryStyle.setAlignment(HorizontalAlignment.LEFT);

        XSSFRow row;
        XSSFCell cell;

        if ((row = sheet.getRow(1)) == null)
            row = sheet.createRow(1);

        cell = row.createCell(12);
        cell.setCellValue("Organism Name");
        cell.setCellStyle(primaryStyle);

        cell = row.createCell(13);
        cell.setCellValue(organism.getName());
        cell.setCellStyle(secondaryStyle);

        if ((row = sheet.getRow(3)) == null)
            row = sheet.createRow(3);

        cell = row.createCell(12);
        cell.setCellValue("Number of nucleotides");
        cell.setCellStyle(primaryStyle);

        cell = row.createCell(13);
        cell.setCellValue(holder.getTotalTrinucleotide() + holder.getTotalDinucleotide());
        cell.setCellStyle(secondaryNumberStyle);

        if ((row = sheet.getRow(5)) == null)
            row = sheet.createRow(5);

        cell = row.createCell(12);
        cell.setCellValue("Number of cds sequences");
        cell.setCellStyle(primaryStyle);

        cell = row.createCell(13);
        cell.setCellValue(holder.getTotalCds());
        cell.setCellStyle(secondaryNumberStyle);

        if ((row = sheet.getRow(7)) == null)
            row = sheet.createRow(7);

        cell = row.createCell(12);
        cell.setCellValue("Number of invalid cds");
        cell.setCellStyle(primaryStyle);

        cell = row.createCell(13);
        cell.setCellValue(holder.getTotalUnprocessedCds());
        cell.setCellStyle(secondaryNumberStyle);

        if ((row = sheet.getRow(9)) == null)
            row = sheet.createRow(9);

        cell = row.createCell(12);
        cell.setCellValue("Modification date");
        cell.setCellStyle(primaryStyle);

        if (organism.getUpdatedDate() != null) {
            DateFormat format = getDateFileFormat();
            cell = row.createCell(13);
            cell.setCellValue(format.format(organism.getUpdatedDate()));
        } else {
            cell = row.createCell(13);
            cell.setCellValue("NC");
        }
        cell.setCellStyle(secondaryStyle);


        if ((row = sheet.getRow(11)) == null)
            row = sheet.createRow(11);

        cell = row.createCell(12);
        cell.setCellValue("BioProject");
        cell.setCellStyle(primaryStyle);

        cell = row.createCell(13);
        cell.setCellValue(organism.getBioProject());
        cell.setCellStyle(secondaryStyle);

        sheet.autoSizeColumn(12);
        sheet.autoSizeColumn(13);

        return sheet;
    }

    //Not used anymore
    private XSSFSheet fillFileInfo(Organism organism, Gene gene, XSSFSheet sheet) {
        return sheet;
    }

    private <T extends NucleotidesHolder> XSSFSheet fillFileTrinu(T g, XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle normalStyle = buildCellStyle(workbook, NORMAL_COLOR);
        CellStyle primaryStyle = buildCellStyle(workbook, PRIMARY_COLOR);
        CellStyle normalNumberStyle = buildCellStyleForNumber(workbook, NORMAL_COLOR);
        CellStyle normalProbaStyle = buildCellStyleForProba(workbook, NORMAL_COLOR);
        CellStyle primaryNumberStyle = buildCellStyleForNumber(workbook, PRIMARY_COLOR);
        CellStyle primaryProbaStyle = buildCellStyleForProba(workbook, PRIMARY_COLOR);
        CellStyle secondaryNumberStyle = buildCellStyleForNumber(workbook, SECONDARY_COLOR);

        normalNumberStyle.setAlignment(HorizontalAlignment.CENTER);
        normalProbaStyle.setAlignment(HorizontalAlignment.CENTER);
        primaryNumberStyle.setAlignment(HorizontalAlignment.CENTER);
        primaryProbaStyle.setAlignment(HorizontalAlignment.CENTER);
        secondaryNumberStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCell tmpCell;
        XSSFRow row;

        if ((row = sheet.getRow(0)) == null)
            row = sheet.createRow(0);

        tmpCell = row.createCell(0);
        tmpCell.setCellValue("");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(1);
        tmpCell.setCellValue("Phase 0");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(2);
        tmpCell.setCellValue("Freq Phase 0");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(3);
        tmpCell.setCellValue("Phase 1");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(4);
        tmpCell.setCellValue("Freq Phase 1");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(5);
        tmpCell.setCellValue("Phase 2");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(6);
        tmpCell.setCellValue("Freq Phase 2");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(7);
        tmpCell.setCellValue("Phase pref 0");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(8);
        tmpCell.setCellValue("Phase pref 1");
        tmpCell.setCellStyle(primaryStyle);
        tmpCell = row.createCell(9);
        tmpCell.setCellValue("Phase pref 2");
        tmpCell.setCellStyle(primaryStyle);

        int i = 1;
        Set<String> keys = g.getTrinuStatPhase0().keySet();

        CellStyle style = normalStyle;
        CellStyle probaStyle = normalProbaStyle;
        CellStyle numberStyle = normalNumberStyle;
        CellStyle prefNumberStyle = normalNumberStyle;

        for (String key : keys) {

            if (i % 2 == 1) {
                style = normalStyle;
                probaStyle = normalProbaStyle;
                numberStyle = normalNumberStyle;
                prefNumberStyle = normalNumberStyle;
            } else {
                style = primaryStyle;
                probaStyle = primaryProbaStyle;
                numberStyle = primaryNumberStyle;
                prefNumberStyle = secondaryNumberStyle;
            }

            if ((row = sheet.getRow(i)) == null)
                row = sheet.createRow(i);

            // Set Trinicludotide
            tmpCell = row.createCell(0);
            tmpCell.setCellValue(key);
            tmpCell.setCellStyle(style);

            // NB phase 0
            tmpCell = row.createCell(1);
            tmpCell.setCellValue(g.getTrinuStatPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp0 += g.trinuStatPhase0.get(key);

            // Proba phase 0
            tmpCell = row.createCell(2);
            tmpCell.setCellValue(g.getTrinuProbaPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            // NB phase 1
            tmpCell = row.createCell(3);
            tmpCell.setCellValue(g.getTrinuStatPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp1 += g.trinuStatPhase1.get(key);

            // Proba phase 1
            tmpCell = row.createCell(4);
            tmpCell.setCellValue(g.getTrinuProbaPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);
            //tmpCell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);

            // NB phase 2
            tmpCell = row.createCell(5);
            tmpCell.setCellValue(g.getTrinuStatPhase2().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);
            //tmp2 += g.trinuStatPhase2.get(key);

            // Proba phase 2
            tmpCell = row.createCell(6);
            tmpCell.setCellValue(g.getTrinuProbaPhase2().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            //Phase préf 0
            tmpCell = row.createCell(7);
            tmpCell.setCellValue(g.getTrinuPrefPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(prefNumberStyle);

            //Phase préf 1
            tmpCell = row.createCell(8);
            tmpCell.setCellValue(g.getTrinuPrefPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(prefNumberStyle);

            //Phase préf 2
            tmpCell = row.createCell(9);
            tmpCell.setCellValue(g.getTrinuPrefPhase2().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(prefNumberStyle);


            i++;
        }

        row = sheet.createRow(i + 1);
        tmpCell = row.createCell(0);
        tmpCell.setCellValue("Total");
        tmpCell.setCellStyle(primaryStyle);

        tmpCell = row.createCell(1);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(2);
        tmpCell.setCellValue(g.getTotalProbaTrinu0());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryProbaStyle);

        tmpCell = row.createCell(3);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(4);
        tmpCell.setCellValue(g.getTotalProbaTrinu1());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryProbaStyle);

        tmpCell = row.createCell(5);
        tmpCell.setCellValue(g.getTotalTrinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(6);
        tmpCell.setCellValue(g.getTotalProbaTrinu2());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryProbaStyle);

        tmpCell = row.createCell(7);
        tmpCell.setCellValue(g.getTotalPrefTrinu0());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(8);
        tmpCell.setCellValue(g.getTotalPrefTrinu1());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(9);
        tmpCell.setCellValue(g.getTotalPrefTrinu2());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);


        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);

        return sheet;
    }

    private <T extends NucleotidesHolder> XSSFSheet fillFileDinu(T g, XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle normalStyle = buildCellStyle(workbook, NORMAL_COLOR);
        CellStyle primaryStyle = buildCellStyle(workbook, PRIMARY_COLOR);
        CellStyle normalNumberStyle = buildCellStyleForNumber(workbook, NORMAL_COLOR);
        CellStyle normalProbaStyle = buildCellStyleForProba(workbook, NORMAL_COLOR);
        CellStyle primaryNumberStyle = buildCellStyleForNumber(workbook, PRIMARY_COLOR);
        CellStyle primaryProbaStyle = buildCellStyleForProba(workbook, PRIMARY_COLOR);

        normalNumberStyle.setAlignment(HorizontalAlignment.CENTER);
        normalProbaStyle.setAlignment(HorizontalAlignment.CENTER);
        primaryNumberStyle.setAlignment(HorizontalAlignment.CENTER);
        primaryProbaStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCell tmpCell;
        XSSFRow row;

        int rowNumber =  g.getTrinuProbaPhase0().size() + 3;

        if ((row = sheet.getRow(rowNumber)) == null)
            row = sheet.createRow(rowNumber);

        CellStyle style = normalStyle;
        CellStyle probaStyle = normalProbaStyle;
        CellStyle numberStyle = normalNumberStyle;
        CellStyle prefNumberStyle = normalNumberStyle;

        Set<String> keys = g.getDinuStatPhase0().keySet();

        int i = rowNumber + 3;
        for (String key : keys) {

            if (i % 2 == 1) {
                style = normalStyle;
                probaStyle = normalProbaStyle;
                numberStyle = normalNumberStyle;
            } else {
                style = primaryStyle;
                probaStyle = primaryProbaStyle;
                numberStyle = primaryNumberStyle;
            }

            if ((row = sheet.getRow(i)) == null)
                row = sheet.createRow(i);

            // Set Dinicludotide
            tmpCell = row.createCell(0);
            tmpCell.setCellValue(key);
            tmpCell.setCellStyle(style);

            // NB phase 0
            tmpCell = row.createCell(1);
            tmpCell.setCellValue(g.getDinuStatPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);

            // Proba phase 0
            tmpCell = row.createCell(2);
            tmpCell.setCellValue(g.getDinuProbaPhase0().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            // NB phase 1
            tmpCell = row.createCell(3);
            tmpCell.setCellValue(g.getDinuStatPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(numberStyle);

            // Proba phase 1
            tmpCell = row.createCell(4);
            tmpCell.setCellValue(g.getDinuProbaPhase1().get(key));
            tmpCell.setCellType(CellType.NUMERIC);
            tmpCell.setCellStyle(probaStyle);

            i++;
        }

        if ((row = sheet.getRow(i + 1)) == null)
            row = sheet.createRow(i + 1);

        tmpCell = row.createCell(0);
        tmpCell.setCellValue("Total");
        tmpCell.setCellStyle(primaryStyle);

        tmpCell = row.createCell(1);
        tmpCell.setCellValue(g.getTotalDinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(2);
        tmpCell.setCellValue(g.getTotalProbaDinu0());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryProbaStyle);

        tmpCell = row.createCell(3);
        tmpCell.setCellValue(g.getTotalDinucleotide());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryNumberStyle);

        tmpCell = row.createCell(4);
        tmpCell.setCellValue(g.getTotalProbaDinu1());
        tmpCell.setCellType(CellType.NUMERIC);
        tmpCell.setCellStyle(primaryProbaStyle);

        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);

        return sheet;
    }

}
