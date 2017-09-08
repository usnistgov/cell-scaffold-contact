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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import io.FitsLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import segment3D.Segment3DImage;
import util.FileOper;

/**
 * This method implements Topological Stable State Thresholding method according
 * to A. Pikaz, A. Averbuch., Digital Image Thresholding Based on Topological
 * Stable State, Pattern Recognition, 29 (1996) 829-843. respectively according
 * to the thresholding survey paper M. Sezgin and B. Sankur, ���Survey over image
 * thresholding techniques and quantitative performance evaluation,��� Journal of
 * Electronic Imaging 13(1), 146���165 (January 2004).
 * 
 * @author peter bajcsy
 * 
 */

public class TopoStableThresh extends Threshold3DImage{
	private static Log _logger = LogFactory.getLog(TopoStableThresh.class);


	/**
	 * This method finds the optimal threshold following the topologically
	 * stable state over a range of thresholds
	 * 
	 * @param img3D
	 *            - input 3D image
	 * @param min
	 *            - minimum threshold
	 * @param max
	 *            - maximum threshold (included)
	 * @param delta
	 *            - delta threshold increment
	 * @return - double threshold value
	 */
	public double findThresh(ImagePlus img3D, double min, double max,
			double delta) {

		// sanity check
		if (img3D == null) {
			_logger.error("Missing array of input images");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();

		double thresh = 0.0;
		int numIter = 1 + (int) ((max - min) / delta);
		long[] count = new long[numIter];
		int iter = 0;
		
		long[] frgCounts = new long[numIter];
		long[] bkgCounts = new long[numIter];
		
		for (thresh = min; thresh <= max; thresh += delta) {
				count[iter] = 0;
			
			// Implementation for objects larger than N pixels (N=500)
			Segment3DImage segment3DImage = new Segment3DImage(img3D);
			segment3DImage.thresholdImage((int) thresh);
			frgCounts[iter] = segment3DImage.getFRGCount();
			bkgCounts[iter] = segment3DImage.getBKGCount();
			
			count[iter] = segment3DImage.getNumberOfObjectsLargerThanNPixels(500, (int) thresh);
			//System.out.println("Number of components larger than 500 pixels is equal " + count[iter] + " for threshold is equal " + thresh);

			iter++;
		}

		// Note: this minIsStable should be an array of length numbands.
		// however, in the current implementation the min is found over all bands
		double optThresh = -1.0;
		double minIsStable = Double.MAX_VALUE;
		double temp;
		double v1, v2, u1,u2;
			
		// the minimum of the first derivative works for most images
		System.out.println("THRESHOLD,NUMBER OF OBJECTS[T],NUMBER OF OBJECTS[T+1],OBJscore1stDer,NUMBER OF FRG Voxels[T],NUMBER OF FRG Voxels[T+1], FRGscore1stDer ");		
		for (iter = 0; iter < numIter - 1; iter++) {
			// in both percentage calculations we assume that with an increasing threshold value (a) the number of objects and (b) the number of foreground pixels 
			// will decrease. This takes care of cases when the threshold becomes too high and cuts a meaningful object into pieces
			v1 = -1.0;
			if(count[iter] > 0 && count[iter] >= count[iter + 1]) {
				v1 = (count[iter] - count[iter + 1])
						/ (double) count[iter];
			}
			v2 = -1.0;
			if(frgCounts[iter] > 0 && frgCounts[iter] >= frgCounts[iter+1]) {
				v2 = (frgCounts[iter] - frgCounts[iter+1])
						/ (double) frgCounts[iter];
			}
			// this if statement is stating the selection criterion
			// an optimal threshold is a value at which the number of objects has not changed by more 1% and
			// the number of foreground pixels have not changed by more than 1% as one goes from T1 to T1+1
			// the number of foreground pixels varies more than the number of objects and therefore v2 is more 
			// suitable for finding the optimal point 
			if(v1 > -1.0 && v1 < 0.01 && v2 > -1.0 && v2 < 0.01){
				if (v2 < minIsStable) {
					minIsStable = v2;
					optThresh = min + iter * delta;
				}
			} else {
				System.err.println("thresh="+(min + iter * delta)+" does not qualify: 1st derivative of #objects = " + v1 + " of #FRG ="	+ v2 + " are not less than 1%");
			}
			System.out.println( iter + ","
					+ count[iter] + "," + count[iter+1] + "," + v1 + ","+ frgCounts[iter] + "," + frgCounts[iter+1] + "," + v2);

		}
		System.out.println();
		if(optThresh < 0){
			System.out.println("Could not find optThresh1stDer");
		}else{
			System.out.println("optThresh1stDer="+optThresh);
		}
		
 		double epsilon = 0.0001;
   		minIsStable = Double.MAX_VALUE;
   		if(optThresh < 0 || Math.abs(optThresh - max) < epsilon) {
   			// for measured large size data and limited range for threshold values, we use the minimum of the second derivative 
   			System.out.println("THRESHOLD,NUMBER OF OBJECTS,NUMBER OF FRG VOXELS,NUMBER OF BKG VOXELS, OBJScore2dDer");		   			
   			for (iter = 0; iter < numIter - 2; iter++) {
   				if(count[iter] > 0 && count[iter + 1] >0 && count[iter] >= count[iter + 1] && count[iter+1] >= count[iter+2]) {
   					v1 = (count[iter] - count[iter + 1])/(double)count[iter];
					v2 = (count[iter +1] - count[iter + 2])/(double) count[iter+1];
					temp = Math.abs(v1-v2);
					if (temp < minIsStable) {
						minIsStable = temp;
						optThresh = min + (iter+1) * delta;
					}
	
   					/*
   					temp = (100*(count[iter] - count[iter + 1])
   							/ (double) count[iter] - 100*(count[iter +1] - count[iter + 2])
   							/ (double) count[iter+1]) / (100*(count[iter] - count[iter + 1])
   							/ (double) count[iter]);
   					if (temp < minIsStable) {
   						minIsStable = temp;
   						optThresh = min + (iter+1) * delta;
   					}
   					*/
   					System.out.println(iter + "," + count[iter] + "," + frgCounts[iter]+ "," + bkgCounts[iter] + "," + temp);   					
   				} else {
   					System.err.println("Number of objects at thresh = " +(min + iter * delta) + " or " + (min + iter * delta+1) + " < 0");
   				}
   			}
   		}
   
		if(optThresh < 0){
			System.out.println("Could not find optThresh");
			optThresh = min;
		}
		System.out.println("optThresh=" + optThresh);

		return optThresh;
	}
	
	/**
	 * This method processes all FITS files in one directory - it computes the optimal threshold value per file name
	 * @param sourceDirPath - pointer to one directory with FITS files
	 * @param inputFilter - filter for files in the folder (e.g., ".fits")
	 * @param destFilePath - output folder for the results
	 */
	public static void processOneFolderWithFITSfiles(String sourceDirPath, String inputFilter, String destFilePath, double minThresh, double maxThresh, double deltaThresh ){
		Collection<String> dirfiles = FileOper.readFileDirectory(sourceDirPath);

		System.out.println("Directory Collection Size=" + dirfiles.size());

		Collection<String> onlyFilter = FileOper.selectFileType(dirfiles,
				inputFilter);
		// FileOper.printCollection(onlyFilter);
		// System.out.println();

		Collection<String> sortedFilter = FileOper.sort(onlyFilter,
				FileOper.SORT_ASCENDING);
		FileOper.printCollection(sortedFilter);
		System.out.println();
		
		PrintStream out;

		try {
			out = new PrintStream(new FileOutputStream(destFilePath + File.separatorChar + new File(sourceDirPath).getName() + "_consoleOutput.txt"));
			System.setOut(out);
			System.setErr(out);
			
			// start time for benchmark
		    long startTime = System.currentTimeMillis();
			
			FileWriter writer = new FileWriter(destFilePath + File.separatorChar + new File(sourceDirPath).getName() + ".csv");
			writer.append("FileName");
		    writer.append(',');
		    writer.append("OptimalThreshold");
		    writer.append('\n');
			
			//int index = 0;
			String inputFilename = new String();
			for (Iterator<String> k = sortedFilter.iterator(); k.hasNext();) {
				inputFilename = k.next();
				try {
					ImagePlus img3D = FitsLoader.read(inputFilename);
					System.out.println(inputFilename);
					System.out.println("File processing start time: "+ new Date().toString());
					
					if (img3D == null) {
						System.err
								.println("Could not load file = " + inputFilename);
					}
					// find opt Threshold
					TopoStableThresh myTopo = new TopoStableThresh();
					double optThresh = myTopo.findThresh(img3D, minThresh, maxThresh, deltaThresh);
			
					// save in csv file
					writer.append(inputFilename);
				    writer.append(',');
				    writer.append(String.valueOf(optThresh));
				    writer.append('\n');
					
				} catch (Exception e) {
					System.err.println("IOException: Could not load file = "
							+ inputFilename);
					e.printStackTrace();
				}
				//index++;
			}
			writer.flush();
		    writer.close();
		    
		// end time for benchmark
	    long endTime = System.currentTimeMillis();
	    System.out.println("Minimum error threshold execution time : " + (endTime - startTime) + " millisecond.");
	    System.out.println();
			
		} catch (IOException e) {
			System.err.println("IOException: Could not open file = "
					+ destFilePath + File.separatorChar + new File(sourceDirPath).getName() + "csv");
			e.printStackTrace();
		}
	}
	/**
	 * This method processes a set of folders with each folder containing a set of FITS files - find the optimal threshold value
	 * @param sourceDirPath
	 * @param inputFilter - filter for files in the folder (e.g., ".fits")
	 * @param destFilePath - output folder for the results
	 */
	public static void processFoldersWithFITSfiles(String sourceDirPath, String inputFilter, String destFilePath, double minThresh, double maxThresh, double deltaThresh ){
		Collection<String> dirfiles = FileOper.readSubDirectories(sourceDirPath);

		System.out.println("Directory: Number of directories=" + dirfiles.size());
		FileOper.printCollection(dirfiles);
		System.out.println();

		String inputDirPath = new String();
		for (Iterator<String> k = dirfiles.iterator(); k.hasNext();) {
			inputDirPath = k.next();
			File temp = new File(inputDirPath);
			if (temp.isDirectory() ){
				TopoStableThresh.processOneFolderWithFITSfiles(inputDirPath, inputFilter, destFilePath,  minThresh, maxThresh, deltaThresh);
			}
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
		if ((args == null) || (args.length < 3)) {
			System.out
					.println("Please, specify the input directory with tiff files, outputdir and input filter");

			return;
		}

		String sourceDirPath = new String(args[0]);
		String destFilePath = new String(args[1]);
		String inputFilter = new String(args[2]);
		//input parameters - threshold values over which the optimization is performed
		double minThresh = 1.0;
		double maxThresh = 255.0;
		double deltaThresh = 1.0;
		
		//process one folder with FITS files
		TopoStableThresh.processOneFolderWithFITSfiles(sourceDirPath, inputFilter, destFilePath, minThresh, maxThresh, deltaThresh);
		// process many folders with FITS files
		//TopoStableThresh.processFoldersWithFITSfiles(sourceDirPath, inputFilter, destFilePath, minThresh, maxThresh, deltaThresh);

		
		/*
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 3)) {
			System.out
					.println("Please, specify the input directory with tiff files, outputdir and input filter");

			return;
		}

		String inputDir0 = new String(args[0]);
		String outputDir = new String(args[1]);
		String inputFilter = new String(args[2]);

		
		Collection<String> dirfiles = FileOper.readFileDirectory(inputDir0);

		System.out.println("Directory Collection Size=" + dirfiles.size());

		Collection<String> onlyFilter = FileOper.selectFileType(dirfiles,
				inputFilter);
		// FileOper.printCollection(onlyFilter);
		// System.out.println();

		Collection<String> sortedFilter = FileOper.sort(onlyFilter,
				FileOper.SORT_ASCENDING);
		FileOper.printCollection(sortedFilter);
		System.out.println();

		FileWriter writer;
		PrintStream out;
	
		try {
			out = new PrintStream(new FileOutputStream(
					outputDir + File.separatorChar + new File(inputDir0).getName() + "_consoleOutput.txt")
			);
			System.setOut(out);
			System.setErr(out);
			
			// start time for benchmark
		    long startTime = System.currentTimeMillis();
			
			writer = new FileWriter(outputDir + File.separatorChar + new File(inputDir0).getName() + ".csv");
			writer.append("FileName");
		    writer.append(',');
		    writer.append("OptimalThreshold");
		    writer.append('\n');
		    
		    //int index = 0;
			String inputFilename = new String();
			for (Iterator<String> k = sortedFilter.iterator(); k.hasNext();) {
				inputFilename = k.next();
				try {
					ImagePlus img3D = FitsLoader.read(inputFilename);
					System.out.println(inputFilename);
					System.out.println("File processing start time: "+ new Date().toString());
					
					if (img3D == null) {
						System.err
								.println("Could not load file = " + inputFilename);
					}
					// find opt Threshold
					TopoStableThresh myTopo = new TopoStableThresh();
					double optThresh = myTopo.findThresh(img3D, 1.0, 255.0, 1.0);
					// save in csv file
					writer.append(inputFilename);
				    writer.append(',');
				    writer.append(String.valueOf(optThresh));
				    writer.append('\n');
					
				} catch (Exception e) {
					System.err.println("IOException: Could not load file = "
							+ inputFilename);
					e.printStackTrace();
				}
				//index++;
			}
			writer.flush();
		    writer.close();
		    
		    // end time for benchmark
		    long endTime = System.currentTimeMillis();
		    System.out.println("Topo stable threshold execution time : " + (endTime - startTime) + " millisecond.");
		    System.out.println();
		    
		} catch (IOException e) {
			System.err.println("IOException: Could not open file = "
					+ outputDir + File.separatorChar + new File(inputDir0).getName() + "csv");
			e.printStackTrace();
		}
		*/
		
		/*String fileName = "/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS files/Spun Coat/061813_DC_SC1_d1_63x_1.fits";
		ImagePlus img3D = FitsLoader.read(fileName);
		System.out.println(fileName);*/
		
		
/*
		try {
			ImageObject[] res3D = TopoStableThresh.threshold(img3D, optThresh);
			// need to save the result into FITS!!!
			String outFilename = new String();
			for (int i = 0; i < res3D.length; i++) {
				if (i > 9) {
					outFilename = outputDir + "thresh_z" + Integer.toString(i)
							+ ".tif";
				} else {
					outFilename = outputDir + "thresh_z0" + Integer.toString(i)
							+ ".tif";
				}
				// System.out.println("writing file: " + outFilename);
				ImageLoader.writeImage(outFilename, res3D[i]);
			}

		} catch (ImageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
	}

}
