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
package pipeline3D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import io.Fits3DWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import segment3D.Image3DCropping;
import segment3D.Image3DSmoothing;
import segment3D.Segment3DImage;
import threshold3D.EGTThresh;
import threshold3D.MinErrorThresh;
import threshold3D.OtsuThresh;
import threshold3D.TriangleThresh;
import util.FileOper;
import validation.OrthogonalProjection;
import validation.ProjectionsConcatenationForValidation;

/**
 * This class was designed for single fiber z-stack analyses
 * 
 * Tasks: (1) create max projections from z-stacks
 * (2) select those skeleton points extracted from single fibers (by segmenting all fibers and estimating their skeleton and radius)
 * that belong to the reference fiber (manually determined in ImageJ/Fiji as a set of lines defined by end points). This is important
 * since the FOV contains multiple fibers and only one reference fiber was used when the reference SEM imaging 
 * was performed.
 *  
 * 
 * 
 * 
 * @author peter bajcsy
 *
 */
public class Single3DFiberPipeline {

	private static Log logger = LogFactory
			.getLog(Single3DFiberPipeline.class);
	private static final String CSV_HEADER = "ImageName,Threshold,ForegroundVovelCount,StartingFrame,EndingFrame";
	private static final String CSV_SEPARATOR = ",";
	private static final char CSV_NEWLINE = '\n';
	
	public ImagePlus segmentImage(ImagePlus img3D, int threshold) {
		
		logger.info("Starting segmentation of image...");
		
		 // create segment3DImage object with the image
		Segment3DImage segment3DImage = new Segment3DImage(img3D);

		//segment3DImage.fillHoles();
		segment3DImage.makeSingleComponent();

		// Update the segmented image ImagePlus object
		return segment3DImage.generateSegmentedImagePlus();		
	}
	
	public void saveImage(ImagePlus resImage, String outputFilePath){
		
//		int resImageHeight = resData.getHeight();
//		int resImageWidth = resData.getWidth();
//		byte[] resData = new byte[resImageWidth * resImageHeight];
//		Arrays.fill(resData, (byte) 255);
//		// construct result image processor with the result data
//		ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth, resImageHeight, resData);
//		
//		// construct result ImagePlus object
//		ImagePlus resImage = new ImagePlus(outputFilePath,
//				imgResProcessor);

		// save result image as JPEG in the output folder
		FileSaver fs = new FileSaver(resImage);
		fs.saveAsJpeg(outputFilePath);		
	}
	public void processImages(String inputImagesFolder,
			String imagesFileNameExtension, String outputDirectory,
			String outputCSVFileName, double thresholdMinimumValue,
			double thresholdMaximumValue, double thresholdStep,
			double voxelDimX, double voxelDimY, double voxelDimZ,
			String voxelDimUnit, String method) {

		// getting images to process
		Collection<String> dirfiles = FileOper
				.readSubDirectories(inputImagesFolder);

		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirfiles,
				FileOper.SORT_ASCENDING);

		try {
			// output logs to log file
			PrintStream out;
			String name = outputDirectory + File.separatorChar
					+ new File(inputImagesFolder).getName() + "_processing.log";
			if (!(new File(name).exists())) {
				File.createTempFile(name, "");
			}
			out = new PrintStream(new FileOutputStream(name));
			
			System.out.println("Logs are available at " + name);
			
			System.setOut(out);
			System.setErr(out);

			// Starting logs
			logger.info("Starting processing images in the Image3DProcessingPipeline, arguments are:");
			logger.info("inputImagesFolder: " + inputImagesFolder);
			logger.info("imagesFileNameExtension: " + imagesFileNameExtension);
			logger.info("outputDirectory: " + outputDirectory);
			logger.info("outputCSVFileName: " + outputCSVFileName);
			logger.info("thresholdMinimumValue: " + thresholdMinimumValue);
			logger.info("thresholdMaximumValue: " + thresholdMaximumValue);
			logger.info("thresholdStep: " + thresholdStep);
			logger.info("voxelDimX: " + voxelDimX);
			logger.info("voxelDimY: " + voxelDimY);
			logger.info("voxelDimZ: " + voxelDimZ);
			logger.info("voxelDimUnit: " + voxelDimUnit);
			logger.info(sortedImagesInFolder.size() + " images to process");

			// start time for benchmark
			long startTime = System.currentTimeMillis();

			// open output file to write features values
			FileWriter writer = new FileWriter(outputDirectory
					+ File.separatorChar + outputCSVFileName);
			writer.append(CSV_HEADER);
			writer.append(CSV_NEWLINE);

			String inputFilename = new String();
			for (Iterator<String> k = sortedImagesInFolder.iterator(); k
					.hasNext();) {
				inputFilename = k.next();
				try {
					// Open ImagePlus object from image sequence and set calibration
					ImagePlus img3D = Fits3DWriter.loadZstack(inputFilename, imagesFileNameExtension); //new ImagePlus(inputFilename);
					Calibration imgCalibration = img3D.getCalibration();
					imgCalibration.pixelWidth = voxelDimX;
					imgCalibration.pixelHeight = voxelDimY;
					imgCalibration.pixelDepth = voxelDimZ;
					imgCalibration.setXUnit(voxelDimUnit);
					imgCalibration.setYUnit(voxelDimUnit);
					imgCalibration.setZUnit(voxelDimUnit);
					
					String shortImageName = new File(inputFilename).getName();
					logger.info("Starting processing stack " + shortImageName
							+ " at time: " + new Date().toString());
					
					// generating orthogonal projections of raw stack before any processing
					logger.info("Generating orthogonal projections of raw stack...");
					ImagePlus rawXYProjection = OrthogonalProjection
							.projectionXY16bits(img3D,
									OrthogonalProjection.projectionType_Max);
					ImagePlus rawZYProjection = OrthogonalProjection
							.projectionZY16bits(img3D,
									OrthogonalProjection.projectionType_Max);
					ImagePlus rawXZProjection = OrthogonalProjection
							.projectionXZ16bits(img3D,
									OrthogonalProjection.projectionType_Max);
					
					//ImagePlus img3Draw = img3D.duplicate();
					
					// smooth image
					logger.info("Smoothing image...");
					Image3DSmoothing.grayscaleFlatErosion(img3D, 1, 1, 0);
					Image3DSmoothing.grayscaleFlatDilation(img3D, 1, 1, 0);
					
	
					int optThresh = 0;
					// segment image with threshold
					logger.info("Segmenting image ...");
					ImagePlus segmentedImage = segmentImage(img3D, optThresh);
					
					// try to call GC to free unused memory
					img3D = null;
					System.gc();

					// save segmented image in a FITS file
					Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName + ".fits", segmentedImage);
					
					// save side-by-side orthogonal projections of raw and segmented stack
	
	
//					writer.append(shortImageName + CSV_SEPARATOR
//							+ String.valueOf(optThresh) + CSV_SEPARATOR
//							+ String.valueOf(frgVoxelCount)+ CSV_SEPARATOR
//							+ String.valueOf(framesRange[0]) + CSV_SEPARATOR
//							+ String.valueOf(framesRange[1]));

	
					
					writer.append(CSV_NEWLINE);
					writer.flush();
					

				} catch (Exception e) {
					logger.error(e.getMessage());
					// logger.error(e.printStackTrace())
				}

			}
			// save CSV file
			writer.flush();
			writer.close();

			// end time for benchmark
			long endTime = System.currentTimeMillis();
			logger.info("Single3DFiberPipeline execution time : "
					+ (endTime - startTime) + " millisecond.");
			System.out.println();

		} catch (IOException e) {
			logger.error(e.getMessage());
			// e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
