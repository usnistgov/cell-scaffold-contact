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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a class for selecting images for manual and automated segmentation
 * according to their scores for one thresholding method and one algorithmic 
 * segmentation sequence, and then computing the orthogonal projections of the
 * selected images
 * 
 * @author Mylene Simon
 *
 */
public class ImageSelectionAndProjection {

	/**
	 * Read the result CSV files for the given scaffold, compute score of each
	 * image file and launch orthogonal projections for the image file with best
	 * score and the image file with worst score
	 * 
	 * @param scaffoldName
	 *            Name of the scaffold
	 * @param inputDirMinErrorThresh
	 *            Path of the directory in which are the CSV files for min error
	 *            thresholding results
	 * @param inputDirTopoStableThresh
	 *            Path of the directory in which are the CSV files for topo
	 *            stable thresholding results
	 * @param outputDir
	 *            Global output directory (specific sub directories will be
	 *            created in this one)
	 */
	public void selectImageAccordingScore(String scaffoldName,
			String inputDirMinErrorThresh, String inputDirTopoStableThresh,
			String outputDir) {
		
		// Extensions of the input CSV files
		final String voxelCountM1Extension = "VoxelCountM1.csv";
		final String voxelCountM2Extension = "VoxelCountM2.csv";
		
		// Initialization of the means
		// For mean error thresholding
		double meanAfterMinErrorThreshAndE = 0.0;
		double meanAfterMinErrorThreshAndEFLM1 = 0.0;
		double meanAfterMinErrorThreshAndEFLM2 = 0.0;
		// For topo stable thresholding
		double meanAfterTopoStableThreshAndE = 0.0;
		double meanAfterTopoStableThreshAndEFLM1 = 0.0;
		double meanAfterTopoStableThreshAndEFLM2 = 0.0;
		
		// Intializations of lists for stdev
		List<Double> valuesAfterMinErrorThreshAndE = new ArrayList<Double>();
		List<Double> valuesAfterMinErrorThreshAndEFLM1 = new ArrayList<Double>();
		List<Double> valuesAfterMinErrorThreshAndEFLM2 = new ArrayList<Double>();
		
		List<Double> valuesAfterTopoStableThreshAndE = new ArrayList<Double>();
		List<Double> valuesAfterTopoStableThreshAndEFLM1 = new ArrayList<Double>();
		List<Double> valuesAfterTopoStableThreshAndEFLM2 = new ArrayList<Double>();
		
		// Initialization of the stdev
		// For mean error thresholding
		double stdevAfterMinErrorThreshAndE = 0.0;
		double stdevAfterMinErrorThreshAndEFLM1 = 0.0;
		double stdevAfterMinErrorThreshAndEFLM2 = 0.0;
		// For topo stable thresholding
		double stdevAfterTopoStableThreshAndE = 0.0;
		double stdevAfterTopoStableThreshAndEFLM1 = 0.0;
		double stdevAfterTopoStableThreshAndEFLM2 = 0.0;
		
		int numberOfFiles = 0;
		
		Map<String, ImageFileAndThreshResults> mapResults = 
				new HashMap<String, ImageFileAndThreshResults>();
		
		// Read CSV files to get all the foreground voxel counts for each file
		// and compute the means
		try {
		
			String csvFile = "";
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			
			// Read the CSV files for min error thresholding to compute the 
			// foreground voxel count means
			
			// For M1 algo sequence and for just T algo sequence
			csvFile = inputDirMinErrorThresh 
					+ File.separatorChar
					+ scaffoldName 
					+ voxelCountM1Extension;
		 
			br = new BufferedReader(new FileReader(csvFile));
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("File_Name,Threshold")) {
				System.err.println("CSV input files should start with the following header line: ");
				System.err.println("File_Name,Threshold,VC_Thresholding,VC_Edge,VC_Fill,VC_Largest,VC_Morpho");
				if (br != null) br.close();
				return;
			}
			
			// Read CSV file
			while ((line = br.readLine()) != null) {
	 
				// Split CSV line
				String[] frgCounts = line.split(cvsSplitBy);
				
				// Create object associating file name and foreground voxel counts
				ImageFileAndThreshResults imageAndThreshResults = 
						new ImageFileAndThreshResults(frgCounts[0]);
				
				imageAndThreshResults.thresholdMinError = Integer
						.parseInt(frgCounts[1]);
				
				imageAndThreshResults.frgCountMinErrorAfterTE = Double
						.parseDouble(frgCounts[3]);
				
				imageAndThreshResults.frgCountMinErrorAfterTEFLM1 = Double
						.parseDouble(frgCounts[6]);
				
				// Update means and number of files
				meanAfterMinErrorThreshAndE += imageAndThreshResults.frgCountMinErrorAfterTE;
				valuesAfterMinErrorThreshAndE.add(imageAndThreshResults.frgCountMinErrorAfterTE);
				meanAfterMinErrorThreshAndEFLM1 += imageAndThreshResults.frgCountMinErrorAfterTEFLM1;
				valuesAfterMinErrorThreshAndEFLM1.add(imageAndThreshResults.frgCountMinErrorAfterTEFLM1);
				numberOfFiles++;
				
				// Add object to the map of results
				mapResults.put(frgCounts[0], imageAndThreshResults);
		
			}
			
			if (br != null) br.close();
		
			// For M2 algo sequence
			csvFile = inputDirMinErrorThresh + File.separatorChar + scaffoldName + voxelCountM2Extension;
			 
			br = new BufferedReader(new FileReader(csvFile));
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong format, stop processing
			if(line == null || ! line.startsWith("File_Name,Threshold")) {
				System.err.println("CSV input files should start with the following header line: ");
				System.err.println("File_Name,Threshold,VC_Thresholding,VC_Edge,VC_Fill,VC_Largest,VC_Morpho");
				if (br != null) br.close();
				return;
			}
			
			// Read CSV file
			while ((line = br.readLine()) != null) {
	 
				// Split CSV line
				String[] frgCounts = line.split(cvsSplitBy);
				
				// Create object associating file name and foreground voxel counts
				ImageFileAndThreshResults imageAndThreshResults = 
						mapResults.get(frgCounts[0]);
				
				if(imageAndThreshResults == null) {
					System.err.println("Incoherence in the data.");
					if (br != null) br.close();
					return;
				}
				
				imageAndThreshResults.frgCountMinErrorAfterTEFLM2 = Double
						.parseDouble(frgCounts[6]);
				
				// Update mean
				meanAfterMinErrorThreshAndEFLM2 += imageAndThreshResults.frgCountMinErrorAfterTEFLM2;
				valuesAfterMinErrorThreshAndEFLM2.add(imageAndThreshResults.frgCountMinErrorAfterTEFLM2);
				
				// Update map of results
				mapResults.put(frgCounts[0], imageAndThreshResults);
		
			}
			
			if (br != null) br.close();
			
			
			// Read the CSV files for topo stable thresholding to compute the 
			// foreground voxel count means
			
			// For M1 algo sequence and for just T algo sequence
			csvFile = inputDirTopoStableThresh + File.separatorChar + scaffoldName + voxelCountM1Extension;
			 
			br = new BufferedReader(new FileReader(csvFile));
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong format, stop processing
			if(line == null || ! line.startsWith("File_Name,Threshold")) {
				System.err.println("CSV input files should start with the following header line: ");
				System.err.println("File_Name,Threshold,VC_Thresholding,VC_Edge,VC_Fill,VC_Largest,VC_Morpho");
				if (br != null) br.close();
				return;
			}
			
			// Read CSV file
			while ((line = br.readLine()) != null) {
	 
				// Split CSV line
				String[] frgCounts = line.split(cvsSplitBy);
				
				// Create object associating file name and foreground voxel counts
				ImageFileAndThreshResults imageAndThreshResults = 
						mapResults.get(frgCounts[0]);
				
				if(imageAndThreshResults == null) {
					System.err.println("Incoherence in the data.");
					if (br != null) br.close();
					return;
				}
				
				imageAndThreshResults.thresholdTopoStable = Integer
						.parseInt(frgCounts[1]);
				
				imageAndThreshResults.frgCountTopoStableAfterTE = Double
						.parseDouble(frgCounts[3]);
				
				imageAndThreshResults.frgCountTopoStableAfterTEFLM1 = Double
						.parseDouble(frgCounts[6]);
				
				// Update means
				meanAfterTopoStableThreshAndE += imageAndThreshResults.frgCountTopoStableAfterTE;
				valuesAfterTopoStableThreshAndE.add(imageAndThreshResults.frgCountTopoStableAfterTE);
				meanAfterTopoStableThreshAndEFLM1 += imageAndThreshResults.frgCountTopoStableAfterTEFLM1;
				valuesAfterTopoStableThreshAndEFLM1.add(imageAndThreshResults.frgCountTopoStableAfterTEFLM1);
				
				// Update map of results
				mapResults.put(frgCounts[0], imageAndThreshResults);
		
			}
			
			if (br != null) br.close();		
			
			// For M2 algo sequence
			csvFile = inputDirTopoStableThresh + File.separatorChar + scaffoldName + voxelCountM2Extension;
			 
			br = new BufferedReader(new FileReader(csvFile));
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong format, stop processing
			if(line == null || ! line.startsWith("File_Name,Threshold")) {
				System.err.println("CSV input files should start with the following header line: ");
				System.err.println("File_Name,Threshold,VC_Thresholding,VC_Edge,VC_Fill,VC_Largest,VC_Morpho");
				if (br != null) br.close();
				return;
			}
			
			// Read CSV file
			while ((line = br.readLine()) != null) {
	 
				// Split CSV line
				String[] frgCounts = line.split(cvsSplitBy);
				
				// Create object associating file name and foreground voxel counts
				ImageFileAndThreshResults imageAndThreshResults = 
						mapResults.get(frgCounts[0]);
				
				if(imageAndThreshResults == null) {
					System.err.println("Incoherence in the data.");
					if (br != null) br.close();
					return;
				}
				
				imageAndThreshResults.frgCountTopoStableAfterTEFLM2 = Double
						.parseDouble(frgCounts[6]);
				
				// Update means
				meanAfterTopoStableThreshAndEFLM2 += imageAndThreshResults.frgCountTopoStableAfterTEFLM2;
				valuesAfterTopoStableThreshAndEFLM2.add(imageAndThreshResults.frgCountTopoStableAfterTEFLM2);
				
				// Update map of results
				mapResults.put(frgCounts[0], imageAndThreshResults);
		
			}
			
			if (br != null) br.close();			
		
		} catch (FileNotFoundException e) {
			System.err.println("CSV file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem while reading the CSV file.");
			e.printStackTrace();
		}
		
		// Compute all the means
		
		// Mean for min error thresholding
		meanAfterMinErrorThreshAndE = meanAfterMinErrorThreshAndE / (double) numberOfFiles;
		meanAfterMinErrorThreshAndEFLM1 = meanAfterMinErrorThreshAndEFLM1 / (double) numberOfFiles;
		meanAfterMinErrorThreshAndEFLM2 = meanAfterMinErrorThreshAndEFLM2 / (double) numberOfFiles;
		
		// Mean for topo stable thresholding
		meanAfterTopoStableThreshAndE = meanAfterTopoStableThreshAndE / (double) numberOfFiles;
		meanAfterTopoStableThreshAndEFLM1 = meanAfterTopoStableThreshAndEFLM1 / (double) numberOfFiles;
		meanAfterTopoStableThreshAndEFLM2 = meanAfterTopoStableThreshAndEFLM2 / (double) numberOfFiles;
		
		// Compute all the deviations
		for(int i = 0; i < numberOfFiles; ++ i) {
			// Deviations for min error thresholding
			stdevAfterMinErrorThreshAndE += 
				Math.pow((valuesAfterMinErrorThreshAndE.get(i) - meanAfterMinErrorThreshAndE), 2);
			stdevAfterMinErrorThreshAndEFLM1 += 
					Math.pow((valuesAfterMinErrorThreshAndEFLM1.get(i) - meanAfterMinErrorThreshAndEFLM1), 2);
			stdevAfterMinErrorThreshAndEFLM2 += 
					Math.pow((valuesAfterMinErrorThreshAndEFLM2.get(i) - meanAfterMinErrorThreshAndEFLM2), 2);
			
			// Deviations for topo stable thresholding
			stdevAfterTopoStableThreshAndE += 
				Math.pow((valuesAfterTopoStableThreshAndE.get(i) - meanAfterTopoStableThreshAndE), 2);
			stdevAfterTopoStableThreshAndEFLM1 += 
					Math.pow((valuesAfterTopoStableThreshAndEFLM1.get(i) - meanAfterTopoStableThreshAndEFLM1), 2);
			stdevAfterTopoStableThreshAndEFLM2 += 
					Math.pow((valuesAfterTopoStableThreshAndEFLM2.get(i) - meanAfterTopoStableThreshAndEFLM2), 2);
		}
		
		// Stdev for min error thresholding
		stdevAfterMinErrorThreshAndE = Math.sqrt(stdevAfterMinErrorThreshAndE / (double) numberOfFiles);
		stdevAfterMinErrorThreshAndEFLM1 = Math.sqrt(stdevAfterMinErrorThreshAndEFLM1 / (double) numberOfFiles);
		stdevAfterMinErrorThreshAndEFLM2 = Math.sqrt(stdevAfterMinErrorThreshAndEFLM2 / (double) numberOfFiles);
		
		// Stdev for topo stable thresholding
		stdevAfterTopoStableThreshAndE = Math.sqrt(stdevAfterTopoStableThreshAndE / (double) numberOfFiles);
		stdevAfterTopoStableThreshAndEFLM1 = Math.sqrt(stdevAfterTopoStableThreshAndEFLM1 / (double) numberOfFiles);
		stdevAfterTopoStableThreshAndEFLM2 = Math.sqrt(stdevAfterTopoStableThreshAndEFLM2 / (double) numberOfFiles);
		
		System.out
				.println("Mean values: ");
		System.out
				.println("Mean after T->E - with min error thresh technique, "
				+ meanAfterMinErrorThreshAndE);
		System.out
				.println("Mean after T->E->F->L->M1 - with min error thresh technique, "
						+ meanAfterMinErrorThreshAndEFLM1);
		System.out
				.println("Mean after T->E->F->L->M2 - with min error thresh technique, "
						+ meanAfterMinErrorThreshAndEFLM2);
		System.out.println("Mean after T->E - with topo stable thresh technique, "
				+ meanAfterTopoStableThreshAndE);
		System.out
				.println("Mean after T->E->F->L->M1 - with topo stable thresh technique, "
						+ meanAfterTopoStableThreshAndEFLM1);
		System.out
				.println("Mean after T->E->F->L->M2 - with topo stable thresh technique, "
						+ meanAfterTopoStableThreshAndEFLM2);
		
		System.out
		.println("Stdev values: ");
		System.out
				.println("Stdev after T->E - with min error thresh technique, "
				+ stdevAfterMinErrorThreshAndE);
		System.out
				.println("Stdev after T->E->F->L->M1 - with min error thresh technique, "
						+ stdevAfterMinErrorThreshAndEFLM1);
		System.out
				.println("Stdev after T->E->F->L->M2 - with min error thresh technique, "
						+ stdevAfterMinErrorThreshAndEFLM2);
		System.out.println("Stdev after T->E - with topo stable thresh technique, "
				+ stdevAfterTopoStableThreshAndE);
		System.out
				.println("Stdev after T->E->F->L->M1 - with topo stable thresh technique, "
						+ stdevAfterTopoStableThreshAndEFLM1);
		System.out
				.println("Stdev after T->E->F->L->M2 - with topo stable thresh technique, "
						+ stdevAfterTopoStableThreshAndEFLM2);
		
		// Compute scores and choose best and worst for each combination thresh method/algo sequence 
		double minScore = Double.MAX_VALUE;
		double maxScore = Double.MIN_VALUE;
		ImageFileAndThreshResults bestScoreImage = new ImageFileAndThreshResults("");
		ImageFileAndThreshResults worstScoreImage = new ImageFileAndThreshResults("");
		for (Map.Entry<String, ImageFileAndThreshResults> entry : mapResults.entrySet()) {
			// Compute and update score
			ImageFileAndThreshResults threshResult = entry.getValue();
			threshResult.score = 
					  (Math.abs(meanAfterMinErrorThreshAndE - threshResult.frgCountMinErrorAfterTE) / stdevAfterMinErrorThreshAndE)
					+ (Math.abs(meanAfterTopoStableThreshAndE - threshResult.frgCountTopoStableAfterTE) / stdevAfterTopoStableThreshAndE)
					+ (Math.abs(meanAfterMinErrorThreshAndEFLM1 - threshResult.frgCountMinErrorAfterTEFLM1) / stdevAfterMinErrorThreshAndEFLM1)
					+ (Math.abs(meanAfterMinErrorThreshAndEFLM2 - threshResult.frgCountMinErrorAfterTEFLM2) / stdevAfterMinErrorThreshAndEFLM2)
					+ (Math.abs(meanAfterTopoStableThreshAndEFLM1 - threshResult.frgCountTopoStableAfterTEFLM1) / stdevAfterTopoStableThreshAndEFLM1)
					+ (Math.abs(meanAfterTopoStableThreshAndEFLM2 - threshResult.frgCountTopoStableAfterTEFLM2) / stdevAfterTopoStableThreshAndEFLM2);
			mapResults.put(entry.getKey(), threshResult);
			
			// Track min and max
			if(threshResult.score < minScore) {
				bestScoreImage = threshResult;
				minScore = threshResult.score;
			}
			if(threshResult.score > maxScore) {
				worstScoreImage = threshResult;
				maxScore = threshResult.score;
			}
		}
		
		System.out.println("File with best score (lowest value) is: "
				+ bestScoreImage.filename + ", score: " 
				+ bestScoreImage.score);
		System.out.println("File with worst score (highest value) is: "
				+ worstScoreImage.filename + ", score: "
				+ worstScoreImage.score);
		
		// Compute orthogonal projections for best and worst files
		computeOrthogonalProjections(worstScoreImage.filename, scaffoldName, "worst", outputDir);
		computeOrthogonalProjections(bestScoreImage.filename, scaffoldName, "best", outputDir);
		
		// Save results (foreground voxel counts and score) in CSV file
		saveResultsInCSV(outputDir + File.separatorChar + scaffoldName + "_FrgVoxelCounts_Score.csv", mapResults);
		
		// Build input CSV file for segmentation, for best and worst selected images
		buildCSVInputForSegmentation(outputDir + File.separatorChar + scaffoldName + "_segmentationInput.csv", scaffoldName, bestScoreImage, worstScoreImage);
	}
	
	/**
	 * Call the OrthogonalProjection class to compute the orthogonal projections of the file
	 * 
	 * @param fileName The path of the input file
	 * @param scaffoldName The scaffold name
	 * @param selectionType The selection type (best or worst)
	 * @param outputDir The output directory (specific sub directories will be created in this one)
	 */
	private void computeOrthogonalProjections(String fileName, String scaffoldName, String selectionType, String outputDir) {
		// Construct path of the output directory for the projections
		String outputSpecificDir = outputDir + File.separatorChar + scaffoldName + "_" + selectionType;
		// If the directory doesn't exist, the directory (and parent directories if necessary) is created
		File outputTest = new File(outputSpecificDir);
		if(! outputTest.exists()) {
			outputTest.mkdirs();
		}
		// Launch orthogonal projections
		System.out.println("Processing orthogonal projections for " + fileName);
		OrthogonalProjection.processOneFile(fileName, outputSpecificDir, OrthogonalProjection.projectionType_Max);
	}
	
	/**
	 * Save the results (foreground voxel counts and score) for each Fits file
	 * 
	 * @param fileName The output file name
	 * @param results The Map of the results
	 */
	public void saveResultsInCSV(String fileName, Map<String, ImageFileAndThreshResults> results) {
		
		FileWriter writer;
		
		try {
			writer = new FileWriter(fileName);
			
			// Header
			writer.append("FileName");
		    writer.append(',');
		    writer.append("FrgCountMinErrorAfterTE");
		    writer.append(',');
		    writer.append("FrgCountMinErrorAfterTEFLM1");
		    writer.append(',');
		    writer.append("FrgCountMinErrorAfterTEFLM2");
		    writer.append(',');
		    writer.append("FrgCountTopoStableAfterTE");
		    writer.append(',');
		    writer.append("FrgCountTopoStableAfterTEFLM1");
		    writer.append(',');
		    writer.append("FrgCountTopoStableAfterTEFLM2");
		    writer.append(',');
		    writer.append("Score");
		    writer.append('\n');
		    
		    // Data
		    for (Map.Entry<String, ImageFileAndThreshResults> entry : results.entrySet()) {
		    	 
				writer.append(entry.getKey());
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountMinErrorAfterTE));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountMinErrorAfterTEFLM1));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountMinErrorAfterTEFLM2));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountTopoStableAfterTE));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountTopoStableAfterTEFLM1));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().frgCountTopoStableAfterTEFLM2));
				writer.append(",");
				writer.append(String.valueOf(entry.getValue().score));
				writer.append('\n');
	 
			}
		    
		    writer.flush();
		    writer.close();
		    
			System.out
					.println("Results for foreground voxel counts and scores saved in file "
							+ fileName);
		    
		} catch (IOException e) {
			System.err.println("Error while printing the foreground voxel and score results.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Build CSV input file for segmentation, for best and worst selected images
	 * 
	 * @param fileName
	 *            The output file name
	 * @param scaffold
	 *            The name of the scaffold
	 * @param bestScoreImage
	 *            The {@link ImageFileAndThreshResults} for the best selected
	 *            image
	 * @param worstScoreImage
	 *            The {@link ImageFileAndThreshResults} for the worst selected
	 *            image
	 */
	public void buildCSVInputForSegmentation(String fileName, String scaffold,
			ImageFileAndThreshResults bestScoreImage,
			ImageFileAndThreshResults worstScoreImage) {
		
		FileWriter writer;
		
		try {
			writer = new FileWriter(fileName);
			
		    // Best
			writer.append(scaffold);
			writer.append(",");
			writer.append("best");
			writer.append(",");
			writer.append(bestScoreImage.filename);
			writer.append(",");
			writer.append(String.valueOf(bestScoreImage.thresholdMinError));
			writer.append(",");
			writer.append(String.valueOf(bestScoreImage.thresholdTopoStable));
			writer.append('\n');
			
			// Worst
			writer.append(scaffold);
			writer.append(",");
			writer.append("worst");
			writer.append(",");
			writer.append(worstScoreImage.filename);
			writer.append(",");
			writer.append(String.valueOf(worstScoreImage.thresholdMinError));
			writer.append(",");
			writer.append(String.valueOf(worstScoreImage.thresholdTopoStable));
			writer.append('\n');
		    
		    writer.flush();
		    writer.close();
		    
			System.out
					.println("CSV input for segmentation saved in file "
							+ fileName);
		    
		} catch (IOException e) {
			System.err.println("Error while printing CSV input for segmentation.");
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length != 4)) {
			System.out
					.println("Please, specify the scaffold name, input dir for "
							+ "the CSV file computed with min error method, "
							+ "input dir for the CSV file computed with topo "
							+ "stable method, and output dir.");

			return;
		}

		// get arguments
		String scaffold = new String(args[0]);
		String inputDirMinErrorThresh = new String(args[1]);
		String inputDirTopoStableThresh = new String(args[2]);
		String outputDir = new String(args[3]);
		
		PrintStream out;

		try {
			// redirect console output in file
			out = new PrintStream(new FileOutputStream(outputDir
					+ File.separatorChar + scaffold + "_consoleOutput.txt"));
			System.setOut(out);
			System.setErr(out);
			
			// start time for benchmark
		    long startTime = System.currentTimeMillis();
			System.out
					.println("Start processing image selection and projection for scaffold "
							+ scaffold + "...");
		
		    // start image selection and projection of best and worst for the scaffold
			ImageSelectionAndProjection imageSelectionAndProjection = new ImageSelectionAndProjection();
			imageSelectionAndProjection.selectImageAccordingScore(scaffold, inputDirMinErrorThresh, inputDirTopoStableThresh, outputDir);
		
			// end time for benchmark
		    long endTime = System.currentTimeMillis();
			System.out
					.println("Image selection and projection execution time for scaffold "
							+ scaffold
							+ ": "
							+ (endTime - startTime)
							+ " millisecond.");
			System.out.println();
			
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: Could not open file = "
					+ outputDir + File.separatorChar + scaffold + "_consoleOutput.txt");
			e.printStackTrace();
		}
	}
	
	/**
	 * Class used in selectImageAccordingScore method (helper class)
	 * 
	 * @author Mylene Simon
	 *
	 */
	private class ImageFileAndThreshResults{
		
		public String filename;
		
		public int thresholdMinError;
		public int thresholdTopoStable;
		
		public double frgCountMinErrorAfterTE;
		public double frgCountTopoStableAfterTE;
		
		public double frgCountMinErrorAfterTEFLM1;
		public double frgCountMinErrorAfterTEFLM2;
		
		public double frgCountTopoStableAfterTEFLM1;
		public double frgCountTopoStableAfterTEFLM2;
		
		public double score;
		
		public ImageFileAndThreshResults(String filename) {
			this.filename= filename;
		}
		
	}

}
