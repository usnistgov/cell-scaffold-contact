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

import ij.ImagePlus;
import ij.measure.Calibration;
import io.Fits3DWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import segment3D.Image3DCropping;
import segment3D.Image3DSmoothing;
import segment3D.Segment3DImage;
import threshold3D.EGTThresh;
import threshold3D.MinErrorThresh;
import threshold3D.OtsuThresh;
import threshold3D.TopoStableThresh;
import threshold3D.TriangleThresh;
import util.ExtractVoxelIntensities;
import util.FileOper;
import validation.OrthogonalProjection;
import validation.ProjectionsConcatenationForValidation;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 * 
 */
public class Image3DProcessingPipeline {

	private static Log logger = LogFactory
			.getLog(Image3DProcessingPipeline.class);
	private static final String CSV_HEADER = "ImageName,Threshold,ForegroundVovelCount,StartingFrame,EndingFrame";
	private static final String CSV_SEPARATOR = ",";
	private static final char CSV_NEWLINE = '\n';

	public Image3DProcessingPipeline() {
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

			// FIXME Shape feature extraction removed from pipeline for now
//			// get list of available features and associated methods
//			final List<String> featureNames = Processed3DImage
//					.availableFeatures();
//			Map<String, Method> methodsAssociatedWithFeatureNames = Processed3DImage
//					.featureNamesToMethodsMap();
//			for (String feature : featureNames) {
//				writer.append("," + feature);
//			}
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
					
					double optThresh = 0.0;
					
					// find opt Threshold (min error)
					if(method.equals("MinError")) {
						MinErrorThresh minErrorThresholding = new MinErrorThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = minErrorThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						logger.info("Optimal threshold is: " + optThresh);
					}
					
					// find opt Threshold (Otsu)
					else if(method.equals("Otsu")) {
						OtsuThresh otsuThresholding = new OtsuThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = otsuThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						logger.info("Optimal threshold from Otsu is: " + optThresh);
					}
					
					// find opt Threshold (EGT 2DSobel)
					else if(method.equals("EGTSobel2D")) {
						EGTThresh egtThresholding = new EGTThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = egtThresholding.getEGTThrehold(img3D,
								-13);
						
						logger.info("Optimal threshold from EGT is: " + optThresh);
					}
					
					// find opt Threshold (EGT 3DSobel)
					else if(method.equals("EGTSobel3D")) {
						EGTThresh egtThresholding = new EGTThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = egtThresholding.findThresh(img3D,
								-13, true);
						
						logger.info("Optimal threshold from EGT is: " + optThresh);
					}
					
					// find opt Threshold (Triangle)
					else if(method.equals("Triangle")) {
						TriangleThresh triangleThresholding = new TriangleThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = triangleThresholding.findThresh(img3D);
						
						logger.info("Optimal threshold from Triangle is: " + optThresh);
					}
					
					// find opt Threshold (DarkFrames)
					else if(method.equals("DarkFrames")) {
						OtsuThresh otsuThresholding = new OtsuThresh();
						logger.info("Looking for optimal threshold...");
						optThresh = otsuThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						logger.info("Optimal threshold from Otsu is: " + optThresh);
					}
					else {
						logger.error("Thresholding method not found.");
						return;
					}
						
					
					// remove useless frames at beginning and end of stack
					logger.info("Removing useless frames at beginning and end of stack...");
					Image3DCropping image3DCropping = new Image3DCropping();
					int[] framesRange = image3DCropping.removeMeaninglessFrames16bits(img3D, (int)optThresh);
					logger.info("Starting frame is " + framesRange[0] + " ending frame is " + framesRange[1]);
					double meanBlackFrames = image3DCropping.getMeanBKGFrames();
					double stdevBlackFrames = image3DCropping.getStdevBKGFrames();
					logger.info("Mean intensity of black frames is " + meanBlackFrames + ", stdev intensity of black frames is " + stdevBlackFrames);

					// find opt Threshold (DarkFrames)
					if(method.equals("DarkFrames")) {
						double thresholdFromBlackFrames = meanBlackFrames + 4.0 * stdevBlackFrames;
						if(thresholdFromBlackFrames > 0)
							optThresh = thresholdFromBlackFrames;
						logger.info("Optimal threshold from black frames is: " + optThresh);
					}
					
					// create segment3DImage object with the image
					Segment3DImage segment3DImage = new Segment3DImage(img3D);
					// try to call GC to free unused memory
					img3D = null;
					System.gc();

					// segment image with threshold
					logger.info("Segmenting image (T-E-L)...");
					ImagePlus segmentedImage = segment3DImage
							.segmentImage(
									(int) optThresh,
									Segment3DImage.NO_MORPHOLOGICAL_OPERATIONS,
									0);
					
					//ExtractVoxelIntensities.extractHistogramsFRGandBKG(img3Draw, segmentedImage, framesRange[0], framesRange[1], outputDirectory, shortImageName);

					// save segmented image in a FITS file
					Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName + ".fits", segmentedImage);
					
					// save side-by-side orthogonal projections of raw and segmented stack
					logger.info("Generating orthogonal projections of segmented stack...");

					ImagePlus segXYProjection = OrthogonalProjection
							.projectionXY(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					ImagePlus segZYProjection = OrthogonalProjection
							.projectionZY(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					ImagePlus segXZProjection = OrthogonalProjection
							.projectionXZ(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					
					ProjectionsConcatenationForValidation
							.concatenateRaw16bitsProjectionsAndSegmented8bitsProjection(
									rawXYProjection, rawZYProjection,
									rawXZProjection, segXYProjection,
									segZYProjection, segXZProjection,
									outputDirectory + File.separatorChar + shortImageName
											+ "_projections.jpeg");
					logger.info("Side-by-side projections saved in file " + outputDirectory + File.separatorChar + shortImageName
							+ "_projections.jpeg");
					
					// get number of foreground voxels after segmentation
					long frgVoxelCount = segment3DImage.getFRGCount();
					logger.info("Foreground voxel count after segmentation: "
							+ frgVoxelCount);

					writer.append(shortImageName + CSV_SEPARATOR
							+ String.valueOf(optThresh) + CSV_SEPARATOR
							+ String.valueOf(frgVoxelCount)+ CSV_SEPARATOR
							+ String.valueOf(framesRange[0]) + CSV_SEPARATOR
							+ String.valueOf(framesRange[1]));

					// FIXME Shape feature extraction removed from pipeline for now
					
//					// if foreground voxels were found, process to feature
//					// extraction
//					if (frgVoxelCount > 0) {
//
//						logger.info("Starting feature extraction...");
//
//						// Initialize 3D image processor for feature extraction
//						Processed3DImage processed3DImage = new Processed3DImage(
//								img3D, (int) optThresh);
//
//						// compute features
//						for (String feature : featureNames) {
//							logger.info("Computing feature: " + feature);
//							if (methodsAssociatedWithFeatureNames
//									.containsKey(feature)) {
//								Method selectedMethod = methodsAssociatedWithFeatureNames
//										.get(feature);
//								if (selectedMethod != null) {
//									writer.append(","
//											+ String.valueOf(selectedMethod
//													.invoke(processed3DImage)));
//								} else {
//									logger.warn("No method found for computing feature: "
//											+ feature);
//									writer.append(",N/A");
//								}
//							} else {
//								logger.warn("No method found for computing feature: "
//										+ feature);
//								writer.append(",N/A");
//							}
//						}
//
//					}
//
//					else {
//						logger.warn("No foreground voxels available after segmentation: no feature extraction will be performed");
//						for (int i = 0; i < featureNames.size(); ++i) {
//							writer.append(",N/A");
//						}
//					}
					
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
			logger.info("Image3DProcessingPipeline execution time : "
					+ (endTime - startTime) + " millisecond.");
			System.out.println();

		} catch (IOException e) {
			logger.error(e.getMessage());
			// e.printStackTrace();
		}
	}

	/**
	 * This method was implemented to take care of 15 cell zstacks that did not pass visual inspection of cell segmentation
	 * and therefore had to be manually cropped and thresholded. Afterwards, this post-processing was executed 
	 * @param inputImagesFolder
	 * @param imagesFileNameExtension
	 * @param outputDirectory
	 * @param outputCSVFileName
	 * @param thresholdMinimumValue
	 * @param thresholdMaximumValue
	 * @param thresholdStep
	 * @param voxelDimX
	 * @param voxelDimY
	 * @param voxelDimZ
	 * @param voxelDimUnit
	 * @param method
	 */
	public void processImagesAfterManualThresh(String inputImagesFolder,
			String imagesFileNameExtension, String outputDirectory,
			String outputCSVFileName, double thresholdMinimumValue,
			double thresholdMaximumValue, double thresholdStep,
			double voxelDimX, double voxelDimY, double voxelDimZ,
			String voxelDimUnit, String method) {

		// getting images to process
		Collection<String> dirfiles = FileOper
				.readSubDirectories(inputImagesFolder);
		//Collection<String> dirfiles = FileOper.readFileDirectory(inputImagesFolder);
		
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
					//ImagePlus img3D = new ImagePlus(inputFilename); // input one fits file per zstack
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
									
					// smooth image
					logger.info("Smoothing image...");
					Image3DSmoothing.grayscaleFlatErosion(img3D, 1, 1, 0);
					Image3DSmoothing.grayscaleFlatDilation(img3D, 1, 1, 0);
					
					double optThresh = 0.0;
														
					// create segment3DImage object with the image
					Segment3DImage segment3DImage = new Segment3DImage(img3D);
					// try to call GC to free unused memory
					img3D = null;
					System.gc();

					// the input is already binaried image with 0 and 255 binary values and therefore  
					optThresh = 128;
					
					// segment image with threshold
					logger.info("Segmenting image (T-E-L)...");
					ImagePlus segmentedImage = segment3DImage
							.segmentImage(
									(int) optThresh,
									Segment3DImage.NO_MORPHOLOGICAL_OPERATIONS,
									0);
					
					// save segmented image in a FITS file
					Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName + ".fits", segmentedImage);
					
					// save side-by-side orthogonal projections of raw and segmented stack
					logger.info("Generating orthogonal projections of segmented stack...");

					ImagePlus segXYProjection = OrthogonalProjection
							.projectionXY(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					ImagePlus segZYProjection = OrthogonalProjection
							.projectionZY(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					ImagePlus segXZProjection = OrthogonalProjection
							.projectionXZ(segmentedImage,
									OrthogonalProjection.projectionType_Max);
					
					ProjectionsConcatenationForValidation
							.concatenateRaw16bitsProjectionsAndSegmented8bitsProjection(
									rawXYProjection, rawZYProjection,
									rawXZProjection, segXYProjection,
									segZYProjection, segXZProjection,
									outputDirectory + File.separatorChar + shortImageName
											+ "_projections.jpeg");
					logger.info("Side-by-side projections saved in file " + outputDirectory + File.separatorChar + shortImageName
							+ "_projections.jpeg");
					
					// get number of foreground voxels after segmentation
					long frgVoxelCount = segment3DImage.getFRGCount();
					logger.info("Foreground voxel count after segmentation: "
							+ frgVoxelCount);

				
					writer.append(CSV_NEWLINE);
					writer.flush();
					

				} catch (Exception e) {
					logger.error(e.getMessage());
				}

			}
			// save CSV file
			writer.flush();
			writer.close();

			// end time for benchmark
			long endTime = System.currentTimeMillis();
			logger.info("Image3DProcessingPipeline execution time : "
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

		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 4)) {
			System.out
					.println("Please, specify the input directory with images, image file name extension, outputdir and input filter");

			return;
		}

		String inputImagesFolder = new String(args[0]);
		String imagesFileNameExtension = new String(args[1]);
		String outputDirectory = new String(args[2]);
		String outputCSVFileName = new String(args[3]);
		int thresholdMinimumValue = Integer.parseInt(new String(args[4]));
		int thresholdMaximumValue = Integer.parseInt(new String(args[5]));
		int thresholdStep = Integer.parseInt(new String(args[6]));
		double voxelDimX = Double.parseDouble(new String(args[7]));
		double voxelDimY = Double.parseDouble(new String(args[8]));
		double voxelDimZ = Double.parseDouble(new String(args[9]));
		String voxelDimUnit = new String(args[10]);
		String method = new String(args[11]);

		Image3DProcessingPipeline pipeline = new Image3DProcessingPipeline();
		pipeline.processImages(inputImagesFolder, imagesFileNameExtension,
				outputDirectory, outputCSVFileName, thresholdMinimumValue,
				thresholdMaximumValue, thresholdStep, voxelDimX, voxelDimY,
				voxelDimZ, voxelDimUnit, method);
		
		
		// example inputs for After Manual Thresh (needed for 15 images not being segmented properly)
		// manualSeg tif manualCellProcessOut outCSVFile.csv 1 65535 1 0.12 0.12 0.46 um MinError
		/* pipeline.processImagesAfterManualThresh(inputImagesFolder, imagesFileNameExtension,
				outputDirectory, outputCSVFileName, thresholdMinimumValue,
				thresholdMaximumValue, thresholdStep, voxelDimX, voxelDimY,
				voxelDimZ, voxelDimUnit, method);
		*/
	}

}
