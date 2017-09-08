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

import ij.ImagePlus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.illinois.ncsa.isda.imagetools.core.datatype.ImageException;
import edu.illinois.ncsa.isda.imagetools.core.datatype.ImageObject;

/**
 * This is an abstract class for the thresholding methods
 * 
 * @author peter bajcsy
 * 
 */
public abstract class Threshold3DImage {
	private static Log _logger = LogFactory.getLog(Threshold3DImage.class);

	abstract double findThresh(ImagePlus img3D, double min, double max,
			double delta);

	/**
	 * This method thresholds a 3D volume and returns a binary 3D volume with
	 * 255 values for voxels with intensities larger than the provided threshold
	 * and 0 values for voxels with intensities less or equal than the provided
	 * threshold
	 * 
	 * @param img3D
	 *            - input 3D image
	 * @param thresh
	 *            - parameter
	 * @return - thresholded 3D volume
	 * @throws ImageException
	 */
	static public ImageObject[] threshold(ImageObject[] img3D, double thresh)
			throws ImageException {

		// sanity check
		if (img3D == null) {
			_logger.error("Missing array of input images");
			return null;
		}
		int numrows = img3D[0].getNumRows();
		int numcols = img3D[0].getNumCols();
		int numzs = img3D.length;
		int numbands = img3D[0].getNumBands();
		int type = img3D[0].getType();

		ImageObject[] res3D = new ImageObject[numzs];
		for (int z = 0; z < numzs; z++) {
			res3D[z] = ImageObject
					.createImage(numrows, numcols, numbands, type);
		}
		long count = 0;
		for (int row = 0; row < numrows; row++) {
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					for (int band = 0; band < numbands; band++) {
						// FRG pixels are those above the threshold
						if (img3D[z].getDouble(row, col, band) > thresh) {
							res3D[z].setDouble(row, col, band, 255.0);
							count++;
						} else {
							res3D[z].setDouble(row, col, band, 0.0);
						}
					}
				}
			}
		}
		System.out.println("number of white voxels: " + count);

		return res3D;
	}

	/**
	 * This method generates a test data set with values between 0 and numrows
	 * 
	 * @return
	 * @throws ImageException
	 */
	static public ImageObject[] createTestVolume() throws ImageException {

		int numrows = 100;
		int numcols = 50;
		int numzs = 5;
		int numbands = 1;
		int type = ImageObject.TYPE_BYTE;

		ImageObject[] res3D = new ImageObject[numzs];
		for (int z = 0; z < numzs; z++) {
			res3D[z] = ImageObject
					.createImage(numrows, numcols, numbands, type);
		}
		for (int row = 0; row < numrows; row++) {
			for (int col = 0; col < numcols; col++) {
				for (int z = 0; z < numzs; z++) {
					for (int band = 0; band < numbands; band++) {
						if(row <(numrows>>1)){
							res3D[z].setDouble(row, col, band, 5*Math.random());
						}else{
							res3D[z].setDouble(row, col, band, 10+5*Math.random());
						}
					}
				}
			}
		}
		
		return res3D;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
