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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;


/**
 * @author pnb
 * This class loads reference lines for single fiber analysis that are defined by a sequence of points
 * The points are obtained from ImageJ/Fiji overlay tools
 *
 */
public class ReferenceLineLoader {

	/**
	 * 
	 * @param TXTFilename
	 * @param delim
	 * @param colName - boolean to indicate whether column names are present
	 * @param headerRows - number of rows that contain the header
	 * @throws Exception
	 */
			
	public static double []  readData(String TXTFilename, String delim, boolean colName,
				int headerRows) throws Exception {
			// System.out.println("Start readTable");

			FileReader fr = new FileReader(TXTFilename);
			BufferedReader reader = new BufferedReader(fr);
			
			int numRows = headerRows;
			boolean firstLine;
			if (colName) {
				firstLine = true;
			} else {
				firstLine = false;
			}

			int numLines = 0;
			int numColumns = 0;

			
			String line = null;
			line = reader.readLine();
			while (line != null && line.length() != 0) {
				if (firstLine) {
					if (numRows == 1)
						firstLine = false;
					numRows--;
				} else {
					numLines++;
				}
				int numTokens = line.split(delim).length;
				if (numColumns < numTokens) {
					numColumns = numTokens;
				}
				line = reader.readLine();
			}
			System.out.println("number of lines =" + numLines);
			if(numLines < 1){
				System.err.println("missing data");
				reader.close();
				return null;
			}
			int i,j;
			// now we know the number of lines.
			fr = new FileReader(TXTFilename);
			reader = new BufferedReader(fr);
			if (colName) {
				firstLine = true;
				numRows = headerRows;
				while ((line = reader.readLine()) != null && line.length() != 0) {
					String[] header_tokens = null;
					header_tokens = line.split(delim);
					System.out.println("header info:");
					for(i = 0; i <header_tokens.length; i++){
						System.out.println("header["+i+"]="+header_tokens[i]);					
					}
					numRows--;
				}
			}
			// read the data
			int curRow = 0;
			double [] data = new double[numLines<<1];
			String[] tokens;
			line = reader.readLine();
			for(j=0; j< numLines;j++){
				tokens = line.split(delim);
				if(tokens.length != 2){
					System.err.println("expected two entries (X, Y) coordinates. Retrieved numEntries="+tokens.length);
					reader.close();
					return null;					
				}
				i=0;
				//System.out.println("value["+i+"]="+tokens[i]);
				//System.out.println("value["+(i+1)+"]="+tokens[i+1]);
				data[curRow<<1] = Double.parseDouble(tokens[i]);
				data[(curRow<<1)+1] = Double.parseDouble(tokens[i+1]);
				curRow++;
				line = reader.readLine();
			}
			reader.close();
			return data;
	}

	/** 
	 * this method saves values in two arrays into a csv file
	 * used for single fiber analysis (cell-scaffold project)
	 *  
	 * @param arr1
	 * @param arr2
	 * @param OutFileName
	 * @return
	 * @throws IOException
	 */
	public static boolean saveArray(int[] arr1, int [] arr2, String header, String OutFileName)
			throws IOException {
		// sanity check
		if (arr1 == null || arr2 == null || OutFileName == null) {
			System.err.println("missing inputs in saveArray");
			return false;
		}
		int length = arr1.length;
		if(arr1.length > arr2.length){
			length = arr2.length;			
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);
		if(header!= null){
			out.write(header+"\n");
		}
		for (int i = 0; i < length; i++) {
			out.write(i+","+arr1[i] + ", "+arr2[i] +"\n");
		}
		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 1)) {
			System.out.println("Please, specify the input TXT file name");
			System.out.println("arg = Input TXT file Name with (X,Y) coordinates per row");
			return;
		}
		
		String TXTFilename = args[0];
		String delim = new String("\t");
		boolean colName = false;
		int headerRows = 0;
		
		double [] arr = ReferenceLineLoader.readData(TXTFilename, delim, colName, headerRows);
		for (i = 0; i<arr.length;i+=2){
			System.out.println("X="+arr[i]+", Y="+arr[i+1]);
		}
		

	}

}
