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
package pipeline3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import util.FileOper;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import io.ReferenceLineLoader;
import io.TubeSkeletonLoader;
import io.TubeSkeletonLoader.Skeleton;

/**
 * @author pnb
 *
 * This class selects those skeleton points extracted from single fibers (by segmenting all fibers and estimating their skeleton and radius)
 * that belong to the reference fiber (manually determined in ImageJ/Fiji as a set of lines defined by end points). This is important
 * since the FOV contains multiple fibers and only one reference fiber was used when the reference SEM imaging 
 * was performed.
 *  
 */
public class FiberSkeletonSelection {

	public static int [] stitchingOffsets = {
		-2048,
		-1477,
		-1329,
		-1135,
		-1548,
		-1812,
		-1780,
		-1780,
		-1890,
		-1723,
		-1881,
		-1876,
		-1765,
		-1721,
		-1833,
		-1834,
		-2048
	};
	/**
	 * This method reads reference lines from a TXT file and creates a set of line objects from them
	 * 
	 * @param TXT_RefFiberFilename
	 * @return
	 * @throws Exception
	 */
	public Im2DLine [] createReferenceLines(String TXT_RefFiberFilename) throws Exception{

		//String TXTFilename = args[0];
		String delim = new String("\t");
		boolean colName = false;
		int headerRows = 0;

		int i;
		double [] arr = ReferenceLineLoader.readData(TXT_RefFiberFilename, delim, colName, headerRows);
		System.out.println("Reference points");
		for ( i = 0; i<arr.length;i+=2){
			System.out.println("X="+arr[i]+", Y="+arr[i+1]);
		}

		Im2DLine [] lineXY = new Im2DLine[(arr.length>>1)-1];
		for(i=0;i<(arr.length>>1)-1;i++){
			Im2DPoint ptsIn1 = new Im2DPoint(arr[i<<1], arr[(i<<1)+1]);
			Im2DPoint ptsIn2 = new Im2DPoint(arr[(i<<1)+2], arr[(i<<1)+3]);			
			lineXY[i] = new Im2DLine(ptsIn1, ptsIn2);

			System.out.println("line idx="+ i +", line: " + lineXY[i].toString());
		}
		return lineXY;
	}

	/**
	 * This method sub-select the Segmentation-based Fiber skeleton points based on [X,Y] coordinates provided 
	 * by the reference lines
	 * Distance is set to the multiple of one [X,Y] diagonal distance in physical units
	 * 
	 * @param TXT_RefFiberFilename
	 * @param TXT_SegFiberFilename
	 * @param micronPerX
	 * @param micronPerY
	 * @param imageHeight
	 * @return
	 * @throws Exception
	 */
	public Vector<Skeleton> selectSkeleton(String TXT_RefFiberFilename, String TXT_SegFiberFilename, double micronPerX, double micronPerY,int imageHeight) throws Exception{

		int i,j;
		TubeSkeletonLoader tube = new TubeSkeletonLoader();
		Skeleton [] arr = tube.readNodes(TXT_SegFiberFilename);
		/*		if(arr != null){
			System.out.println("Tube skeleton points");
			for (i = 0; i<arr.length;i++){
				System.out.println(arr[i].toString());
			}
		}*/
		// correct the coordinate center from left LOWER to left UPPER image corner
		double imageHeightUnit = imageHeight*micronPerY;
		// round to three decimal digits
		//imageHeightUnit =  ((int)(imageHeightUnit*1000.0)) * 1000.0;
		arr = tube.flipCoordinateCenter(arr, imageHeightUnit);


		Im2DLine [] lineXY = createReferenceLines(TXT_RefFiberFilename);
		if(lineXY==null || lineXY.length < 1){
			System.err.println("no reference lines");
			return null;
		}
		Vector<Skeleton> res = new Vector<Skeleton>(300) ;
		double dist = 0.0;
		// thresh is twice the diagonal of a pixel defined by X-Y in physical units
		double thresh = 5.0*Math.sqrt(micronPerX*micronPerX + micronPerY*micronPerY);
		System.out.println("DIST thresh="+thresh);
		
		for (i = 0; i<arr.length;i++){
			// pts - the skeleton coordinates provided by VesselKnife 
			Im2DPoint pts = new Im2DPoint(arr[i].X, arr[i].Y); 
			for(j=0;j<lineXY.length;j++){
				// select the points based on distance to a line
				dist = lineXY[j].distLineToPoint(lineXY[j], pts);	
				if(dist < thresh){
					// select a point that is between the line end points
					Im2DPoint ptsOnLine = lineXY[j].closestLinePointToPoint(lineXY[j], pts);
					if(Im2DLine.isPointBetweenLineEndPoints(lineXY[j], ptsOnLine) ){
						res.add(arr[i]);
						j=lineXY.length; //exit the line loop to avoid adding the same point twice
						
		/*				if(arr[i].Y< 220*micronPerY && arr[i].X <1370 *micronPerX && arr[i].X >1330*micronPerX){
							System.out.println("Outliers: dist["+i+"]["+j+"]="+dist+" \t X="+ Double.toString(arr[i].X) +", Y="+ Double.toString(arr[i].Y));
							System.out.println("Outliers: dist["+i+"]["+j+"]="+dist+" \t X="+ Double.toString(arr[i].X/micronPerX) +", Y="+ Double.toString(arr[i].Y/micronPerY));
							dist = lineXY[j].distLineToPoint(lineXY[j], pts);
							System.out.println("closest point on line:"+lineXY[j].closestLinePointToPoint(lineXY[j], pts).toString());
						}
		*/															
					}else{
/*						if(j == lineXY.length-1 && arr[i].Y > 0.2*2048*micronPerY && arr[i].Y <0.8*2048*micronPerY){
							//System.out.println("not selected row although : dist =" + dist+ "\n"+ arr[i].toString());
							System.out.println("Outliers: dist["+i+"]["+j+"]="+dist+" \t X="+ Double.toString(arr[i].X) +", Y="+ Double.toString(arr[i].Y));
							System.out.println("Outliers: dist["+i+"]["+j+"]="+dist+" \t X="+ Double.toString(arr[i].X/micronPerX) +", Y="+ Double.toString(arr[i].Y/micronPerY));
							dist = lineXY[j].distLineToPoint(lineXY[j], pts);
							System.out.println("closest point on line:"+lineXY[j].closestLinePointToPoint(lineXY[j], pts).toString());
						}
*/					}
				}
			}
		}
		System.out.println("Number of total="+ arr.length+", selected="+res.size());

 		String IMG_ResFilename = new String(TXT_SegFiberFilename);
		IMG_ResFilename = IMG_ResFilename.substring(0, IMG_ResFilename.length() - 4) + "_select.jpg";
		System.out.println("save jpg file in ="+ IMG_ResFilename);
		saveColorImgSelectedSkeleton(res, arr, micronPerX, micronPerY,imageHeight, IMG_ResFilename);

		return res;

	}

	///// not used right now
	public void saveImgSelectedSkeleton(Vector<Skeleton> res, Skeleton [] in, double micronPerX, double micronPerY,int imageHeight, String outputFilePath){

		// NOTE: the height and width are assumed to be the same
		int resImageHeight = imageHeight;
		int resImageWidth =  imageHeight;

		// create new data array for the result image
		byte[] resData = new byte[resImageWidth * resImageHeight];
		Arrays.fill(resData, (byte) 255);

		// populate resData with the skeleton points
		int row, col, idx;
		for(int i = 0; i< res.size();i++){
			Skeleton sk = res.elementAt(i);
			col = (int) Math.round(sk.X/micronPerX);
			row = (int) Math.round(sk.Y/micronPerY);
			idx = row*resImageWidth + col;
			resData[idx] = (byte)0;			
		}

		for(int i = 0; i< in.length;i++){			
			col = (int) Math.round(in[i].X/micronPerX);
			row = (int) Math.round(in[i].Y/micronPerY);
			idx = row*resImageWidth + col;
			resData[idx] = (byte)128;			
		}		
		// get projections data
		//ImageProcessor rawXYImageProcessor = proj1XY.getProcessor().convertToByteProcessor();

		// construct result image processor with the result data
		ByteProcessor imgResProcessor = new ByteProcessor(resImageWidth,	resImageHeight, resData);

		// construct result ImagePlus object
		ImagePlus resImage = new ImagePlus(outputFilePath, imgResProcessor);

		// save result image as JPEG in the output folder
		FileSaver fs = new FileSaver(resImage);
		fs.saveAsJpeg(outputFilePath);

	}

	/**
	 * This method saves an image 
	 * @param res
	 * @param in
	 * @param micronPerX
	 * @param micronPerY
	 * @param imageHeight
	 * @param outputFilePath
	 */
	public void saveColorImgSelectedSkeleton(Vector<Skeleton> res, Skeleton [] in, double micronPerX, double micronPerY,int imageHeight, String outputFilePath){

		// NOTE: the height and width are assumed to be the same
		int resImageHeight = imageHeight;
		int resImageWidth =  imageHeight;
		int resImageSize = resImageWidth * resImageHeight;

		// create new data array for the result image
		int[] resData = new int[resImageWidth * resImageHeight];
		Arrays.fill(resData,0);

		int row, col, idx;
		// pupulate resData with original skeleton locations
		for(int i = 0; i< in.length;i++){			
			col = (int) Math.round(in[i].X/micronPerX);
			row = (int) Math.round(in[i].Y/micronPerY);
			idx = row*resImageWidth + col;
			if(idx<0 ){
				System.err.println("convertion from ["+in[i].X+","+in[i].Y+"] to pixels ["+col+","+row+"] is "+idx+" out of min bound = 0");
				idx = 0;
			}
			if(idx >=resImageSize){
				System.err.println("convertion from ["+in[i].X+","+in[i].Y+"] to pixels ["+col+","+row+"] is "+idx+" out of Max bound = "+resImageSize);
				idx = resImageSize-1;				
			}
			resData[idx] = 0xffff0000;// red color 		
		}	

		// populate resData with the skeleton points
		for(int i = 0; i< res.size();i++){
			Skeleton sk = res.elementAt(i);
			col = (int) Math.round(sk.X/micronPerX);
			row = (int) Math.round(sk.Y/micronPerY);
			idx = row*resImageWidth + col;
			resData[idx] = Integer.MAX_VALUE;//0xff0000ff;	// blue color		
		}


		// get projections data
		//ImageProcessor rawXYImageProcessor = proj1XY.getProcessor().convertToByteProcessor();

		// construct result image processor with the result data
		ColorProcessor imgResProcessor = new ColorProcessor(resImageWidth,	resImageHeight, resData);

		// construct result ImagePlus object
		ImagePlus resImage = new ImagePlus(outputFilePath, imgResProcessor);

		// save result image as JPEG in the output folder
		FileSaver fs = new FileSaver(resImage);
		fs.saveAsJpeg(outputFilePath);

	}
	
	public void processBatch(String TXT_RefFiberFolderName, String TXT_SegFiberFolderName, double micronPerX, double micronPerY,int imageHeight) throws Exception{

		// read the TXT reference fiber files in a RefFiberFolder
		Collection<String> dirfiles = FileOper.readFileDirectory(TXT_RefFiberFolderName);
		System.out.println("Directory Collection Size=" + dirfiles.size());
		//FileOper.printCollection(dirfiles);
		Collection<String> onlyTXT_RefFiber = FileOper.selectFileType(dirfiles,	".txt");
		System.out.println("filtered TXT_RefFiberFolderName Size=" + onlyTXT_RefFiber.size());
		//FileOper.printCollection(onlyTXT_RefFiber);

		Collection<String> sorted = FileOper.sort(onlyTXT_RefFiber,	FileOper.SORT_ASCENDING);
		System.out.println("sorted filtered TXT_RefFiberFolderName in descending order=" + sorted.size());
		FileOper.printCollection(sorted);

		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		// read the TXT fiber files from segmentation of a single fiber 
		Collection<String> dirfiles2 = FileOper.readFileDirectory(TXT_SegFiberFolderName);
		System.out.println("Directory Collection Size=" + dirfiles2.size());
		//FileOper.printCollection(dirfiles2);
		Collection<String> onlyTXT_SegFiber = FileOper.selectFileType(dirfiles2,	"_d.txt");
		System.out.println("filtered TXT_SegFiberFolderName Size=" + onlyTXT_SegFiber.size());
		//FileOper.printCollection(onlyTXT_SegFiber);

		Collection<String> sorted2 = FileOper.sort(onlyTXT_SegFiber,	FileOper.SORT_ASCENDING);
		System.out.println("sorted filtered TXT_SegFiberFolderName in descending order=" + sorted2.size());
		//FileOper.printCollection(sorted2);

		double lowerY_limit = 0.0;
		double upperY_limit = imageHeight*micronPerY;
		int count = 0;

		int [] numFOVPts = new int[sorted.size()];
		int [] numInternalPts = new int[sorted.size()];
		int index = 0;
		Vector<Skeleton> cummulativeRes = new Vector<Skeleton>(1000);
		   Iterator <String> itrRef = sorted.iterator();			
		   Iterator <String> itrSeg = sorted2.iterator();		   
		    while(itrRef.hasNext() && itrSeg.hasNext()) {
		         String  TXT_RefFiberFilename = (String) itrRef.next();
		         String TXT_SegFiberFilename = (String) itrSeg.next();
		         System.out.println("processing files: \n ref=" + TXT_RefFiberFilename + "\n segm="+TXT_SegFiberFilename);
		         Vector<Skeleton> res = selectSkeleton(TXT_RefFiberFilename, TXT_SegFiberFilename, micronPerX, micronPerY,imageHeight);
				String CSV_ResFilename = new String(TXT_SegFiberFilename);
				CSV_ResFilename = CSV_ResFilename.substring(0, CSV_ResFilename.length() - 4) + "_select.csv";
				TubeSkeletonLoader.saveSkeletons(res, CSV_ResFilename);
				
				// in pixels --> in micrometers
					lowerY_limit = (2048 + stitchingOffsets[index+1])*micronPerY; 
					upperY_limit = -stitchingOffsets[index]*micronPerY;
					count = 0;
				for(int idx = 0; idx < res.size(); idx++){
					Skeleton obj = res.get(idx);
	
					if (obj.Y >= lowerY_limit && obj.Y <= upperY_limit){
						cummulativeRes.addElement(obj);
						count++;
					}else{
						// label overlapping pixels by setting n to -1
						obj.n = -1;
						cummulativeRes.addElement(obj);
					}
					
				}

				numFOVPts[index] = res.size();
				numInternalPts[index] = count;				
				index++;

		    }
		    
		    //save results for all FOVs
		     itrSeg = sorted2.iterator();		   
		    String  CSV_ResInternalFilename = (String) itrSeg.next();
		    String CSV_PointCountFilename = new String(CSV_ResInternalFilename);
			CSV_ResInternalFilename = CSV_ResInternalFilename.substring(0, CSV_ResInternalFilename.length() - 4) + "_labelOverlap.csv";
			TubeSkeletonLoader.saveSkeletons(cummulativeRes, CSV_ResInternalFilename);
		
		    ////////////////////////// 
			String header = new String("FOV index, number of pts in  FOV, number of internal pts in FOV");
		    System.out.println(header);
		    for (int j = 0;j<numFOVPts.length;j++){
		    	//System.out.println("FOV all collection["+j+"]= "+ numFOVPts[j]);		    	
		    	//System.out.println("FOV internal collection["+j+"]= "+ numInternalPts[j]);
		    	System.out.println(j+", "+ numFOVPts[j]+", "+numInternalPts[j]);
		    }
			
		    CSV_PointCountFilename = CSV_PointCountFilename.substring(0, CSV_PointCountFilename.length() - 4) + "_pointCount.csv";
				    
		    ReferenceLineLoader.saveArray(numFOVPts, numInternalPts, header,CSV_PointCountFilename);
	
		
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		int i;
		if ((args == null) || (args.length < 3)) {
			System.out.println("Please, specify the input TXT reference file name and TXT segment skeleton file");
			System.out.println("arg0 = Input TXT file Name with (X,Y) coordinates per row");
			System.out.println("arg1 = Input TXT skeleton file Name with (X,Y,Z, n, radius) per row");
			return;
		}
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		FiberSkeletonSelection test = new FiberSkeletonSelection();
		String TXT_RefFiberFilename = args[0];
		String TXT_SegFiberFilename = args[1];
		String IMG_ResFilename = args[2];
		double micronPerX, micronPerY;
		micronPerX = micronPerY = 0.12;//1.2E-5;
		int imageHeight = 2048;
/*		Vector<Skeleton> res = test.selectSkeleton(TXT_RefFiberFilename, TXT_SegFiberFilename, micronPerX, micronPerY,imageHeight);

		String CSV_ResFilename = new String(TXT_SegFiberFilename);
		CSV_ResFilename = CSV_ResFilename.substring(0, CSV_ResFilename.length() - 4) + "_select.csv";
		TubeSkeletonLoader.saveSkeletons(res, CSV_ResFilename);
*/

/*		String TXT_RefFiberFolderName1 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName1 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg1_v1/skeleton";
		test.processBatch(TXT_RefFiberFolderName1, TXT_SegFiberFolderName1, micronPerX, micronPerY, imageHeight);
	
		String TXT_RefFiberFolderName2 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName2 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg2_v1/skeleton";
		test.processBatch(TXT_RefFiberFolderName2, TXT_SegFiberFolderName2, micronPerX, micronPerY, imageHeight);
	
		
	
		String TXT_RefFiberFolderName4 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName4 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg4_v1/skeleton";
		test.processBatch(TXT_RefFiberFolderName4, TXT_SegFiberFolderName4, micronPerX, micronPerY, imageHeight);

		
		String TXT_RefFiberFolderName5 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName5 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg5_v1/skeleton";
		test.processBatch(TXT_RefFiberFolderName5, TXT_SegFiberFolderName5, micronPerX, micronPerY, imageHeight);
*/
		
/*		String TXT_RefFiberFolderName6 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName6 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg6_G03/skeleton";
		test.processBatch(TXT_RefFiberFolderName6, TXT_SegFiberFolderName6, micronPerX, micronPerY, imageHeight);
	
		String TXT_RefFiberFolderName7 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName7 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg7_F10/skeleton";
		test.processBatch(TXT_RefFiberFolderName7, TXT_SegFiberFolderName7, micronPerX, micronPerY, imageHeight);
*/
		String TXT_RefFiberFolderName8 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/referenceLines";
		String TXT_SegFiberFolderName8 = "C:/PeterB/Presentations/NISTCollab/CarlSimon/Cell-scaffold-contact/singleFiber/FiberResults/Alg8_F15/skeleton";
		test.processBatch(TXT_RefFiberFolderName8, TXT_SegFiberFolderName8, micronPerX, micronPerY, imageHeight);

		
	}

}
