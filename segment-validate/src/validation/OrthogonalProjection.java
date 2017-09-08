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
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import io.Fits2DWriter;
import io.FitsLoader;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.FileOper;

/**
 * This class is for performing the three orthogonal projections of a 3D volume
 * into XY, XZ and ZY planes
 * 
 * @author peter bajcsy & mylene simon
 * 
 */

public class OrthogonalProjection {

	private static Log _logger = LogFactory.getLog(OrthogonalProjection.class);

	public static final int projectionType_Max = 0;
	public static final int projectionType_Mean = 1;

	/**
	 * This method performs the max or average projection into the XY plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected ImagePlus
	 */
	static public ImagePlus projectionXY(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		byte[] projectedData = new byte[numcols*numrows];
		
		switch (projectionType) {
		// max projection
		case 0:
			for (int row = 0; row < numrows; row++) {
				for (int col = 0; col < numcols; col++) {
					max = -Double.MAX_VALUE;
					for (int z = 0; z < numzs; z++) {
						double voxel = imgStack.getVoxel(col,  row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[row * numcols + col] = (byte) max;
				}
			}
			break;
		// average projection
		case 1:
			for (int row = 0; row < numrows; row++) {
				for (int col = 0; col < numcols; col++) {
					max = 0;
					for (int z = 0; z < numzs; z++) {
						double voxel = imgStack.getVoxel(col,  row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numzs);
					projectedData[row * numcols + col] = (byte) max;
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;

		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ByteProcessor imgProc = new ByteProcessor(numcols, numrows, projectedData);
		res.setProcessor(imgProc);
		
		return res;
	}

	/**
	 * This method performs the max or average projection into the ZY plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected ImagePlus
	 */
	static public ImagePlus projectionZY(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		byte[] projectedData = new byte[numzs*numrows];
		
		switch (projectionType) {
		case 0:
			// max projection
			for (int z = 0; z < numzs; z++) {
				for (int row = 0; row < numrows; row++) {
					max = -Double.MAX_VALUE;
					for (int col = 0; col < numcols; col++) {
						double voxel = imgStack.getVoxel(col, row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[row * numzs + z] = (byte) max;
					//projectedData[z * numrows + row] = (byte) max;
					//res.setDouble(z, col, band, max);
				}
			}
			break;
		case 1:
			// average projection
			for (int z = 0; z < numzs; z++) {
				for (int row = 0; row < numrows; row++) {
					max = 0.0;
					for (int col = 0; col < numcols; col++) {
						double voxel = imgStack.getVoxel(col, row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numrows);
					projectedData[row * numzs + z] = (byte) max;
					//projectedData[z * numrows + row] = (byte) max;
					//res.setDouble(z, col, band, max);
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;
		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ByteProcessor imgProc = new ByteProcessor(numzs, numrows, projectedData);
		res.setProcessor(imgProc);
		// Set the calibration for the horizontal axis
		Calibration newCalibration = res.getCalibration();
		newCalibration.pixelWidth = img3D.getCalibration().pixelDepth;
		newCalibration.setXUnit(img3D.getCalibration().getZUnit());
		res.setCalibration(newCalibration);
		
		return res;
	}

	/**
	 * This method performs the orthogonal 3D projection into the XZ plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected XZ ImagePlus
	 */
	static public ImagePlus projectionXZ(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		byte[] projectedData = new byte[numcols*numzs];
		switch (projectionType) {
		case 0:
			// max projection
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					max = -Double.MAX_VALUE;
					for (int row = 0; row < numrows; row++) {
						double voxel = imgStack.getVoxel(col, row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[z * numcols + col] = (byte) max;
					//res.setDouble(row, z, band, max);
				}
			}
			break;
		case 1:
			// average projection
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					max = -Double.MAX_VALUE;
					for (int row = 0; row < numrows; row++) {
						double voxel = imgStack.getVoxel(col, row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numcols);
					projectedData[z * numcols + col] = (byte) max;
					//res.setDouble(row, z, band, max);
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;
		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ByteProcessor imgProc = new ByteProcessor(numcols, numzs, projectedData);
		res.setProcessor(imgProc);
		// Set the calibration for the vertical axis
		Calibration newCalibration = res.getCalibration();
		newCalibration.pixelHeight = img3D.getCalibration().pixelDepth;
		newCalibration.setYUnit(img3D.getCalibration().getZUnit());
		res.setCalibration(newCalibration);
				
		return res;
	}
	
	/**
	 * This method performs the max or average projection into the XY plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected ImagePlus
	 */
	static public ImagePlus projectionXY16bits(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		short[] projectedData = new short[numcols*numrows];
		
		switch (projectionType) {
		// max projection
		case 0:
			for (int row = 0; row < numrows; row++) {
				for (int col = 0; col < numcols; col++) {
					max = -Double.MAX_VALUE;
					for (int z = 0; z < numzs; z++) {
						double voxel = imgStack.getVoxel(col,  row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[row * numcols + col] = (short) max;
				}
			}
			break;
		// average projection
		case 1:
			for (int row = 0; row < numrows; row++) {
				for (int col = 0; col < numcols; col++) {
					max = 0;
					for (int z = 0; z < numzs; z++) {
						double voxel = imgStack.getVoxel(col,  row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numzs);
					projectedData[row * numcols + col] = (short) max;
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;

		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ShortProcessor imgProc = new ShortProcessor(numcols, numrows, projectedData, null);
		res.setProcessor(imgProc);
		
		return res;
	}

	/**
	 * This method performs the max or average projection into the ZY plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected ImagePlus
	 */
	static public ImagePlus projectionZY16bits(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		short[] projectedData = new short[numzs*numrows];
		
		switch (projectionType) {
		case 0:
			// max projection
			for (int z = 0; z < numzs; z++) {
				for (int row = 0; row < numrows; row++) {
					max = -Double.MAX_VALUE;
					for (int col = 0; col < numcols; col++) {
						double voxel = imgStack.getVoxel(col, row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[row * numzs + z] = (short) max;
					//projectedData[z * numrows + row] = (byte) max;
					//res.setDouble(z, col, band, max);
				}
			}
			break;
		case 1:
			// average projection
			for (int z = 0; z < numzs; z++) {
				for (int row = 0; row < numrows; row++) {
					max = 0.0;
					for (int col = 0; col < numcols; col++) {
						double voxel = imgStack.getVoxel(col, row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numrows);
					projectedData[row * numzs + z] = (short) max;
					//projectedData[z * numrows + row] = (byte) max;
					//res.setDouble(z, col, band, max);
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;
		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ShortProcessor imgProc = new ShortProcessor(numzs, numrows, projectedData, null);
		res.setProcessor(imgProc);
		// Set the calibration for the horizontal axis
		Calibration newCalibration = res.getCalibration();
		newCalibration.pixelWidth = img3D.getCalibration().pixelDepth;
		newCalibration.setXUnit(img3D.getCalibration().getZUnit());
		res.setCalibration(newCalibration);
		
		return res;
	}

	/**
	 * This method performs the orthogonal 3D projection into the XZ plane
	 * 
	 * @param img3D
	 *            - input 3D volume
	 * @return - projected XZ ImagePlus
	 */
	static public ImagePlus projectionXZ16bits(ImagePlus img3D, int projectionType) {
		// sanity check
		if (img3D == null) {
			_logger.error("Missing input image");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		double max = 0;
		ImageStack imgStack = img3D.getImageStack();
		
		ImagePlus res = img3D.createImagePlus();
		
		short[] projectedData = new short[numcols*numzs];
		switch (projectionType) {
		case 0:
			// max projection
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					max = -Double.MAX_VALUE;
					for (int row = 0; row < numrows; row++) {
						double voxel = imgStack.getVoxel(col, row, z);
						if (max < voxel) {
							max = voxel;
						}
					}
					projectedData[z * numcols + col] = (short) max;
					//res.setDouble(row, z, band, max);
				}
			}
			break;
		case 1:
			// average projection
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					max = -Double.MAX_VALUE;
					for (int row = 0; row < numrows; row++) {
						double voxel = imgStack.getVoxel(col, row, z);
						max += voxel;
					}
					max = Math.round(max / (double) numcols);
					projectedData[z * numcols + col] = (short) max;
					//res.setDouble(row, z, band, max);
				}
			}
			break;
		default:
			System.err.println("the projection type " + projectionType
					+ " is not supported");
			res = null;
			break;
		}

		// Create the ImageProcessor with the array of projected pixels and set it in the result ImagePlus
		ShortProcessor imgProc = new ShortProcessor(numcols, numzs, projectedData, null);
		res.setProcessor(imgProc);
		// Set the calibration for the vertical axis
		Calibration newCalibration = res.getCalibration();
		newCalibration.pixelHeight = img3D.getCalibration().pixelDepth;
		newCalibration.setYUnit(img3D.getCalibration().getZUnit());
		res.setCalibration(newCalibration);
				
		return res;
	}
	
	/**
	 * This method opens the 3D FITS file and performs three max or average
	 * projections into XY, XZ and YZ and saves them into an outputDir
	 * 
	 * @param inputFile
	 *            - input 3D FITS file
	 * @param outputDir
	 *            - directory to which the method saves three orthogonal max
	 *            or average projections
	 * @param projectionType
	 * 			  - projection type (0 for maximum or 1 for average)
	 */
	static public void processOneFile(String inputFile, String outputDir, int projectionType) {
		
		System.out.println("Processing 3D Fits file " + inputFile + "...");
		
		// Load 3D FITS image in IJ ImagePlus object
		ImagePlus img3D = FitsLoader.read(inputFile);
		if(img3D == null) {
			System.err.println("Could not load file = " + inputFile);
			return;
		}
		
		// create the output suffix based on projection type
		String projTypeSuffix = new String();
		switch (projectionType) {
		case 0:
			projTypeSuffix = "max";
			break;
		case 1:
			projTypeSuffix = "ave";
			break;
		default:
			System.err.println("Projection type is not supported");
			break;
		}
		
		int indexLastFileSeparatorChar = inputFile.lastIndexOf(File.separatorChar);
		int indexLastDot = inputFile.lastIndexOf(".");
		String outFilename = inputFile.substring(indexLastFileSeparatorChar, indexLastDot);

		// Compute projection
		System.out.println("XY " + projTypeSuffix + " projection");
		ImagePlus res = OrthogonalProjection.projectionXY(img3D, projectionType);
		if(res == null) {
			System.err.println("XY " + projTypeSuffix + " projection failed.");
			return;
		}
		Fits2DWriter.write(outputDir + outFilename + "_" + projTypeSuffix + "XY.fits", res);
		//ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"XY.tif", res);

		System.out.println("XZ " + projTypeSuffix + " projection");
		res = OrthogonalProjection.projectionXZ(img3D, projectionType);
		if(res == null) {
			System.err.println("XZ " + projTypeSuffix + " projection failed.");
			return;
		}
		Fits2DWriter.write(outputDir + outFilename + "_" + projTypeSuffix + "XZ.fits", res);
		//ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"XZ.tif", res);

		System.out.println("ZY " + projTypeSuffix + " projection");
		res = OrthogonalProjection.projectionZY(img3D, projectionType);
		if(res == null) {
			System.err.println("ZY " + projTypeSuffix + " projection failed.");
			return;
		}
		Fits2DWriter.write(outputDir + outFilename + "_" + projTypeSuffix + "ZY.fits", res);
		//ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"ZY.tif", res);
	}

	/**
	 * This method takes all files that form one 3D volume in one folders and
	 * performs three max projections into XY, XZ and YZ and saves them into an
	 * outputDir
	 * 
	 * @param inputDir
	 *            - input folder
	 * @param outputDir
	 *            - directory to which the method saves three orthogonal max
	 *            projections
	 * @param inputFilter
	 *            - filter for selecting input files
	 */
	@Deprecated
	static public void processOneDir(String inputDir, String outputDir,
			String inputFilter, int projectionType) {

		Collection<String> dirfiles = FileOper.readFileDirectory(inputDir);

		System.out.println("Directory Collection Size=" + dirfiles.size());

		Collection<String> onlyFilter = FileOper.selectFileType(dirfiles,
				inputFilter);
		// FileOper.printCollection(onlyFilter);
		// System.out.println();

		Collection<String> sortedFilter = FileOper.sort(onlyFilter,
				FileOper.SORT_ASCENDING);
		FileOper.printCollection(sortedFilter);
		System.out.println();

		// create the output suffix based on projection type
		/*String projTypeSuffix = new String();
		switch (projectionType) {
		case 0:
			projTypeSuffix = "max";
			break;
		case 1:
			projTypeSuffix = "ave";
			break;
		default:
			System.err.println("Projection type is not supported");
			break;
		}

		ImageObject[] img3D = new ImageObject[onlyFilter.size()];

		int index = 0;
		String inputFilename = new String();
		for (Iterator<String> k = sortedFilter.iterator(); k.hasNext();) {
			inputFilename = k.next();
			try {
				img3D[index] = ImageLoader.readImage(inputFilename);

				if (img3D[index] == null) {
					System.err
							.println("Could not load file = " + inputFilename);
				}
			} catch (IOException e) {
				System.err.println("IOException: Could not load file = "
						+ inputFilename);
				e.printStackTrace();
			} catch (Exception e) {
				System.err.println("Exception: Could not load file = "
						+ inputFilename);
				e.printStackTrace();
			}
			index++;
		}

		int idx = inputDir.lastIndexOf(File.separatorChar);
		String outFilename = inputDir.substring(idx, inputDir.length());

		// ////////////////////////////
		// max projection
		try {
			System.out.println("XY projection");
			ImageObject res = OrthogonalProjection.projectionXY(img3D,
					projectionType);
			ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"XY.tif", res);

			System.out.println("XZ projection");
			res = OrthogonalProjection.projectionXZ(img3D, projectionType);
			ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"XZ.tif", res);

			System.out.println("ZY projection");
			res = OrthogonalProjection.projectionZY(img3D, projectionType);
			ImageLoader.writeImage(outputDir + outFilename + "_"+projTypeSuffix+"ZY.tif", res);

		} catch (ImageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

	/**
	 * This method takes a path to a set of folders with all files in each
	 * folder that form one 3D volume. It performs three projections into XY, XZ
	 * and YZ and saves them into an outputDir
	 * 
	 * @param inputDir
	 *            - input folder
	 * @param outputDir
	 *            - directory to which the method saves three orthogonal max
	 *            projections
	 * @param inputFilter
	 *            - filter for selecting input files
	 * @param projectionType
	 *            - type of orthogonal projection
	 */
	@Deprecated
	static public void processManyDir(String inputDir, String outputDir,
			String inputFilter, int projectionType) {

		Collection<String> dirs = FileOper.readSubDirectories(inputDir);

		System.out.println("Directory Collection Size=" + dirs.size());

		FileOper.printCollection(dirs);
		System.out.println();

		String inputDirname = new String();
		for (Iterator<String> k = dirs.iterator(); k.hasNext();) {
			inputDirname = k.next();
			OrthogonalProjection.processOneDir(inputDirname, outputDir,
					inputFilter, projectionType);

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
		if ((args == null) || (args.length < 2)) {
			System.out
					.println("Please, specify the input 3D FITS file and outputdir");

			return;
		}

		String inputFile = new String(args[0]);
		String outputDir = new String(args[1]);

		OrthogonalProjection.processOneFile(inputFile, outputDir, OrthogonalProjection.projectionType_Max);
		OrthogonalProjection.processOneFile(inputFile, outputDir, OrthogonalProjection.projectionType_Mean);
		
		// this is a single folder option
		/*OrthogonalProjection.processOneDir(inputDir0, outputDir, inputFilter,
				OrthogonalProjection.projectionType_Max);

		OrthogonalProjection.processOneDir(inputDir0, outputDir, inputFilter,
				OrthogonalProjection.projectionType_Mean);*/

		// this is a multiple folder option
		// OrthogonalProjection.processManyDir(inputDir0, outputDir,
		// inputFilter,
		// OrthogonalProjection.projectionType_Max);

	}

}
