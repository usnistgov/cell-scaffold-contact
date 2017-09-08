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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.FileOper;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * This is a class for image comparison and extraction of similarity metrics
 * (Venn Diagram and Dice index)
 * 
 * @author Mylene Simon
 *
 */
public class SimilarityMetricsExtractor {

	/**
	 * Hash map allowing storage of similarity metrics from orthogonal
	 * projections by z-stack over configuration
	 */
	private Map<String, SimilarityMetricsFor3D> similarityMetricsByConfiguration;
	
	/**
	 * Constructor, initializes the hash map
	 */
	public SimilarityMetricsExtractor() {
		similarityMetricsByConfiguration = 
				new HashMap<String, SimilarityMetricsFor3D>();
	}
	
	/**
	 * Browse through the directory of manual segmented images and the directory
	 * of auto segmented images and compare manual and auto segmented images for
	 * the selected scaffold
	 * 
	 * @param selectedScaffold
	 *            Scaffold of interest (name of the directory, ie 'Alvetex',
	 *            'Alvetex_best', 'Big NF', etc.)
	 * @param foregroundThreshold
	 *            Threshold value for a pixel to be considered as foreground
	 *            (pixel = foreground if pixel value >= threshold)
	 * @param manualInputsDir
	 *            Path of the directory containing all the sub directories for
	 *            the scaffolds, the manual segmented images being in these sub
	 *            directories
	 * @param autoInputsDir
	 *            Path of the directory containing all the sub directories for
	 *            the scaffolds, the auto segmented images being stored in
	 *            directories contained in these sub directories
	 * @param outputDir
	 *            Path of the directory where results will be stored
	 */
	public void browseDirectoriesAndComputeSimilarityMetrics(
			String selectedScaffold, int foregroundThreshold,
			String manualInputsDir, String autoInputsDir, String outputDir) {

		// file name and directory extensions
		final String autoImageFormatExtension = ".tif";
		final String manualImageFormatExtension = ".tif";
		final String resultsExtensionCSV = "_comparisonMetrics.csv";
		
		// clear hash map similarityMetricsByConfiguration
		similarityMetricsByConfiguration.clear();
		
		// create specific sub directory in output directory 
		String outputSpecificDir = outputDir 
				+ File.separatorChar + selectedScaffold;
		// if the directory doesn't exist, the directory 
		// (and parent directories if necessary) is created
		File outputTest = new File(outputSpecificDir);
		if(! outputTest.exists()) {
			outputTest.mkdirs();
		}
		
		// get Fits files in the manual input directory
		Collection<String> filesinManualInputDir = FileOper
				.readFileDirectory(manualInputsDir + File.separatorChar
						+ selectedScaffold);
		
		Collection<String> manualInputFiles = FileOper.selectFileType(
				filesinManualInputDir, manualImageFormatExtension);		
		
		System.out.println("Pocessing selected scaffold " + selectedScaffold
				+ "...");
		System.out.println();
		
		// iterate through the manual input files
		for (Iterator<String> k = manualInputFiles.iterator(); k.hasNext();) {
			
			// get file path
			String manualSegFileFullPath = k.next();
			
			// get file name without path and extension
			String manualSegFileNameWithoutExtension = manualSegFileFullPath.substring(
					manualSegFileFullPath.lastIndexOf(File.separatorChar),
					manualSegFileFullPath.lastIndexOf(manualImageFormatExtension));
			
			// get orthogonal projection type of the image (ex : 'maxXY')
			String orthogonalProjectionType = manualSegFileNameWithoutExtension
					.substring(manualSegFileNameWithoutExtension.lastIndexOf("_"));	
			
			// get list of sub directories containing the orthogonal projections
			// in directory of auto segmented images for the selected scaffold
			Collection<String> subDirOrthogonalProjections = FileOper
					.readSubDirectories(autoInputsDir + File.separatorChar
							+ selectedScaffold);
			
			Collection<String> projectedFilesToCompare = new ArrayList<String>();
			
			// get list of images with similar projections to compare in the 
			// sub directories
			for (Iterator<String> subDir = subDirOrthogonalProjections
					.iterator(); subDir.hasNext();) {
			 	
				Collection<String> filesInSubDirOrthogonalProjections = FileOper.readFileDirectory(subDir.next());
				projectedFilesToCompare.addAll(FileOper.selectFileType(
						filesInSubDirOrthogonalProjections, 
								orthogonalProjectionType
								+ autoImageFormatExtension));

			}
			
			System.out.println();
			
			// construct CSV file name
			String outputCSVFileName = outputSpecificDir 
					+ File.separatorChar
					+ manualSegFileNameWithoutExtension + resultsExtensionCSV;
			
			// compute and save similarity metrics
			computeSimilarityMetricsAndSaveInCSV(manualSegFileFullPath,
					projectedFilesToCompare, outputCSVFileName,
					foregroundThreshold);
			
		}
		
		System.out.println();
		
		// find sets of orthogonal projections with best and worst dice index 
		// means, and store in CSV file the corresponding images and results
		
		String mostAndLeastSimilarImagesCSVFileName = outputSpecificDir 
				+ File.separatorChar
				+ "MostAndLeastConformSegmentation"
				+ resultsExtensionCSV;
		
		String diceIndexMeansOverConfigurationCSVFileName = outputSpecificDir 
				+ File.separatorChar
				+ "DiceIndexMeansOverConfiguration"
				+ resultsExtensionCSV;
		
		save3DSimilarityMetricsAndFindMostAndLeastSimilar(selectedScaffold,
				diceIndexMeansOverConfigurationCSVFileName,
				mostAndLeastSimilarImagesCSVFileName);
	}
	
	/**
	 * Compute Similarity Metrics (Venn Diagram quantities and Dice index)
	 * between a reference image and several images to compare, and save results
	 * in a CSV file
	 * 
	 * @param refImageFileName
	 *            FileName of reference image
	 * @param imagesToCompareList
	 *            List of file names of images to compare with reference image
	 * @param outputCSVFileName
	 *            File name of CSV output file
	 * @param foregroundThreshold
	 *            Threshold value for a pixel to be considered as foreground
	 *            (pixel = foreground if pixel value >= threshold)
	 */
	public void computeSimilarityMetricsAndSaveInCSV(String refImageFileName,
			Collection<String> imagesToCompareList, String outputCSVFileName,
			int foregroundThreshold) {
		
		// file writer to write results in CSV file
		FileWriter writer;
		
		// objects to store information for most similar image
		String imageWithBestDiceIndex = "";
		double bestDiceIndex = Double.MIN_VALUE;
		VennDiagram vennDiagramForBestDiceIndexImage = new VennDiagram();
		
		// objects to store information for least similar image
		String imageWithWorstDiceIndex = "";
		double worstDiceIndex = Double.MAX_VALUE;
		VennDiagram vennDiagramForWorstDiceIndexImage = new VennDiagram();
		
		// write comparison metrics in CSV file
		try {
			writer = new FileWriter(outputCSVFileName);
			
			// Header
			writer.append("Manual_Segmented_FileName");
		    writer.append(',');
		    writer.append("Auto_Segmented_FileName");
		    writer.append(',');
		    writer.append("VennDiagram_similar_frg_pixels");
		    writer.append(',');
		    writer.append("VennDiagram_only_ref_frg_pixels");
		    writer.append(',');
		    writer.append("VennDiagram_only_img_frg_pixels");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_similar_frg_pixels");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_only_ref_frg_pixels");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_only_img_frg_pixels");
		    writer.append(',');
		    writer.append("Dice_index");
		    writer.append('\n');
		    
		    // load reference image
		    ImagePlus refImage = new ImagePlus(refImageFileName);
		    
		    // load each image to compare, compute comparison metrics and save 
		    // them in the CSV file
		    for (Iterator<String> k = imagesToCompareList.iterator(); k.hasNext();) {
		    	
		    	// get image to compare file name
				String imageToCompareFileName = k.next();
				
				// load image to compare
				ImagePlus imageToCompare = new ImagePlus(imageToCompareFileName);
				
				// extract Venn Diagram quantities
				VennDiagram vennDiagramResults = extractVennDiagramQuantities(
						refImage, imageToCompare, foregroundThreshold);
				
				// compute Dice index
				double diceIndex = computeDiceIndex(refImage, imageToCompare,
						foregroundThreshold);
				
				// save results in CSV
				writer.append(refImageFileName);
			    writer.append(',');
			    writer.append(imageToCompareFileName);
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.similarFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.onlyRefFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.onlyImgFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.proportionSimilarFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.proportionOnlyRefFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(vennDiagramResults.proportionOnlyImgFrgPixels));
			    writer.append(',');
			    writer.append(String.valueOf(diceIndex));
			    writer.append('\n');
			    
			    // check if dice index is the best
			    if(diceIndex > bestDiceIndex) {
			    	bestDiceIndex = diceIndex;
			    	imageWithBestDiceIndex = imageToCompareFileName;
			    	vennDiagramForBestDiceIndexImage = vennDiagramResults;
			    }
			    
			    // check is dice index is the worst
			    if(diceIndex < worstDiceIndex) {
			    	worstDiceIndex = diceIndex;
			    	imageWithWorstDiceIndex = imageToCompareFileName;
			    	vennDiagramForWorstDiceIndexImage = vennDiagramResults;
			    }
				
			    // update the hash map storing the values of Dice index and Venn 
			    // Diagram quantities in case of max orthogonal projections
				updateSimilarityMetricsHashMap(imageToCompareFileName,
						diceIndex, vennDiagramResults);
		    }
		    
		    writer.flush();
		    writer.close();
		    
			System.out
					.println("Comparison metrics saved in file "
							+ outputCSVFileName);
			
			// print most similar and least similar images file names
			System.out
					.println("Best Dice index found for image "
							+ imageWithBestDiceIndex);
			System.out
					.println("Dice index = " + bestDiceIndex);
			System.out
					.println("Number of similar foreground pixels with "
							+ "reference image = "
							+ vennDiagramForBestDiceIndexImage.similarFrgPixels);
			System.out
					.println("Proportion of similar foreground pixels with "
							+ "reference image = "
							+ vennDiagramForBestDiceIndexImage
								.proportionSimilarFrgPixels);

			System.out
					.println("Worst Dice index found for image "
							+ imageWithWorstDiceIndex);
			System.out
					.println("Dice index = " + worstDiceIndex);
			System.out
					.println("Number of similar foreground pixels with reference"
							+ " image = "
							+ vennDiagramForWorstDiceIndexImage.similarFrgPixels);
			System.out
					.println("Proportion of similar foreground pixels with "
							+ "reference image = "
							+ vennDiagramForWorstDiceIndexImage
								.proportionSimilarFrgPixels);
			System.out.println();
			
		    
		} catch (IOException e) {
			System.err.println("Error while saving the comparison metrics.");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Extract Venn Diagram quantities from two images to compare
	 * 
	 * @param manualSegmentedImage
	 *            ImagePlus object for the manual segmented image
	 * @param autoSegmentedImage
	 *            ImagePlus object for the auto segmented image
	 * @param foregroundThreshold
	 *            Threshold value for a pixel to be considered as foreground
	 *            (pixel = foreground if pixel value >= threshold)
	 * @return VennDiagram object containing Venn Diagram quantities or null if
	 *         the two images cannot be compared
	 */
	public VennDiagram extractVennDiagramQuantities(ImagePlus manualSegmentedImage,
			ImagePlus autoSegmentedImage, int foregroundThreshold) {

		int similarFrgPixels = 0;
		int onlyRefFrgPixels = 0;
		int onlyImgFrgPixels = 0;
		
		double proportionSimilarFrgPixels = 0.0;
		double proportionOnlyRefFrgPixels = 0.0;
		double proportionOnlyImgFrgPixels = 0.0;
		
		// initialize VennDiagram object to store Venn Diagram results
		VennDiagram vennDiagramResult = new VennDiagram();

		System.out.println("Extract Venn Diagram Quantities...");
		System.out.println("Manual segmented image: "
				+ manualSegmentedImage.getTitle());
		System.out.println("Auto segmented image: "
				+ autoSegmentedImage.getTitle());
		
		// get image processors for the two images
		ImageProcessor manualSegmentedImageProc = manualSegmentedImage
				.getProcessor();
		ImageProcessor autoSegmentedImageProc = autoSegmentedImage
				.getProcessor();

		// check if images can be compared
		if (manualSegmentedImage.getHeight() != autoSegmentedImage.getHeight()
			|| manualSegmentedImage.getWidth() != autoSegmentedImage.getWidth()) {
			
			System.err
					.println("The two images cannot be compared, the sizes are "
							+ "not the same.");
			System.err
					.println("Venn Diagram quantities computation stopped for "
							+ "this pair.");
			System.out.println();
			
			return null;
		}

		int imgWidth = manualSegmentedImage.getWidth();
		int imgHeight = manualSegmentedImage.getHeight();
		int totalPixels = imgWidth * imgHeight;
		
		// count number of similar and different foreground pixels between the
		// two images
		for (int x = 0; x < imgWidth; ++ x) {
			
			for(int y = 0; y < imgHeight; ++ y) {
				
				int manualSegmentedPixel = manualSegmentedImageProc.getPixel(x,y);
				int autoSegmentedPixel = autoSegmentedImageProc.getPixel(x,y);
				
				if (manualSegmentedPixel >= foregroundThreshold
						&& autoSegmentedPixel >= foregroundThreshold)
					similarFrgPixels ++;
				else if(autoSegmentedPixel >= foregroundThreshold)
					onlyImgFrgPixels ++;
				else if(manualSegmentedPixel >= foregroundThreshold)
					onlyRefFrgPixels ++;
			}
		}
		
		System.out.println("Similar foreground pixels: " + similarFrgPixels);
		System.out.println("Foreground pixels only in manual segmented image: "
				+ onlyRefFrgPixels);
		System.out.println("Foreground pixels only in auto segmented image: "
				+ onlyImgFrgPixels);
		
		// compute proportions of similar and different pixels (in %)
		proportionSimilarFrgPixels = 
				((double) similarFrgPixels 
						/ (double) totalPixels) 
				* 100.0;
		proportionOnlyRefFrgPixels = 
				((double) onlyRefFrgPixels 
						/ (double) totalPixels) 
				* 100.0;
		proportionOnlyImgFrgPixels = 
				((double) onlyImgFrgPixels 
						/ (double) totalPixels) 
				* 100.0;
		
		System.out
				.println("Proportion of similar foreground pixels: "
						+ proportionSimilarFrgPixels + " %.");
		System.out
				.println("Proportion of foreground pixels only in manual "
						+ "segmented image: "
						+ proportionOnlyRefFrgPixels + " %.");
		System.out
				.println("Proportion of foreground pixels only in auto "
						+ "segmented image: "
						+ proportionOnlyImgFrgPixels + " %.");
		System.out.println();
		
		// update Venn Diagram object to return the results
		vennDiagramResult.similarFrgPixels = similarFrgPixels;
		vennDiagramResult.onlyImgFrgPixels = onlyImgFrgPixels;
		vennDiagramResult.onlyRefFrgPixels = onlyRefFrgPixels;
		vennDiagramResult.proportionSimilarFrgPixels = proportionSimilarFrgPixels;
		vennDiagramResult.proportionOnlyImgFrgPixels = proportionOnlyImgFrgPixels;
		vennDiagramResult.proportionOnlyRefFrgPixels = proportionOnlyRefFrgPixels;
		
		return vennDiagramResult;
	}
	
	/**
	 * Compute Dice index between two images 
	 * Compare foreground overlap between the two images to compute the Dice 
	 * index
	 * 
	 * @param manualSegmentedImage
	 *            ImagePlus object for the manual segmented image
	 * @param autoSegmentedImage
	 *            ImagePlus object for the auto segmented image
	 * @param foregroundThreshold
	 *            Foreground threshold value (minimum value for a pixel to be
	 *            considered as foreground)
	 * @return Dice index (or -1.0 if images cannot be compared)
	 */
	public static double computeDiceIndex(ImagePlus manualSegmentedImage,
			ImagePlus autoSegmentedImage, int foregroundThreshold) {
		
		double diceIndex = 0.0;
		
		int countFrgOverlapPixels = 0;
		int countFrgManualPixels = 0;
		int countFrgAutoPixels = 0;
		
		System.out.println("Compute Dice Index...");
		System.out.println("Manual segmented image: "
				+ manualSegmentedImage.getTitle());
		System.out.println("Auto segmented image: "
				+ autoSegmentedImage.getTitle());
		
		// get image processors for the two images
		ImageProcessor manualSegmentedImageProc = manualSegmentedImage
				.getProcessor();
		ImageProcessor autoSegmentedImageProc = autoSegmentedImage
				.getProcessor();
		
		// check if images can be compared
		if (manualSegmentedImage.getHeight() != autoSegmentedImage.getHeight()
		 || manualSegmentedImage.getWidth() != autoSegmentedImage.getWidth()) {
			
			System.err
					.println("The two images cannot be compared, the sizes are "
							+ "not the same.");
			System.err
					.println("Dice index computation stopped for "
							+ "this pair.");
			return -1.0;
		}
		
		int imgWidth = manualSegmentedImage.getWidth();
		int imgHeight = manualSegmentedImage.getHeight();
		
		// count number of similar and different foreground pixels between the
		// two images
		for (int x = 0; x < imgWidth; ++ x) {
			
			for(int y = 0; y < imgHeight; ++ y) {
				
				int manualSegmentedPixel = manualSegmentedImageProc.getPixel(x,y);
				int autoSegmentedPixel = autoSegmentedImageProc.getPixel(x,y);
				
				if (manualSegmentedPixel >= foregroundThreshold
						&& autoSegmentedPixel >= foregroundThreshold) {
					countFrgOverlapPixels ++;
					countFrgAutoPixels ++;
					countFrgManualPixels ++;
				}
				else if(autoSegmentedPixel >= foregroundThreshold)
					countFrgAutoPixels ++;
				else if(manualSegmentedPixel >= foregroundThreshold)
					countFrgManualPixels ++;
			}
		}
		
		// if there is no foreground pixel in the two images, 
		// images are considered as similar
		if(countFrgManualPixels == 0 && countFrgAutoPixels == 0) {
			System.out.println("No foreground pixels found in the two images.");
			System.out.println("Images are considered as similar, "
					+ "Dice index = 1.0.");
			return 1.0;
		}
		
		// compute dice index
		diceIndex = (2 * (double) countFrgOverlapPixels)
				/ ((double) countFrgManualPixels + (double) countFrgAutoPixels);
		
		System.out.println("The Dice index is " + diceIndex);
		System.out.println();
		
		return diceIndex;
		
	}
	
	
	/**
	 * Update the SimilarityMetricsHashMap with the values for the max
	 * projection processed. Do not save any results if the processed file is
	 * not a maximum projection. The aim is to regroup the similarity metrics
	 * for the three orthogonal projections of the same 3D file.
	 * 
	 * @param fileName
	 *            The file name of the processed file
	 * @param diceIndex
	 *            The Dice index computed for the processed file
	 * @param vennDiagram
	 *            The Venn Diagram extracted for the processed file
	 */
	private void updateSimilarityMetricsHashMap(String fileName,
			double diceIndex, VennDiagram vennDiagram) {

		final String maxXYprojectionExtension = "_maxXY";
		final String maxZYprojectionExtension = "_maxZY";
		final String maxXZprojectionExtension = "_maxXZ";

		// Find what is the projection type of the processed file
		String projectionExtension = fileName.substring(
				fileName.lastIndexOf("_"), fileName.lastIndexOf("."));
		String fileNameWithoutExtension = fileName.substring(0,
				fileName.lastIndexOf("."));
		String fileNameExtension = fileName
				.substring(fileName.lastIndexOf("."));
		String fileNameWithoutProjectionExtension = fileNameWithoutExtension
				.substring(0, fileNameWithoutExtension
						.lastIndexOf(projectionExtension))
				+ fileNameExtension;

		// If the processed file name does contain the extension an
		// orthogonal max projection, the similarity metrics will be added to
		// the hash map
		if (projectionExtension != null && projectionExtension.length() != 0) {
			
			// Get the entry corresponding to the file in the hash map (or 
			// create it if it doesn't exist)
			SimilarityMetricsFor3D similarityMetricsOfFile = 
					similarityMetricsByConfiguration
					.get(fileNameWithoutProjectionExtension);
			if(similarityMetricsOfFile == null) {
				similarityMetricsByConfiguration.put(
						fileNameWithoutProjectionExtension,
						new SimilarityMetricsFor3D());
				similarityMetricsOfFile = 
						similarityMetricsByConfiguration
						.get(fileNameWithoutProjectionExtension);
			}
			
			// Case of max XY projection
			if (projectionExtension.equals(maxXYprojectionExtension)) {
				
				similarityMetricsOfFile.diceXY = diceIndex;
				similarityMetricsOfFile.vennDiagramXY = vennDiagram;
				similarityMetricsByConfiguration.put(
						fileNameWithoutProjectionExtension,
						similarityMetricsOfFile);
				System.out
						.println("Stored similary metrics in memory for maxXY "
								+ "projection of file "
								+ fileNameWithoutProjectionExtension);
			
			// Case of max ZY projection
			} else if (projectionExtension.equals(maxZYprojectionExtension)) {
				
				similarityMetricsOfFile.diceZY = diceIndex;
				similarityMetricsOfFile.vennDiagramZY = vennDiagram;
				similarityMetricsByConfiguration.put(
						fileNameWithoutProjectionExtension,
						similarityMetricsOfFile);
				System.out
						.println("Stored similary metrics in memory for maxZY "
								+ "projection of file "
								+ fileNameWithoutProjectionExtension);
			
			// Case of max XZ projection
			} else if (projectionExtension.equals(maxXZprojectionExtension)) {
				
				similarityMetricsOfFile.diceXZ = diceIndex;
				similarityMetricsOfFile.vennDiagramXZ = vennDiagram;
				similarityMetricsByConfiguration.put(
						fileNameWithoutProjectionExtension,
						similarityMetricsOfFile);
				System.out
						.println("Stored similary metrics in memory for maxXZ "
								+ "projection of file "
								+ fileNameWithoutProjectionExtension);
			
			// If the processed file name does not contain the extension an
			// orthogonal max projection, the similarity metrics are not
			// stored in the hash map
			} else {
				
				System.out
						.println("This file name doesn't seem to include a max "
								+ "projection extension, no max projection "
								+ "similarity metrics stored in memory");
			}
		}
		
		System.out.println();

	}
	
	/**
	 * Find least and most similar file according to the Dice index mean (for
	 * the 3 projections), save the results in a CSV file and save all the Dice
	 * index means over configuration in another CSV file.
	 * 
	 * @param selectedScaffold
	 *            Scaffold of interest
	 * @param similarityResultsFor3DFileName
	 *            File name of the CSV file used to store Dice index means over
	 *            configuration
	 * @param mostAndLeastConformConfigFileName
	 *            File name of the CSV file used to store similarity metrics for
	 *            most and least similar image
	 */
	public void save3DSimilarityMetricsAndFindMostAndLeastSimilar(
			String selectedScaffold,
			String similarityResultsFor3DFileName,
			String mostAndLeastConformConfigFileName) {
		
		TreeMap<String, Double> diceIndexMeanByConfiguration = new TreeMap<String, Double>();
		
		// find sets of orthogonal projections with best and worst dice index 
		// means
		double bestDiceIndexMean = Double.MIN_VALUE;
		double worstDiceIndexMean = Double.MAX_VALUE;
		Entry<String, SimilarityMetricsFor3D> mostSimilarImage = 
				new AbstractMap.SimpleEntry<String, SimilarityMetricsFor3D>(
				"", new SimilarityMetricsFor3D());
		Entry<String, SimilarityMetricsFor3D> leastSimilarImage = 
				new AbstractMap.SimpleEntry<String, SimilarityMetricsFor3D>(
				"", new SimilarityMetricsFor3D());
		
		for (Iterator<Entry<String, SimilarityMetricsFor3D>> entry = similarityMetricsByConfiguration
				.entrySet().iterator(); entry.hasNext();) {
			Entry<String, SimilarityMetricsFor3D> currentEntry = entry.next();
			double diceMean = (currentEntry.getValue().diceXY
					+ currentEntry.getValue().diceXZ 
					+ currentEntry.getValue().diceZY) 
					/ 3.0;
			if (diceMean > bestDiceIndexMean) {
				bestDiceIndexMean = diceMean;
				mostSimilarImage = currentEntry;
			}
			if (diceMean < worstDiceIndexMean) {
				worstDiceIndexMean = diceMean;
				leastSimilarImage = currentEntry;
			}
			
			// extract segmentation configuration from the FITS file name
			String segmentationConfiguration = currentEntry.getKey();
			int indexLastUnderscore = segmentationConfiguration.lastIndexOf("_");
			int indexLastDot = segmentationConfiguration.lastIndexOf(".");
			// extract configuration if found in file name, otherwise let full file name
			if (indexLastUnderscore != -1
					&& indexLastUnderscore != (segmentationConfiguration.length() - 1) 
					&& indexLastDot != -1
					&& indexLastDot != segmentationConfiguration.length()) {
				segmentationConfiguration = segmentationConfiguration
						.substring(indexLastUnderscore + 1, indexLastDot);
			}
			
			diceIndexMeanByConfiguration.put(segmentationConfiguration, diceMean);
		}
		
		// save best and worst results in CSV file
		
		// file writer to write results in CSV file
		FileWriter writer;
		
		// write Dice index means over configuration in CSV file
		try {
			writer = new FileWriter(similarityResultsFor3DFileName);
			
			// Header
			writer.append("Configuration");
		    writer.append(',');
		    writer.append(selectedScaffold);
		    writer.append('\n');
		    
		    // Data (dice index means over configuration)
		    for (Iterator<Map.Entry<String,Double>> entry = diceIndexMeanByConfiguration
					.entrySet().iterator(); entry.hasNext();) {
			    
		    	Map.Entry<String,Double> currentEntry = entry.next();
			    
		    	writer.append(currentEntry.getKey());
			    writer.append(',');
			    writer.append(String.valueOf(currentEntry.getValue()));
			    writer.append('\n');
			    
		    }
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving the Dice index means over configuration");
			e.printStackTrace();
		}
		
		// write comparison metrics in CSV file
		try {
			writer = new FileWriter(mostAndLeastConformConfigFileName);
			
			// Header
			writer.append("Conformity");
		    writer.append(',');
		    writer.append("Auto_Segmented_FileName");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_similar_frg_pixels_mean");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_only_ref_frg_pixels_mean");
		    writer.append(',');
		    writer.append("VennDiagram_proportion_only_img_frg_pixels_mean");
		    writer.append(',');
		    writer.append("Dice_index_mean");
		    writer.append('\n');
		    
		    // most similar
		    writer.append("Most Similar");
		    writer.append(',');
		    writer.append(mostSimilarImage.getKey());
		    writer.append(',');
			writer.append(String.valueOf(
					(mostSimilarImage.getValue().vennDiagramXY.proportionSimilarFrgPixels
					+ mostSimilarImage.getValue().vennDiagramXZ.proportionSimilarFrgPixels 
					+ mostSimilarImage.getValue().vennDiagramZY.proportionSimilarFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(
					(mostSimilarImage.getValue().vennDiagramXY.proportionOnlyRefFrgPixels
					+ mostSimilarImage.getValue().vennDiagramXZ.proportionOnlyRefFrgPixels 
					+ mostSimilarImage.getValue().vennDiagramZY.proportionOnlyRefFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(
					(mostSimilarImage.getValue().vennDiagramXY.proportionOnlyImgFrgPixels
					+ mostSimilarImage.getValue().vennDiagramXZ.proportionOnlyImgFrgPixels 
					+ mostSimilarImage.getValue().vennDiagramZY.proportionOnlyImgFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(bestDiceIndexMean));
		    writer.append('\n');
		    
		    // least similar
		    writer.append("Least Similar");
		    writer.append(',');
		    writer.append(leastSimilarImage.getKey());
		    writer.append(',');
		    writer.append(String.valueOf(
					(leastSimilarImage.getValue().vennDiagramXY.proportionSimilarFrgPixels
					+ leastSimilarImage.getValue().vennDiagramXZ.proportionSimilarFrgPixels 
					+ leastSimilarImage.getValue().vennDiagramZY.proportionSimilarFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(
					(leastSimilarImage.getValue().vennDiagramXY.proportionOnlyRefFrgPixels
					+ leastSimilarImage.getValue().vennDiagramXZ.proportionOnlyRefFrgPixels 
					+ leastSimilarImage.getValue().vennDiagramZY.proportionOnlyRefFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(
					(leastSimilarImage.getValue().vennDiagramXY.proportionOnlyImgFrgPixels
					+ leastSimilarImage.getValue().vennDiagramXZ.proportionOnlyImgFrgPixels 
					+ leastSimilarImage.getValue().vennDiagramZY.proportionOnlyImgFrgPixels) 
					/ 3.0));
		    writer.append(',');
		    writer.append(String.valueOf(worstDiceIndexMean));
		    writer.append('\n');
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving the comparison metrics for "
					+ "most and least similar segmented images");
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
		if ((args == null) || (args.length != 5)) {
			System.out
					.println("Please, specify the scaffold name, foreground "
							+ "threshold, input dir for manual segmented images"
							+ ", input dir for auto segmented images, and "
							+ "output dir.");

			return;
		}

		// get arguments
		String scaffold = new String(args[0]);
		int foregroundThreshold = Integer.parseInt(new String(args[1]));
		String inputDirManualSegmentation = new String(args[2]);
		String inputDirAutoSegmentation = new String(args[3]);
		String outputDir = new String(args[4]);
		
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
					.println("Start computing similarity metrics for scaffold "
							+ scaffold + "...");
		
			// compute similarity metrics and save results in CSV file
		    String scaffoldBest = scaffold + "_Sample1";
		    String scaffoldWorst = scaffold + "_Sample2";
		    String scaffold3 = scaffold + "_Sample3";

			SimilarityMetricsExtractor mySimilarityMetricsExtractor = 
					new SimilarityMetricsExtractor();
			
			mySimilarityMetricsExtractor
					.browseDirectoriesAndComputeSimilarityMetrics(scaffoldBest,
							foregroundThreshold, inputDirManualSegmentation,
							inputDirAutoSegmentation, outputDir);
			
			mySimilarityMetricsExtractor
					.browseDirectoriesAndComputeSimilarityMetrics(
							scaffoldWorst, foregroundThreshold,
							inputDirManualSegmentation,
							inputDirAutoSegmentation, outputDir);
			
			mySimilarityMetricsExtractor
					.browseDirectoriesAndComputeSimilarityMetrics(
							scaffold3, foregroundThreshold,
							inputDirManualSegmentation,
							inputDirAutoSegmentation, outputDir);

			// end time for benchmark
		    long endTime = System.currentTimeMillis();
			System.out
					.println("Similarity computation execution time for scaffold "
							+ scaffold
							+ ": "
							+ (endTime - startTime)
							+ " millisecond.");
			System.out.println();
			
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: Could not open file = "
					+ outputDir + File.separatorChar + scaffold 
					+ "_consoleOutput.txt");
			e.printStackTrace();
		}

	}
	
	/**
	 * Class used to store Venn Diagram results (helper class)
	 * 
	 * @author Mylene Simon
	 *
	 */
	public class VennDiagram {
		
		public int similarFrgPixels;
		
		public int onlyRefFrgPixels;
		
		public int onlyImgFrgPixels;
		
		public double proportionSimilarFrgPixels;
		
		public double proportionOnlyRefFrgPixels;
		
		public double proportionOnlyImgFrgPixels;
		
	}
	
	/**
	 * Class used to store results for the three orthogonal projections of 3D
	 * image (helper class)
	 * 
	 * @author Mylene Simon
	 *
	 */
	private class SimilarityMetricsFor3D {
		
		public double diceXY;
		
		public double diceZY;
		
		public double diceXZ;
		
		public VennDiagram vennDiagramXY;
		
		public VennDiagram vennDiagramZY;
		
		public VennDiagram vennDiagramXZ;
		
		public SimilarityMetricsFor3D() {
			
			vennDiagramXY = new VennDiagram();
			vennDiagramZY = new VennDiagram();
			vennDiagramXZ = new VennDiagram();
			
		}
		
	}

}
