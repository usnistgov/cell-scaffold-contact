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
package shapeFeatures2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import validation.OrthogonalProjection;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import io.FitsLoader;

/**
 * This is a class to compute 2D shape metrics on segmented z-stacks (FITS files)
 * 
 * @author Mylene Simon
 * @deprecated Deprecated because the feature extraction methods have been moved to a new project.
 * 				Use ShapeFeatureExtraction3D project instead
 *
 */
@Deprecated 
public class ShapeFeatures2D {
	
	public final static String FEATURES2D_NAMES_HEADER = 
			"Z-Perimeter,Y-Perimeter,X-Perimeter,"
			+ "L1-Perimeter,L2-Perimeter,L3-Perimeter,"
			+ "Z-Area,Y-Area,X-Area,"
			+ "L1-Area,L2-Area,L3-Area,"
			+ "Z-AspectRatio,Y-AspectRatio,X-AspectRatio,"
			+ "L1-AspectRatio,L2-AspectRatio,L3-AspectRatio";

	/**
	 * Compute 2D area of 2D image, computing number of foreground pixels and
	 * using pixel dimensions (units must be uniform for all pixel dimensions)
	 * 
	 * @param img2D
	 *            The 2D ImagePlus
	 * @param threshold
	 *            Threshold value (pixel = foreground if >= threshold)
	 * @return The 2D area value (unit is the same as the one in the 2D
	 *         ImagePlus)
	 */
	public static double area2D(ImagePlus img2D, int threshold){
		
		// initializations
		double area2D = 0.0;
		
		// compute number of foreground pixels
		for(int x = 0; x < img2D.getWidth(); ++ x) {
			for(int y = 0; y < img2D.getHeight(); ++ y) {
				if(img2D.getProcessor().getPixel(x, y) >= threshold)
					area2D ++;
			}
		}
		
		// compute area and calibrate with physical pixel size
		area2D = area2D * img2D.getCalibration().pixelWidth * img2D.getCalibration().pixelHeight;
		
		if(! img2D.getCalibration().getXUnit().equals(img2D.getCalibration().getYUnit())) {
			System.err.println("Units are not uniform for the two axis.");
			System.err.println(img2D.getTitle());
		}
		
		return area2D;
	}
	
	/**
	 * Compute 2D aspect ratio of 2D image, computing the bounding box of
	 * foreground and using pixel dimensions (units must be uniform for all
	 * pixel dimensions)
	 * 
	 * @param img2D
	 *            The 2D ImagePlus
	 * @param threshold
	 *            Threshold value (pixel = foreground if >= threshold)
	 * @return The 2D aspect ratio 
	 */
	public static double aspectRatio2D(ImagePlus img2D, int threshold){
		
		// initializations
		double aspectRatio2D = 0.0;
		int minX = Integer.MAX_VALUE;
    	int maxX = Integer.MIN_VALUE;
    	int minY = Integer.MAX_VALUE;
    	int maxY = Integer.MIN_VALUE;
    	double width = 0.0;
    	double height = 0.0;
		
    	// find bounding box
		for(int x = 0; x < img2D.getWidth(); ++ x) {
			for(int y = 0; y < img2D.getHeight(); ++ y) {
				int currentPixel = img2D.getProcessor().getPixel(x, y);
				if(currentPixel >= threshold) {
					if(x > maxX)
		    			maxX = x;
		    		if(x < minX)
		    			minX = x;
		    		if(y > maxY)
		    			maxY = y;
		    		if(y < minY)
		    			minY = y;
				}	
			}
		}
		
		// compute width and height
    	width = ((double) ((maxX - minX) + 1)) * img2D.getCalibration().pixelWidth;
    	height = ((double) ((maxY - minY) + 1)) * img2D.getCalibration().pixelHeight;
    	
    	if(! img2D.getCalibration().getXUnit().equals(img2D.getCalibration().getYUnit())) {
			System.err.println("Units are not uniform for the two axis.");
			System.err.println(img2D.getTitle());
		}
    	
    	// compute aspect ratio
    	if(width > height)
    		aspectRatio2D = width / height;
    	else
    		aspectRatio2D = height / width;
		
		return aspectRatio2D;
		
	}
	
	/**
	 * Compute 2D perimeter of 2D image, using marching square algorithm and 
	 * weights (units must be uniform for all pixel dimensions)
	 * 
	 * @param img2D
	 *            The 2D ImagePlus
	 * @param threshold
	 *            Threshold value (pixel = foreground if >= threshold)
	 * @return The 2D perimeter value (unit is the same as the one in the 2D
	 *         ImagePlus)
	 */
	public static double perimeter2D(ImagePlus img2D, int threshold) {
		
		// Perimeter contribution coefficient
		final double contributionL1 =  (Math.PI / 8.0) * 
				(img2D.getCalibration().pixelHeight / 2.0 
				+ img2D.getCalibration().pixelWidth / 2.0 
				+ Math.sqrt(Math.pow(img2D.getCalibration().pixelHeight / 2.0, 2)
						+ Math.pow(img2D.getCalibration().pixelWidth / 2.0, 2)));
		
		final double contributionL2_HORIZONTAL = 0.948 * img2D.getCalibration().pixelWidth;
		final double contributionL2_VERTICAL = 0.948 * img2D.getCalibration().pixelHeight;
		final double contributionL3 = 2.0 * contributionL1;
		
		double perimeter2D = 0.0;
		
		// look for foreground pixels | Marching square algorithm
		for(int x = 0; x < img2D.getWidth() - 1; ++ x) {
			for(int y = 0; y < img2D.getHeight() - 1; ++ y) {
				
				int topLeftCellCorner = img2D.getProcessor().getPixel(x, y) 
						>= threshold ? 8 : 0;
				int topRightCellCorner = img2D.getProcessor().getPixel(x+1, y) 
						>= threshold ? 4 : 0;
				int bottomRightCellCorner = img2D.getProcessor().getPixel(x+1, y+1)
						>= threshold ? 2 : 0;
				int bottomLeftCellCorner = img2D.getProcessor().getPixel(x, y+1) 
						>= threshold ? 1 : 0;
				
				// Lookup table
				int sumCellCorners = topLeftCellCorner + topRightCellCorner
						+ bottomLeftCellCorner + bottomRightCellCorner;
				// Add contributions to perimeter
				if (sumCellCorners == 1 || sumCellCorners == 2
						|| sumCellCorners == 4 || sumCellCorners == 7
						|| sumCellCorners == 8 || sumCellCorners == 11 
						|| sumCellCorners == 13 || sumCellCorners == 14) {
					perimeter2D += contributionL1;
				} else if (sumCellCorners == 3 || sumCellCorners == 12) {
					perimeter2D += contributionL2_HORIZONTAL;
				} else if (sumCellCorners == 6 || sumCellCorners == 9) {
					perimeter2D += contributionL2_VERTICAL;
				} else if (sumCellCorners == 5 || sumCellCorners == 10) {
					perimeter2D += contributionL3;
				}
	
			}
		}
		
		if(! img2D.getCalibration().getXUnit().equals(img2D.getCalibration().getYUnit())) {
			System.err.println("Units are not uniform for the two axis.");
			System.err.println(img2D.getTitle());
		}
		
		return perimeter2D;
	}
	
	/**
	 * Compute max projection of 3D image on a 2D plan based on LHorizontal and
	 * LVertical directions
	 * 
	 * @param img3D
	 *            The 3D ImagePlus
	 * @param xLHorizontalDirection
	 *            The x coordinate of LHorizontal vector directions
	 * @param yLHorizontalDirection
	 *            The y coordinate of LHorizontal vector directions
	 * @param zLHorizontalDirection
	 *            The z coordinate of LHorizontal vector directions
	 * @param xLVerticalDirection
	 *            The x coordinate of LVertical vector directions
	 * @param yLVerticalDirection
	 *            The y coordinate of LVertical vector directions
	 * @param zLVerticalDirection
	 *            The z coordinate of LVertical vector directions
	 * @return The 2D ImagePlus corresponding to the projection on the 2D plan
	 */
	public static ImagePlus maxProjectionLDirection(ImagePlus img3D,
			double xLHorizontalDirection, double yLHorizontalDirection, 
			double zLHorizontalDirection, double xLVerticalDirection, 
			double yLVerticalDirection, double zLVerticalDirection) {
		
		// sanity check
		if (img3D == null) {
			System.err.println("Missing input image");
			return null;
		}
		
		// get image dimensions
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		
		// get image stack
		ImageStack imgStack = img3D.getImageStack();
		
		// create res image
		ImagePlus res = img3D.createImagePlus();
		
		// compute LHorizontal vector norm
		final double LHorizontalVectorNorm = Math.sqrt(xLHorizontalDirection
				* xLHorizontalDirection + yLHorizontalDirection
				* yLHorizontalDirection + zLHorizontalDirection
				* zLHorizontalDirection);
		
		// compute LVertical vector norm
		final double LVerticalVectorNorm = Math.sqrt(xLVerticalDirection
				* xLVerticalDirection + yLVerticalDirection
				* yLVerticalDirection + zLVerticalDirection
				* zLVerticalDirection);

		// compute projected x indices of 3D image corners
		int frontTopLeftCornerProjectedX = computeCoordinateInProjected2DPlane(
				0, 0, 0, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);
		int frontTopRightCornerProjectedX = computeCoordinateInProjected2DPlane(
				numcols - 1, 0, 0, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);
		int frontBottomLeftCornerProjectedX = computeCoordinateInProjected2DPlane(
				0, numrows - 1, 0, xLHorizontalDirection, yLHorizontalDirection,
				zLHorizontalDirection, LHorizontalVectorNorm);
		int frontBottomRightCornerProjectedX = computeCoordinateInProjected2DPlane(
				numcols - 1, numrows - 1, 0, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);

		int backTopLeftCornerProjectedX = computeCoordinateInProjected2DPlane(
				0, 0, numzs - 1, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);
		int backTopRightCornerProjectedX = computeCoordinateInProjected2DPlane(
				numcols - 1, 0, numzs - 1, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);
		int backBottomLeftCornerProjectedX = computeCoordinateInProjected2DPlane(
				0, numrows - 1, numzs - 1, xLHorizontalDirection, yLHorizontalDirection,
				zLHorizontalDirection, LHorizontalVectorNorm);
		int backBottomRightCornerProjectedX = computeCoordinateInProjected2DPlane(
				numcols - 1, numrows - 1, numzs - 1, xLHorizontalDirection,
				yLHorizontalDirection, zLHorizontalDirection,
				LHorizontalVectorNorm);

		// compute projected y indices of 3D image corners
		int frontTopLeftCornerProjectedY = computeCoordinateInProjected2DPlane(
				0, 0, 0, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);
		int frontTopRightCornerProjectedY = computeCoordinateInProjected2DPlane(
				numcols - 1, 0, 0, xLVerticalDirection,
				yLVerticalDirection, zLVerticalDirection, LVerticalVectorNorm);
		int frontBottomLeftCornerProjectedY = computeCoordinateInProjected2DPlane(
				0, numrows - 1, 0, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);
		int frontBottomRightCornerProjectedY = computeCoordinateInProjected2DPlane(
				numcols - 1, numrows - 1, 0, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);

		int backTopLeftCornerProjectedY = computeCoordinateInProjected2DPlane(
				0, 0, numzs - 1, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);
		int backTopRightCornerProjectedY = computeCoordinateInProjected2DPlane(
				numcols - 1, 0, numzs - 1, xLVerticalDirection,
				yLVerticalDirection, zLVerticalDirection, LVerticalVectorNorm);
		int backBottomLeftCornerProjectedY = computeCoordinateInProjected2DPlane(
				0, numrows - 1, numzs - 1, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);
		int backBottomRightCornerProjectedY = computeCoordinateInProjected2DPlane(
				numcols - 1, numrows - 1, numzs - 1, xLVerticalDirection, yLVerticalDirection,
				zLVerticalDirection, LVerticalVectorNorm);

		// find max and min projected x indices
		int maxImgResX = Math.max(
				frontTopLeftCornerProjectedX,
				Math.max(frontTopRightCornerProjectedX,
				Math.max(frontBottomLeftCornerProjectedX, 
				Math.max(frontBottomRightCornerProjectedX, 
				Math.max(backTopLeftCornerProjectedX, 
				Math.max(backTopRightCornerProjectedX, 
				Math.max(backBottomLeftCornerProjectedX, 
						backBottomRightCornerProjectedX)))))))
				+ 1;
		int minImgResX = Math.min(
				frontTopLeftCornerProjectedX,
				Math.min(frontTopRightCornerProjectedX,
				Math.min(frontBottomLeftCornerProjectedX, 
				Math.min(frontBottomRightCornerProjectedX, 
				Math.min(backTopLeftCornerProjectedX, 
				Math.min(backTopRightCornerProjectedX, 
				Math.min(backBottomLeftCornerProjectedX, 
						backBottomRightCornerProjectedX)))))))
				- 1;
		
		// find max and min projected y indices
		int maxImgResY = Math.max(
				frontTopLeftCornerProjectedY,
				Math.max(frontTopRightCornerProjectedY,
				Math.max(frontBottomLeftCornerProjectedY, 
				Math.max(frontBottomRightCornerProjectedY, 
				Math.max(backTopLeftCornerProjectedY, 
				Math.max(backTopRightCornerProjectedY, 
				Math.max(backBottomLeftCornerProjectedY, 
						backBottomRightCornerProjectedY)))))))
				+ 1;
		int minImgResY = Math.min(
				frontTopLeftCornerProjectedY,
				Math.min(frontTopRightCornerProjectedY,
				Math.min(frontBottomLeftCornerProjectedY, 
				Math.min(frontBottomRightCornerProjectedY, 
				Math.min(backTopLeftCornerProjectedY, 
				Math.min(backTopRightCornerProjectedY, 
				Math.min(backBottomLeftCornerProjectedY, 
						backBottomRightCornerProjectedY)))))))
				- 1;
		
		// compute res img width and height and initialize byte array
		int resImgWidth = Math.abs(maxImgResX - minImgResX) + 4;
		int resImgHeight = Math.abs(maxImgResY - minImgResY) + 4;
		byte[] projectedData = new byte[resImgWidth*resImgHeight];
		
		// compute needed translation vector needed if x and y bounds indices are negative
		int translationX = 0;
		if(minImgResX < 0) 
			translationX = Math.abs(minImgResX) + 1;
		int translationY = 0;
		if(minImgResY < 0) 
			translationY = Math.abs(minImgResY) + 1;
		
		// max projection
		for (int z = 0; z < numzs; z++) {
			for (int row = 0; row < numrows; row++) {
				for (int col = 0; col < numcols; col++) {
					// get voxel value
					double voxel = imgStack.getVoxel(col, row, z);
					// compute x and y coordinates for projected pixel
					int projectedX = computeCoordinateInProjected2DPlane(col,
							row, z, xLHorizontalDirection,
							yLHorizontalDirection, zLHorizontalDirection,
							LHorizontalVectorNorm) + translationX;
					int projectedY = computeCoordinateInProjected2DPlane(col,
							row, z, xLVerticalDirection,
							yLVerticalDirection, zLVerticalDirection,
							LVerticalVectorNorm) + translationY;
					// update pixel value in byte array if value is higher
					int currentProjectedVoxel = (int) (projectedData[projectedY * resImgWidth + projectedX] & 0xff);
					if( currentProjectedVoxel < voxel) {
						projectedData[projectedY * resImgWidth + projectedX] = (byte) voxel;
					}
				}
			}
		}

		// Create the ImageProcessor with the array of projected pixels and set 
		// it in the result ImagePlus
		ByteProcessor imgProc = new ByteProcessor(resImgWidth, resImgHeight, projectedData);
		res.setProcessor(imgProc);
		
		// Compute new pixel dimensions and set the calibration for the image
		Calibration newCalibration = res.getCalibration();
		newCalibration.pixelWidth = Math.sqrt(
				Math.pow(xLHorizontalDirection
						* img3D.getCalibration().pixelWidth, 2)
				+ Math.pow(yLHorizontalDirection
						* img3D.getCalibration().pixelHeight, 2)
				+ Math.pow(zLHorizontalDirection
						* img3D.getCalibration().pixelDepth, 2));
		newCalibration.pixelHeight = Math.sqrt(
				Math.pow(xLVerticalDirection
						* img3D.getCalibration().pixelWidth, 2)
				+ Math.pow(yLVerticalDirection
						* img3D.getCalibration().pixelHeight, 2)
				+ Math.pow(zLVerticalDirection
						* img3D.getCalibration().pixelDepth, 2));
		res.setCalibration(newCalibration);
		
		return res;
		
	}
	
	public static int computeCoordinateInProjected2DPlane(int x, int y, int z, 
			double xLDirection, double yLDirection, double zLDirection, 
			double LVectorNorm) {
		
		return (int) Math.round(
				((double) x * xLDirection + 
				(double) y * yLDirection + 
				(double) z * zLDirection)
				/ LVectorNorm);
	}
	
	/**
	 * Parse CSV file containing cell names and 3D features values, compute 2D
	 * features and write in a new CSV file 3D + 2D features values for each
	 * cell
	 * 
	 * @param inputCSVFilePath
	 *            Path of the input CSV file containing the cell names and 3D
	 *            features values
	 * @param outputDirectory
	 *            Path of output directory
	 * @param outputCSVFileName
	 *            CSV output file name
	 * @param inputImagesFolder
	 *            Path of folder containing the segmented 3D cell images
	 * @param imagesFileNameExtension
	 *            File name extension of the segmented 3D cell images (ex:
	 *            "_seg.fits")
	 * @param threshold
	 *            Threshold value (pixel = foreground if >= threshold)
	 */
	public static void compute2DShapeFeatures(String inputCSVFilePath, 
			String outputDirectory, String outputCSVFileName,
			String inputImagesFolder, String imagesFileNameExtension,
			int threshold) {
		
		// Read CSV files to get cell names and 3D features
		try {
		
			BufferedReader br = null;
			String line = "";
		 
			br = new BufferedReader(new InputStreamReader(
	                  new FileInputStream(new File(inputCSVFilePath)), 
	                  StandardCharsets.ISO_8859_1));
			String cvsSplitBy = ",";
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("File_Name")) {
				System.err.println("Features CSV input file should start with "
						+ "a header line starting with: ");
				System.err.println("File_Name");
				if (br != null) br.close();
				return;
			}
			// Find positions of L axis directions in CSV columns
			String[] cellFeaturesHeaders = line.split(cvsSplitBy);
			int[] LDirectionsMatrix = new int[9];
			for(int i = 0; i < cellFeaturesHeaders.length; ++i) {
				if(cellFeaturesHeaders[i].equals("L1x"))
					LDirectionsMatrix[0] = i;
				if(cellFeaturesHeaders[i].equals("L2x"))
					LDirectionsMatrix[1] = i;
				if(cellFeaturesHeaders[i].equals("L3x"))
					LDirectionsMatrix[2] = i;
				if(cellFeaturesHeaders[i].equals("L1y"))
					LDirectionsMatrix[3] = i;
				if(cellFeaturesHeaders[i].equals("L2y"))
					LDirectionsMatrix[4] = i;
				if(cellFeaturesHeaders[i].equals("L3y"))
					LDirectionsMatrix[5] = i;
				if(cellFeaturesHeaders[i].equals("L1z"))
					LDirectionsMatrix[6] = i;
				if(cellFeaturesHeaders[i].equals("L2z"))
					LDirectionsMatrix[7] = i;
				if(cellFeaturesHeaders[i].equals("L3z"))
					LDirectionsMatrix[8] = i;
			}
			
			// If output folder doesn't exist, the folder (and parent folders
			// if necessary) is created
			File outputTest = new File(outputDirectory);
			if (!outputTest.exists()) {
				outputTest.mkdirs();
			}
			
			// Open printer for output file
			PrintStream writer;
			writer = new PrintStream(new FileOutputStream(new File(
					outputDirectory + File.separatorChar + outputCSVFileName)),
					true, "ISO_8859_1");
				
			// Output file header
			writer.append(line + "," + FEATURES2D_NAMES_HEADER);
			writer.append('\n');
			
			// Read input CSV file
			while ((line = br.readLine()) != null) {
	 
				// write 3D features
				writer.append(line);
				
				// get 3D image and compute 2D projections
				String[] cellFeatures = line.split(cvsSplitBy);
				String imageCellName = inputImagesFolder + File.separatorChar + 
						cellFeatures[0];// + imagesFileNameExtension;
				// 3D image
				ImagePlus img3D = FitsLoader.read(imageCellName);
				//2D orthogonal projections
				ImagePlus maxProjectionAlongZAxis = OrthogonalProjection
						.projectionXY(img3D,
								OrthogonalProjection.projectionType_Max);
				ImagePlus maxProjectionAlongYAxis = OrthogonalProjection
						.projectionXZ(img3D,
								OrthogonalProjection.projectionType_Max);
				ImagePlus maxProjectionAlongXAxis = OrthogonalProjection
						.projectionZY(img3D,
								OrthogonalProjection.projectionType_Max);
				// Get L vectors directions
				double L1x = Double.parseDouble(cellFeatures[LDirectionsMatrix[0]]);
				double L2x = Double.parseDouble(cellFeatures[LDirectionsMatrix[1]]);
				double L3x = Double.parseDouble(cellFeatures[LDirectionsMatrix[2]]);
				double L1y = Double.parseDouble(cellFeatures[LDirectionsMatrix[3]]);
				double L2y = Double.parseDouble(cellFeatures[LDirectionsMatrix[4]]);
				double L3y = Double.parseDouble(cellFeatures[LDirectionsMatrix[5]]);
				double L1z = Double.parseDouble(cellFeatures[LDirectionsMatrix[6]]);
				double L2z = Double.parseDouble(cellFeatures[LDirectionsMatrix[7]]);
				double L3z = Double.parseDouble(cellFeatures[LDirectionsMatrix[8]]);
				
				
				// 2D oriented projections
				ImagePlus maxProjectionAlongL1Axis = maxProjectionLDirection(
						img3D, L3x, L3y, L3z, L2x, L2y, L2z);
				ImagePlus maxProjectionAlongL2Axis = maxProjectionLDirection(
						img3D, L3x, L3y, L3z, L1x, L1y, L1z);
				ImagePlus maxProjectionAlongL3Axis = maxProjectionLDirection(
						img3D, L1x, L1y, L1z, L2x, L2y, L2z);
				
				// Write 2D projections
				/*Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "XAxisProj_" + img3D.getTitle(),
						maxProjectionAlongXAxis);
				Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "YAxisProj_" + img3D.getTitle(),
						maxProjectionAlongYAxis);
				Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "ZAxisProj_" + img3D.getTitle(),
						maxProjectionAlongZAxis);
				Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "L1AxisProj_" + img3D.getTitle(),
						maxProjectionAlongL1Axis);
				Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "L2AxisProj_" + img3D.getTitle(),
						maxProjectionAlongL2Axis);
				Fits2DWriter.write(outputDirectory + File.separatorChar
						+ "L3AxisProj_" + img3D.getTitle(),
						maxProjectionAlongL3Axis);*/
				
				// compute and write 2D features
				double zPerimeter = perimeter2D(maxProjectionAlongZAxis, threshold);
				writer.append(',' + String.valueOf(zPerimeter));
				double yPerimeter = perimeter2D(maxProjectionAlongYAxis, threshold);
				writer.append(',' + String.valueOf(yPerimeter));
				double xPerimeter = perimeter2D(maxProjectionAlongXAxis, threshold);
				writer.append(',' + String.valueOf(xPerimeter));
				
				double L1Perimeter = perimeter2D(maxProjectionAlongL1Axis, threshold);
				writer.append(',' + String.valueOf(L1Perimeter));
				double L2Perimeter = perimeter2D(maxProjectionAlongL2Axis, threshold);
				writer.append(',' + String.valueOf(L2Perimeter));
				double L3Perimeter = perimeter2D(maxProjectionAlongL3Axis, threshold);
				writer.append(',' + String.valueOf(L3Perimeter));
				
				double zArea = area2D(maxProjectionAlongZAxis, threshold);
				writer.append(',' + String.valueOf(zArea));
				double yArea = area2D(maxProjectionAlongYAxis, threshold);
				writer.append(',' + String.valueOf(yArea));
				double xArea = area2D(maxProjectionAlongXAxis, threshold);
				writer.append(',' + String.valueOf(xArea));
				
				double L1Area = area2D(maxProjectionAlongL1Axis, threshold);
				writer.append(',' + String.valueOf(L1Area));
				double L2Area = area2D(maxProjectionAlongL2Axis, threshold);
				writer.append(',' + String.valueOf(L2Area));
				double L3Area = area2D(maxProjectionAlongL3Axis, threshold);
				writer.append(',' + String.valueOf(L3Area));
				
				double zAspectRatio = aspectRatio2D(maxProjectionAlongZAxis, threshold);
				writer.append(',' + String.valueOf(zAspectRatio));
				double yAspectRatio = aspectRatio2D(maxProjectionAlongYAxis, threshold);
				writer.append(',' + String.valueOf(yAspectRatio));
				double xAspectRatio = aspectRatio2D(maxProjectionAlongXAxis, threshold);
				writer.append(',' + String.valueOf(xAspectRatio));
				
				double L1AspectRatio = aspectRatio2D(maxProjectionAlongL1Axis, threshold);
				writer.append(',' + String.valueOf(L1AspectRatio));
				double L2AspectRatio = aspectRatio2D(maxProjectionAlongL2Axis, threshold);
				writer.append(',' + String.valueOf(L2AspectRatio));
				double L3AspectRatio = aspectRatio2D(maxProjectionAlongL3Axis, threshold);
				writer.append(',' + String.valueOf(L3AspectRatio));
				
				writer.append('\n');
			}
			
			if (br != null) br.close();	
			if(writer != null) {
				writer.flush();
				writer.close();
			}
			
			
		} catch (FileNotFoundException e) {
			System.err.println("CSV file not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Problem while reading or writing the CSV file.");
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.err.println("Problem while trying to get L vectors directions"
					+ " in the CSV file.");
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
		if ((args == null) || (args.length < 6)) {
			System.out
					.println("Please, specify the input directory with tiff files, outputdir and input filter");

			return;
		}

		String inputCSVFilePath = new String(args[0]);
		String outputDirectory = new String(args[1]);
		String outputCSVFileName = new String(args[2]);
		String inputImagesFolder = new String(args[3]); 
		String imagesFileNameExtension = new String(args[4]);
		int threshold = Integer.parseInt(args[5]);
		
		ShapeFeatures2D.compute2DShapeFeatures(inputCSVFilePath,
				outputDirectory, outputCSVFileName, inputImagesFolder,
				imagesFileNameExtension, threshold);
		
	}

}
