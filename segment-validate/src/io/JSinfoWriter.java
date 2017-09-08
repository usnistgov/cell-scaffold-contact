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

import io.TubeSkeletonLoader.Skeleton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import util.FileOper;

/**
 * @author peter bajcsy
 * 
 * This class was written to generate the information for JavaScript files used for validating cell-scaffold contact points
 * based on movies presented in a web browser. The generated information goes to files such as 
 * \git\3d-measurements\segmentation-validation-web\app\validationSpunCoat06252015
 * 
 * contact_validation-service-SpunCoat06252015.js
 * 
 *
 */
public class JSinfoWriter {

	
	public static String createJSoutput(String inputImagesFolder, String contactMethod, String videodir){
		
		// check the videodir so that we do not have multiple slashes
		if(!videodir.endsWith("/")){
			videodir += "/";
		}
		// getting images to process
		/*Collection<String> dirfiles = FileOper
				.readSubDirectories(inputImagesFolder);*/
		Collection<String> dirfiles = FileOper.readFileDirectory(inputImagesFolder);
		Collection<String> jpegfiles = FileOper.selectFileType(dirfiles, ".jpeg");
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(jpegfiles,FileOper.SORT_ASCENDING);
		// Example:
		//061115_SC_1_63x_Pos045_projections.jpeg
		//061115_SC_1_63x_Pos046_projections.jpeg
		System.out.println("jpeg images: " + sortedImagesInFolder.toString());
		
		// convert jpeg file names to mp4 file names
		// 061115_SC_1_63x_Pos045_OutA2_XSCP.mp4
		// 061115_SC_1_63x_Pos045_OutA2_XSP.mp4
		// 061115_SC_1_63x_Pos045_OutA2_XP.mp4
		// 061115_SC_1_63x_Pos045_OutA2_YSCP.mp4
		// 061115_SC_1_63x_Pos045_OutA2_YSP.mp4
		// 061115_SC_1_63x_Pos045_OutA2_YP.mp4	
		
		// convert mp4 file names to json file format
		/*		{
			name: "061115_SC_1_63x_Pos045",
			path: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_XSCP.mp4",
			path1: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_XSP.mp4",
			path2: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_XP.mp4",
			path3: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_YSCP.mp4",
			path4: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_XSP.mp4",
			path5: "data/Contact_SpunCoat-06252015/061115_SC_1_63x_Pos045_OutG03_YP.mp4",
			status: "excellent"
		},*/
		
		String rootName = new String("061115_SC_1_63x_Pos045");
		String result = new String();
		String inputFilename = new String();
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			inputFilename = k.next();
			int idxBeg = inputFilename.lastIndexOf("\\") +1;
			int idxEnd = inputFilename.lastIndexOf("_projections.jpeg");
			rootName = inputFilename.substring(idxBeg, idxEnd);
			result += "{\n";
			result += "  name: \"" + rootName +"\",\n";
			result += "  path: \""+videodir+rootName+"_Out"+contactMethod+"_XSCP.mp4\",\n";
			result += "  path1: \""+videodir+rootName+"_Out"+contactMethod+"_XSP.mp4\",\n";
			result += "  path2: \""+videodir+rootName+"_Out"+contactMethod+"_XP.mp4\",\n";
			result += "  path3: \""+videodir+rootName+"_Out"+contactMethod+"_YSCP.mp4\",\n";
			result += "  path4: \""+videodir+rootName+"_Out"+contactMethod+"_YSP.mp4\",\n";
			result += "  path5: \""+videodir+rootName+"_Out"+contactMethod+"_YP.mp4\",\n";
			result += "  status: \"excellent\"\n";
			result += "},\n";
		}			
		System.out.println(result);
		return result;

	}
	/**
	 * This method saves the formatted JS text for copying t
	 * @param result - resulting formatted text
	 * @param OutFileName - output file name
	 * @return
	 * @throws IOException
	 */
	public static boolean saveJStext(String result, String OutFileName) throws IOException	 {
		// sanity check
		if (result == null || OutFileName == null) {
			System.err.println("result text is null or OutFileName is null");
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".txt")) {
			output = OutFileName;
		} else {
			output += ".txt";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);
		out.write(result);
		
		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if ((args == null) || (args.length < 4)) {
			System.out
					.println("Please, specify (1) the input directory with jpeg images,"
							+ "(2) contact method [A2, G03, F10],  (3) output video dir and (4) output file name");

			return;
		}
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}

		String inputImagesFolder = new String(args[0]);
		String contactMethod = new String(args[1]);
		String videodir = new String(args[2]);
		String OutFileName = new String(args[3]);
		

	
		String result  = new String();
		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/SpunCoat-06252015";
		contactMethod = "A2";
		videodir = "data/SpunCoat-06252015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_SpunCoat-06252015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);
	
		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/SpunCoat-07072015";
		contactMethod = "A2";
		videodir = "data/SpunCoat-07072015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_SpunCoat-07072015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);

		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/Microfiber-06252015";
		contactMethod = "A2";
		videodir = "data/Microfiber-06252015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_Microfiber-06252015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);

		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/Microfiber-07072015";
		contactMethod = "A2";
		videodir = "data/Microfiber-07072015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_Microfiber-07072015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);

		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/MediumMicrofiber-07102015";
		contactMethod = "A2";
		videodir = "data/MediumMicrofiber-07102015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_MediumMicrofiber-07102015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);
		
		/////////////////////////
		inputImagesFolder = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/data/MediumMicrofiber-07232015";
		contactMethod = "A2";
		videodir = "data/MediumMicrofiber-07232015/VideoA2";
		OutFileName = "C:/PeterB/Projects/git/3d-measurements/segmentation-validation-web/app/JS_MediumMicrofiber-07232015.txt";		
		result = JSinfoWriter.createJSoutput(inputImagesFolder, contactMethod, videodir);
		JSinfoWriter.saveJStext(result, OutFileName);
		
		
	}

}
