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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Vector;



/**
 * 
 * This class is designed to read skeleton and radius estimates provided by VesselKnife
 * https://gitlab.com/vesselknife/vesselknife
 * 	WARNING: the skeleton coordinates provided by VesselKnife are w.r.t. a coordinate system with [0,0] located in the left LOWER  image corner
 * 		
 * @author pnb
 *
 */
public class TubeSkeletonLoader {

	private int _numOfAllNodes = 0 ;
	private int _numOfHeaderLines = 0;
	// data structure to represent the loaded data
	public class Skeleton{
		public double X = 0.0;//column
		public double Y = 0.0;//row
		public double Z = 0.0; //depth
		public int n = 0;
		public double radius = 0.0;
		public String toString(){
			String str = new String();
			str = "X="+Double.toString(X) + ", Y=" +Double.toString(Y) + ", Z="+Double.toString(Z) + "\n";
			str +="n=" +Integer.toString(n) +", radius="+Double.toString(radius) +"\t";
			return str;
		}
	}

	public TubeSkeletonLoader(){
		_numOfAllNodes = 0 ;
		_numOfHeaderLines = 0;		
	}
	
	public int getNumOfHeaderLines(){
		return _numOfHeaderLines;
	}
	public int getNumOfAllNodes(){
		return _numOfAllNodes;
	}
	

	/**
	 * This method just counts the number of header lines starting with AT sign
	 * and extracts the number of nodes
	 * 
	 * @author pnb
	 * 
	 * @param TXTFilename
	 * @return
	 * @throws IOException
	 */
	public int readNumberOfAllNodes(String TXTFilename) throws IOException{
		FileReader fr = new FileReader(TXTFilename);
		BufferedReader reader = new BufferedReader(fr);

		_numOfAllNodes = 0 ;
		_numOfHeaderLines = 0;
		String line = null;
		line = reader.readLine();
		while (line != null && line.length() != 0) {
			if(line.startsWith("@") ){
				_numOfHeaderLines++;
				if(line.startsWith("@NumberOfAllNodes")){
					int beginIndex = "@NumberOfAllNodes ".length();
					String temp = line.substring(beginIndex, line.length());
					_numOfAllNodes = Integer.parseInt(temp);					
				}							
				line = reader.readLine();
			}else{
				line = null;
			}
		}
		//System.out.println("number of OfAllNodes =" + _numOfAllNodes);
		reader.close();		
		return _numOfAllNodes;
	}

	/**
	 * This method reads all nosed 
	 * @param TXTFilename - input text file with skeleton node entries 
	 * @return - Skeleton is a public class that encapsulates the five values of 3D skeleton and its radius
	 * 
	 * @throws Exception
	 */
	public  Skeleton []  readNodes(String TXTFilename) throws Exception {
		
		// these are the settings for the file structures from VesselKnife
		// https://gitlab.com/vesselknife/vesselknife
		String delim = " ";
		boolean colName = false;
		int headerRows = 2;
		
		System.out.println("Start readNodes");
		int numNodes = readNumberOfAllNodes(TXTFilename);
		if(numNodes < 1){
			System.err.println("missing data");
			return null;
		}
		int numRows = headerRows;
		if(headerRows != getNumOfHeaderLines()){
			numRows = getNumOfHeaderLines();			
			System.out.println("headerRows="+headerRows+", detectedHeaderRows= "+numRows);
		}

		
		FileReader fr = new FileReader(TXTFilename);
		BufferedReader reader = new BufferedReader(fr);
		int i,j;
		int numColumns = 5;
		System.out.println("number of Nodes =" + numNodes);
		String line = null;
		// skip the header rows
		for(i=0;i<numRows;i++){
			line = reader.readLine();			
		}
		// read the data
		Skeleton [] data = new Skeleton[numNodes];
		String[] tokens;
		line = reader.readLine();
		for(j=0; j< numNodes;j++){
			tokens = line.split(delim);
			if(tokens.length != numColumns){
				System.err.println("expected three entries (X, Y, Z) coordinates, N - node connectivity, and radius. Retrieved numEntries="+tokens.length);
				reader.close();
				return null;					
			}
			data[j] = new Skeleton();
			i=0;
/*			System.out.println("value["+i+"]="+tokens[i]);
			System.out.println("value["+(i+1)+"]="+tokens[i+1]);
			System.out.println("value["+(i+2)+"]="+tokens[i+2]);
			System.out.println("value["+(i+3)+"]="+tokens[i+3]);
			System.out.println("value["+(i+4)+"]="+tokens[i+4]);*/
			data[j].X = Double.parseDouble(tokens[i]); 
			data[j].Y = Double.parseDouble(tokens[i+1]);
			data[j].Z = Double.parseDouble(tokens[i+2]);
			data[j].n = Integer.parseInt(tokens[i+3]);
			data[j].radius = Double.parseDouble(tokens[i+4]);
			line = reader.readLine();
		}
		reader.close();
		return data;

	}
	/**
	 * This method converts the coordinate center from left LOWER image corner
	 * to left UPPER image corner
	 *  
	 * @param data - input data with Y coordinate to be flipped
	 * 
	 * @param maxHeight - this can be in physical units or pixels depending on what numerical limage values were loaded
	 * @return
	 */
	public Skeleton [] flipCoordinateCenter(Skeleton [] data, double maxHeight){
		
		int numNodes = data.length; 
		for(int j=0; j< numNodes;j++){
			data[j].Y = maxHeight - data[j].Y;			
		}
		return data;		
	}

	public static boolean saveSkeletons(Vector<Skeleton> arr, String OutFileName) throws IOException	 {
		// sanity check
		if (arr == null || OutFileName == null) {
			System.err.println("rray is null or OutFileName is null");
			return false;
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
		out.write("X" + ", " + "Y" + ", " + "Z" + ", " + "n" + ", " + "radius" + "\n");
		
		for (int i = 0; i < arr.size(); i++) {
			out.write(arr.elementAt(i).X + ", " + arr.elementAt(i).Y +  ", " + arr.elementAt(i).Z + ", " + arr.elementAt(i).n + ", " + arr.elementAt(i).radius + "\n");
		}
		
/*		for (int i = 0; i < arr.length ; i++) {
			out.write(arr[i].X + ", " + arr[i].Y +  ", " + arr[i].Z + ", " + arr[i].n + ", " + arr[i].radius + "\n");
		}
*/
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
			System.out.println("arg = Input TXT file Name with (X,Y,Z, n, radius) per row");
			return;
		}
		
		String TXTFilename = args[0];
		
		TubeSkeletonLoader tube = new TubeSkeletonLoader();
		Skeleton [] arr = tube.readNodes(TXTFilename);
		if(arr != null){
			for (i = 0; i<arr.length;i++){
				System.out.println(arr[i].toString());
			}
		}
	}

}
