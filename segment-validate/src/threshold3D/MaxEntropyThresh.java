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
package threshold3D;

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

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import io.Fits3DWriter;
import io.Nifti_Writer;
import pipeline3D.Image3DProcessingPipeline;
import segment3D.Image3DSmoothing;
import segment3D.Segment3DImage;
import util.FileOper;
import validation.OrthogonalProjection;
import validation.ProjectionsConcatenationForValidation;

/**
 * @author peter bajcsy
 * 
 * based on the ImageJ/Fiji implementation
 * 
 * * Automatic thresholding technique based on the entopy of the histogram.
* See: P.K. Sahoo, S. Soltani, K.C. Wong and, Y.C. Chen "A Survey of
* Thresholding Techniques", Computer Vision, Graphics, and Image
* Processing, Vol. 41, pp.233-260, 1988.
*
*
 *
 */
public class MaxEntropyThresh extends Threshold3DImage {

	private static Log _logger = LogFactory
			.getLog(MaxEntropyThresh.class);

	public int [] findThreshPerSlice(ImagePlus img3D, double min, double max, double delta) {
		   
		
/*		int[] hist = imageProcessor.getHistogram();
		int threshold = entropySplit(hist);
		imageProcessor.threshold(threshold);
*/		
		
		//this.segmentedImagePlus = img3d.createImagePlus();
		
		// sanity check
		if (img3D == null) {
			_logger.error("Input image is null, no threshold to be found.");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		int bitDepth = img3D.getBitDepth();
		int numberOfVoxels = numrows * numcols * numzs;
		
		int [] optThresh = new int[numzs];
		
		ImageStack imgStack = img3D.getStack();
		int numberGreyValues = (int) Math.pow(2, bitDepth);
		
		int[] histogram = new int[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			for(int idx=0;idx<numberGreyValues; idx++){
				histogram[idx]=0;
			}
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					histogram[temp] ++;
				}
			}
			optThresh[z] = entropySplit(histogram);

		}
		
		return optThresh;
	}
	@Override
	public double findThresh(ImagePlus img3D, double min, double max, double delta) {
		   
		
/*		int[] hist = imageProcessor.getHistogram();
		int threshold = entropySplit(hist);
		imageProcessor.threshold(threshold);
*/		
		
		//this.segmentedImagePlus = img3d.createImagePlus();
		
		// sanity check
		if (img3D == null) {
			_logger.error("Input image is null, no threshold to be found.");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		int bitDepth = img3D.getBitDepth();
		//int numberOfVoxels = numrows * numcols * numzs;
		
		double optThresh = 0.0;
		
		ImageStack imgStack = img3D.getStack();
		int numberGreyValues = (int) Math.pow(2, bitDepth);
		
		int[] histogram = new int[numberGreyValues];
		//double[] probabilities = new double[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					// ignore the background values after a cell mask has been applied
					// consider only values from the foreground of the cell mask
					if(temp != 0)
						histogram[temp] ++;
				}
			}
		}
		
		optThresh = entropySplit(histogram);

		return optThresh;
	}
	
	/**
	  * Calculate maximum entropy split of a histogram.
	  *
	  * @param hist histogram to be thresholded.
	  *
	  * @return index of the maximum entropy split.`
	  */
	 private int entropySplit(int[] hist) {

	   // Normalize histogram, that is makes the sum of all bins equal to 1.
	   double sum = 0;
	   for (int i = 0; i < hist.length; ++i) {
	     sum += hist[i];
	   }
	   if (sum == 0) {
	     // This should not normally happen, but...
	     throw new IllegalArgumentException("Empty histogram: sum of all bins is zero.");
	   }

	   double[] normalizedHist = new double[hist.length];
	   for (int i = 0; i < hist.length; i++) {
	     normalizedHist[i] = hist[i] / sum;
	   }

	   //
	   double[] pT = new double[hist.length];
	   pT[0] = normalizedHist[0];
	   for (int i = 1; i < hist.length; i++) {
	     pT[i] = pT[i - 1] + normalizedHist[i];
	   }

	   // Entropy for black and white parts of the histogram
	   final double epsilon = Double.MIN_VALUE;
	   double[] hB = new double[hist.length];
	   double[] hW = new double[hist.length];
	   for (int t = 0; t < hist.length; t++) {
	     // Black entropy
	     if (pT[t] > epsilon) {
	       double hhB = 0;
	       for (int i = 0; i <= t; i++) {
	         if (normalizedHist[i] > epsilon) {
	           hhB -= normalizedHist[i] / pT[t] * Math.log(normalizedHist[i] / pT[t]);
	         }
	       }
	       hB[t] = hhB;
	     } else {
	       hB[t] = 0;
	     }

	     // White  entropy
	     double pTW = 1 - pT[t];
	     if (pTW > epsilon) {
	       double hhW = 0;
	       for (int i = t + 1; i < hist.length; ++i) {
	         if (normalizedHist[i] > epsilon) {
	           hhW -= normalizedHist[i] / pTW * Math.log(normalizedHist[i] / pTW);
	         }
	       }
	       hW[t] = hhW;
	     } else {
	       hW[t] = 0;
	     }
	   }

	   // Find histogram index with maximum entropy
	   double jMax = hB[0] + hW[0];
	   int tMax = 0;
	   for (int t = 1; t < hist.length; ++t) {
	     double j = hB[t] + hW[t];
	     if (j > jMax) {
	       jMax = j;
	       tMax = t;
	     }
	   }

	   return tMax;
	 }


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	/*	String inputImagesFolder = "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test/"; 
		String outputDirectory = "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test//Results/";
		String outputCSVFileName = "D:/Cell-scafffold-contact/FluoroFiberImages/ContactProb-test/log.csv";
		*/
		
		String inputImagesFolder = "C:/PeterB/Projects/CarlSimon/ContactProb-test"; 
		String inputCellImagesFolder = "C:/PeterB/Projects/CarlSimon/ContactProb-test/Cell_BW_FITS";
		
		String outputDirectory = "C:/PeterB/Projects/CarlSimon/ContactProb-test/Results";
		String outputCSVFileName = "log.csv";
		
		String imagesFileNameExtension = "fits";
		double thresholdMinimumValue = 1.0;
		double thresholdMaximumValue = Math.pow(2,  15);
		double thresholdStep = 1.0;
		double voxelDimX = 0.12;
		double voxelDimY = 0.12;
		double voxelDimZ = 0.34;
		String voxelDimUnit = "micrometers";
		
		String method = "MaxEntropy";
		//String method = "Otsu";
		//String method = "Triangle";
		//String method = "MinError";
		
		
		final String CSV_HEADER = "ImageName,Threshold,ForegroundVovelCount,StartingFrame,EndingFrame";
		final String CSV_SEPARATOR = ",";
		final char CSV_NEWLINE = '\n';
		
		// getting images to process
		//Collection<String> dirfiles = FileOper.readSubDirectories(inputImagesFolder);
		Collection<String> dirfiles = FileOper.readFileDirectory(inputImagesFolder);
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirfiles,
				FileOper.SORT_ASCENDING);

		try {
			// output logs to log file
			PrintStream out;
			String name;
			name = outputDirectory + new File(inputImagesFolder).getName() + "_processing.log";				
			if(!outputDirectory.endsWith("\\") ){
					name = outputDirectory + File.separatorChar
					+ new File(inputImagesFolder).getName() + "_processing.log";
			}else{
				name = outputDirectory + new File(inputImagesFolder).getName() + "_processing.log";				
			}
/*			if (!(new File(name).exists())) {
				File.createTempFile(name, "");
			}*/
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
			_logger.info("thresholdMinimumValue: " + thresholdMinimumValue);
			_logger.info("thresholdMaximumValue: " + thresholdMaximumValue);
			_logger.info("thresholdStep: " + thresholdStep);
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
				System.out.println("open file: "+ inputFilename);
				
				try {
					// Open ImagePlus object from image sequence and set calibration
					//ImagePlus img3D = Fits3DWriter.loadZstack(inputFilename, imagesFileNameExtension); //new ImagePlus(inputFilename);
					ImagePlus img3D = new ImagePlus(inputFilename); // input one fits file per zstack
					if(img3D.getImageStack()==null){
						System.err.println("failed to load the file: "+ inputFilename);
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
												
					// generating orthogonal projections of raw stack before any processing
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
						
					// per frame thresholding
					//int [] optThresh = new int[img3D.getNSlices()];
					double optThresh = 0.0;
					// find opt Threshold (min error)
					if(method.equals("MaxEntropy")) {
						MaxEntropyThresh maxEntropyThresholding = new MaxEntropyThresh();
						_logger.info("Looking for optimal threshold...");
						optThresh = maxEntropyThresholding.findThresh(img3D,	thresholdMinimumValue, thresholdMaximumValue,thresholdStep);
						// per frame thresholding
						///optThresh = maxEntropyThresholding.findThreshPerSlice(img3D, thresholdMinimumValue, thresholdMaximumValue,thresholdStep);
						//for(int idx=0;idx<optThresh.length;idx++){
						//	System.out.println("Optimal threshold ["+idx+"] is: " + optThresh[idx]);
						//}
						System.out.println("Optimal threshold is: " + optThresh);
					}else{
						// find opt Threshold (min error)
						if(method.equals("MinError")) {
							MinErrorThresh minErrorThresholding = new MinErrorThresh();
							_logger.info("Looking for optimal threshold...");
							optThresh = minErrorThresholding.findThresh(img3D,
									thresholdMinimumValue, thresholdMaximumValue,
									thresholdStep);
							_logger.info("Optimal threshold is: " + optThresh);
						}						
						// find opt Threshold (Otsu)
						else if(method.equals("Otsu")) {
							OtsuThresh otsuThresholding = new OtsuThresh();
							_logger.info("Looking for optimal threshold...");
							optThresh = otsuThresholding.findThresh(img3D,
									thresholdMinimumValue, thresholdMaximumValue,
									thresholdStep);
							_logger.info("Optimal threshold from Otsu is: " + optThresh);
						}
						
						// find opt Threshold (Triangle)
						else if(method.equals("Triangle")) {
							TriangleThresh triangleThresholding = new TriangleThresh();
							_logger.info("Looking for optimal threshold...");
							optThresh = triangleThresholding.findThresh(img3D);
							
							_logger.info("Optimal threshold from Triangle is: " + optThresh);
						}

						
					}
							
					
					// create segment3DImage object with the image
					Segment3DImage segment3DImage = new Segment3DImage(img3D);
					// try to call GC to free unused memory
					img3D = null;
					System.gc();
					// segment image with threshold
					_logger.info("Segmenting image (T-E-L)...");
					//ImagePlus segmentedImage = segment3DImage.segmentImage(	(int) optThresh,	Segment3DImage.NO_MORPHOLOGICAL_OPERATIONS,	0);
					segment3DImage.thresholdImage((int)optThresh);
					// per frame thresholding
					//ImagePlus segmentedImage = segment3DImage	.segmentImagePerFrame( optThresh,	Segment3DImage.NO_MORPHOLOGICAL_OPERATIONS,	0);
					
					// load the cell segmentation and apply it as a mask
					String inputCellImageFilename = new String(inputCellImagesFolder+File.separatorChar +shortImageName);
					ImagePlus cellimg3D = new ImagePlus(inputCellImageFilename); // input one fits file per zstack
					_logger.info("Dilate cell  image...");
					Image3DSmoothing.grayscaleFlatDilation(cellimg3D, 1, 1, 0);
				
					// apply the cell mask
					if(!segment3DImage.applyBinaryMask(cellimg3D, 0)){
						_logger.info("Failed applyBinaryMask");
						return;
					}
					ImagePlus segmentedImage = segment3DImage	.generateSegmentedImagePlus();					
					
					// save segmented image in a FITS file
					Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName , segmentedImage);
					// save segmented image in a NII file
					String outNII = new String(shortImageName.substring(0, shortImageName.length() - 5));
					outNII = outputDirectory + File.separatorChar + outNII + ".nii";
					nifti.write(segmentedImage, outNII, outputType);
					
					// save side-by-side orthogonal projections of raw and segmented stack
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
									outputDirectory + File.separatorChar + shortImageName
											+ "_projections.jpeg");
					
					_logger.info("Side-by-side projections saved in file " + outputDirectory + File.separatorChar + shortImageName
							+ "_projections.jpeg");
					
					// get number of foreground voxels after segmentation
					long frgVoxelCount = segment3DImage.getFRGCount();
					_logger.info("Foreground voxel count after segmentation: "
							+ frgVoxelCount);

				
					writer.append(CSV_NEWLINE);
					writer.flush();
					

				} catch (Exception e) {
					_logger.error(e.getMessage());
				}

			}
			// save CSV file
			writer.flush();
			writer.close();

			// end time for benchmark
			long endTime = System.currentTimeMillis();
			_logger.info("execution time : "
					+ (endTime - startTime) + " millisecond.");
			System.out.println();

		} catch (IOException e) {
			_logger.error(e.getMessage());
			// e.printStackTrace();
		}
	
		}
	
}
