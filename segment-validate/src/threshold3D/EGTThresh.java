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

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class EGTThresh {
	
	private static Log logger = LogFactory.getLog(EGTThresh.class);
	
	private static final int NB_HISTOGRAM_MODES = 3;

	/**
	 * 
	 */
	public EGTThresh() {
		// TODO Auto-generated constructor stub
	}

	
	public double findThresh(ImagePlus img3D, int greedy, boolean sobel3D) {

		// sanity check
		if (img3D == null) {
			logger.error("Input image is null, no threshold to be found.");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();

		ImageStack imgStack = img3D.getStack();

		// find edges
		if(sobel3D)
			findEdgesSobel(img3D);

		int nonZeroVoxelsCount = 0;

		for (int z = 0; z < numzs; ++z) {
			ImageProcessor imgProc = imgStack.getProcessor(z + 1);
			if(! sobel3D)
				imgProc.findEdges();
			for (int x = 0; x < numcols; ++x) {
				for (int y = 0; y < numrows; ++y) {
					if (imgProc.getPixel(x, y) > 0)
						nonZeroVoxelsCount++;
				}
			}
		}
		double[] nonZeroVoxels = new double[nonZeroVoxelsCount];
		int kNonZeroVoxels = 0;

		double minValue = Double.MAX_VALUE;
		double maxValue = Double.MIN_VALUE;
		for (int z = 0; z < numzs; ++z) {
			ImageProcessor imgProc = imgStack.getProcessor(z + 1);
			for (int x = 0; x < numcols; ++x) {
				for (int y = 0; y < numrows; ++y) {
					int voxel = imgProc.getPixel(x, y);
					if (voxel > 0) {
						nonZeroVoxels[kNonZeroVoxels] = (double) voxel;
						if (voxel > maxValue)
							maxValue = voxel;
						if (voxel < minValue)
							minValue = voxel;
						kNonZeroVoxels++;
					}
				}
			}
		}

		// check that min and max are valid
		if (minValue == Double.MAX_VALUE || maxValue == Double.MIN_VALUE) {
			throw new IllegalArgumentException(
					"Input Image has no nonzero gradient pixels");
		}

		// generate the histogram of the nonzero pixels
		int nbBins = 1000;
		double[] histData = new double[nbBins + 1];

		// populate the histogram bins
		double rescale = nbBins / (maxValue - minValue);
		for (int i = 0; i < nonZeroVoxels.length; i++) {
			histData[(int) ((nonZeroVoxels[i] - minValue) * rescale + 0.5)]++;
			// + 0.5 is to center the bins at [0 1] instead of [-0.5 0.5]
		}

		// compute the averaged mode for the histogram
		double[] modes = new double[NB_HISTOGRAM_MODES];
		int[] modeIdxs = new int[NB_HISTOGRAM_MODES];
		for (int i = 0; i < modes.length; i++) {
			modes[i] = 0;
			modeIdxs[i] = 0;
		}

		// compute the top nbHistogramModes modes
		for (int k = 0; k < histData.length; k++) {
			for (int l = 0; l < modes.length; l++) {
				if (histData[k] > modes[l]) {
					// slide all modes down one, to make room for the new one
					for (int m = modes.length - 1; m > l; m--) {
						modes[m] = modes[m - 1];
						modeIdxs[m] = modeIdxs[m - 1];
					}
					modes[l] = histData[k];
					modeIdxs[l] = k;
					break;
				}
			}
		}

		// compute the average of the nbHistogramModes mode locations
		double sum = 0;
		for (int k = 0; k < modes.length; k++) {
			sum += modeIdxs[k];
		}
		int histModeLoc = (int) Math.round(sum / NB_HISTOGRAM_MODES);

		// normalize the hist between 0-1
		sum = 0;
		for (int k = 0; k < histData.length; k++) {
			sum += histData[k];
		}
		sum /= 100;
		for (int k = 0; k < histData.length; k++) {
			histData[k] /= sum;
		}

		// compute the bounds for generating the density metric
		int lowerBound = 3 * (histModeLoc + 1); // +1 is to convert to 1 based
												// indexing for the math
		if (lowerBound >= histData.length) {
			lowerBound = histData.length - 1;
		}
		lowerBound--; // to convert from 1 based index math to 0 based

		// find the maximum value in the histogram
		double maxHistVal = 0;
		for (int k = 0; k < histData.length; k++) {
			maxHistVal = (histData[k] > maxHistVal) ? histData[k] : maxHistVal;
		}

		// find one of the alternate upper bounds
		int idx = 0;
		for (int k = histModeLoc; k < histData.length; k++) {
			if (histData[k] / maxHistVal < 0.05) {
				idx = k;
				break;
			}
		}

		// find the upper bound
		int upperBound = Math.max(idx, 18 * (histModeLoc + 1)); // +1 is to
																// convert to 1
																// based
																// indexing for
																// the math
		if (upperBound >= histData.length) {
			upperBound = histData.length - 1;
		}
		upperBound--; // to convert from 1 based index math to 0 based

		// compute the density metric
		double densityMetric = 0;
		for (int k = lowerBound; k <= upperBound; k++) {
			densityMetric += histData[k];
		}

		// fit a line between the 80th and the 40th percentiles
		double saturation1 = 3;
		double saturation2 = 42;
		double a = (95.0 - 40.0) / (saturation1 - saturation2);
		double b = 95.0 - a * saturation1;

		double prctValue = Math.round(a * densityMetric + b);
		prctValue = (prctValue > 98) ? 98 : prctValue;
		prctValue = (prctValue < 25) ? 25 : prctValue;

		// account for the greedy value
		prctValue -= Math.round(greedy);
		prctValue = (prctValue > 100) ? 100 : prctValue;
		prctValue = (prctValue < 1) ? 1 : prctValue;

		// Log.debug("Percentile Threshold Value: " + prctValue);

		// computing percentiles
		prctValue /= 100.0;
		// compute the percentile threshold, which is the percentile prctValue
		Arrays.sort(nonZeroVoxels);
		// find the prctValue (th) percentile
		double threshold = (nonZeroVoxels.length + 1) * prctValue;
		// constrain
		if (threshold > (nonZeroVoxels.length - 1)) {
			threshold = nonZeroVoxels.length - 1;
		}
		if (threshold < 0) {
			threshold = 0;
		}

		threshold = nonZeroVoxels[(int) threshold];
		// Log.debug("Pixel Threshold Value: " + threshold);

		return threshold;
	}
	
	
	
	
	public static void findEdgesSobel(ImagePlus img3D) {
		
		int xSize = img3D.getWidth();
		int ySize = img3D.getHeight();
		int zSize = img3D.getNSlices();
		ImageStack imgStack = img3D.getStack();
		
		// Sobel filters
		
		final int[][][] sobel3dOperatorZ = {
				{ { -1, -2, -1 }, { -2, -4, -2 }, { -1, -2, -1 } },
				{ { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } },
				{ { 1, 2, 1 }, { 2, 4, 2 }, { 1, 2, 1 } } };
		final int[][][] sobel3dOperatorY = {
				{ { 1, 2, 1 }, { 0, 0, 0 }, { -1, -2, -1 } },
				{ { 2, 4, 2 }, { 0, 0, 0 }, {-2, -4, -2 } },
				{ { 1, 2, 1 }, { 0, 0, 0 }, { -1, -2, -1 } } };
		final int[][][] sobel3dOperatorX = {
				{ { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } },
				{ { -2, 0, 2 }, { -4, 0, 4 }, { -2, 0, 2 } },
				{ { -1, 0, 1 }, { -2, 0, 2 }, {-1, 0, 1 } } };
		
		double minSobel16bits = - 1045860.0;
		double maxSobel16bits = 1045860.0;
		double scale16bits = 65535.0 / (maxSobel16bits - minSobel16bits);
		
		// save current values in array
		int[][][] imageDataBeforeProcessing = new int[xSize][ySize][zSize];
		for(int z = 0; z < zSize; ++ z) {
			ImageProcessor proc = imgStack.getProcessor(z+1);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					imageDataBeforeProcessing[x][y][z] = proc.getPixel(x, y);
				}
			}
		}
		
		// process erosion (min of neighbors)
		for(int z = 0; z < zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					
					double sumGx = 0.0;
					double sumGy = 0.0;
					double sumGz = 0.0;
					
					for(int k = - 1; k <= 1; k ++) {
						int zNeighbor = z - k;
						if(zNeighbor >= 0 && zNeighbor < zSize) {
							
							for(int i = - 1; i <= 1; i ++) {
								int xNeighbor = x - i;
								if(xNeighbor >= 0 && xNeighbor < xSize) {
									
									for(int j = - 1; j <= 1; j ++) {
										int yNeighbor = y - j;
										if(yNeighbor >= 0 && yNeighbor < ySize) {	
											
											sumGx += imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor] * sobel3dOperatorX[k+1][i+1][j+1];
											sumGy += imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor] * sobel3dOperatorY[k+1][i+1][j+1];
											sumGz += imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor] * sobel3dOperatorZ[k+1][i+1][j+1];
										}
									}
								}
								
							}
						}
						
					}
					
					double sobelValue = Math.sqrt(sumGx*sumGx + sumGy*sumGy + sumGz*sumGz);
					sobelValue = (sobelValue - minSobel16bits) * scale16bits;
					if (sobelValue<0.0) sobelValue = 0.0;
	                if (sobelValue>65535.0) sobelValue = 65535.0;
	                imgStack.setVoxel(x, y, z, sobelValue);
					
				}
			}
		}
	}
	
	
	
	public double getEGTThrehold(ImagePlus grayImp, int greedy) {

	    ImageStack grayStack = grayImp.getStack();
	    int nbSlices = grayStack.getSize();

	    // compute the gradient for the input image stack
	    ImageStack edgeStack = new ImageStack(grayImp.getWidth(), grayImp.getHeight());
	    for (int i = 1; i <= nbSlices; i++) {
	      ImageProcessor ip = grayStack.getProcessor(i);
	      ip = ip.convertToFloat();
	      ip.findEdges();
	      edgeStack.addSlice(ip);
	    }

	    double[] hist_data = computeHistogram(edgeStack);

	    double percThreshold = computePercentileThreshold(hist_data);

	    // account for the greedy adjustment
	    percThreshold -= Math.round(greedy);
	    // ensure valid bounds
	    percThreshold = (percThreshold > 100) ? 100 : percThreshold;
	    percThreshold = (percThreshold < 1) ? 1 : percThreshold;

	    double pixelThreshold = convertPercThresholdToPixelThreshold(edgeStack, percThreshold);

	    System.out.println("Computed Threshold: " + pixelThreshold);
	    return pixelThreshold;
	  }


	  private double convertPercThresholdToPixelThreshold(ImageStack edgeStack, double percThreshold) {

	    int nbSlices = edgeStack.getSize();

	    int nNonZeroPixels = 0;
	    // generate sorted list of non zero pixel values
	    for (int i = 1; i <= nbSlices; i++) {
	      float[] pix = (float[]) edgeStack.getProcessor(i).getPixels();
	      for (int k = 0; k < pix.length; k++) {
	        if (pix[k] > 0) {
	          nNonZeroPixels++;
	        }
	      }
	    }

	    float[] nonzeroPixels = new float[nNonZeroPixels];
	    int k2 = 0;
	    for (int i = 1; i <= nbSlices; i++) {
	      float[] pix = (float[]) edgeStack.getProcessor(i).getPixels();
	      for (int k = 0; k < pix.length; k++) {
	        if (pix[k] > 0) {
	          nonzeroPixels[k2++] = pix[k];
	        }
	      }
	    }

	    // sort the pixels
	    Arrays.sort(nonzeroPixels);

	    // find the pixel value threshold index using the percentile threshold
	    double pixelThresholdIndex = (nonzeroPixels.length + 1) * (percThreshold/100);

	    // constrain to valid values
	    if (pixelThresholdIndex > (nonzeroPixels.length - 1)) {
	      pixelThresholdIndex = nonzeroPixels.length - 1;
	    }
	    if (pixelThresholdIndex < 0) {
	      pixelThresholdIndex = 0;
	    }

	    // find and return the pixel value threshold using the index
	    return nonzeroPixels[(int) pixelThresholdIndex];
	  }

	  private double computePercentileThreshold(double[] hist_data) {
	    // compute the averaged mode for the histogram
	    double[] modes = new double[NB_HISTOGRAM_MODES];
	    int[] modeIdxs = new int[NB_HISTOGRAM_MODES];
	    for (int k = 0; k < modes.length; k++) {
	      modes[k] = 0;
	      modeIdxs[k] = 0;
	    }

	    // compute the top nbHistogramModes modes
	    for (int k = 0; k < hist_data.length; k++) {
	      for (int l = 0; l < modes.length; l++) {
	        if (hist_data[k] > modes[l]) {
	          // slide all modes down one, to make room for the new one
	          for (int m = modes.length - 1; m > l; m--) {
	            modes[m] = modes[m - 1];
	            modeIdxs[m] = modeIdxs[m - 1];
	          }
	          modes[l] = hist_data[k];
	          modeIdxs[l] = k;
	          break;
	        }
	      }
	    }

	    // compute the average of the nbHistogramModes mode locations
	    double sum = 0;
	    for (int k = 0; k < modes.length; k++) {
	      sum += modeIdxs[k];
	    }
	    int histModeLoc = (int) Math.round(sum / NB_HISTOGRAM_MODES);

	    // normalize the hist between 0-1
	    sum = 0;
	    for (int k = 0; k < hist_data.length; k++) {
	      sum += hist_data[k];
	    }
	    sum /= 100;
	    for (int k = 0; k < hist_data.length; k++) {
	      hist_data[k] /= sum;
	    }

	// compute the bounds for generating the density metric
	    int lowerBound = 3 * (histModeLoc + 1); // +1 is to convert to 1 based indexing for the math
	    if (lowerBound >= hist_data.length) {
	      lowerBound = hist_data.length - 1;
	    }
	    lowerBound--; // to convert from 1 based index math to 0 based

	    // find the maximum value in the histogram
	    double maxHistVal = 0;
	    for (int k = 0; k < hist_data.length; k++) {
	      maxHistVal = (hist_data[k] > maxHistVal) ? hist_data[k] : maxHistVal;
	    }

	// find one of the alternate upper bounds
	    int idx = 0;
	    for (int k = histModeLoc; k < hist_data.length; k++) {
	      if (hist_data[k] / maxHistVal < 0.05) {
	        idx = k;
	        break;
	      }
	    }

	    // find the upper bound
	    int upperBound =
	        Math.max(idx, 18 * (histModeLoc + 1)); // +1 is to convert to 1 based indexing for the math
	    if (upperBound >= hist_data.length) {
	      upperBound = hist_data.length - 1;
	    }
	    upperBound--; // to convert from 1 based index math to 0 based

	    // compute the density metric
	    double densityMetric = 0;
	    for (int k = lowerBound; k <= upperBound; k++) {
	      densityMetric += hist_data[k];
	    }

	    // fit a line between the 80th and the 40th percentiles
	    double saturation1 = 3;
	    double saturation2 = 42;
	    double a = (95.0 - 40.0) / (saturation1 - saturation2);
	    double b = 95.0 - a * saturation1;

	    double prctValue = Math.round(a * densityMetric + b);
	    prctValue = (prctValue > 98) ? 98 : prctValue;
	    prctValue = (prctValue < 25) ? 25 : prctValue;

	    // return the percentile threshold
	    return prctValue;
	  }


	  private double[] computeHistogram(ImageStack edgeStack) {
	    int nbSlices = edgeStack.getSize();

	    // extract min and max of the non zero pixels
	    float minValue = Float.MAX_VALUE;
	    float maxValue = Float.MIN_VALUE;
	    for (int i = 1; i <= nbSlices; i++) {
	      ImageProcessor ip = edgeStack.getProcessor(i);
	      float[] pix = (float[]) ip.getPixels();
	      for (int k = 0; k < pix.length; k++) {
	        if (pix[k] > 0) {
	          minValue = (pix[k] < minValue) ? pix[k] : minValue;
	          maxValue = (pix[k] > maxValue) ? pix[k] : maxValue;
	        }
	      }
	    }

	    // check that min and max are valid
	    if (minValue == Float.MAX_VALUE || maxValue == Float.MIN_VALUE) {
	      throw new IllegalArgumentException("Input Image has no nonzero gradient pixels");
	    }

	    // generate the histogram of the nonzero pixels
	    int nbBins = 1000;
	    double[] hist_data = new double[nbBins + 1];

	    // populate the histogram bins
	    double rescale = nbBins / (maxValue - minValue);
	    for (int i = 1; i <= nbSlices; i++) {
	      ImageProcessor ip = edgeStack.getProcessor(i);
	      float[] pix = (float[]) ip.getPixels();
	      for (int k = 0; k < pix.length; k++) {
	        if (pix[k] > 0) {
	          // + 0.5 is to center the bins at [0 1] instead of [-0.5 0.5]
	          hist_data[(int) ((pix[k] - minValue) * rescale + 0.5)]++;
	        }
	      }
	    }
	    return hist_data;
	  }

	
	
	
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
