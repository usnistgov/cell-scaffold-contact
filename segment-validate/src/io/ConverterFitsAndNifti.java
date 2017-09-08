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
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ij.*;
import ij.measure.Calibration;
import ij.process.*;
import util.FileOper;
/**
 * @author pnb
 *
 */
public class ConverterFitsAndNifti {
	private static Log _logger = LogFactory
			.getLog(ConverterFitsAndNifti.class);

	/**
	 * This method converts one folder of files to another folder of files the other type
	 * 
	 * @param inputNiftiFileFolder
	 * @param outputFitsFileFolder
	 * @param suffixFrom
	 */
	public static void convertNifti2FitsFolder(String inputNiftiFileFolder,
			String outputFitsFileFolder, String suffixFrom, String inputFileSelect) {

		// getting images to process
		Collection<String> dirNiftiFiles = FileOper
				.readFileDirectory(inputNiftiFileFolder);

		// select images with the right suffix
		Collection<String> dirSelectNiftiFiles = FileOper.selectFileType(dirNiftiFiles, inputFileSelect);	
		//Collection<String> dirSelectNiftiFiles = FileOper.selectFileType(dirNiftiFiles, suffixFrom);

		// sort stacks to process
		Collection<String> sortedNiftiImagesInFolder = FileOper.sort(dirSelectNiftiFiles,
				FileOper.SORT_ASCENDING);
		
		
		// start time for benchmark
		long startTime = System.currentTimeMillis();

		String suffixTo = ".nii";
		if(suffixFrom.equalsIgnoreCase(".nii")){
			suffixTo = ".fits";
		}
		String inputFilename = new String();
		String outputFilename = new String();
		for (Iterator<String> k = sortedNiftiImagesInFolder.iterator(); k.hasNext();) {
			inputFilename = k.next();
			String name = new File(inputFilename).getName();
			int idx = name.lastIndexOf(suffixFrom);
			if(idx<0 || idx >= name.length()){	
				System.err.println("this file should not have been selected = " + name);
				continue;
			}
			name = name.substring(0, idx);
			outputFilename = outputFitsFileFolder + File.separatorChar
					+ name	+ suffixTo;
			System.out.println("input file: " + inputFilename);
			System.out.println("output file: " + outputFilename);
				
			if(suffixFrom.endsWith(".nii")){
				convertNifti2FitsFile(inputFilename,outputFilename);
			}else{
				convertFits2NiftiFile(inputFilename,outputFilename);
			}
			
			// for fixing the flip
			//fixFlip_convertFits2NiftiFile(inputFilename,outputFilename);
			
		}

		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();

	}

	/**
	 * This method converts NIFTI file format to FITS file format
	 * @param inputNiftiFileName
	 * @param outputFitsFileName
	 */
	public static void convertNifti2FitsFile(String inputNiftiFileName,
			String outputFitsFileName) {

		try {
			// Open ImagePlus object from image sequence 
			System.out.println("open NIFTI file: " + inputNiftiFileName);
			Nifti_Reader niftiLoader = new Nifti_Reader();
			ImagePlus img3D = null;
			img3D = niftiLoader.read(inputNiftiFileName);
/*			// added a vertical flip to match the FITS file
			ImageStack stack = img3D.getStack();
			for (int i=1; i<=stack.getSize(); i++) {
				ImageProcessor ip = stack.getProcessor(i);
				ip.flipVertical();
			}
			img3D.setStack(stack);*/
			if (img3D.getImageStack() == null) {
				System.err.println("failed to load the file: "
						+ inputNiftiFileName);
			}			

			System.out.println("Writing FITS file: "+(outputFitsFileName) );
			Fits3DWriter.write(outputFitsFileName, img3D);

		} catch (Exception e) {
			_logger.error(e.getMessage());
		}

	}

	/**
	 * This method converts FITS file format to NIFTI file format
	 * @param inputFitsFileName
	 * @param outputNiftiFileName
	 */
	public static void convertFits2NiftiFile(String inputFitsFileName,
			String outputNiftiFileName) {

		try {
			// Open ImagePlus object from image sequence 
			System.out.println("open FITS file: " + inputFitsFileName);							
			ImagePlus img3D = null;
			img3D = new ImagePlus(inputFitsFileName); 
			if (img3D.getImageStack() == null) {
				System.err.println("failed to load the file: "
						+ inputFitsFileName);
			}			

			System.out.println("Writing NII file: "+(outputNiftiFileName) );
			/////////////////////////
			// write output file
			Nifti_Writer nifti = new Nifti_Writer();
			String outputType = new String("::NIFTI_FILE:");
			nifti.write(img3D, outputNiftiFileName, outputType);

		} catch (Exception e) {
			_logger.error(e.getMessage());
		}

	}

	public static void fixFlip_convertFits2NiftiFile(String inputFitsFileName,
			String outputNiftiFileName) {

		try {
			// Open ImagePlus object from image sequence 
			System.out.println("open FITS file: " + inputFitsFileName);							
			ImagePlus img3D = null;
			img3D = new ImagePlus(inputFitsFileName); 
			if (img3D.getImageStack() == null) {
				System.err.println("failed to load the file: "
						+ inputFitsFileName);
			}			

			System.out.println("Writing NII file: "+(outputNiftiFileName) );
			// added a vertical flip to match the NII internal coordinate system
			Calibration c = img3D.getCalibration();			
			ImageStack stack = img3D.getStack();
			for (int i=1; i<=stack.getSize(); i++) {
				ImageProcessor ip = stack.getProcessor(i);
				ip.flipVertical();
			}
			img3D.setStack(stack);
			img3D.setCalibration(c);

			/////////////////////////
			// write ouutput file
			Nifti_Writer nifti = new Nifti_Writer();
			String outputType = new String("::NIFTI_FILE:");
			nifti.write(img3D, outputNiftiFileName, outputType);

		} catch (Exception e) {
			_logger.error(e.getMessage());
		}

	}

	// do not use!!!!!!!!!!!!!!!!!!!
	public static void flipNiftiImageFolder(String inputNiftiFileFolder,
			String outputNiftiFileFolder) {

		// getting images to process
		Collection<String> dirNiftiFiles = FileOper
				.readFileDirectory(inputNiftiFileFolder);

		// select images with the right .nii suffix
		Collection<String> dirSelectNiftiFiles = FileOper.selectFileType(dirNiftiFiles, ".nii");

		// sort stacks to process
		Collection<String> sortedNiftiImagesInFolder = FileOper.sort(dirSelectNiftiFiles,
				FileOper.SORT_ASCENDING);
		
		
		// start time for benchmark
		long startTime = System.currentTimeMillis();

		String inputFilename = new String();
		String outputFilename = new String();
		for (Iterator<String> k = sortedNiftiImagesInFolder.iterator(); k.hasNext();) {
			inputFilename = k.next();
			String name = new File(inputFilename).getName();
			outputFilename = outputNiftiFileFolder + File.separatorChar
					+ name;
			System.out.println("input file: " + inputFilename);
			System.out.println("output file: " + outputFilename);
				

			try {
				// Open ImagePlus object from image sequence 
				System.out.println("open NIFTI file: " + inputFilename);
				Nifti_Reader niftiLoader = new Nifti_Reader();
				ImagePlus img3D = null;
				img3D = niftiLoader.read(inputFilename);
				//Calibration c = img3D.getCalibration();
				// added a vertical flip to match the FITS file
				ImageStack stack = img3D.getStack();
				for (int i=1; i<=stack.getSize(); i++) {
					ImageProcessor ip = stack.getProcessor(i);
					ip.flipVertical();
				}
				img3D.setStack(stack);
				if (img3D.getImageStack() == null) {
					System.err.println("failed to load the file: "
							+ inputFilename);
				}	
				// there is some issue with the NIFTI reader 
				// reading and converting the signed short NII file to unsigned short representation
				// and then NIFTI writer converting the unsigned short into signed short
				// but without changing the calibration coefficients to coeff[0] = 32768.0 and coeff[1] = 1.0
				
				
				// set the calibration information that include the flag about unsigned versus signed short
	/*			double [] coeff = c.getCoefficients();
				boolean isSigned16Bit = true;//c.isSigned16Bit();
				if (coeff[1] == 0.0) coeff[1] = 1.0; // If zero slope, assume unit slope
				if (isSigned16Bit) coeff[0] -= 32768.0 * coeff[1];
				c.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");				
				img3D.setCalibration(c);*/
				
				System.out.println("Writing flipped NIFTI file: "+(outputFilename) );				
				Nifti_Writer nifti = new Nifti_Writer();
				String outputType = new String("::NIFTI_FILE:");
				nifti.write(img3D, outputFilename, outputType);
				

			} catch (Exception e) {
				_logger.error(e.getMessage());
			}

			
		}

		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();

	}
	
	public static void batchScaffoldChannel(){

		long startTime = System.currentTimeMillis();
		
		String inputFitsFileFolder = new String();
		String outputNiftiFileFolder = new String();
		String suffixFrom = new String(".fits");
		String inputFileSelect = new String("_ch01.fits");		

		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
	
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/FITS/Gamma";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/NII_ch01";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();		
	}

	public static void batchContactGeom(){
	
		long startTime = System.currentTimeMillis();
		
		String inputFitsFileFolder = new String();
		String outputNiftiFileFolder = new String();
		String suffixFrom = new String(".fits");
		String inputFileSelect = new String(".fits");		
		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FiberModel/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FiberModel/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
	
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FiberModel/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FiberModel/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FiberModel/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FiberModel/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FiberModel/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FiberModel/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);	
		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/Plane/FITS_Intersect";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/Plane/FITS_IntersectNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/Plane/FITS_Intersect";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/Plane/FITS_IntersectNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		
		
		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();
	}
	
	public static void batchCellSegmentation(){

		long startTime = System.currentTimeMillis();
		
		String inputFitsFileFolder = new String();
		String outputNiftiFileFolder = new String();
		String suffixFrom = new String(".fits");
		String inputFileSelect = new String(".fits");		

		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
	
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/FITS_CellBW";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/NII_CellBW";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();		
	}
	
	public static void batchContactStats(){
		
		long startTime = System.currentTimeMillis();
		
		String inputFitsFileFolder = new String();
		String outputNiftiFileFolder = new String();
		String suffixFrom = new String(".fits");
		String inputFileSelect = new String(".fits");		
		
		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-06252015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-06252015_ZProfile/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
	
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-07072015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/Microfiber-07072015_ZProfile/BinaryContactNII";	
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07102015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07102015_ZProfile/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);

		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07232015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/MediumMicrofiber-07232015_ZProfile/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-06252015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-06252015_ZProfile/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		inputFitsFileFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-07072015_ZProfile/BinaryContact";
		outputNiftiFileFolder= "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/ContactPointEstimation/Alg2_v1/SpunCoat-07072015_ZProfile/BinaryContactNII";				
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputFitsFileFolder, outputNiftiFileFolder,suffixFrom, inputFileSelect);
		
		long endTime = System.currentTimeMillis();
		_logger.info("execution time : " + (endTime - startTime)
				+ " millisecond.");
		System.out.println();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args == null || args.length < 2){
			System.err.println("Error: arguments should be inputNiftiFileName, outputFitsFileName");			
			return;
		}
/*		
  		// D:\Cell-scafffold-contact\testData\061715_MF_1_63x_Pos006.nii  D:\Cell-scafffold-contact\testData\061715_MF_1_63x_Pos006.fits
 		String inputNiftiFileName = args[0];
		String outputFitsFileName = args[1];
		ConverterFitsAndNifti.convertNifti2FitsFile(inputNiftiFileName, outputFitsFileName);
		*/
		
/*		// D:\Cell-scafffold-contact\testData\inFolder D:\Cell-scafffold-contact\testData\outFolder
		String inputNiftiFileFolder = args[0];
		String outputFitsFileFolder = args[1];
		ConverterFitsAndNifti.convertNifti2FitsFolder(inputNiftiFileFolder, outputFitsFileFolder,".fits");
	*/
		
		
/*		// D:\Cell-scafffold-contact\testData\inFolder D:\Cell-scafffold-contact\testData\outFolder
  		String inputNiftiFileFolder = args[0];
		String outputNiftiFileFolder = args[1];
		ConverterFitsAndNifti.flipNiftiImageFolder(inputNiftiFileFolder, outputNiftiFileFolder);
		
*/
		
		ConverterFitsAndNifti.batchContactGeom();
		//ConverterFitsAndNifti.batchScaffoldChannel();
		
		//ConverterFitsAndNifti.batchCellSegmentation();
		//ConverterFitsAndNifti.batchContactStats();
			
	}

}
