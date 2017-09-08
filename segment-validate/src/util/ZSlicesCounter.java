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

import ij.ImagePlus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * This a class to count the number of z-frames per z-stacks for a given scaffold
 * 
 * @author Mylene Simon
 *
 */
public class ZSlicesCounter {

	/**
	 * Count the number of Z frames per z stack for one scaffold in the folder
	 * where the FITS files are stored (FITS files of interest stored in
	 * fitsDir/scaffold)
	 * 
	 * @param fitsDir
	 *            The folder where the FITS files (for all scaffold) are stored
	 * @param scaffold
	 *            The scaffold of interest (FITS files of interest stored in
	 *            fitsDir/scaffold)
	 * @param outputDir
	 *            Output directory for CSV files
	 */
	public static void countZframesPerZstackForScaffold(String fitsDir,
			String scaffold, String outputDir) {
		
		final String fitsExtension = ".fits";
		final String csvExtension = "_ZSlicesCount.csv";
		
		System.out.println(scaffold);
		
		// Get files in scaffold fits directory
		Collection<String> fitsDirFiles = FileOper.readFileDirectory(fitsDir
				+ File.separatorChar + scaffold);
		// keep only fits files
		Collection<String> scaffoldFITSFiles = FileOper.selectFileType(
				fitsDirFiles, fitsExtension);
		
		// file writer to write results in CSV file
		FileWriter writer;
				
		// write z slices counts in CSV file
		try {
			writer = new FileWriter(outputDir 
					+ File.separatorChar
					+ scaffold
					+ csvExtension);
			
			// Header
			writer.append(scaffold);
			writer.append('\n');
			
			// iterate over fits files to get number of z slices per file
			for (Iterator<String> k = scaffoldFITSFiles.iterator(); k.hasNext();) {
						
				// get file path
				String fileName = k.next();
				// open file as ImagePlus object
				ImagePlus img = new ImagePlus(fileName);
				// print number of slices in image
				writer.append(String.valueOf(img.getNSlices()));
				writer.append('\n');
			}
		    
		    writer.flush();
		    writer.close();
		    
		} catch (IOException e) {
			System.err.println("Error while saving the z slices counts in CSV "
					+ "file");
			e.printStackTrace();
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// initializations
		String fitsDir = args[0];
		String scaffold = args[1];
		String outputDir = args[2];
		
		// count zslices for the scaffold
		ZSlicesCounter.countZframesPerZstackForScaffold(fitsDir, scaffold,
				outputDir);
		
	}

}
