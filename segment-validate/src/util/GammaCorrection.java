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
package util;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import io.Fits3DWriter;
import io.Nifti_Reader;
import io.Nifti_Writer;
import pipeline3D.FiberScaffold2BinaryContact;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * This class will correct intensity values for the display non-linear distortion:
 * see http://www.cgsd.com/papers/gamma_intro.html
 * It can perhaps simulate exposure time of a microscope by
 * applying a gamma correction to each intensity according to the formula
 * I_new = maxIntensity*(I_old/maxIntensity)^(1.0/gamma)
 * more information can be found at http://en.wikipedia.org/wiki/Gamma_correction
 * 
 * 
 * @author Peter Bajcsy
 * @version 1.0
 */

public class GammaCorrection {

	private static Log _logger = LogFactory.getLog(GammaCorrection.class);

	private double _gamma = 2.5;
	
	private double _minGamma = 0.2;
	private double _maxGamma = 2.0;
	private double _deltaGamma = 0.2;

	int [] _gammatable = null;
	public GammaCorrection() {
	}

	public GammaCorrection(double gamma) {
		_gamma = gamma;
	}

	public void reset(){
		_gammatable = null;
		_gamma = 2.5; // this is the value recommended at http://www.cgsd.com/papers/gamma_intro.html		
	}
	public boolean setMinGamma(double val){
		if(val <= 0){
			_logger.error("Gamma should be larger than zero.");
			return false;
		}
		_minGamma = val;
		return true;
	}
	public double getMinGamma(){
		return _minGamma;
	}
	
	public boolean setMaxGamma(double val){
		if(val <= 0){
			_logger.error("Gamma should be larger than zero.");
			return false;
		}
		_maxGamma = val;
		return true;
	}
	public double getMaxGamma(){
		return _maxGamma;
	}
	public boolean setDeltaGamma(double val){
		if(val <= 0){
			_logger.error("Gamma should be larger than zero.");
			return false;
		}
		_deltaGamma = val;
		return true;
	}
	public double getDeltaGamma(){
		return _deltaGamma;
	}
	public boolean setGamma(double val){
		if(val <= 0){
			_logger.error("Gamma should be larger than zero.");
			return false;
		}
		_gamma = val;
		return true;
	}
	public double getGamma(){
		return _gamma;
	}
	/**
	 * Apply gamma correction to an image
	 * with a pre-set value of gamma
	 * @param img - input image
	 * @return image object
	 * @throws CloneNotSupportedException
	 */
	public ImagePlus applyGammaCorrection(ImagePlus img)
			throws CloneNotSupportedException {
		return applyGammaCorrection(img, getGamma());

	}
	/**
	 * Apply gamma correction to an image
	 * @param img - input image
	 * @param gamma - input gamma value
	 * 
	 * @return image object 
	 * @throws CloneNotSupportedException
	 */
	public ImagePlus applyGammaCorrection(ImagePlus img, double gamma)
			throws CloneNotSupportedException {
		// sanity check
		if (img == null) {
			_logger.error("Missing input image");
			return null;
		}
		if (gamma <= 0) {
			_logger.error("Gamma should be larger than zero.");
		}
		if (Math.abs(gamma - 1.0) < 0.00001) {
			return (ImagePlus) img.clone();
		}
		_gammatable = null;
		_gammatable = buildGammaTable(img.getType(), gamma);
	
		ImagePlus res = (ImagePlus) img.clone();
		int xSize = res.getWidth();
		int ySize = res.getHeight();
		int zSize = res.getNSlices();
		ImageStack imgStack = res.getStack();
		int val = 0;
				
		for(int z = 0; z < zSize; ++ z) {
			ImageProcessor proc = imgStack.getProcessor(z+1);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					val = _gammatable[(int) proc.getPixel(x, y)];
					proc.set(x, y, val);
				}
			}
		}
		
		return res;
	}

	/**
	 * Build a look-up table to save computation
	 * @param imageType
	 * @param gamma
	 * @return - an array of pairs (old intensity value, new intensity value)  
	 */
	protected int [] buildGammaTable(int imageType, double gamma){
		
		int dim = computeGammaTableDim(imageType);
		// test
		System.out.println("INFO: dim=" + dim + ", (int)Math.pow(2, dim)="
				+ (int) Math.pow(2, dim));
		
		int i;
		int[] gammatable = new int[(int) Math.pow(2, dim)];
		for (i = 0; i < gammatable.length; i++) {
			gammatable[i] = (int) ((gammatable.length - 1) * Math.pow(
					((double) i / (gammatable.length - 1)), (1.0 / gamma)));
	/*		System.out.println("i/gammatable="
					+ ((double) i / (gammatable.length - 1))
					+ ", 1./gamma="
					+ (1 / gamma)
					+ ", pow ="
					+ Math.pow(((double) i / (gammatable.length - 1)),
							(1.0 / gamma)));
*/
			if (gammatable[i] > (gammatable.length - 1)){
				System.out.println("larger than max="+gammatable.length+", i="+i+", i/gammatable="
						+ ((double) i / (gammatable.length - 1))
						+ ", 1./gamma="
						+ (1 / gamma)
						+ ", pow ="
						+ Math.pow(((double) i / (gammatable.length - 1)),
								(1.0 / gamma)));				
				gammatable[i] = gammatable.length - 1;
			}

			if (gammatable[i] < 0){
				System.out.println("less than zero i="+i+", i/gammatable="
						+ ((double) i / (gammatable.length - 1))
						+ ", 1./gamma="
						+ (1 / gamma)
						+ ", pow ="
						+ Math.pow(((double) i / (gammatable.length - 1)),
								(1.0 / gamma)));
				gammatable[i] = 0;
			}
			
/*			if (gammatable[i] != 0)
				System.out.println("INFO: gammatable[" + i + "]="
						+ gammatable[i]);*/
		}


		return gammatable;
	}
	/**
	 * 
	 * @param imageType
	 * @return
	 */
	private int computeGammaTableDim(int imageType){
		//////////////////////////////////
		// definitions from https://imagej.nih.gov/ij/source/ij/ImagePlus.java
/*		*//** 8-bit grayscale (unsigned)*//*
		public static final int GRAY8 = 0;		
		*//** 16-bit grayscale (unsigned) *//*
		public static final int GRAY16 = 1;		
		*//** 32-bit floating-point grayscale *//*
		public static final int GRAY32 = 2;	
		*//** 8-bit indexed color *//*
		public static final int COLOR_256 = 3;		
		*//** 32-bit RGB color *//*
		public static final int COLOR_RGB = 4;*/
		
		int dim = 8;
		switch (imageType) {
		case ImagePlus.GRAY8://TYPE_BYTE:
			dim = 8;
			break;
		case ImagePlus.GRAY16://TYPE_USHORT:
			dim = 16;
			break;
/*		case ImagePlus.TYPE_SHORT:
			dim = 16;
			break;*/
		case ImagePlus.COLOR_RGB://TYPE_INT:
			dim = 32;
			break;
/*		case ImagePlus.TYPE_LONG:
			dim = 64;
			break;*/

		default:
			_logger.info("the image type is different from BYTE, SHORT and INT: assiging 8 bits per value");
			dim = 8;
			break;
		}
		// test
/*		System.out.println("INFO: dim=" + dim + ", (int)Math.pow(2, dim)="
				+ (int) Math.pow(2, dim));*/
		//System.out.println("INFO: max = " + img.getMax() + ", min="+ img.getMin());

	 return dim;
	}
	/**
	 * This method applies sweeps the gamma parameter and applies
	 * the gamma correction to an input intensity image 
	 * @param InFileNameImage - input image
	 * @param OutFileName - output image
	 * @return false/true
	 * @throws Exception
	 */
	public boolean applyGammaCorrection(String InFileNameImage,
			String OutFileName) throws Exception {

		Nifti_Reader niftiLoaderCell = new Nifti_Reader();
		ImagePlus img  = null;
		if(InFileNameImage.endsWith(".nii")){
			img = niftiLoaderCell.read(InFileNameImage);		
		}else{
			if(InFileNameImage.endsWith(".fits")){
				img = new ImagePlus(InFileNameImage); 
			}else{
				_logger.info("did not recognize the input file format ...");
				System.out.println("failed loading  image...");
				return false;
			}
		}		
		System.out.println("INFO: File Name = " + InFileNameImage);

		String rootFileName = new String(OutFileName);
		String suffix = new String();
		if (OutFileName.endsWith(".nii")) {
			rootFileName = OutFileName.substring(0, OutFileName.length() - 4);
			suffix = ".nii";
		}else{
			if (OutFileName.endsWith(".fits")) {
				rootFileName = OutFileName.substring(0, OutFileName.length() - 5);
				suffix = ".fits";
			}else{
				_logger.info("did not recognize the output file format ...");
				System.out.println("failed format ...");
				return false;				
			}
		}
	
		ImagePlus res = null;

		String gammaText = null;
		double gamma;
		for (gamma = getMinGamma(); gamma <= getMaxGamma(); gamma += getDeltaGamma()) {
			res = applyGammaCorrection(img, gamma);
			// display and write the result
			gammaText = Double.toString(Math.round(gamma*1000)/1000.0);       
	        int dot = gammaText.indexOf('.');
	        gammaText = gammaText.substring(0, dot) + gammaText.substring(dot +1, dot+2);;
	        
/*			ImDisplay.displayImg(res, "Result " + gammaText );
			String outfile = rootFileName + "-gammaCorrection" + gammaText
					+ ".tif";
			ImageLoader.writeImage(outfile, res);
			System.out.println("INFO: Output File Name = " + outfile);*/
			
			String outfile = rootFileName + "-GammaCorrection" + gammaText
					+ suffix;
			if(suffix.equalsIgnoreCase(".nii")){
				Nifti_Writer nifti = new Nifti_Writer();
				String outputType = new String("::NIFTI_FILE:");
				//nifti.write(res, outfile, outputType);// used by gamma correction experiments
				nifti.write(res, OutFileName, outputType);
				
			}else{
				if(suffix.equalsIgnoreCase(".fits")){
					//Fits3DWriter.write(outfile, res);		// used by gamma correction experiments			
					Fits3DWriter.write(OutFileName, res);
				}else{
					_logger.info("did not recognize the output file format ...");
					System.out.println("failed saving   image...");
					return false;
				}
			}
	
		}

		return true;
	}
	public static void batchProcessManyFolders() throws Exception{

		String inputScaffoldImagesFolder, outputDirectory;		
		String inputSuffix = "_ch01.fits";
		
		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07102015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);
		
		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/MediumMicrofiber-07232015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);

		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-06252015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);
		
		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/Microfiber-07072015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);

		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-06252015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);

		inputScaffoldImagesFolder = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/FITS";
		outputDirectory = "//itlnas/bio-data/CarlSimon/Cell-scafffold-contact/Fluoro-Fiber-Images/Segmentations/SpunCoat-07072015_ZProfile/NII_ch01/Gamma14";
		batchProcessOneFolder(inputScaffoldImagesFolder, inputSuffix, outputDirectory);
		
		
	}
	public static void batchProcessOneFolder(String inputScaffoldImagesFolder, String inputSuffix, String outputDirectory) throws Exception{

		GammaCorrection myTest = new GammaCorrection();
		myTest.setDeltaGamma(1.0);
		myTest.setMinGamma(1.4);
		myTest.setMaxGamma(1.4);
	
		// getting images to process
		//Collection<String> dirfiles = FileOper.readSubDirectories(inputImagesFolder);
		Collection<String> dirFiles = FileOper.readFileDirectory(inputScaffoldImagesFolder);		
		Collection<String> selectedFiles = FileOper.selectFileType(dirFiles,inputSuffix);		
		String inputScaffoldFilename = new String();
		String OutFileName = new String();
		for (Iterator<String> k = selectedFiles.iterator(); k.hasNext();) {
			inputScaffoldFilename = k.next();
			OutFileName = outputDirectory + File.separatorChar + new File(inputScaffoldFilename).getName();
			// the web visualization (movie creator code can work only with .nii files)
			OutFileName = OutFileName.substring(0, OutFileName.length()-5) + ".nii";

			System.out.println("open scaffold file: " + inputScaffoldFilename);								
			myTest.applyGammaCorrection(inputScaffoldFilename, OutFileName);			
			System.out.println("save gamma corrected file: "+ OutFileName);			
		}
	}
	
	public static void main(String args[]) throws Exception {
/*
		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 2)) {
			System.out
					.println("Please, specify the input image name,  output file name");
			// System.out.println("arg = Output_ImageName");
			return;
		}

	
  		this code was used for generating gamma correction samples for visual evaluation
  		
  		String OutFileName,  InFileNameImage;
		Boolean ret = true;

		InFileNameImage = args[0];
		System.out.println(InFileNameImage);

		OutFileName = args[1];
		System.out.println(OutFileName);

		boolean testPassed = true;
		GammaCorrection myTest = new GammaCorrection();
		myTest.setDeltaGamma(0.1);
		myTest.setMinGamma(1.0);
		myTest.setMaxGamma(3.0);

		myTest.applyGammaCorrection(InFileNameImage, OutFileName);

		System.out.println("Test Result = " + testPassed);*/
		
		GammaCorrection.batchProcessManyFolders();
	}


}
