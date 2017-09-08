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
package segment3D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class Image3DCropping {
	
	private static Log logger = LogFactory
			.getLog(Image3DCropping.class);
	
	private double meanBKGFrames;
	private double stdevBKGFrames;
	
	public Image3DCropping() {
		
	}
	
	public int[] removeMeaninglessFrames16bits(ImagePlus img3D, int threshold) {

	
		int xSize = img3D.getWidth();
		int ySize = img3D.getHeight();
		int zSize = img3D.getNSlices();
		
		int[] framesRange = new int[] {0, zSize-1};
		
		int byteThreshold = (int) threshold/257;
		
		// Fill thresholdedImage with voxel intensities
		ImageStack imgStack = img3D.getStack();
		int nbFRGpixels = 0;
		int z = 1;
		
		logger.info("Finding useless frames at the beginning of the stack...");
		while(z <= zSize && nbFRGpixels < 500) {
			
			ImageProcessor imgProc = imgStack.getProcessor(z);
			
			nbFRGpixels = 0;
			
			//double min = imgProc.getMin();
            //double max = imgProc.getMax();
            ByteProcessor tmpProc = (ByteProcessor) imgProc.convertToByte(true);
            //tmpProc.setAutoThreshold(ImageProcessor.ISODATA2, ImageProcessor.NO_LUT_UPDATE);
            //float t1 = (float)(min + (max-min)*(tmpProc.getMinThreshold()/255.0));
            //float t2 = (float)(min + (max-min)*(tmpProc.getMaxThreshold()/255.0));
            //int t1 = (int) imgProc.getMinThreshold();
            //int t2 = (int) tmpProc.getMaxThreshold();
            for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					int pixel = tmpProc.getPixel(x, y);
					if( pixel > byteThreshold)
						tmpProc.putPixel(x, y, 255);
					else 
						tmpProc.putPixel(x, y, 0);
				}
			}
            tmpProc.erode(1, 0);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if( tmpProc.getPixel(x, y) == 255)
						nbFRGpixels ++;
				}
			}
			
			//logger.info("Nb FRG pixels..." + nbFRGpixels);
			
			z ++;
		}
		
		int startingFrame = (z>1 ? (z-2) : 0);
		
		z = zSize;
		nbFRGpixels = 0;
		
		logger.info("Finding useless frames at the end of the stack...");
		while(z > 0  && nbFRGpixels < 500) {
			
			ImageProcessor imgProc = imgStack.getProcessor(z);
			
			nbFRGpixels = 0;
			
            ByteProcessor tmpProc = (ByteProcessor) imgProc.convertToByte(true);
            //tmpProc.setAutoThreshold(ImageProcessor.ISODATA2, ImageProcessor.NO_LUT_UPDATE);
            //int t1 = (int) tmpProc.getMinThreshold();
            //int t2 = (int) tmpProc.getMaxThreshold();
            for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					int pixel = tmpProc.getPixel(x, y);
					if( pixel > byteThreshold ) {
						tmpProc.putPixel(x, y, 255);
					}
					else 
						tmpProc.putPixel(x, y, 0);
				}
			}
            tmpProc.erode(1, 0);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if( tmpProc.getPixel(x, y) == 255)
						nbFRGpixels ++;
				}
			}
			//logger.info("Nb FRG pixels..." + nbFRGpixels);
			z --;
			
		}
		
		logger.info("Removing frames and computing statistics about BKG...");
		int endingFrame = (z<zSize ? z : zSize-1);
		
		double meanBKG = 0.0;
		double stdevBKG = 0.0;
		int countFrames = 0;
		
		if(startingFrame < endingFrame) {
			framesRange[0] = startingFrame;
			framesRange[1] = endingFrame;
			
			for(int i=0; i<startingFrame; i++) {
				double mean = imgStack.getProcessor(1).getStatistics().mean;
				double stdev = imgStack.getProcessor(1).getStatistics().stdDev;
				//logger.info("Mean for this slice is : " + mean + " and stdev : " + stdev);
				meanBKG += mean;
				stdevBKG += stdev;
				countFrames ++;
				imgStack.deleteSlice(1);
			}
			
			for(int i=endingFrame+1; i<zSize; i++) {
				double mean = imgStack.getProcessor(img3D.getNSlices()).getStatistics().mean;
				double stdev = imgStack.getProcessor(img3D.getNSlices()).getStatistics().stdDev;
				//logger.info("Mean for this slice is : " + mean + " and stdev : " + stdev);
				meanBKG += mean;
				stdevBKG += stdev;
				countFrames ++;
				imgStack.deleteLastSlice();
			}
			
			if(countFrames != 0) {
				meanBKG /= (double) countFrames;
				stdevBKG /= (double) countFrames;
				//logger.info("Global mean is : " + meanBKG + " and stdev : " + stdevBKG);
			}
		}
		
		meanBKGFrames = meanBKG;
		stdevBKGFrames = stdevBKG;
		
		return framesRange;
	}

	/**
	 * @return the meanBKGFrames
	 */
	public double getMeanBKGFrames() {
		return meanBKGFrames;
	}

	/**
	 * @return the stdevBKGFrames
	 */
	public double getStdevBKGFrames() {
		return stdevBKGFrames;
	}
	
	

}
