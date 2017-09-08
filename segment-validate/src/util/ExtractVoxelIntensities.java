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
package util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author mhs1
 *
 */
public class ExtractVoxelIntensities {

	/**
	 * Count the number of Z frames per z stack for one scaffold in the folder
	 * where the FITS files are stored (FITS files of interest stored in
	 * fitsDir/scaffold)
	 * 
	 * @param fitsDir
	 *            The folder where the FITS files (for all scaffold) are stored
	 * @param scaffold
	 *            The scaffold of interest (FITS files of interest stored in
	 *            fitsDir/scaffold)
	 * @param outputDir
	 *            Output directory for CSV files
	 */
	public static void extractIntensitiesPerZstackForScaffold(String fitsDir,
			String scaffold, String outputDir) {
		
		final String fitsExtension = ".fits";
		final String csvExtension = "_intensities.csv";
		
		System.out.println(scaffold);
		
		// Get files in scaffold fits directory
		Collection<String> fitsDirFiles = FileOper.readFileDirectory(fitsDir
				);
		// keep only fits files
		Collection<String> scaffoldFITSFiles = FileOper.selectFileType(
				fitsDirFiles, fitsExtension);
		
		// file writer to write results in CSV file
		FileWriter writer;
		//FileWriter writerGlobal;
		
		double[] intensitiesFrenquencyGlobal = new double[65536];
		int numberOfVoxels = 0;
		int numberOfImages = 0;
				
		// write z slices counts in CSV file
		try {
			
//			writerGlobal = new FileWriter(outputDir 
//					+ File.separatorChar
//					+ scaffold
//					+ csvExtension);
			
			// Header
//			writerGlobal.append("Intensity,Frequency");
//			writerGlobal.append('\n');
			
			// iterate over fits files to get number of z slices per file
			for (Iterator<String> k = scaffoldFITSFiles.iterator(); k.hasNext();) {
				
				// get file path
				String fileName = k.next();
				
				// open file as ImagePlus object
				ImagePlus img = new ImagePlus(fileName);
				
				int xSize = img.getWidth();
				int ySize = img.getHeight();
				int zSize = img.getNSlices();
				
				writer = new FileWriter(outputDir 
						+ File.separatorChar
						+ img.getShortTitle()
						+ csvExtension);
				
				// Header
				writer.append("Intensity,Frequency");
				writer.append('\n');
				
				int[] intensitiesFrenquency = new int[65536];
				
				// Find intensities
				ImageStack imgStack = img.getStack();
				for(int z = 1; z <= zSize; ++ z) {
					ImageProcessor imgProc = imgStack.getProcessor(z);
					for(int x = 0; x < xSize; ++ x) {
						for(int y = 0; y < ySize; ++ y) {
							intensitiesFrenquency[imgProc.getPixel(x, y)] ++; 
						}
					}
				}
				numberOfImages ++;
				numberOfVoxels = xSize * ySize * zSize;
				
				// print intensities frequency
				for(int i=0; i<65536; i++)
				{
					//intensitiesFrenquencyGlobal[i] += ((double) intensitiesFrenquency[i] / (double) numberOfVoxels);
					writer.append(i + "," + intensitiesFrenquency[i]);
					writer.append('\n');
				}
				
				 writer.flush();
				 writer.close();
			}
//			for(int i=0; i<256; i++)
//			{
//				writerGlobal.append(i + "," + (intensitiesFrenquencyGlobal[i] / (double) numberOfImages));
//				writerGlobal.append('\n');
//			}
//			
//			writerGlobal.flush();
//			writerGlobal.close();
//		    
		} catch (IOException e) {
			System.err.println("Error while saving the intensities in CSV "
					+ "file");
			e.printStackTrace();
		}
		
	}
	
	
	
	public static void extractHistogramsFRGandBKG(ImagePlus rawStack, ImagePlus segStack, int firstFrame, int lastFrame,
			String outputDir, String imageName) {
		
		final String csvExtension = ".csv";
		
		// file writer to write results in CSV file
		FileWriter writerHistogramBlackFrames;
		FileWriter writerHistogramBKGCell;
		FileWriter writerHistogramFRGCell;
		FileWriter writerHistogramWholeImage;
				
		try {
			
			
			int xSize = rawStack.getWidth();
			int ySize = rawStack.getHeight();
			int zSize = rawStack.getNSlices();
			
			writerHistogramBlackFrames = new FileWriter(outputDir 
					+ File.separatorChar
					+ imageName + "_HistogramBlackFrames"
					+ csvExtension);
			
			writerHistogramBKGCell = new FileWriter(outputDir 
					+ File.separatorChar
					+ imageName + "_HistogramBKGCell"
					+ csvExtension);
			
			writerHistogramFRGCell = new FileWriter(outputDir 
					+ File.separatorChar
					+ imageName + "_HistogramFRGCell"
					+ csvExtension);
			
			writerHistogramWholeImage = new FileWriter(outputDir 
					+ File.separatorChar
					+ imageName + "_HistogramWholeImage"
					+ csvExtension);
			
			// Headers
			writerHistogramBlackFrames.append("Intensity,Frequency");
			writerHistogramBlackFrames.append('\n');
			
			writerHistogramBKGCell.append("Intensity,Frequency");
			writerHistogramBKGCell.append('\n');
			
			writerHistogramFRGCell.append("Intensity,Frequency");
			writerHistogramFRGCell.append('\n');
			
			writerHistogramWholeImage.append("Intensity,Frequency");
			writerHistogramWholeImage.append('\n');
			
			int[] histogramBlackFrames = new int[65536];
			int[] histogramBKGCell = new int[65536];
			int[] histogramFRGCell = new int[65536];
			int[] histogramWholeImage = new int[65536];
			
			// Find intensities
			ImageStack imgRawStack = rawStack.getStack();
			ImageStack imgSegStack = segStack.getStack();
			
			for(int z = 0; z < firstFrame; ++ z) {
				ImageProcessor imgProc = imgRawStack.getProcessor(z+1);
				for(int x = 0; x < xSize; ++ x) {
					for(int y = 0; y < ySize; ++ y) {
						histogramBlackFrames[imgProc.getPixel(x, y)] ++; 
						histogramWholeImage[imgProc.getPixel(x, y)] ++; 
					}
				}
			}
			
			for(int z = firstFrame; z <= lastFrame; ++ z) {
				ImageProcessor imgRawProc = imgRawStack.getProcessor(z+1);
				ImageProcessor imgSegProc = imgSegStack.getProcessor(z - firstFrame + 1);
				for(int x = 0; x < xSize; ++ x) {
					for(int y = 0; y < ySize; ++ y) {
						
						if(imgSegProc.getPixel(x, y) >= 255) 
							histogramFRGCell[imgRawProc.getPixel(x, y)] ++;
						else
							histogramBKGCell[imgRawProc.getPixel(x, y)] ++;
						
						histogramWholeImage[imgRawProc.getPixel(x, y)] ++; 
					}
				}
			}
			
			for(int z = lastFrame + 1; z < zSize; ++ z) {
				ImageProcessor imgProc = imgRawStack.getProcessor(z+1);
				for(int x = 0; x < xSize; ++ x) {
					for(int y = 0; y < ySize; ++ y) {
						histogramBlackFrames[imgProc.getPixel(x, y)] ++; 
						histogramWholeImage[imgProc.getPixel(x, y)] ++;
					}
				}
			}
			
			
			// print intensities frequency
			for(int i=0; i<65536; i++)
			{
				writerHistogramBlackFrames.append(i + "," + histogramBlackFrames[i]);
				writerHistogramBlackFrames.append('\n');
				
				writerHistogramFRGCell.append(i + "," + histogramFRGCell[i]);
				writerHistogramFRGCell.append('\n');
				
				writerHistogramBKGCell.append(i + "," + histogramBKGCell[i]);
				writerHistogramBKGCell.append('\n');
				
				writerHistogramWholeImage.append(i + "," + histogramWholeImage[i]);
				writerHistogramWholeImage.append('\n');
			}
			
			writerHistogramBlackFrames.flush();
			writerHistogramBlackFrames.close();
			
			writerHistogramFRGCell.flush();
			writerHistogramFRGCell.close();
			
			writerHistogramBKGCell.flush();
			writerHistogramBKGCell.close();
			
			writerHistogramWholeImage.flush();
			writerHistogramWholeImage.close();

		    
		} catch (IOException e) {
			System.err.println("Error while saving the intensities in CSV "
					+ "file");
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// initializations
		/*String fitsDir = args[0];
		String scaffold = args[1];
		String outputDir = args[2];*/
		
		// count zslices for the scaffold
//		ExtractVoxelIntensities
//				.extractIntensitiesPerZstackForScaffold(
//						"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//						"Alvetex",
//						"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//		
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Big NF",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Collagen Fibrils",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Collagen Gel",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Fibrin Gel",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Matrigel",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Nanofiber",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Nanofiber+OS",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Alvetex",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Big NF",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Big NF Bis",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Nanofiber",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Spun Coat",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"New Spun Coat+OS",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Spun Coat",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");
//
//		ExtractVoxelIntensities
//		.extractIntensitiesPerZstackForScaffold(
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS_files",
//				"Spun Coat+OS",
//				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/SegmentationSyntheticData/Intensities");

		ExtractVoxelIntensities
		.extractIntensitiesPerZstackForScaffold(
				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/Cell-scaffold-contact/inputFITScells",
				"Big NF",
				"/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/Cell-scaffold-contact/intensities");

		
	}


}
