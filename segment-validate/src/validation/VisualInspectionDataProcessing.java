/*
 * This software was developed by employees of the National Institute of 
 * Standards and Technology (NIST), an agency of the Federal Government. 
 * Pursuant to title 17 United States Code Section 105, works of NIST employees 
 * are not subject to copyright protection in the United States and are considered 
 * to be in the public domain. Permission to freely use, copy, modify, and distribute 
 * this software and its documentation without fee is hereby granted, provided that 
 * this notice and disclaimer of warranty appears in all copies.
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, 
 * IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SOFTWARE 
 * WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
 * DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL 
 * BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT 
 * LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, 
 * RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED 
 * UPON WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY 
 * PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR 
 * AROSE OUT OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package validation;

import ij.ImagePlus;
import io.FitsLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This is a class for processing the data from 
 * 
 * @author Mylene Simon
 *
 */
public class VisualInspectionDataProcessing {

	private String segmentedV1ProjectionsFolder;
	private String segmentedV2ProjectionsFolder;
	private String outputFolder;
	
	private TreeMap<String, Double> diceIndexesPerImageSegV1;
	private TreeMap<String, Double> diceIndexesPerImageSegV2;
	
	private List<String> missedCellsSegV1;
	private List<String> inaccurateShapesSegV1;
	private List<String> missedCellsSegV2;
	private List<String> inaccurateShapesSegV2;
	
	private List<String> usableCellsSegV1;
	private List<String> usableCellsSegV2;
	
	private double tunnelInspectionSegV1;
	private double tunnelInspectionSegV2;
	
	public VisualInspectionDataProcessing(String segmentedV1ProjectionsFolder,
			String segmentedV2ProjectionsFolder, String outputFolder) {
		
		this.segmentedV1ProjectionsFolder = segmentedV1ProjectionsFolder;
		this.segmentedV2ProjectionsFolder = segmentedV2ProjectionsFolder;
		this.outputFolder = outputFolder;
		
		diceIndexesPerImageSegV1 = new TreeMap<String, Double> ();
		diceIndexesPerImageSegV2 = new TreeMap<String, Double> ();
		
		missedCellsSegV1 = new ArrayList<String> ();
		inaccurateShapesSegV1 = new ArrayList<String> ();
		missedCellsSegV2 = new ArrayList<String> ();
		inaccurateShapesSegV2 = new ArrayList<String> ();
		
		usableCellsSegV1 = new ArrayList<String> ();
		usableCellsSegV2 = new ArrayList<String> ();
		
	}
	
	public void loadVisualInspectionData(String dataCsvFileName) {
		
		int tunnelAlertsSegV1 = 0;
		int tunnelAlertsSegV2 = 0;
		int numberOfSkippedLines = 0;
		
		// Read CSV file
		try {

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			int lineNumber = 0;

			br = new BufferedReader(new FileReader(dataCsvFileName));

			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("Image Name")) {
				System.err
						.println("Visual Inspection CSV input file doesn't "
								+ "seem to start with the expected header "
								+ "line, data processing stopped.");
				if (br != null)
					br.close();
				return;
			}

			// Read CSV file
			while ((line = br.readLine()) != null) {
				
				lineNumber ++;
				
				// Split CSV line
				String[] values = line.split(cvsSplitBy, -1);

				String sideBySideImageName = values[0];
				String imageName = sideBySideImageName.substring(0,
						sideBySideImageName.lastIndexOf("_segComparison.jpeg"));
				
				boolean anotherCellIsSegmentedSegV1 = false;
				boolean anotherCellIsSegmentedSegV2 = false;
				boolean shapeIsNotAccurateSegV1 = false;
				boolean shapeIsNotAccurateSegV2 = false;
				
				if(! values[1].isEmpty()) {
					anotherCellIsSegmentedSegV1 = true;
					missedCellsSegV1.add(imageName);
				}
				if(! values[2].isEmpty()) {
					anotherCellIsSegmentedSegV2 = true;
					missedCellsSegV2.add(imageName);
				}
				if(! values[3].isEmpty()) {
					shapeIsNotAccurateSegV1 = true;
					inaccurateShapesSegV1.add(imageName);
				}
				if(! values[4].isEmpty()) {
					shapeIsNotAccurateSegV2 = true;
					inaccurateShapesSegV2.add(imageName);
				}
				
				switch (anotherCellIsSegmentedSegV1 + "-"
						+ anotherCellIsSegmentedSegV2 + "-"
						+ shapeIsNotAccurateSegV1 + "-"
						+ shapeIsNotAccurateSegV2) {
					
					case("false-false-false-false") :
						diceIndexesPerImageSegV1.put(imageName, 1.0);
						diceIndexesPerImageSegV2.put(imageName, 1.0);
						usableCellsSegV1.add(imageName);
						usableCellsSegV2.add(imageName);
					break;
					
					case("true-false-false-false") :
						diceIndexesPerImageSegV1.put(imageName, 0.0);
						diceIndexesPerImageSegV2.put(imageName, 1.0); 
						usableCellsSegV2.add(imageName);
					break;
					
					case("false-true-false-false") :
						diceIndexesPerImageSegV1.put(imageName, 1.0);
						diceIndexesPerImageSegV2.put(imageName, 0.0); 
						usableCellsSegV1.add(imageName);
					break;
					
					case("true-true-false-false") :
						diceIndexesPerImageSegV1.put(imageName, 0.0);
						diceIndexesPerImageSegV2.put(imageName, 0.0); 
					break;
					
					case("false-false-true-false") :
						diceIndexesPerImageSegV1.put(imageName, 
								computeDiceIndexBetweenTwoSegmentations(
										imageName, 2));
						diceIndexesPerImageSegV2.put(imageName, 1.0); 
						usableCellsSegV2.add(imageName);
					break;
					
					case("false-false-false-true") :
						diceIndexesPerImageSegV1.put(imageName, 1.0);
						diceIndexesPerImageSegV2.put(imageName, 
								computeDiceIndexBetweenTwoSegmentations(
										imageName, 1));  
						usableCellsSegV1.add(imageName);
					break;
					
					case("false-false-true-true") :
						System.out.println("Manual segmentation would be "
								+ "necessary to compute Dice index. This line "
								+ "will be skipped (line: " + lineNumber 
								+ ", image name: " + imageName + ")."); 
						numberOfSkippedLines ++;
					break;
					
					case("true-false-false-true") :
						System.out.println("Manual segmentation would be "
								+ "necessary to compute Dice index. This line "
								+ "will be skipped (line: " + lineNumber 
								+ ", image name: " + imageName + ").");  
						numberOfSkippedLines ++;
					break;
					
					case("false-true-true-false") :
						System.out.println("Manual segmentation would be "
								+ "necessary to compute Dice index. This line "
								+ "will be skipped (line: " + lineNumber 
								+ ", image name: " + imageName + ").");  
						numberOfSkippedLines ++;
					break;
					
					default: System.err.println("The line " + lineNumber 
							+ " starting by " + imageName + " in"
							+ " CSV file doesn't seem to fit in any of the "
							+ "valid rules. Line will be skipped.");
						numberOfSkippedLines ++;
					break;
				}
				
				if(! values[5].isEmpty()) {
					if(values[5].equals("M")) 
						tunnelAlertsSegV1 ++;
					else if(values[5].equals("R"))
						tunnelAlertsSegV2 ++;
					else {
						tunnelAlertsSegV1 ++;
						tunnelAlertsSegV2 ++;
					}
				}
			}

			if (br != null)
				br.close();
			
			tunnelInspectionSegV1 = (double) tunnelAlertsSegV1 / (double) lineNumber;
			tunnelInspectionSegV2 = (double) tunnelAlertsSegV2 / (double) lineNumber;
			
			System.out
					.println("Tunnel Inspection in segmentation 1 (average): "
							+ tunnelInspectionSegV1);
			System.out
					.println("Tunnel Inspection in segmentation 2 (average): "
							+ tunnelInspectionSegV2);
			
			System.out.println("Number of skipped lines: " + numberOfSkippedLines);

			writeDiceIndexResults(outputFolder + File.separatorChar
					+ "visualInspectionDiceIndexes.csv");
			
			writeStringListInFile(outputFolder + File.separatorChar
					+ "missedCellsListBeforeRejectingCellsSegV1.csv",
					missedCellsSegV1);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "missedCellsListBeforeRejectingCellsSegV2.csv",
					missedCellsSegV2);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "inaccurateShapesListBeforeRejectingCellsSegV1.csv",
					inaccurateShapesSegV1);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "inaccurateShapesListBeforeRejectingCellsSegV2.csv",
					inaccurateShapesSegV2);

		} catch (FileNotFoundException e) {
			System.err.println("CSV file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem while reading the CSV file.");
			e.printStackTrace();
		}
	}
	
	public void loadSamplesValidationData(String dataCsvFileName) {
		
		// Read CSV file
		try {

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";

			br = new BufferedReader(new FileReader(dataCsvFileName));

			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("Image Name")) {
				System.err
						.println("Samples Validation CSV input file doesn't "
								+ "seem to start with the expected header "
								+ "line, data processing stopped.");
				if (br != null)
					br.close();
				return;
			}

			// Read CSV file
			while ((line = br.readLine()) != null) {
				
				// Split CSV line
				String[] values = line.split(cvsSplitBy);

				String imageName = values[0];
				double diceIndexSegV1 = Double.parseDouble(values[1]);
				double diceIndexSegV2 = Double.parseDouble(values[2]);
				
				if(diceIndexesPerImageSegV1.containsKey(imageName)) {
					double visualInspectionDiceIndexSegV1 = 
							diceIndexesPerImageSegV1.get(imageName).doubleValue();
					diceIndexesPerImageSegV1.put(imageName,
							((visualInspectionDiceIndexSegV1 + diceIndexSegV1) / 2.0));
				}
				else {
					diceIndexesPerImageSegV1.put(imageName, diceIndexSegV1);
					System.out.println("No visual inspection data was found for "
							+ "seg V1, image " + imageName);
				}
				
				if(diceIndexesPerImageSegV2.containsKey(imageName)) {
					double visualInspectionDiceIndexSegV2 = 
							diceIndexesPerImageSegV2.get(imageName).doubleValue();
					diceIndexesPerImageSegV2.put(imageName,
							((visualInspectionDiceIndexSegV2 + diceIndexSegV2) / 2.0));
				}
				else {
					diceIndexesPerImageSegV2.put(imageName, diceIndexSegV2);
					System.out.println("No visual inspection data was found for "
							+ "seg V2, image " + imageName);
				}
					
			}

			if (br != null)
				br.close();
			
			writeDiceIndexResults(outputFolder + File.separatorChar
					+ "visualInspectionAndSamplesValidationDiceIndexes.csv");

		} catch (FileNotFoundException e) {
			System.err.println("CSV file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem while reading the CSV file.");
			e.printStackTrace();
		}
		
	}
	
public void eliminateUnusableData(String dataCsvFileName) {
		
		// Read CSV file
		try {

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";

			br = new BufferedReader(new FileReader(dataCsvFileName));

			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("Image Name")) {
				System.err
						.println("Unusable Data CSV input file doesn't "
								+ "seem to start with the expected header "
								+ "line, data processing stopped.");
				if (br != null)
					br.close();
				return;
			}

			int numberOfRejected = 0;
			
			// Read CSV file
			while ((line = br.readLine()) != null) {
				
				// Split CSV line
				String[] values = line.split(cvsSplitBy);

				String imageName = values[0];
				
				if(! imageName.isEmpty()) {
				
					numberOfRejected ++;
					
					diceIndexesPerImageSegV1.remove(imageName);
					diceIndexesPerImageSegV2.remove(imageName);
					missedCellsSegV1.remove(imageName);
					missedCellsSegV2.remove(imageName);
					inaccurateShapesSegV1.remove(imageName);
					inaccurateShapesSegV2.remove(imageName);
					usableCellsSegV1.remove(imageName);
					usableCellsSegV2.remove(imageName);
					System.out.println(imageName + " was flagged as unusable, so it is removed from the data set");
				}
					
			}

			if (br != null)
				br.close();
			
			System.out.println("Number of rejected cells: " + numberOfRejected);
			
			writeDiceIndexResults(outputFolder + File.separatorChar
					+ "FinalDiceIndexes.csv");
			
			writeStringListInFile(outputFolder + File.separatorChar
					+ "missedCellsListAfterRejectingCellsSegV1.csv",
					missedCellsSegV1);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "missedCellsListAfterRejectingCellsSegV2.csv",
					missedCellsSegV2);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "inaccurateShapesListAfterRejectingCellsSegV1.csv",
					inaccurateShapesSegV1);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "inaccurateShapesListAfterRejectingCellsSegV2.csv",
					inaccurateShapesSegV2);
			
			writeStringListInFile(outputFolder + File.separatorChar
					+ "usableCellsSegV1.csv",
					usableCellsSegV1);
			writeStringListInFile(outputFolder + File.separatorChar
					+ "usableCellsSegV2.csv",
					usableCellsSegV2);


		} catch (FileNotFoundException e) {
			System.err.println("CSV file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem while reading the CSV file.");
			e.printStackTrace();
		}
		
	}
	
	public void writeDiceIndexResults(String fileName) {
		FileWriter writer;
		
		try {
			writer = new FileWriter(fileName);
			
			// Header
			writer.append("Image Name");
		    writer.append(',');
		    writer.append("Dice Index Seg V1");
		    writer.append(',');
		    writer.append("Dice Index Seg V2");
		    writer.append("\n");
		    
			Iterator<Entry<String, Double>> entriesSegV1 = diceIndexesPerImageSegV1
					.entrySet().iterator();
		    
			while (entriesSegV1.hasNext()) {
				Entry<String, Double> entry = entriesSegV1.next();
				
				writer.append(entry.getKey());
			    writer.append(',');
			    writer.append(String.valueOf(entry.getValue()));
			    writer.append(',');
			    writer.append(String.valueOf(diceIndexesPerImageSegV2
			    								.get(entry.getKey())));
			    writer.append("\n");
		    }
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving the Dice indexes in " 
								+ fileName);
			e.printStackTrace();
		}
	}
	
	public double computeDiceIndexBetweenTwoSegmentations(String imageName, 
			int groundTruthSegVersion) {
		
		String folderGroundTruth = "";
		String folderInaccurateShape = "";
		if(groundTruthSegVersion == 1) {
			folderGroundTruth = segmentedV1ProjectionsFolder;
			folderInaccurateShape = segmentedV2ProjectionsFolder;
		}
		else {
			folderGroundTruth = segmentedV2ProjectionsFolder;
			folderInaccurateShape = segmentedV1ProjectionsFolder;
		}
		
		ImagePlus groundTruthMaxXY = FitsLoader.read(folderGroundTruth
				+ File.separatorChar + imageName + "_seg_maxXY.fits");
		ImagePlus inaccurateShapeMaxXY = FitsLoader.read(folderInaccurateShape 
				+ File.separatorChar + imageName + "_seg_maxXY.fits");
		
		double diceIndexMaxXY = SimilarityMetricsExtractor.computeDiceIndex(
				groundTruthMaxXY, 
				inaccurateShapeMaxXY, 
				255);
		
		ImagePlus groundTruthMaxXZ = FitsLoader.read(folderGroundTruth
				+ File.separatorChar + imageName + "_seg_maxXZ.fits");
		ImagePlus inaccurateShapeMaxXZ = FitsLoader.read(folderInaccurateShape 
				+ File.separatorChar + imageName + "_seg_maxXZ.fits");
		
		double diceIndexMaxXZ = SimilarityMetricsExtractor.computeDiceIndex(
				groundTruthMaxXZ, 
				inaccurateShapeMaxXZ, 
				255);
		
		ImagePlus groundTruthMaxZY = FitsLoader.read(folderGroundTruth
				+ File.separatorChar + imageName + "_seg_maxZY.fits");
		ImagePlus inaccurateShapeMaxZY = FitsLoader.read(folderInaccurateShape 
				+ File.separatorChar + imageName + "_seg_maxZY.fits");
		
		double diceIndexMaxZY = SimilarityMetricsExtractor.computeDiceIndex(
				groundTruthMaxZY, 
				inaccurateShapeMaxZY, 
				255);
		
		return ((diceIndexMaxXY + diceIndexMaxXZ + diceIndexMaxZY) / 3.0);
	}
	
	public void writeStringListInFile(String fileName, List<String> stringListToPrint) {
		
		FileWriter writer;
		
		try {
			writer = new FileWriter(fileName);
			
			// Header
			writer.append("Image Name");
		    writer.append("\n");
		    
		    if(stringListToPrint != null) {
		    
		    	Collections.sort(stringListToPrint);
		    	
			    Iterator<String> listIt = stringListToPrint.iterator();
				while (listIt.hasNext()) {
					String imageName = listIt.next();
					
					writer.append(imageName);
				    writer.append("\n");
			    }
		    }
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving string list in " 
								+ fileName);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length != 6)) {
			System.out
					.println("Please, specify the segmentedV1ProjectionsFolder, "
							+ "segmentedV2ProjectionsFolder, outputFolder,  "
							+ "visualInpectionCsvFileName, "
							+ "samplesValidationCsvFileName and "
							+ "rejectedCsvFileName.");

			return;
		}

		// get arguments
		String segmentedV1ProjectionsFolder = new String(args[0]);
		String segmentedV2ProjectionsFolder = new String(args[1]);
		String outputFolder = new String(args[2]);
		String visualInpectionCsvFileName = new String(args[3]);
		String samplesValidationCsvFileName = new String(args[4]);
		String rejectedCsvFileName = new String(args[5]);
		
		PrintStream out;

		try {
			// redirect console output in file
			out = new PrintStream(new FileOutputStream(outputFolder
					+ File.separatorChar + "ConsoleOutput.txt"));
			System.setOut(out);
			System.setErr(out);
			
			// start time for benchmark
		    long startTime = System.currentTimeMillis();
			System.out
					.println("Start processing visual inspection data...");
			
			VisualInspectionDataProcessing myVisualInspectionDataProcessing =
					new VisualInspectionDataProcessing(
					segmentedV1ProjectionsFolder, segmentedV2ProjectionsFolder,
					outputFolder);	
			
			myVisualInspectionDataProcessing.loadVisualInspectionData(visualInpectionCsvFileName);
			System.out
			.println("Start processing samples validation data...");
			myVisualInspectionDataProcessing.loadSamplesValidationData(samplesValidationCsvFileName);
			System.out
			.println("Start rejecting unusable cells...");
			myVisualInspectionDataProcessing.eliminateUnusableData(rejectedCsvFileName);
			
			// end time for benchmark
		    long endTime = System.currentTimeMillis();
			System.out
					.println("Visual inspection data processing execution time "
							+ ": "
							+ (endTime - startTime)
							+ " millisecond.");
			System.out.println();
			
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: Could not open file = "
					+ outputFolder + File.separatorChar + "ConsoleOutput.txt");
			e.printStackTrace();
		}
	}

}
