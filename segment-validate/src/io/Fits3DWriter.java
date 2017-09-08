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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;

import util.FileOper;

/**
 * This is a class for saving 3D 8-bits FITS images
 * 
 * @author Mylene Simon, Peter Bajcsy
 * @see IJ.FITS_Writer added loading one z-stack from a set of tiff files and
 *      converting it FITS files and performing this over a set of
 *      folders/z-stacks
 *
 */
public class Fits3DWriter {

	/**
	 * This method loads a z-stack from a folders with tif images
	 * 
	 * @param sourceFilePath
	 *            - path to a directory with tif files
	 * @return ImagePlus data structure
	 */
	public static ImagePlus loadZstack(String sourceFilePath, String inputFilter) {

		Collection<String> dirfiles = FileOper
				.readFileDirectory(sourceFilePath);

		System.out.println("Directory: Number of files=" + dirfiles.size());

		Collection<String> onlyFilter = FileOper.selectFileType(dirfiles,
				inputFilter);
		// FileOper.printCollection(onlyFilter);
		// System.out.println();

		Collection<String> sortedFilter = FileOper.sort(onlyFilter,
				FileOper.SORT_ASCENDING);
		if (sortedFilter == null) {
			System.out.println("Did not find a match to file specifications ="
					+ inputFilter + " in a folder=" + sourceFilePath);
			return null;
		}
		FileOper.printCollection(sortedFilter);
		System.out.println();

		int w = 128;
		int h = 128;
		ImageStack stack = null;// new ImageStack(w,h);
		// ImagePlus stack = new ImagePlus();

		int index = 0;
		String inputFilename = new String();
		for (Iterator<String> k = sortedFilter.iterator(); k.hasNext();) {
			inputFilename = k.next();
			try {
				ImagePlus imgSlice = IJ.openImage(inputFilename);
				System.out.println(inputFilename);
				// System.out.println("File processing start time: "+ new
				// Date().toString());

				if (imgSlice == null) {
					System.err
							.println("Could not load file = " + inputFilename);
				} else {

					if (index == 0) {
						// initialize ImageStack based on the width and height
						// of the first frame
						w = imgSlice.getProcessor().getWidth();
						h = imgSlice.getProcessor().getHeight();
						stack = new ImageStack(w, h);
					}

					int bitDepth = imgSlice.getProcessor().getBitDepth();
					switch (bitDepth) {
					case 8:
						byte[] p8 = (byte[]) imgSlice.getProcessor()
								.getPixels();
						stack.addSlice(Integer.toString(index), p8);
						break;
					case 16:
						short[] p16 = (short[]) imgSlice.getProcessor()
								.getPixels();
						stack.addSlice(Integer.toString(index), p16);
						break;

					default:
						byte[] p = (byte[]) imgSlice.getProcessor().getPixels();
						stack.addSlice(Integer.toString(index), p);
						break;
					}

				}

			} catch (Exception e) {
				System.err.println("IOException: Could not load file = "
						+ inputFilename);
				e.printStackTrace();
			}
			index++;
		}
		ImagePlus image = new ImagePlus("stack", stack);
		// for debugging purposes
		// image.show();
		return image;

	}

	/**
	 * Write the 3D 8bpp or 16bpp ImagePlus in a FITS file
	 * 
	 * @param destFilePath
	 *            Path for the destination file
	 * @param imp
	 *            ImagePlus 3D image to save
	 */
	public static void write(String destFilePath, ImagePlus imp) {

		// Check if this is an 8-bits image
		int bitDepth = imp.getBitDepth();
		if (bitDepth != 8 && bitDepth != 16) {
			System.err.println("Only 3D 8-bpp or 16bpp images are supported.");
			return;
		}

		// Create file, overwrite if already exists
		File f = new File(destFilePath);
		if (f.exists())
			f.delete();

		// Compute filler length to put at the end of the image data
		// (Fits data are blocks of 2880 bytes)
		int numBytes = imp.getBytesPerPixel();
		int fillerLength = 2880 - ((numBytes * imp.getWidth() * imp.getHeight() * imp
				.getNSlices()) % 2880);

		// Create header for the Fits file
		createHeader(destFilePath, imp, bitDepth);

		// Write data
		writeData3D(destFilePath, imp);
		char[] endFiller = new char[fillerLength];
		appendFile(endFiller, destFilePath);
	}

	/**
	 * Creates a FITS header for the image
	 */
	private static void createHeader(String path, ImagePlus imp, int bitPix) {

		int numCards = 14;
		String bitperpix = " " + bitPix;
		double cdelt1 = imp.getCalibration().pixelWidth;
		double cdelt2 = imp.getCalibration().pixelHeight;
		double cdelt3 = imp.getCalibration().pixelDepth;
		String ctype1 = imp.getCalibration().getXUnit();
		String ctype2 = imp.getCalibration().getYUnit();
		String ctype3 = imp.getCalibration().getZUnit();

		// Conversions for the micro symbol (avoid encoding problems)
		if (ctype1.equals("µm"))
			ctype1 = "um";
		if (ctype2.equals("µm"))
			ctype2 = "um";
		if (ctype3.equals("µm"))
			ctype3 = "um";

		appendFile(writeCard("SIMPLE", " T", "Created by NIST with ImageJ"),
				path);
		appendFile(
				writeCard("BITPIX", bitperpix, "number of bits per data pixel"),
				path);
		appendFile(writeCard("NAXIS", " 3", "number of data axes"), path);
		appendFile(
				writeCard("NAXIS1", " " + imp.getWidth(),
						"length of data axis 1"), path);
		appendFile(
				writeCard("NAXIS2", " " + imp.getHeight(),
						"length of data axis 2"), path);
		appendFile(
				writeCard("NAXIS3", " " + imp.getNSlices(),
						"length of data axis 3"), path);

		appendFile(
				writeCard("CDELT1",
						" " + new DecimalFormat("#0.000").format(cdelt1),
						"length of each pixel along axis 1"), path);
		appendFile(
				writeCard("CDELT2",
						" " + new DecimalFormat("#0.000").format(cdelt2),
						"length of each pixel along axis 2"), path);
		appendFile(
				writeCard("CDELT3",
						" " + new DecimalFormat("#0.000").format(cdelt3),
						"length of each pixel along axis 3"), path);
		appendFile(writeCard("CTYPE1", " " + ctype1, "units along axis 1"),
				path);
		appendFile(writeCard("CTYPE2", " " + ctype2, "units along axis 2"),
				path);
		appendFile(writeCard("CTYPE3", " " + ctype3, "units along axis 3"),
				path);

		if(bitPix==8)
			appendFile(writeCard("BZERO", " -128", "data range offset"), path);
		else
			appendFile(writeCard("BZERO", "-32768", "data range offset"), path);
		
		appendFile(writeCard("BSCALE", " 1", "default scaling factor"), path);

		int fillerSize = 2880 - ((numCards * 80 + 3) % 2880);
		char[] end = new char[3];
		end[0] = 'E';
		end[1] = 'N';
		end[2] = 'D';
		char[] filler = new char[fillerSize];
		for (int i = 0; i < fillerSize; i++)
			filler[i] = ' ';
		appendFile(end, path);
		appendFile(filler, path);
	}

	/**
	 * Writes one line of a FITS header
	 */
	private static char[] writeCard(String title, String value, String comment) {
		char[] card = new char[80];
		for (int i = 0; i < 80; i++)
			card[i] = ' ';
		s2ch(title, card, 0);
		card[8] = '=';
		s2ch(value, card, 10);
		card[31] = '/';
		card[32] = ' ';
		s2ch(comment, card, 33);
		return card;
	}

	/**
	 * Converts a String to a char[]
	 */
	private static void s2ch(String str, char[] ch, int offset) {
		int j = 0;
		for (int i = offset; i < 80 && i < str.length() + offset; i++)
			ch[i] = str.charAt(j++);
	}

	/**
	 * Appends 'line' to the end of the file specified by 'path'.
	 */
	private static void appendFile(char[] line, String path) {
		try {
			FileWriter output = new FileWriter(path, true);
			output.write(line);
			output.close();
		} catch (IOException e) {
			System.err.println("Error writing Fits file.");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Appends the data of the current 3D image to the end of the file specified
	 * by path.
	 */
	private static void writeData3D(String path, ImagePlus imp) {
		int w = imp.getWidth();
		int h = imp.getHeight();

		ImageStack stack = imp.getStack(); // origin is at bottom left corner

		for (int ipNumber = 1; ipNumber <= stack.getSize(); ipNumber++) {

			int bitDepth = stack.getProcessor(ipNumber).getBitDepth();

			try {
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(path,
								true)));
				switch (bitDepth) {
				case 8:
					byte[] pixels = (byte[]) stack.getProcessor(ipNumber)
							.getPixels();
					for (int i = h - 1; i >= 0; i--)
						for (int j = i * w; j < w * (i + 1); j++)
							dos.write(pixels[j] & 0xff);
					dos.close();
					break;
				case 16:
					short[] pixelsShort = (short[]) stack
							.getProcessor(ipNumber).getPixels();
					for (int i = h - 1; i >= 0; i--)
						for (int j = i * w; j < w * (i + 1); j++) {
							// according to
							// http://www.ifa.hawaii.edu/~kaiser/imcat/byteorder.html
							// FITS supports by default big endian ordering but only unsigned short type for 16bpp!!!!
							//therefore input  unsigned short from TIFF files  --> output signed short for FITS files
							int signedShort = pixelsShort[j]  - 32768;
							byte lowByte = (byte) (signedShort & 0xff);
							byte highByte = (byte) ((signedShort >> 8) & 0xff);		
							
							/*int unsignedShort = pixelsShort[j] & 0xffff;
							byte lowByte = (byte) (unsignedShort & 0xff);
							byte highByte = (byte) ((unsignedShort >> 8) & 0xff);
							*/
							// big endian order
							dos.write(highByte );
							dos.write(lowByte);
						}
					dos.close();
					break;
				default:
					byte[] pixelsDefault = (byte[]) stack
							.getProcessor(ipNumber).getPixels();
					for (int i = h - 1; i >= 0; i--)
						for (int j = i * w; j < w * (i + 1); j++)
							dos.write(pixelsDefault[j] & 0xff);
					dos.close();
					break;
				}
			} catch (IOException e) {
				System.err.println("Error writing Fits file.");
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * This method converts a set of tif files in a folder to a FITS file that
	 * represents that zstack The computation works over a set of folders and
	 * generates one FITS file per folder
	 * 
	 * @param sourceDirPath
	 *            - input folder with sub-folders containing frames of each
	 *            z-stack
	 * @param destFilePath
	 *            - output file directory for the FITS files
	 */
	public static void convertZstackToFITS(String sourceDirPath,
			String destFilePath, String inputFilter) {
		Collection<String> dirfiles = FileOper
				.readSubDirectories(sourceDirPath);

		System.out.println("Directory: Number of directories="
				+ dirfiles.size());
		FileOper.printCollection(dirfiles);
		System.out.println();

		String inputDirPath = new String();
		for (Iterator<String> k = dirfiles.iterator(); k.hasNext();) {
			inputDirPath = k.next();
			File temp = new File(inputDirPath);
			if (temp.isDirectory()) {
				// process all directories
				ImagePlus loaded = Fits3DWriter.loadZstack(inputDirPath,
						inputFilter);
				if (loaded != null) {
					String fitsFileName = inputDirPath.substring(
							sourceDirPath.length(), inputDirPath.length());
					fitsFileName = destFilePath + fitsFileName + ".fits";
					Fits3DWriter.write(fitsFileName, loaded);
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args == null || args.length < 3) {
			System.err
					.println("specify sourceFilePath, destFilePath, and inputFilter");
			return;
		}
		String sourceDirPath = args[0];
		// String sourceFilePath = new
		// String("C:\\PeterB\\Presentations\\Papers\\3Dsegmentation\\JournalOfMicroscopy\\review04-13-2015\\simulations\\Experiment2\\addData\\Gaussian60");
		String destFilePath = args[1];
		String inputFilter = args[2];// new String("tif");
		// converts a set of z-stacks
		// Fits3DWriter.convertZstackToFITS(sourceDirPath, destFilePath,
		// inputFilter);

		// converts one zstack
		ImagePlus loaded = Fits3DWriter.loadZstack(sourceDirPath, inputFilter);
		if (loaded != null) {
			int beginIndex = sourceDirPath.lastIndexOf("\\");
			String fitsFileName = sourceDirPath.substring(beginIndex,
					sourceDirPath.length());
			System.out.println("fitsFileName1 =" + fitsFileName);
			fitsFileName = destFilePath + fitsFileName + ".fits";
			System.out.println("fitsFileName2 =" + fitsFileName);
			Fits3DWriter.write(fitsFileName, loaded);
		}

	}

}
