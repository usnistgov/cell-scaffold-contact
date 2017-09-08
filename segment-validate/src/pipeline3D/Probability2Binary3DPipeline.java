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
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import io.Fits3DWriter;
import io.Nifti_Writer;

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

import segment3D.Image3DSmoothing;
import segment3D.Segment3DImage;
import threshold3D.MaxEntropyThresh;
import threshold3D.MinErrorThresh;
import threshold3D.OtsuThresh;
import threshold3D.TriangleThresh;
import util.FileOper;
import validation.OrthogonalProjection;
import validation.ProjectionsConcatenationForValidation;

/**
 * This class is designed to read  files from fiber scaffold segmentation obtained from statistical methods 
 * (16 bits per pixel FITS files with prob. 0 ~0 and prob. 1 ~ 2^16-1 )
 * generate three max projections or raw z-stack for cell verification
 * apply the verified cell segmentation mask after dilating it by 3x3 kernel
 * threshold scaffold segmentation according to maxError, Max Entropy or Triagle adaptive method
 * generate three max projections of binarized z-stack for cell verification
 * save jpg images of projections
 * save binarized z-stacks in NIFTI and FITS file formats.
 * 
 *  @author pnb
 *
 */
 
public class Probability2Binary3DPipeline {

	private static Log _logger = LogFactory
			.getLog(Probability2Binary3DPipeline.class);

	private static final String CSV_HEADER = "ImageName,Threshold (intensity), Threshold (prob), ForegroundVovelCount";
	private static final String CSV_SEPARATOR = ",";
	private static final char CSV_NEWLINE = '\n';

	public static void processProbability3DImage( String inputImagesFolder,
			String imagesFileNameExtension, String inputCellImagesFolder, String outputDirectory,
			String outputCSVFileName, int probIntensityMinimumValue,
			int probIntensityMaximumValue, double voxelDimX, double voxelDimY, double voxelDimZ,
			String voxelDimUnit, String method) {


		// getting images to process
		// Collection<String> dirfiles =
		// FileOper.readSubDirectories(inputImagesFolder);
		Collection<String> dirfiles = FileOper
				.readFileDirectory(inputImagesFolder);

		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirfiles,
				FileOper.SORT_ASCENDING);

		try {
			// output logs to log file
			PrintStream out;
			String name;
			name = outputDirectory + new File(inputImagesFolder).getName()
					+ "_processing.log";
			if (!outputDirectory.endsWith("\\")) {
				name = outputDirectory + File.separatorChar
						+ new File(inputImagesFolder).getName()
						+ "_processing.log";
			} else {
				name = outputDirectory + new File(inputImagesFolder).getName()
						+ "_processing.log";
			}
			/*
			 * if (!(new File(name).exists())) { File.createTempFile(name, "");
			 * }
			 */
			out = new PrintStream(new FileOutputStream(name));

			System.out.println("Logs are available at " + name);

			System.setOut(out);
			System.setErr(out);

			// Starting logs
			_logger.info("Starting processing images in the Image3DProcessingPipeline, arguments are:");
			_logger.info("inputImagesFolder: " + inputImagesFolder);

			_logger.info("inputCellImagesFolder: " + inputCellImagesFolder);

			_logger.info("imagesFileNameExtension: " + imagesFileNameExtension);
			_logger.info("outputDirectory: " + outputDirectory);
			_logger.info("outputCSVFileName: " + outputCSVFileName);
			_logger.info("probIntensityMinimumValue: " + probIntensityMinimumValue);
			_logger.info("probIntensityMaximumValue: " + probIntensityMaximumValue);
			_logger.info("voxelDimX: " + voxelDimX);
			_logger.info("voxelDimY: " + voxelDimY);
			_logger.info("voxelDimZ: " + voxelDimZ);
			_logger.info("voxelDimUnit: " + voxelDimUnit);
			_logger.info(sortedImagesInFolder.size() + " images to process");

			//
			// start time for benchmark
			long startTime = System.currentTimeMillis();

			// open output file to write features values
			FileWriter writer = new FileWriter(outputDirectory
					+ File.separatorChar + outputCSVFileName);
			writer.append(CSV_HEADER);
			writer.append(CSV_NEWLINE);

			// needed to save out nii file format
			Nifti_Writer nifti = new Nifti_Writer();
			String outputType = new String("::NIFTI_FILE:");

			String inputFilename = new String();
			for (Iterator<String> k = sortedImagesInFolder.iterator(); k
					.hasNext();) {
				inputFilename = k.next();
				System.out.println("open file: " + inputFilename);

				try {
					// Open ImagePlus object from image sequence and set
					// calibration
					// ImagePlus img3D = Fits3DWriter.loadZstack(inputFilename,
					// imagesFileNameExtension); //new ImagePlus(inputFilename);
					
					// input one fits file per/ zstack
					ImagePlus img3D = new ImagePlus(inputFilename); 
					if (img3D.getImageStack() == null) {
						System.err.println("failed to load the file: "
								+ inputFilename);
					}
					Calibration imgCalibration = img3D.getCalibration();
					imgCalibration.pixelWidth = voxelDimX;
					imgCalibration.pixelHeight = voxelDimY;
					imgCalibration.pixelDepth = voxelDimZ;
					imgCalibration.setXUnit(voxelDimUnit);
					imgCalibration.setYUnit(voxelDimUnit);
					imgCalibration.setZUnit(voxelDimUnit);

					String shortImageName = new File(inputFilename).getName();
					_logger.info("Starting processing stack " + shortImageName
							+ " at time: " + new Date().toString());

					
					// generating orthogonal projections of raw stack before any
					// processing
					_logger.info("Generating orthogonal projections of raw stack...");
					ImagePlus rawXYProjection = OrthogonalProjection
							.projectionXY16bits(img3D,
									OrthogonalProjection.projectionType_Max);
					ImagePlus rawZYProjection = OrthogonalProjection
							.projectionZY16bits(img3D,
									OrthogonalProjection.projectionType_Max);
					ImagePlus rawXZProjection = OrthogonalProjection
							.projectionXZ16bits(img3D,
									OrthogonalProjection.projectionType_Max);

					// create segment3DImage object with the image
					Segment3DImage segment3DImage = new Segment3DImage(img3D);
					// try to call GC to free unused memory
					img3D = null;
					System.gc();
					
					// load the cell segmentation and apply it as a mask
					String inputCellImageFilename = new String(
							inputCellImagesFolder + File.separatorChar
									+ shortImageName);
					// input one fits file per zstack
					ImagePlus cellimg3D = new ImagePlus(inputCellImageFilename); 
					if(cellimg3D == null || cellimg3D.getImageStack()==null){
						_logger.info("failed loading cell  image...");
						System.out.println("failed loading cell  image...");
						continue;
					}
					
					_logger.info("Dilate cell  image...");
					Image3DSmoothing.grayscaleFlatDilation(cellimg3D, 1, 1, 0);
					
					// apply the cell mask
					if (!segment3DImage.applyBinaryMask(cellimg3D, 0)) {
						_logger.info("Failed applyBinaryMask");
						return;
					}
					// get the raw image after applying the mask (16 bits per pixel)
					// the name segmentedImage might not be appropriate but the object will be reused
					ImagePlus segmentedImage = segment3DImage
							.generateImagePlus();

					// per frame thresholding
					// int [] optThresh = new int[img3D.getNSlices()];
					double optThresh = 0.0;
					// find opt Threshold (min error)
					if (method.equals("MaxEntropy")) {
						MaxEntropyThresh maxEntropyThresholding = new MaxEntropyThresh();
						_logger.info("MaxEntropy: Looking for optimal threshold...");
						// in this method, the threshold is found over masked images
						// it is assumed that the background pixels are labeled with zero!!!
						optThresh = maxEntropyThresholding.findThresh(segmentedImage,
								probIntensityMinimumValue, probIntensityMaximumValue,
								1.0);
/*						System.out
						.println("Optimal threshold before offset is: " + optThresh);
*/						//optThresh += (int)(0.03*((double)probIntensityMaximumValue-probIntensityMinimumValue));
						
						// per frame thresholding
						// /optThresh =
						// maxEntropyThresholding.findThreshPerSlice(img3D,
						// thresholdMinimumValue,
						// thresholdMaximumValue,thresholdStep);
						// for(int idx=0;idx<optThresh.length;idx++){
						// System.out.println("Optimal threshold ["+idx+"] is: "
						// + optThresh[idx]);
						// }
						System.out
								.println("Optimal threshold is: " + optThresh);
					} else {
						// find opt Threshold (min error)
						if (method.equals("MinError")) {
							MinErrorThresh minErrorThresholding = new MinErrorThresh();
							_logger.info("MinError: Looking for optimal threshold...");
							optThresh = minErrorThresholding.findThresh(segmentedImage,
									probIntensityMinimumValue, probIntensityMaximumValue,
									1.0);
							_logger.info("Optimal threshold is: " + optThresh);
						}
						// find opt Threshold (Otsu)
						else if (method.equals("Otsu")) {
							OtsuThresh otsuThresholding = new OtsuThresh();
							_logger.info("Otsu: Looking for optimal threshold...");
							optThresh = otsuThresholding.findThresh(segmentedImage,
									probIntensityMinimumValue, probIntensityMaximumValue,
									1.0);
							_logger.info("Optimal threshold from Otsu is: "
									+ optThresh);
						}

						// find opt Threshold (Triangle)
						else if (method.equals("Triangle")) {
							TriangleThresh triangleThresholding = new TriangleThresh();
							_logger.info("Triangle: Looking for optimal threshold...");
							optThresh = triangleThresholding.findThresh(segmentedImage);

							_logger.info("Optimal threshold from Triangle is: "
									+ optThresh);
						}

					}


					// segment image with threshold
					_logger.info("Segmenting probability 3D image ...");
					segment3DImage.thresholdImage((int) optThresh);
					
					segmentedImage = segment3DImage.generateSegmentedImagePlus();

					// per frame thresholding
					// ImagePlus segmentedImage = segment3DImage
					// .segmentImagePerFrame( optThresh,
					// Segment3DImage.NO_MORPHOLOGICAL_OPERATIONS, 0);

			
					// save side-by-side orthogonal projections of raw and
					// segmented stack
					_logger.info("Generating orthogonal projections of segmented stack...");

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
									outputDirectory + File.separatorChar
											+ shortImageName
											+ "_projections.jpeg");

					_logger.info("Side-by-side projections saved in file "
							+ outputDirectory + File.separatorChar
							+ shortImageName + "_projections.jpeg");

					// get number of foreground voxels after segmentation
					long frgVoxelCount = segment3DImage.getFRGCount();
					_logger.info("Foreground voxel count after segmentation: "
							+ frgVoxelCount);
					writer.write(shortImageName+CSV_SEPARATOR+Integer.toString((int) optThresh)+CSV_SEPARATOR+Double.toString(optThresh/(double)probIntensityMaximumValue)+CSV_SEPARATOR+Long.toString(frgVoxelCount));
					writer.append(CSV_NEWLINE);
					writer.flush();
					
					// save segmented image in a FITS file
					String outFITS = new String(shortImageName.substring(0,
							shortImageName.length() - 5));
					outFITS = outputDirectory + File.separatorChar + outFITS
							+ "_OutA2.fits";					
					_logger.info("Writing FITS file: "+(outFITS) );
					Fits3DWriter.write(outFITS, segmentedImage);
					
					// save segmented image in a NII file
					String outNII = new String(shortImageName.substring(0,
							shortImageName.length() - 5));
					outNII = outputDirectory + File.separatorChar + outNII
							+ "_OutA2.nii";
					_logger.info("Writing NII file: "+(outNII) );
					// inside flip vertically image to match other channels in movie creator
					// this means that segmentedImage is vertically flipped after nifti.write()
					// and should not be used unless filpped again
/*					ImageStack stack = segmentedImage.getStack();
					for (int i=1; i<=stack.getSize(); i++) {
						ImageProcessor ip = stack.getProcessor(i);
						ip.flipVertical();
					}
					segmentedImage.setStack(stack);*/
					nifti.write(segmentedImage, outNII, outputType);


				} catch (Exception e) {
					_logger.error(e.getMessage());
				}

			}
			// save CSV file
			writer.flush();
			writer.close();

			// end time for benchmark
			long endTime = System.currentTimeMillis();
			_logger.info("execution time : " + (endTime - startTime)
					+ " millisecond.");
			System.out.println();

		} catch (IOException e) {
			_logger.error(e.getMessage());
			// e.printStackTrace();
		}

	}
	/**
	 * This method is executed to process all files on itlnas and convert the contact probability
	 * to binary contact using the verified and cropped cell masks 
	 * 
	 */
	public static void batchProcess(){
		String inputImagesFolder;
		String imagesFileNameExtension;
		String inputCellImagesFolder;
		String outputDirectory;
		String outputCSVFileName;
		int probIntensityMinimumValue;
		int probIntensityMaximumValue;
		double voxelDimX, voxelDimY,  voxelDimZ;
		String voxelDimUnit; 
		String method;
		
		outputCSVFileName = "log.csv";
		imagesFileNameExtension = "fits";
		probIntensityMinimumValue = 0;
		probIntensityMaximumValue = (int) Math.pow(2, 16);
		voxelDimX = 0.12;
		voxelDimY = 0.12;
		voxelDimZ = 0.419;
		voxelDimUnit = "micrometers";

		 method = "MaxEntropy";
	
		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07102015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07102015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07232015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07232015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-06252015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-06252015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-07072015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-07072015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-06252015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-06252015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

		inputImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-07072015_ZProfile/FITS";
		inputCellImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/FITS_CellBW";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-07072015_ZProfile/BinaryContact";
		Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
				inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
				probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);

	}

	public static void main(String[] args) {
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 11)) {
			System.out
					.println("Please, specify inputImagesFolder, imagesFileNameExtension, inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue, probIntensityMaximumValue,"
							+ "voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method   ");

			return;
		}

		String inputImagesFolder = new String(args[0]);
		String imagesFileNameExtension = new String(args[1]);
		String inputCellImagesFolder = new String(args[2]);
		String outputDirectory = new String(args[3]);
		String outputCSVFileName = new String(args[4]);
		int probIntensityMinimumValue = Integer.parseInt(new String(args[5]));
		int probIntensityMaximumValue = Integer.parseInt(new String(args[6]));
		double voxelDimX = Double.parseDouble(new String(args[7]));
		double voxelDimY = Double.parseDouble(new String(args[8]));
		double voxelDimZ = Double.parseDouble(new String(args[9]));
		String voxelDimUnit = new String(args[10]);
		String method = new String(args[11]);
	
		outputCSVFileName = "log.csv";

		imagesFileNameExtension = "fits";
		probIntensityMinimumValue = 0;
		probIntensityMaximumValue = (int) Math.pow(2, 16);
		voxelDimX = 0.12;
		voxelDimY = 0.12;
		voxelDimZ = 0.462;
		voxelDimUnit = "micrometers";

		 method = "MaxEntropy";
		//String method = "Otsu";
		//String method = "Triangle";
		//String method = "MinError";

		inputImagesFolder = "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test";
		inputCellImagesFolder =
		 "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test/Cell_Mask";
		 outputDirectory =
		 "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test/Results";

		 long startTime = System.currentTimeMillis();
		 Probability2Binary3DPipeline.processProbability3DImage(inputImagesFolder, imagesFileNameExtension,
					inputCellImagesFolder, outputDirectory, outputCSVFileName, probIntensityMinimumValue,
					probIntensityMaximumValue, voxelDimX, voxelDimY, voxelDimZ, voxelDimUnit, method);
		 long endTime = System.currentTimeMillis();
		 System.out.println("execution time : " + (endTime - startTime)
					+ " millisecond.");
		 System.out.println();	
		 
		//Probability2Binary3DPipeline.batchProcess();
		 
	}

}
