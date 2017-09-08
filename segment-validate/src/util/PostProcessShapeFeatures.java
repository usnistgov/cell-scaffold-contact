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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * This is a class for filter Shape features or Foreground counts CSV files 
 * according to a list of usable cells
 * 
 * @author Mylene Simon
 *
 */
public class PostProcessShapeFeatures {

	/**
	 * Filter Shape features or Foreground counts CSV files according to a list
	 * of usable cells
	 * 
	 * @param featuresFile
	 *            CSV input file
	 * @param usableCellsFile
	 *            CSV containing list of usable cells
	 * @param outputFile
	 *            Output file path and name
	 * @param imageFileExtension
	 *            File extension of the cell image names (i.e. ".fits")
	 */
	static void filterUsableCells(String featuresFile, String usableCellsFile, 
			String outputFile, String imageFileExtension) {
		
		List<String> usableCells = new ArrayList<String>();
		
		// Read CSV files to filter them according to usable cells and write 
		// results in output file
		try {
		
			BufferedReader br = null;
			String line = "";
		 
			br = new BufferedReader(new FileReader(usableCellsFile));
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("File_Name")) {
				System.err.println("Usable cells CSV input file should start "
						+ "with the following header line: ");
				System.err.println("File_Name");
				if (br != null) br.close();
				return;
			}
			
			// Read usable cells CSV file
			while ((line = br.readLine()) != null) {
	 
				usableCells.add(line);
			}
			
			if (br != null) br.close();		
			
			br = new BufferedReader(new InputStreamReader(
	                  new FileInputStream(new File(featuresFile)), 
	                  StandardCharsets.ISO_8859_1));
			String cvsSplitBy = ",";
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("File_Name")) {
				System.err.println("Features CSV input file should start with "
						+ "the following header line: ");
				System.err.println("File_Name");
				if (br != null) br.close();
				return;
			}
			
			// Open printer for output file
			PrintStream writer;
			writer = new PrintStream(new FileOutputStream(new File(outputFile)), 
					true, "ISO_8859_1");
				
			// Output file header
			writer.append(line);
			writer.append('\n');
			
			// Read input CSV file
			while ((line = br.readLine()) != null) {
	 
				String[] cellFeatures = line.split(cvsSplitBy);
				String cellName = cellFeatures[0].substring(
						cellFeatures[0].lastIndexOf(File.separatorChar) + 1, 
						cellFeatures[0].lastIndexOf(imageFileExtension));
				
				// if line corresponds to usable cell, write in output file
				if(usableCells.contains(cellName)) {
					
					writer.append(cellName);
					
					for(int i = 1; i < cellFeatures.length; ++i) {
						writer.append(',');
						writer.append(cellFeatures[i]);
					}
					
					writer.append('\n');
				}
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
		}
	}
	
	/**
	 * Filter Shape features or Foreground counts CSV files according to a list
	 * of usable cells
	 * 
	 * @param featuresFile
	 *            CSV input file
	 * @param usableCellsFile
	 *            CSV containing list of usable cells
	 * @param outputFile
	 *            Output file path and name
	 * @param imageFileExtension
	 *            File extension of the cell image names (i.e. ".fits")
	 */
	static void addSegExtensionCells(String featuresFile,
			String outputFile) {
		
		
		// Read CSV files to filter them according to usable cells and write 
		// results in output file
		try {
		
			BufferedReader br = null;
			String line = "";
			
			br = new BufferedReader(new InputStreamReader(
	                  new FileInputStream(new File(featuresFile)), 
	                  StandardCharsets.ISO_8859_1));
			String cvsSplitBy = ",";
			
			// Skip header line
			line = br.readLine();
			// If no header line is present, the file might be in a wrong
			// format, stop processing
			if (line == null || !line.startsWith("File_Name")) {
				System.err.println("Features CSV input file should start with "
						+ "the following header line: ");
				System.err.println("File_Name");
				if (br != null) br.close();
				return;
			}
			
			// Open printer for output file
			PrintStream writer;
			writer = new PrintStream(new FileOutputStream(new File(outputFile)), 
					true, "ISO_8859_1");
				
			// Output file header
			writer.append(line);
			writer.append('\n');
			
			// Read input CSV file
			while ((line = br.readLine()) != null) {
	 
				String[] cellFeatures = line.split(cvsSplitBy);
				
				writer.append(cellFeatures[0] + "_seg");
				
				for(int i = 1; i < cellFeatures.length; ++i) {
					writer.append(',');
					writer.append(cellFeatures[i]);
				}
				
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
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// initializations
		String featuresFile = args[0];
		String usableCellsFile = args[1];
		String outputFile = args[2];
		String imageFileExtension = args[3];
		
		filterUsableCells(featuresFile, usableCellsFile, outputFile, 
				imageFileExtension);
		
		//addSegExtensionCells(featuresFile, outputFile);
	}

}
