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
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.FitsLoader;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import util.FileOper;


/**
 * This class is for concatenating same plane orthogonal projections of
 * different versions of the same image
 * 
 * @author Mylene Simon
 * 
 */
public class ProjectionsConcatenationForValidation {

	/**
	 * Concatenate the three projections (raw, segmentedV1 and segmentedV2) in a
	 * JPEG image
	 * 
	 * @param rawImagesFolder
	 *            The folder were the raw image projections are stored
	 * @param segmentedVersion1Folder
	 *            The folder were the segmented (version 1) image projections
	 *            are stored
	 * @param segmentedVersion2Folder
	 *            The folder were the segmented (version 2) image projections
	 *            are stored
	 * @param rawProjectionExtension
	 *            The file name extension for the raw image projections
	 * @param segmentedVersion1ProjectionExtension
	 *            The file name extension for the segmented (version 1) image
	 *            projections
	 * @param segmentedVersion2ProjectionExtension
	 *            The file name extension for the segmented (version 2) image
	 *            projections
	 * @param outputFolder
	 *            The output folder
	 */
	public static void concatenateRawAndTwoSegmentedVersions(
			String rawImagesFolder, String segmentedVersion1Folder,
			String segmentedVersion2Folder, String rawProjectionExtension,
			String segmentedVersion1ProjectionExtension,
			String segmentedVersion2ProjectionExtension,
			String outputFolder) {
		
		// get projections in the Raw folder
		Collection<String> filesInRawFolder = FileOper
				.readFileDirectory(rawImagesFolder);
		Collection<String> rawProjections = FileOper.selectFileType(
				filesInRawFolder, rawProjectionExtension);
		
		// browse raw projections
		for(Iterator<String> k = rawProjections.iterator(); k.hasNext();) {
			
			// get raw projection file name
			String rawProjectionFileName = k.next();
			
			// construct segmented V1 and V2 projection file names
			String baseName = rawProjectionFileName.substring(
					rawProjectionFileName.lastIndexOf(File.separatorChar),
					rawProjectionFileName.indexOf(rawProjectionExtension));
			
			String segmentedV1ProjectionFileName = segmentedVersion1Folder
					+ File.separatorChar + baseName
					+ segmentedVersion1ProjectionExtension;
			
			String segmentedV2ProjectionFileName = segmentedVersion2Folder
					+ File.separatorChar + baseName
					+ segmentedVersion2ProjectionExtension;
			
			// check if there are two segmentations available for the raw projection
			if(new File(segmentedV1ProjectionFileName).exists() 
					&& new File(segmentedV2ProjectionFileName).exists()) {
				
				// load the three projections in ImagePlus objects
				ImagePlus rawImage = FitsLoader.read(rawProjectionFileName);
				ImagePlus segmentedVersion1Image = FitsLoader
						.read(segmentedV1ProjectionFileName);
				ImagePlus segmentedVersion2Image = FitsLoader
						.read(segmentedV2ProjectionFileName);
				
				// compute projection sizes and result image size
				int width = rawImage.getWidth();
				int height = rawImage.getHeight();
				int borderSize = 10;
				int resImageWidth = width * 3 + borderSize * 4;
				int resImageHeight = height + 2 * borderSize;
				
				// create new data array for the result image
				byte[] resData = new byte[resImageWidth * resImageHeight];
				
				// draw borders on result image
				// top and bottom borders
				for(int i = 0; i < resImageWidth; ++ i) {
					for(int j = 0; j < borderSize; ++ j) {
						resData[j * resImageWidth + i] = (byte) 255;
						resData[(j + borderSize + height) * resImageWidth + i] 
								= (byte) 255;
					}
					
				}
				// vertical borders
				for(int j = borderSize; j < (height + borderSize); ++ j) {
					for(int i = 0; i < borderSize; ++ i) {
						resData[j * resImageWidth + i] = (byte) 255;
						resData[j * resImageWidth + (i + borderSize + width)] = 
								(byte) 255;
						resData[j * resImageWidth
								+ (i + 2 * borderSize + 2 * width)] = (byte) 255;
						resData[j * resImageWidth
								+ (i + 3 * borderSize + 3 * width)] = (byte) 255;
					}
					
				}
				
				// get projections data
				ByteProcessor rawImageProcessor = 
						(ByteProcessor) rawImage
						.getProcessor();
				ByteProcessor segentedV1ImageProcessor = 
						(ByteProcessor) segmentedVersion1Image
						.getProcessor();
				ByteProcessor segentedV2ImageProcessor = 
						(ByteProcessor) segmentedVersion2Image
						.getProcessor();
				
				// draw the three projections on result image
				// left: raw projection, 
				// center: segmented V1 projection, 
				// right: segmented V2 projection
				for (int x = 0; x < width; ++x) {
					for (int y = 0; y < height; ++y) {
						resData[(y + borderSize) * resImageWidth + (x + borderSize)] = (byte) rawImageProcessor
								.getPixel(x, y);
						resData[(y + borderSize) * resImageWidth
								+ (x + borderSize * 2 + width)] = (byte) segentedV1ImageProcessor
								.getPixel(x, y);
						resData[(y + borderSize) * resImageWidth
								+ (x + borderSize * 3 + width * 2)] = (byte) segentedV2ImageProcessor
								.getPixel(x, y);

					}
				}
				
				// construct result image processor with the result data
				ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,
						resImageHeight, resData);
				
				// construct result ImagePlus object
				ImagePlus resImage = new ImagePlus(outputFolder
						+ File.separatorChar + baseName + "_segComparison.jpeg",
						imgResProcessor);

				// save result image as JPEG in the output folder
				FileSaver fs = new FileSaver(resImage);
				fs.saveAsJpeg(outputFolder + File.separatorChar + baseName
						+ "_segComparison.jpeg");
				
			}
			
		}

	}
	
	/**
	 * Concatenate the three projections (XY, XZ and ZY) of three 3D volume
	 * versions (raw, segmentedV1 and segmentedV2) in a JPEG image
	 * 
	 * @param rawImagesFolder
	 *            The folder were the raw image projections are stored
	 * @param segmentedVersion1Folder
	 *            The folder were the segmented (version 1) image projections
	 *            are stored
	 * @param segmentedVersion2Folder
	 *            The folder were the segmented (version 2) image projections
	 *            are stored
	 * @param segmentedV1Suffix
	 *            The suffix indicating that an image is a segmented image in
	 *            the filename (i.e. "_seg")
	 * @param segmentedV2Suffix
	 *            The suffix indicating that an image is a segmented image in
	 *            the filename (i.e. "_seg")
	 * @param maxXYProjectionExtension
	 *            The extension of the filename for a max XY projection image
	 *            (i.e. "_maxXY.fits")
	 * @param maxXZProjectionExtension
	 *            The extension of the filename for a max XZ projection image
	 *            (i.e. "_maxXZ.fits")
	 * @param maxZYProjectionExtension
	 *            The extension of the filename for a max ZY projection image
	 *            (i.e. "_maxZY.fits")
	 * @param outputFolder
	 *            The output folder
	 */
	public static void concatenateRawAndTwoSegmentedVersionsThreeProjections(
			String rawImagesFolder, String segmentedVersion1Folder,
			String segmentedVersion2Folder, String segmentedV1Suffix,
			String segmentedV2Suffix, String maxXYProjectionExtension,
			String maxXZProjectionExtension, String maxZYProjectionExtension,
			String outputFolder) {
		
		// get XY projections in the Raw folder
		Collection<String> filesInRawFolder = FileOper
				.readFileDirectory(rawImagesFolder);
		Collection<String> rawXYProjections = FileOper.selectFileType(
				filesInRawFolder, maxXYProjectionExtension);
		
		// browse raw projections
		for(Iterator<String> k = rawXYProjections.iterator(); k.hasNext();) {
			
			// get raw projection file name
			String rawXYProjectionFileName = k.next();
			
			// construct all the raw and segmented projections file names
			String baseName = rawXYProjectionFileName.substring(
					rawXYProjectionFileName.lastIndexOf(File.separatorChar) + 1,
					rawXYProjectionFileName.indexOf(maxXYProjectionExtension));
			
			// raw projections
			String rawXZProjectionFileName = rawImagesFolder
					+ File.separatorChar + baseName
					+ maxXZProjectionExtension;
			
			String rawZYProjectionFileName = rawImagesFolder
					+ File.separatorChar + baseName
					+ maxZYProjectionExtension;
			
			// V1 segmented projections
			String segmentedV1XYProjectionFileName = segmentedVersion1Folder
					+ File.separatorChar + baseName
					+ segmentedV1Suffix
					+ maxXYProjectionExtension;
			
			String segmentedV1XZProjectionFileName = segmentedVersion1Folder
					+ File.separatorChar + baseName
					+ segmentedV1Suffix
					+ maxXZProjectionExtension;
			
			String segmentedV1ZYProjectionFileName = segmentedVersion1Folder
					+ File.separatorChar + baseName
					+ segmentedV1Suffix
					+ maxZYProjectionExtension;
			
			// V2 segmented projections
			String segmentedV2XYProjectionFileName = segmentedVersion2Folder
					+ File.separatorChar + baseName
					+ segmentedV2Suffix
					+ maxXYProjectionExtension;
			
			String segmentedV2XZProjectionFileName = segmentedVersion2Folder
					+ File.separatorChar + baseName
					+ segmentedV2Suffix
					+ maxXZProjectionExtension;
			
			String segmentedV2ZYProjectionFileName = segmentedVersion2Folder
					+ File.separatorChar + baseName
					+ segmentedV2Suffix
					+ maxZYProjectionExtension;
			
			// check if all the projections are present for the raw and 
			// segmented images
			if(new File(rawXYProjectionFileName).exists() 
			&& new File(rawXZProjectionFileName).exists() 
			&& new File(rawZYProjectionFileName).exists()
			&& new File(segmentedV1XYProjectionFileName).exists()
			&& new File(segmentedV1XZProjectionFileName).exists()
			&& new File(segmentedV1ZYProjectionFileName).exists()
			&& new File(segmentedV2XYProjectionFileName).exists()
			&& new File(segmentedV2XZProjectionFileName).exists()
			&& new File(segmentedV2ZYProjectionFileName).exists()) {
				
				// load the projections in ImagePlus objects
				ImagePlus rawXYImage = FitsLoader.read(rawXYProjectionFileName);
				ImagePlus rawXZImage = FitsLoader.read(rawXZProjectionFileName);
				ImagePlus rawZYImage = FitsLoader.read(rawZYProjectionFileName);
				
				ImagePlus segmentedVersion1XYImage = FitsLoader
						.read(segmentedV1XYProjectionFileName);
				ImagePlus segmentedVersion1XZImage = FitsLoader
						.read(segmentedV1XZProjectionFileName);
				ImagePlus segmentedVersion1ZYImage = FitsLoader
						.read(segmentedV1ZYProjectionFileName);
				
				
				ImagePlus segmentedVersion2XYImage = FitsLoader
						.read(segmentedV2XYProjectionFileName);
				ImagePlus segmentedVersion2XZImage = FitsLoader
						.read(segmentedV2XZProjectionFileName);
				ImagePlus segmentedVersion2ZYImage = FitsLoader
						.read(segmentedV2ZYProjectionFileName);
				
				// compute projection sizes and result image size
				int widthXY = rawXYImage.getWidth();
				int heightXY = rawXYImage.getHeight();
				int widthXZ = rawXZImage.getWidth();
				int heightXZ = rawXZImage.getHeight();
				int widthZY = rawZYImage.getWidth();
				int heightZY = rawZYImage.getHeight();
				int smallBorderSize = 10;
				int largeBorderSize = smallBorderSize * 3;
				int resImageWidth = widthXY * 3 + widthZY * 3 
						+ largeBorderSize * 4 + smallBorderSize * 3;
				int resImageHeight = heightXY + heightXZ 
						+ largeBorderSize * 2 + smallBorderSize;
				
				// create new data array for the result image
				byte[] resData = new byte[resImageWidth * resImageHeight];
				Arrays.fill(resData, (byte) 255);
				
				// get projections data
				ByteProcessor rawXYImageProcessor = 
						(ByteProcessor) rawXYImage
						.getProcessor();
				ByteProcessor rawXZImageProcessor = 
						(ByteProcessor) rawXZImage
						.getProcessor();
				ByteProcessor rawZYImageProcessor = 
						(ByteProcessor) rawZYImage
						.getProcessor();
				ByteProcessor segmentedV1XYImageProcessor = 
						(ByteProcessor) segmentedVersion1XYImage
						.getProcessor();
				ByteProcessor segmentedV1XZImageProcessor = 
						(ByteProcessor) segmentedVersion1XZImage
						.getProcessor();
				ByteProcessor segmentedV1ZYImageProcessor = 
						(ByteProcessor) segmentedVersion1ZYImage
						.getProcessor();
				ByteProcessor segmentedV2XYImageProcessor = 
						(ByteProcessor) segmentedVersion2XYImage
						.getProcessor();
				ByteProcessor segmentedV2XZImageProcessor = 
						(ByteProcessor) segmentedVersion2XZImage
						.getProcessor();
				ByteProcessor segmentedV2ZYImageProcessor = 
						(ByteProcessor) segmentedVersion2ZYImage
						.getProcessor();
				
				// draw the projections on result image
				// left: raw projections, 
				// center: segmented V1 projections, 
				// right: segmented V2 projections
				
				// XY projections
				for (int x = 0; x < widthXY; ++x) {
					for (int y = 0; y < heightXY; ++y) {
						
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth 
						        + (x + largeBorderSize + smallBorderSize + widthZY)] 
						        = (byte) rawXYImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth
								+ (x + largeBorderSize * 2 + smallBorderSize * 2 
										+ widthZY * 2 + widthXY)] 
								= (byte) segmentedV1XYImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth
								+ (x + largeBorderSize * 3 + smallBorderSize * 3 
										+ widthZY * 3 + widthXY * 2)] 
								= (byte) segmentedV2XYImageProcessor.getPixel(x, y);

					}
				}
				
				// XZ projections
				for (int x = 0; x < widthXZ; ++x) {
					for (int y = 0; y < heightXZ; ++y) {
						
						resData[(y + largeBorderSize) * resImageWidth 
						        + (x + largeBorderSize + smallBorderSize + widthZY)] 
						        = (byte) rawXZImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize) * resImageWidth
								+ (x + largeBorderSize * 2 + smallBorderSize * 2 
										+ widthZY * 2 + widthXY)] 
								= (byte) segmentedV1XZImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize) * resImageWidth
								+ (x + largeBorderSize * 3 + smallBorderSize * 3 
										+ widthZY * 3 + widthXY * 2)] 
								= (byte) segmentedV2XZImageProcessor.getPixel(x, y);

					}
				}
				
				// ZY projections
				for (int x = 0; x < widthZY; ++x) {
					for (int y = 0; y < heightZY; ++y) {
						
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth 
						        + (x + largeBorderSize)] 
						        = (byte) rawZYImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth
								+ (x + largeBorderSize * 2 + smallBorderSize 
										+ widthZY + widthXY)] 
								= (byte) segmentedV1ZYImageProcessor.getPixel(x, y);
						resData[(y + largeBorderSize + smallBorderSize + heightXZ) 
						        * resImageWidth
								+ (x + largeBorderSize * 3 + smallBorderSize * 2 
										+ widthZY * 2 + widthXY * 2)] 
								= (byte) segmentedV2ZYImageProcessor.getPixel(x, y);

					}
				}
				
				// construct result image processor with the result data
				ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,
						resImageHeight, resData);
				
				// construct result ImagePlus object
				ImagePlus resImage = new ImagePlus(outputFolder
						+ File.separatorChar + baseName + "_segComparison.jpeg",
						imgResProcessor);

				// save result image as JPEG in the output folder
				FileSaver fs = new FileSaver(resImage);
				fs.saveAsJpeg(outputFolder + File.separatorChar + baseName
						+ "_segComparison.jpeg");
				
			}
			
			else {
				System.out.println("All the files were not found for this image:");
				System.out.println(baseName);
			}
			
		}

	}
	
	/**
	 * Concatenate the two projections in a JPEG image
	 * 
	 * @param projectionsVersion1Folder
	 *            The folder were the version 1 image projections are stored
	 * @param projectionsVersion2Folder
	 *            The folder were the version 2 image projections are stored
	 * @param version1ProjectionExtension
	 *            The file name extension for the version 1 image projections
	 * @param version2ProjectionExtension
	 *            The file name extension for the version 2 image projections
	 * @param outputFolder
	 *            The output folder
	 */
	public static void concatenateTwoProjections(
			String projectionsVersion1Folder,
			String projectionsVersion2Folder,
			String version1ProjectionExtension,
			String version2ProjectionExtension,
			String outputFolder) {
		
		// get projections in the Raw folder
		Collection<String> filesInVersion1Folder = FileOper
				.readFileDirectory(projectionsVersion1Folder);
		Collection<String> version1Projections = FileOper.selectFileType(
				filesInVersion1Folder, version1ProjectionExtension);
		
		// browse raw projections
		for(Iterator<String> k = version1Projections.iterator(); k.hasNext();) {
			
			// get the projection file name
			String version1ProjectionFileName = k.next();
			
			// construct segmented V1 and V2 projection file names
			String baseName = version1ProjectionFileName.substring(
					version1ProjectionFileName.lastIndexOf(File.separatorChar),
					version1ProjectionFileName.indexOf(version1ProjectionExtension));
			
			String version2ProjectionFileName = projectionsVersion2Folder
					+ File.separatorChar + baseName
					+ version2ProjectionExtension;
			
			// check if the second projection exists
			if(new File(version2ProjectionFileName).exists() ) {
				
				// load the two projections in ImagePlus objects
				ImagePlus version1Image = FitsLoader
						.read(version1ProjectionFileName);
				ImagePlus version2Image = FitsLoader
						.read(version2ProjectionFileName);
				
				// compute projection sizes and result image size
				int width = version1Image.getWidth();
				int height = version1Image.getHeight();
				int borderSize = 10;
				int resImageWidth = width * 2 + borderSize * 3;
				int resImageHeight = height + 2 * borderSize;
				
				// create new data array for the result image
				byte[] resData = new byte[resImageWidth * resImageHeight];
				
				// draw borders on result image
				// top and bottom borders
				for(int i = 0; i < resImageWidth; ++ i) {
					for(int j = 0; j < borderSize; ++ j) {
						resData[j * resImageWidth + i] = (byte) 255;
						resData[(j + borderSize + height) * resImageWidth + i] 
								= (byte) 255;
					}
					
				}
				// vertical borders
				for(int j = borderSize; j < (height + borderSize); ++ j) {
					for(int i = 0; i < borderSize; ++ i) {
						resData[j * resImageWidth + i] = (byte) 255;
						resData[j * resImageWidth + (i + borderSize + width)] = 
								(byte) 255;
						resData[j * resImageWidth
								+ (i + 2 * borderSize + 2 * width)] = (byte) 255;
					}
					
				}
				
				// get projections data
				ByteProcessor version1ImageProcessor = 
						(ByteProcessor) version1Image
						.getProcessor();
				ByteProcessor version2ImageProcessor = 
						(ByteProcessor) version2Image
						.getProcessor();
				
				// draw the two projections on result image
				// left: version 1 projection, 
				// right: version 2 projection
				for (int x = 0; x < width; ++x) {
					for (int y = 0; y < height; ++y) {
						resData[(y + borderSize) * resImageWidth + (x + borderSize)] = (byte) version1ImageProcessor
								.getPixel(x, y);
						resData[(y + borderSize) * resImageWidth
								+ (x + borderSize * 2 + width)] = (byte) version2ImageProcessor
								.getPixel(x, y);

					}
				}
				
				// construct result image processor with the result data
				ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,
						resImageHeight, resData);
				
				// construct result ImagePlus object
				ImagePlus resImage = new ImagePlus(outputFolder
						+ File.separatorChar + baseName + "_comparison.jpeg",
						imgResProcessor);

				// save result image as JPEG in the output folder
				FileSaver fs = new FileSaver(resImage);
				fs.saveAsJpeg(outputFolder + File.separatorChar + baseName
						+ "_comparison.jpeg");
				
			}
			
			
			
		}

	}
	
	/**
	 * Concatenate the two projections in a JPEG image
	 */
	public static void concatenateRaw16bitsProjectionsAndSegmented8bitsProjection(ImagePlus proj1XY,
			ImagePlus proj1ZY, ImagePlus proj1XZ, ImagePlus proj2XY,
			ImagePlus proj2ZY, ImagePlus proj2XZ, String outputFilePath) {
		
		// compute projection sizes and result image size
		int widthXY = proj1XY.getWidth();
		int heightXY = proj1XY.getHeight();
		int widthXZ = proj1XZ.getWidth();
		int heightXZraw = proj1XZ.getHeight();
		int widthZYraw = proj1ZY.getWidth();
		int heightZY = proj1ZY.getHeight();
		int heightXZseg = proj2XZ.getHeight();
		int widthZYseg = proj2ZY.getWidth();
		int smallBorderSize = 10;
		int largeBorderSize = smallBorderSize * 3;
		int resImageWidth = widthXY * 2 + widthZYraw + widthZYseg
				+ largeBorderSize * 3 + smallBorderSize * 2;
		int resImageHeight = heightXY + heightXZraw 
				+ largeBorderSize * 2 + smallBorderSize;
		
		// create new data array for the result image
		byte[] resData = new byte[resImageWidth * resImageHeight];
		Arrays.fill(resData, (byte) 255);
		
		// get projections data
		ImageProcessor rawXYImageProcessor = 
				proj1XY
				.getProcessor().convertToByteProcessor();
		ImageProcessor rawXZImageProcessor = 
				 proj1XZ
				.getProcessor().convertToByteProcessor();
		ImageProcessor rawZYImageProcessor = 
				 proj1ZY
				.getProcessor().convertToByteProcessor();
		ImageProcessor segmentedV1XYImageProcessor = 
				 proj2XY
				.getProcessor();
		ImageProcessor segmentedV1XZImageProcessor = 
				 proj2XZ
				.getProcessor();
		ImageProcessor segmentedV1ZYImageProcessor = 
				 proj2ZY
				.getProcessor();
		
		// draw the projections on result image
		// left: raw projections, 
		// center: segmented V1 projections, 
		// right: segmented V2 projections
		
		// XY projections
		for (int x = 0; x < widthXY; ++x) {
			for (int y = 0; y < heightXY; ++y) {
				
				resData[(y + largeBorderSize + smallBorderSize + heightXZraw) 
				        * resImageWidth 
				        + (x + largeBorderSize + smallBorderSize + widthZYraw)] 
				        = (byte) rawXYImageProcessor.getPixel(x, y);
				resData[(y + largeBorderSize + smallBorderSize + heightXZraw) 
				        * resImageWidth
						+ (x + largeBorderSize * 2 + smallBorderSize * 2 
								+ widthZYraw * 2 + widthXY)] 
						= (byte) segmentedV1XYImageProcessor.getPixel(x, y);

			}
		}
		
		// XZ projections
		for (int x = 0; x < widthXZ; ++x) {
			for (int y = 0; y < heightXZraw; ++y) {
				
				resData[(y + largeBorderSize) * resImageWidth 
				        + (x + largeBorderSize + smallBorderSize + widthZYraw)] 
				        = (byte) rawXZImageProcessor.getPixel(x, y);

			}
			
			for (int y = 0; y < heightXZseg; ++y) {
				
				resData[(y + largeBorderSize) * resImageWidth
						+ (x + largeBorderSize * 2 + smallBorderSize * 2 
								+ widthZYraw * 2 + widthXY)] 
						= (byte) segmentedV1XZImageProcessor.getPixel(x, y);

			}
		}
		
		// ZY projections
		for (int y = 0; y < heightZY; ++y) {

			for (int x = 0; x < widthZYraw; ++x) {
				
				resData[(y + largeBorderSize + smallBorderSize + heightXZraw) 
				        * resImageWidth 
				        + (x + largeBorderSize)] 
				        = (byte) rawZYImageProcessor.getPixel(x, y);

			}
			for (int x = 0; x < widthZYseg; ++x) {
				
				resData[(y + largeBorderSize + smallBorderSize + heightXZraw) 
				        * resImageWidth
						+ (x + largeBorderSize * 2 + smallBorderSize 
								+ widthZYraw + widthXY)] 
						= (byte) segmentedV1ZYImageProcessor.getPixel(x, y);

			}
		}
		
		// construct result image processor with the result data
		ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,
				resImageHeight, resData);
		
		// construct result ImagePlus object
		ImagePlus resImage = new ImagePlus(outputFilePath,
				imgResProcessor);

		// save result image as JPEG in the output folder
		FileSaver fs = new FileSaver(resImage);
		fs.saveAsJpeg(outputFilePath);

	}
	
	public static void main(String[] args) {
		
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length != 9)) {
			System.out
					.println("Please, specify the raw images folder, the "
							+ "segmented version 1 images folder, the segmented "
							+ "version 2 folder, the suffix for segmented files "
							+ "V1, the suffix for segmented files V2,"
							+ " the file name extension for max XY projection "
							+ "images, the file name extension for max XZ "
							+ "projection images, the file name extension for "
							+ "max ZY projection images, and the output folder.");

			return;
		}

		// get arguments
		String rawImagesFolder = new String(args[0]);
		String segmentedVersion1Folder = new String(args[1]);
		String segmentedVersion2Folder = new String(args[2]); 
		String segmentedV1Suffix = new String(args[3]);
		String segmentedV2Suffix = new String(args[4]);
		String maxXYProjectionExtension = new String(args[5]);
		String maxXZProjectionExtension = new String(args[6]);
		String maxZYProjectionExtension = new String(args[7]);
		String outputFolder = new String(args[8]);
		
		ProjectionsConcatenationForValidation
				.concatenateRawAndTwoSegmentedVersionsThreeProjections(
						rawImagesFolder, segmentedVersion1Folder,
						segmentedVersion2Folder, segmentedV1Suffix,
						segmentedV2Suffix, maxXYProjectionExtension,
						maxXZProjectionExtension, maxZYProjectionExtension,
						outputFolder);

	}

}
