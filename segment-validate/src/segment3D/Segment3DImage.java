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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * This is a class to segment a 3D FITS file with a given threshold and compute
 * the number of objects in the image larger than N voxels
 * 
 * @author Mylene Simon & Derek Juba
 * 
 */

public class Segment3DImage {
	
	private static Log logger = LogFactory
			.getLog(Segment3DImage.class);
	
	private ImagePlus segmentedImagePlus;

	private int[][][] imageData;
	private int xSize;
	private int ySize;
	private int zSize;
	private long frgCount;
	private long bkgCount;
	
	public final static int NO_MORPHOLOGICAL_OPERATIONS = 0;
	public final static int CLOSING_FIRST_MORPHOLOGICAL_OPERATIONS = 1;
	public final static int OPENING_FIRST_MORPHOLOGICAL_OPERATIONS = 2;
	public final static int OPENING_MORPHOLOGICAL_OPERATIONS = 3;
	public final static int CLOSING_MORPHOLOGICAL_OPERATIONS = 4;
	public final static int DILATE_MORPHOLOGICAL_OPERATIONS = 5;
	public final static int ERODE_MORPHOLOGICAL_OPERATIONS = 6;
	
	private boolean isThresholded = false;

	/**
	 * Constructor of Segment3DImage class
	 * Constructs the 3D thresholded voxel table
	 * 
	 * @param image3D The input 3D ImagePlus
	 * @param threshold The threshold
	 */
	public Segment3DImage(ImagePlus image3D) {
		
		this.segmentedImagePlus = image3D.createImagePlus();
		
		this.xSize = image3D.getWidth();
		this.ySize = image3D.getHeight();
		this.zSize = image3D.getNSlices();
		
		imageData = new int[xSize][ySize][zSize];
		frgCount = 0;
		bkgCount = 0;
		
		// Fill thresholdedImage with voxel intensities
		ImageStack imgStack = image3D.getStack();
		for(int z = 1; z <= zSize; ++ z) {
			ImageProcessor imgProc = imgStack.getProcessor(z);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					imageData[x][y][z-1] = imgProc.getPixel(x, y);
				}
			}
		}
		//System.out.println("Frg count = " + _frgCount + " and bkg count = "
		//		+ _bkgCount + " for threshold = " + threshold);
	}
	
	
	/**
	 * Compute the number of objects larger than a given number of pixels
	 * 
	 * @param n Number of pixels
	 * @return The objects larger than n pixels
	 */
	public int getNumberOfObjectsLargerThanNPixels(int n, int threshold) {
		
		// Initializations
		int numberOfObjectsFound = 0;
		
		// threshold the image if it is not already thresholded
		if(! isThresholded)
			thresholdImage(threshold);
		
		int nextComponentId = 2;
		
		// Get objects sizes
		for(int z = 0; z < zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					
					if(imageData[x][y][z] == 1) {
						
						int componentSize = nonRecursiveFlood(1,
								nextComponentId, x, y, z);
						
						if(componentSize > n)
							numberOfObjectsFound ++;
						
						nextComponentId ++;
					}
				}
			}
		}
		
		return numberOfObjectsFound;
	}
	
	public ImagePlus segmentImage(int threshold, int morphologicalOperationChoice, int morphologicalOperationRadius) {
		
		logger.info("Starting segmentation of image...");
		
		thresholdImage(threshold);
		//removeEdgeComponents(); used with cell segmentation
		//fillHoles(); was not used with cell segmentation
		//makeSingleComponent(); used with cell segmentation
		
		if(morphologicalOperationChoice != NO_MORPHOLOGICAL_OPERATIONS)
			applyMorphologicalOperations(morphologicalOperationRadius, morphologicalOperationChoice);
		
		// Update the segmented image ImagePlus object
		return generateSegmentedImagePlus();
		
	}
	
	public ImagePlus segmentImagePerFrame(int [] threshold, int morphologicalOperationChoice, int morphologicalOperationRadius) {
		
		logger.info("Starting segmentation of image...");
		
		thresholdImage(threshold);
		removeEdgeComponents();
		//fillHoles();
		makeSingleComponent();
		if(morphologicalOperationChoice != NO_MORPHOLOGICAL_OPERATIONS)
			applyMorphologicalOperations(morphologicalOperationRadius, morphologicalOperationChoice);
		
		// Update the segmented image ImagePlus object
		return generateSegmentedImagePlus();
		
	}
	/**
	 * @return the segmentedImagePlus (8bit binary image generated after all the
	 *         segmentation steps applied)
	 */
	public ImagePlus generateSegmentedImagePlus() {
		// Update the segmented image ImagePlus object
		logger.info("Creating the segmented ImagePlus object");
		ImageStack imgStack = new ImageStack(xSize, ySize);
		for(int z = 0; z < zSize; ++ z) {
			
			byte[] sliceData = new byte[xSize * ySize];
			
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					sliceData[y * xSize + x] = (byte) ((imageData[x][y][z] == 1) ? 255 : 0);
				}
			}
			
			ByteProcessor sliceProc = new ByteProcessor(xSize, ySize,
					sliceData);
			imgStack.addSlice(sliceProc);
		}
		segmentedImagePlus.setStack(imgStack);
		
		return segmentedImagePlus;
	}
	
	/**
	 * @return the ImagePlus object of the internal INT 3D image
	 */
	public ImagePlus generateImagePlus() {
		// Update the segmented image ImagePlus object
		logger.info("Creating the segmented ImagePlus object");
		ImageStack imgStack = new ImageStack(xSize, ySize);
		for(int z = 0; z < zSize; ++ z) {
			
			short[] sliceData = new short[xSize * ySize];
			
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					sliceData[y * xSize + x] = (short) (imageData[x][y][z] );
				}
			}
			
			ShortProcessor sliceProc = new ShortProcessor(xSize, ySize,sliceData,null);
			imgStack.addSlice(sliceProc);
		}
		ImagePlus objImagePlus = new ImagePlus();
		objImagePlus.setStack(imgStack);		
		return objImagePlus;
	}
	/**
	 * This method performs boolean AND between the input image and a binary mask 
	 * @param mask3D - mask 3D image
	 * @param bkgValue - the value in mask3D image for which image3D values will be set to zero
	 * @return
	 */
	public boolean applyBinaryMask(ImagePlus mask3D, int bkgValue) {
		
		if(mask3D == null){
			System.err.println("mask3D is null");
			return false;
		}
		
		int xMaskSize = mask3D.getWidth();
		int yMaskSize = mask3D.getHeight();
		int zMaskSize = mask3D.getNSlices();
		if(xMaskSize!= this.xSize || yMaskSize!= this.ySize || zMaskSize!= this.zSize){
			System.err.println("mask3D does not match image3D ");
			System.err.println("mask3D x="+ xMaskSize +", y="+yMaskSize+", z="+zMaskSize);
			System.err.println("image3D x="+ xSize +", y="+ySize+", z="+zSize);		
			return false;			
		}
		logger.info("Applying 3D mask by AND operation...");
		
		// zero voxel intensities that are outside of the mask
		ImageStack imgStack = mask3D.getStack();
		for(int z = 1; z <= zSize; ++ z) {
			ImageProcessor imgProc = imgStack.getProcessor(z);
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if( imgProc.getPixel(x, y)  == bkgValue ){
						imageData[x][y][z-1] = 0;
					}
				}
			}
		}
	
		logger.info("Applying 3D mask by AND operation");
		
		return true;
	}

	
	public long thresholdImage(int threshold) {
		
		logger.info("Thresholding image...");
		frgCount = 0;
		bkgCount = 0;
		
		// Fill thresholdedImage with 1 if pixel value > threshold, 0 otherwise
		
		for(int z = 1; z <= zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if(imageData[x][y][z-1] > threshold) {
						frgCount ++;
						imageData[x][y][z-1] = 1;
					}
					else {
						bkgCount ++;
						imageData[x][y][z-1] = 0;
					}
				}
			}
		}
		
		isThresholded = true;
		
		logger.info("Thresholding done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		
		return frgCount;
	}
	
	/**
	 * This class allows to threshold each frame of a z-stack with a separate threshold
	 * 
	 * @param threshold array of size equal to the number of z-frames
	 * @return
	 */
	public long thresholdImage(int [] threshold) {
		if(threshold == null){
			logger.error("missing threshold array");
			return -1;
		}
		if(threshold.length != zSize){
			logger.error(" threshold array size does not match zSize");
			return -1;			
		}
		logger.info("Thresholding image...");
		frgCount = 0;
		bkgCount = 0;
		
		// Fill thresholdedImage with 1 if pixel value > threshold, 0 otherwise
		
		for(int z = 1; z <= zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if(imageData[x][y][z-1] > threshold[z-1]) {
						frgCount ++;
						imageData[x][y][z-1] = 1;
					}
					else {
						bkgCount ++;
						imageData[x][y][z-1] = 0;
					}
				}
			}
		}
		
		isThresholded = true;
		
		logger.info("Thresholding done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		
		return frgCount;
	}
	
	public long removeEdgeComponents() {
		
		logger.info("Removing edge components...");

		int numVoxels[] = new int[] { xSize, ySize, zSize };

		// only check the xz and yz planes
		for (int skipDim = 0; skipDim < 2; skipDim++) {
			int deltas[] = new int[3];

			for (int dim = 0; dim < 3; dim++) {
				if ((dim == skipDim) && (numVoxels[dim] > 1)) {
					deltas[dim] = numVoxels[dim] - 1;
				} else {
					deltas[dim] = 1;
				}
			}

			for (int z = 0; z < zSize; z += deltas[2]) {
				for (int y = 0; y < ySize; y += deltas[1]) {
					for (int x = 0; x < xSize; x += deltas[0]) {
						if(imageData[x][y][z] == 1) {
							//logger.info("removing edge component at coordinate: " + x + " " + y + " " + z);
							nonRecursiveFlood(1, 0, x, y, z);
						}
					}
				}
			}
		}

		updateForegroundBackgroundCounts();

		logger.info("Removing edge components done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		
		return frgCount;
	}

	public long fillHoles() {
		
		logger.info("Filling holes...");

		if (imageData[0][0][0] != 0) {
			System.err
					.println("Error filling holes- corner voxel is not empty");
			return (1);
			// TODO : throw specific error
		}

		nonRecursiveFlood(0, 2, 0, 0, 0);
		logger.info("Marking bck done.");

		for (int z = 0; z < zSize; z++) {
			for (int y = 0; y < ySize; y++) {
				for (int x = 0; x < xSize; x++) {

					if (imageData[x][y][z] == 2) {
						imageData[x][y][z] = 0;
					} else if (imageData[x][y][z] == 0) {
						imageData[x][y][z] = 1;
					}
				}
			}
		}

		updateForegroundBackgroundCounts();
		logger.info("Filling holes done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		return frgCount;
	}

	public long makeSingleComponent() {
		
		logger.info("Making single component...");

		int maxComponentId = 2;
		int maxComponentSize = 0;
		
		int nextComponentId = 2;
		
		// Get objects sizes and find largest component
		for(int z = 0; z < zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					
					if(imageData[x][y][z] == 1) {
						
						int componentSize = nonRecursiveFlood(1,
								nextComponentId, x, y, z);
						if(componentSize > maxComponentSize) {
							maxComponentId = nextComponentId;
							maxComponentSize = componentSize;
						}
						
						nextComponentId ++;
					}
				}
			}
		}
		
		for (int z = 0; z < zSize; z++) {
			for (int y = 0; y < ySize; y++) {
				for (int x = 0; x < xSize; x++) {

					if (imageData[x][y][z] == maxComponentId) {
						imageData[x][y][z] = 1;
					} else if (imageData[x][y][z] != 0) {
						imageData[x][y][z] = 0;
					}
				}
			}
		}

		updateForegroundBackgroundCounts();
		
		logger.info("Making single component done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		
		return frgCount;
	}

	public long applyMorphologicalOperations(int radius, int morphologicalOperationChoice) {
		
		logger.info("Applying morphological operations...");

		switch(morphologicalOperationChoice) {
			case NO_MORPHOLOGICAL_OPERATIONS: break;

			case CLOSING_MORPHOLOGICAL_OPERATIONS: 
				// closing
				dilate(radius);
				erode(radius);
				break;

			case OPENING_MORPHOLOGICAL_OPERATIONS: 
				// opening
				erode(radius);
				dilate(radius);
				break;
				
			case DILATE_MORPHOLOGICAL_OPERATIONS: 
				dilate(radius);
				break;

			case ERODE_MORPHOLOGICAL_OPERATIONS: 
				erode(radius);
				break;
				
			case CLOSING_FIRST_MORPHOLOGICAL_OPERATIONS: 
				// closing
				dilate(radius);
				erode(radius);
				// opening
				erode(radius);
				dilate(radius);
				break;
			
			case OPENING_FIRST_MORPHOLOGICAL_OPERATIONS: 
				// opening
				erode(radius);
				dilate(radius);
				// closing
				dilate(radius);
				erode(radius);
				break;
				
			default: 
				System.err.println("Invalid choice for morphological operations.");
				//throw new Exception("Invalid choice for morphological operations.");
				break;
			
		}
		// make single component to avoid disconnected segments
		makeSingleComponent();
		
		updateForegroundBackgroundCounts();
		logger.info("Applying morphological operations done. FRG count: " + frgCount + " BKG count: " + bkgCount);
		
		return frgCount;
	}
	
	public void dilate(int radius) {
		dilateOrErode(radius, 0, 1);
	}
	
	public void erode(int radius) {
		dilateOrErode(radius, 1, 0);
	}
	
	private void dilateOrErode(int radius, int fromValue, int toValue) {

		int centerXYZ[] = new int[3];
		int offsetXYZ[] = new int[3];

		for (centerXYZ[2] = 0; centerXYZ[2] < zSize; centerXYZ[2]++) {
			for (centerXYZ[1] = 0; centerXYZ[1] < ySize; centerXYZ[1]++) {
				for (centerXYZ[0] = 0; centerXYZ[0] < xSize; centerXYZ[0]++) {

					if (imageData[centerXYZ[0]][centerXYZ[1]][centerXYZ[2]] == toValue) {

						for (offsetXYZ[2] = centerXYZ[2] - radius; offsetXYZ[2] <= centerXYZ[2]
								+ radius; offsetXYZ[2]++) {

							for (offsetXYZ[1] = centerXYZ[1] - radius; offsetXYZ[1] <= centerXYZ[1]
									+ radius; offsetXYZ[1]++) {

								for (offsetXYZ[0] = centerXYZ[0] - radius; offsetXYZ[0] <= centerXYZ[0]
										+ radius; offsetXYZ[0]++) {

									if ((offsetXYZ[2] >= 0)
											&& (offsetXYZ[2] < zSize)
											&& (offsetXYZ[1] >= 0)
											&& (offsetXYZ[1] < ySize)
											&& (offsetXYZ[0] >= 0)
											&& (offsetXYZ[0] < xSize)) {

										if (imageData[offsetXYZ[0]][offsetXYZ[1]][offsetXYZ[2]] == fromValue) {
											imageData[offsetXYZ[0]][offsetXYZ[1]][offsetXYZ[2]] = 2;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (int z = 0; z < zSize; ++z) {
			for (int x = 0; x < xSize; ++x) {
				for (int y = 0; y < ySize; ++y) {
					if (imageData[x][y][z] == 2) {
						imageData[x][y][z] = toValue;
					}
				}
			}
		}

	}
	
	
	/**
	 * Find and label a component in the image flooding from the pixel at
	 * position (x,y,z)
	 * 
	 * @param fromLabel
	 *            Label of the pixel in thresholded image to find
	 * @param toLabel
	 *            Label put on the found label
	 * @param x
	 *            Start x position
	 * @param y
	 *            Start y position
	 * @param z
	 *            Start z position
	 * @return Size in pixels of the component
	 */
	private int nonRecursiveFlood(int fromLabel, int toLabel, int x, int y, int z) {
		
		//logger.info("flooding from " + x + " " + y + " " + z + "...");
		// Initialize number of pixels in the component and number of pixels to visit
		int numVoxelsFilled = 0;
		int xyzToVisitSize = 0;
		
		int arraySize = xSize * ySize * (zSize/2 + 1);
		if(arraySize == 0) arraySize = 1;
		
		int[] voxelToVisitX = new int[arraySize];
		int[] voxelToVisitY = new int[arraySize];
		int[] voxelToVisitZ = new int[arraySize];
		
		// List of pixels to visit (located with their (x,y,z) coordinates)
		//List<XYZToVisit> xyzToVisitList = new ArrayList<XYZToVisit>();
		//xyzToVisitList.add(new XYZToVisit(x, y, z));
		voxelToVisitX[0] = x;
		voxelToVisitY[0] = y;
		voxelToVisitZ[0] = z;
		
		xyzToVisitSize ++;
		
		// Visit pixels to visit
		while(xyzToVisitSize > 0) {
			xyzToVisitSize --;
			
			XYZToVisit currentXYZ = new XYZToVisit(voxelToVisitX[xyzToVisitSize], voxelToVisitY[xyzToVisitSize], voxelToVisitZ[xyzToVisitSize]);
			
			// If pixel is labeled with label of interest, change label and flood
			if(imageData[currentXYZ.x][currentXYZ.y][currentXYZ.z] == fromLabel) {
				imageData[currentXYZ.x][currentXYZ.y][currentXYZ.z] = toLabel;
				
				for (int zOffset = -1; zOffset <= 1; zOffset++) {
			      for (int yOffset = -1; yOffset <= 1; yOffset++) {
				      for (int xOffset = -1; xOffset <= 1; xOffset++) {
				    	  
				    	  if (Math.abs(zOffset) + Math.abs(yOffset) + Math.abs(xOffset) != 1) {
				    		  continue;
				    	  }
				    	  
				    	  if ((currentXYZ.z + zOffset < 0) || 
			    			  (currentXYZ.y + yOffset < 0) || 
			    			  (currentXYZ.x + xOffset < 0) || 
			    			  (currentXYZ.z + zOffset >= zSize) ||
			    			  (currentXYZ.y + yOffset >= ySize) ||
			    			  (currentXYZ.x + xOffset >= xSize)) {
				    		  continue;
				    	  }
				    	  
							// Add a new pixel to visit in the list and increase
							// number of pixels to visit
							/*if (xyzToVisitList.size() <= xyzToVisitSize)
								xyzToVisitList.add(new XYZToVisit(currentXYZ.x
										+ xOffset, currentXYZ.y + yOffset,
										currentXYZ.z + zOffset));
							else
								xyzToVisitList.set(xyzToVisitSize,
										new XYZToVisit(currentXYZ.x + xOffset,
												currentXYZ.y + yOffset,
												currentXYZ.z + zOffset));*/
				    	  
				    	    if(xyzToVisitSize == arraySize) {
				    	    	
				    	    	logger.info("Increasing array size to: " + arraySize * 2);
				    	    	int[] newVoxelToVisitX = new int[arraySize * 2];
				    			int[] newVoxelToVisitY = new int[arraySize * 2];
				    			int[] newVoxelToVisitZ = new int[arraySize * 2];
				    			System.arraycopy(voxelToVisitX, 0, newVoxelToVisitX, 0, arraySize);
				    			System.arraycopy(voxelToVisitY, 0, newVoxelToVisitY, 0, arraySize);
				    			System.arraycopy(voxelToVisitZ, 0, newVoxelToVisitZ, 0, arraySize);
				    			voxelToVisitX = newVoxelToVisitX;
				    			voxelToVisitY = newVoxelToVisitY;
				    			voxelToVisitZ = newVoxelToVisitZ;
				    			arraySize *= 2;
				    			
				    			System.gc();
				    	    }
				    	  
				    	    voxelToVisitX[xyzToVisitSize] = currentXYZ.x + xOffset;
				    	    voxelToVisitY[xyzToVisitSize] = currentXYZ.y + yOffset;
				    	    voxelToVisitZ[xyzToVisitSize] = currentXYZ.z + zOffset;

							xyzToVisitSize++;

				      }
			      }
				}
				 // Count number of pixels in the component
				numVoxelsFilled ++;
			}
		}
		
		// Return number of pixels in the component
		return numVoxelsFilled;
	}
	
	private long updateForegroundBackgroundCounts() {
		  
		this.frgCount = 0;
		this.bkgCount = 0;
		
		for(int z = 1; z <= zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					if(imageData[x][y][z-1] == 1) {
						frgCount ++;
					}
					else {
						bkgCount ++;
					}
				}
			}
		}
		
		return frgCount;
		}
	
	
	public long getFRGCount(){
		updateForegroundBackgroundCounts();
		return frgCount;
	}
	public long getBKGCount(){
		updateForegroundBackgroundCounts();
		return bkgCount;
	}

	/**
	 * Class used in nonRecursiveFlood method (helper class)
	 * 
	 * @author Mylene Simon
	 *
	 */
	private class XYZToVisit {
		
		public int x;
		public int y;
		public int z;
		
		public XYZToVisit(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

	}
}
