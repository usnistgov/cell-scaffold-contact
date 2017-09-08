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
package io;

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
import ij.measure.Calibration;
import pipeline3D.Image3DProcessingPipeline;
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
 * This class is for converting a folder with a set of tif frames forming
 * one z-stack to one file in FITS file format
 *  
 * @author pnb
 *
 */
public class ConvertTiffStackToFITS {

	private static Log logger = LogFactory
			.getLog(ConvertTiffStackToFITS.class);

	
	public static void convert(String inputImagesFolder,
			String imagesFileNameExtension, String outputDirectory) {

		// getting images to process
		Collection<String> dirfiles = FileOper
				.readSubDirectories(inputImagesFolder);
		
		//Collection<String> filesList = FileOper.readFileDirectory(inputImagesFolder);		
		

		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirfiles,FileOper.SORT_ASCENDING);

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
			logger.info(sortedImagesInFolder.size() + " images to process");

			// start time for benchmark
			long startTime = System.currentTimeMillis();
	
			String inputFilename = new String();
			for (Iterator<String> k = sortedImagesInFolder.iterator(); k
					.hasNext();) {
				inputFilename = k.next();
				System.out.println("INFO: loading file "+ inputFilename);
				try {
					// Open ImagePlus object from image sequence and set calibration
					ImagePlus img3D = Fits3DWriter.loadZstack(inputFilename, imagesFileNameExtension); //new ImagePlus(inputFilename);
				
					String shortImageName = new File(inputFilename).getName();
					logger.info("Converting z-stack " + shortImageName
							+ " at time: " + new Date().toString());
					
						
					// save z-stack in a FITS file format
					Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName + ".fits", img3D);
				} catch (Exception e) {
					logger.error(e.getMessage());				
				}

			}
			// end time for benchmark
			long endTime = System.currentTimeMillis();
			logger.info("Image3DProcessingPipeline execution time : "
					+ (endTime - startTime) + " millisecond.");
			System.out.println();

		} catch (IOException e) {
			logger.error(e.getMessage());
			
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args != null){
			System.out.println("length="+args.length);
			for(int i=0;i<args.length;i++)
				System.out.println("args["+i+"]="+args[i] +"\n");	
		}else{
			System.err.println("two input parameters: inputImagesFolder and  outputDirectory");			
			return;
		}
		String inputImagesFolder = new String(args[0]);
		String imagesFileNameExtension = new String("tif");
		//String outputDirectory = new String("C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/csPaper/WERB/BMC_Bioinformatics_Reviews/temp/");	
		//String outputDirectory = new String("D:\\Cell-scafffold-contact\\singleFiberFluorescentFITS");
		String outputDirectory = new String(args[1]);
		System.out.println("inputImagesFolder="+inputImagesFolder);
		System.out.println("outputDirectory="+outputDirectory);
		
		ConvertTiffStackToFITS.convert(inputImagesFolder, imagesFileNameExtension, outputDirectory);

	}

}
