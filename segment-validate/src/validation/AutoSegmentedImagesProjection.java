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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import util.FileOper;

/**
 * This is a class for going through the auto segmented images and computing
 * their orthogonal projections
 * 
 * @author Mylene Simon
 * 
 */
public class AutoSegmentedImagesProjection {

	/**
	 * Go through the directory where auto segmented images are stored in sub
	 * directories and compute orthogonal projections
	 * 
	 * @param autoSegmentedImagesDir
	 *            The directory where auto segmented images are stored in sub
	 *            directories
	 * @param imageFormatExtension
	 *            The image file extension (i.e. ".fits")
	 * @param orthogonalProjectionsDirSuffix
	 *            The suffix for output directories (i.e.
	 *            "OrthogonalProjections")
	 */
	public void browseAutoSegmentedImagesAndComputeOrthogonalProjections(
			String autoSegmentedImagesDir, String imageFormatExtension,
			String orthogonalProjectionsDirSuffix) {

		// Get all sub directories of auto segmented images directory
		Collection<String> subDirectories = FileOper
				.readSubDirectories(autoSegmentedImagesDir);

		// For each sub directory, get all files matching image format extension
		for (Iterator<String> itSubDir = subDirectories.iterator(); itSubDir
				.hasNext();) {
			
			String subDirName = itSubDir.next();
			Collection<String> filesInSubDirectory = FileOper
					.readFileDirectory(subDirName);
			Collection<String> imagesToProcess = FileOper.selectFileType(
					filesInSubDirectory, imageFormatExtension);

			// Compute orthogonal projections of each image and save them in
			// specific sub directory
			for (Iterator<String> itImagesToProcess = imagesToProcess
					.iterator(); itImagesToProcess.hasNext();) {
				
				computeOrthogonalProjections(itImagesToProcess.next(),
						subDirName, orthogonalProjectionsDirSuffix);
			}
		}

	}
	
	/**
	 * Call the OrthogonalProjection class to compute the orthogonal projections
	 * of the file
	 * 
	 * @param fileName
	 *            The path of the input file
	 * @param outputDir
	 *            The output directory (specific sub directory will be created
	 *            in this one)
	 * @param orthogonalProjectionsDirSuffix
	 *            The suffix of the specific output directory for the
	 *            projections
	 */
	private void computeOrthogonalProjections(String fileName,
			String outputDir, String orthogonalProjectionsDirSuffix) {

		// Construct name of the output directory for the projections
		int indexLastFileSeparatorChar = fileName
				.lastIndexOf(File.separatorChar);
		int indexLastDot = fileName.lastIndexOf(".");
		String directoryPrefix = fileName.substring(indexLastFileSeparatorChar,
				indexLastDot);

		// Construct path of the output directory for the projections
		String outputSpecificDir = outputDir + File.separatorChar
				+ directoryPrefix + "_" + orthogonalProjectionsDirSuffix;

		// If the directory doesn't exist, the directory (and parent directories
		// if necessary) is created
		File outputTest = new File(outputSpecificDir);
		if (!outputTest.exists()) {
			outputTest.mkdirs();
		}
		
		// Launch orthogonal projections
		System.out.println("Processing orthogonal projections for " + fileName);
		OrthogonalProjection.processOneFile(fileName, outputSpecificDir,
				OrthogonalProjection.projectionType_Max);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length != 3)) {
			System.out
					.println("Please, specify the directory where auto segmented"
							+ " images are stored in sub directories, the image"
							+ " file extension (i.e. \".fits\"), and the suffix"
							+ " for output directories (i.e. "
							+ "\"OrthogonalProjections\")");

			return;
		}

		// get arguments
		String autoSegmentedImagesDir = new String(args[0]);
		String imageFormatExtension = new String(args[1]);
		String orthogonalProjectionsDirSuffix = new String(args[2]);
		
		PrintStream out;

		try {
			// redirect console output in file
			out = new PrintStream(new FileOutputStream(autoSegmentedImagesDir
					+ File.separatorChar 
					+ "autoSegmentedImagesProjections_consoleOutput.txt"));
			System.setOut(out);
			System.setErr(out);
			
			// start time for benchmark
		    long startTime = System.currentTimeMillis();
			System.out
					.println("Start processing auto segmented images contained "
							+ "in sub directries of directory "
							+ autoSegmentedImagesDir + "...");
		
			// compute projections for auto segmented images
			AutoSegmentedImagesProjection myAutoSegmentedImagesProjection = 
					new AutoSegmentedImagesProjection();
			
			myAutoSegmentedImagesProjection
					.browseAutoSegmentedImagesAndComputeOrthogonalProjections(
							autoSegmentedImagesDir, imageFormatExtension,
							orthogonalProjectionsDirSuffix);
		
			// end time for benchmark
		    long endTime = System.currentTimeMillis();
			System.out
					.println("Auto segmented images projection execution time "
							+ "for directory "
							+ autoSegmentedImagesDir
							+ ": "
							+ (endTime - startTime)
							+ " millisecond.");
			System.out.println();
			
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: Could not open file = "
					+ autoSegmentedImagesDir 
					+ File.separatorChar 
					+ "autoSegmentedImagesProjections_consoleOutput.txt");
			e.printStackTrace();
		}

	}

}
