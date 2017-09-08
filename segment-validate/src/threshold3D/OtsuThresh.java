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
package threshold3D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class OtsuThresh extends Threshold3DImage {
	
	private static Log _logger = LogFactory.getLog(OtsuThresh.class);

	/**
	 * 
	 */
	public OtsuThresh() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see threshold3D.Threshold3DImage#findThresh(ij.ImagePlus, double, double, double)
	 */
	@Override
	public double findThresh(ImagePlus img3D, double min, double max, double delta) {
		
		// sanity check
		if (img3D == null) {
			_logger.error("Input image is null, no threshold to be found.");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		int bitDepth = img3D.getBitDepth();
		int numberOfVoxels = numrows * numcols * numzs;
		
		double optThresh = 0.0;
		
		ImageStack imgStack = img3D.getStack();
		int numberGreyValues = (int) Math.pow(2, bitDepth);
		
		int[] histogram = new int[numberGreyValues];
		double[] probabilities = new double[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					histogram[temp] ++;
				}
			}
		}
		
		// compute probabilities
		for (int i = 0; i < numberGreyValues; i++) {
			probabilities[i] = (double) histogram[i] / (double) numberOfVoxels;
		}
		
		
		double maxInterClassVariance = Double.MIN_VALUE;
		
		for (double thresh = min; thresh <= max; thresh += delta) {
			
			double wB = 0.0;
			double wF = 0.0;
			double meanB = 0.0;
			double meanF = 0.0;
			
			for(int i=0; i<= thresh; i++) {
				wB += probabilities[i];
				meanB += probabilities[i] * i;
			}
			meanB /= wB;
			for(int i=(int)thresh + 1; i< numberGreyValues; i++) {
				wF += probabilities[i];
				meanF += probabilities[i] * i;
			}
			meanF /= wF;
			
			double interClassVariance = wB * wF * Math.pow((meanB - meanF), 2);
			if(interClassVariance > maxInterClassVariance) {
				optThresh = thresh;
				maxInterClassVariance = interClassVariance;
			}
			
		}
	
		return optThresh;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
