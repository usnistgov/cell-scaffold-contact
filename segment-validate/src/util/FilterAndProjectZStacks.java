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
import ij.io.FileSaver;
import ij.process.ByteProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import validation.OrthogonalProjection;

/**
 * This a class to count the number of z-frames per z-stacks for a given scaffold
 * and then write JPEG images containing the three orthogonal projections of all
 * z-stacks containing a number of z-frames fitting in the range given in input
 * 
 * @author Mylene Simon
 *
 */
public class FilterAndProjectZStacks {

	/**
	 * Count the number of Z frames per z stack for one scaffold in the folder
	 * where the FITS files are stored (FITS files of interest stored in
	 * fitsDir/scaffold) and then write JPEG images containing the three
	 * orthogonal projections of all z-stacks containing a number of z-frames
	 * fitting in the range given in input
	 * 
	 * @param fitsDir
	 *            The folder where the FITS files (for all scaffold) are stored
	 * @param scaffold
	 *            The scaffold of interest (FITS files of interest stored in
	 *            fitsDir/scaffold)
	 * @param outputDir
	 *            Output directory for CSV files
	 * @param zFrameMin
	 *            Number minimum of z-frames of the range of interest
	 * @param zFrameMax
	 *            Number maximum of z-frames of the range of interest
	 */
	public static void countZframesPerZstackForScaffold(String fitsDir,
			String scaffold, String outputDir, int zFrameMin, int zFramesMax) {
		
		final String fitsExtension = ".fits";
		final String csvExtension = "CollagenGelCandidates.csv";
		
		System.out.println(scaffold);
		
		// Get files in scaffold fits directory
		Collection<String> fitsDirFiles = FileOper.readFileDirectory(fitsDir
				+ File.separatorChar + scaffold);
		// keep only fits files
		Collection<String> scaffoldFITSFiles = FileOper.selectFileType(
				fitsDirFiles, fitsExtension);
		
		// file writer to write results in CSV file
		FileWriter writer;
				
		// write candidates counts in CSV file
		try {
			writer = new FileWriter(outputDir 
					+ File.separatorChar
					+ csvExtension);
			
			// Header
			writer.append(scaffold + " Candidates,Number of z-frames");
			writer.append('\n');
			
			// iterate over fits files to get number of z slices per file
			for (Iterator<String> k = scaffoldFITSFiles.iterator(); k.hasNext();) {
						
				// get file path
				String fileName = k.next();
				// open file as ImagePlus object
				ImagePlus img = new ImagePlus(fileName);
				// print number of slices in image
				int nSlices = img.getNSlices();
				if(nSlices >= zFrameMin && nSlices <= zFramesMax) {
					String zStackName = fileName.substring(
							fileName.lastIndexOf(File.separatorChar) + 1,
							fileName.lastIndexOf(".fits"));
					writer.append(zStackName);
					writer.append("," + String.valueOf(nSlices));
					writer.append('\n');
					
					// compute projection sizes and result image size
					ImagePlus projectionXY = OrthogonalProjection.projectionXY(img, OrthogonalProjection.projectionType_Max);
					ImagePlus projectionXZ = OrthogonalProjection.projectionXZ(img, OrthogonalProjection.projectionType_Max);
					ImagePlus projectionZY = OrthogonalProjection.projectionZY(img, OrthogonalProjection.projectionType_Max);
					int widthXY = projectionXY.getWidth();
					int heightXY = projectionXY.getHeight();
					int widthXZ = projectionXZ.getWidth();
					int heightXZ = projectionXZ.getHeight();
					int widthZY = projectionZY.getWidth();
					int heightZY = projectionZY.getHeight();
					int smallBorderSize = 10;
					int largeBorderSize = smallBorderSize * 3;
					int resImageWidth = widthXY + widthZY 
							+ largeBorderSize * 2 + smallBorderSize;
					int resImageHeight = heightXY + heightXZ 
							+ largeBorderSize * 2 + smallBorderSize;
					
					// create new data array for the result image
					byte[] resData = new byte[resImageWidth * resImageHeight];
					Arrays.fill(resData, (byte) 255);
					
					// get projections data
					ByteProcessor rawXYImageProcessor = 
							(ByteProcessor) projectionXY
							.getProcessor();
					ByteProcessor rawXZImageProcessor = 
							(ByteProcessor) projectionXZ
							.getProcessor();
					ByteProcessor rawZYImageProcessor = 
							(ByteProcessor) projectionZY
							.getProcessor();
					
					// draw the projections on result image
					
					// XY projections
					for (int x = 0; x < widthXY; ++x) {
						for (int y = 0; y < heightXY; ++y) {
							
							resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
							        * resImageWidth 
							        + (x + largeBorderSize + smallBorderSize + widthZY)] 
							        = (byte) rawXYImageProcessor.getPixel(x, y);
						}
					}
					
					// XZ projections
					for (int x = 0; x < widthXZ; ++x) {
						for (int y = 0; y < heightXZ; ++y) {
							
							resData[(y + largeBorderSize) * resImageWidth 
							        + (x + largeBorderSize + smallBorderSize + widthZY)] 
							        = (byte) rawXZImageProcessor.getPixel(x, y);
						}
					}
					
					// ZY projections
					for (int x = 0; x < widthZY; ++x) {
						for (int y = 0; y < heightZY; ++y) {
							
							resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
							        * resImageWidth 
							        + (x + largeBorderSize)] 
							        = (byte) rawZYImageProcessor.getPixel(x, y);
						}
					}
					
					// construct result image processor with the result data
					ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,
							resImageHeight, resData);
					
					// construct result ImagePlus object
					ImagePlus resImage = new ImagePlus(outputDir
							+ File.separatorChar + zStackName + "_projection.jpeg",
							imgResProcessor);

					// save result image as JPEG in the output folder
					FileSaver fs = new FileSaver(resImage);
					fs.saveAsJpeg(outputDir
							+ File.separatorChar + zStackName + "_projection.jpeg");
				}
			}
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving the z slices counts in CSV "
					+ "file");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// initializations
		String fitsDir = args[0];
		String scaffold = args[1];
		String outputDir = args[2];
		int zFrameMin = Integer.parseInt(args[3]);
		int zFramesMax = Integer.parseInt(args[4]);
		
		countZframesPerZstackForScaffold(fitsDir,
				scaffold, outputDir, zFrameMin, zFramesMax);
		
	}

}
