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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class Image3DSmoothing {

	public static void grayscaleFlatErosion(ImagePlus img3D, int xRadius, int yRadius, int zRadius) {
		
		int xSize = img3D.getWidth();
		int ySize = img3D.getHeight();
		int zSize = img3D.getNSlices();
		ImageStack imgStack = img3D.getStack();
		
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
					
					int minNeighborsValue = Integer.MAX_VALUE;
					
					for(int zNeighbor = z - zRadius; zNeighbor <= z + zRadius; zNeighbor ++) {
						if(zNeighbor >= 0 && zNeighbor < zSize) {
							
							for(int xNeighbor = x - xRadius; xNeighbor <= x + xRadius; xNeighbor ++) {
								if(xNeighbor >= 0 && xNeighbor < xSize) {
									
									for(int yNeighbor = y - yRadius; yNeighbor <= y + xRadius; yNeighbor ++) {
										if(yNeighbor >= 0 && yNeighbor < ySize) {
											
											if(imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor] < minNeighborsValue)
												minNeighborsValue = imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor];
										}
									}
								}
								
							}
						}
						
					}
					
					if(minNeighborsValue < Integer.MAX_VALUE) 
						imgStack.setVoxel(x, y, z, minNeighborsValue);
				}
			}
		}
	}
	
	public static void grayscaleFlatDilation(ImagePlus img3D, int xRadius, int yRadius, int zRadius) {
		
		int xSize = img3D.getWidth();
		int ySize = img3D.getHeight();
		int zSize = img3D.getNSlices();
		ImageStack imgStack = img3D.getStack();
		
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
				
		// process dilation (max of neighbors)
		for(int z = 0; z < zSize; ++ z) {
			for(int x = 0; x < xSize; ++ x) {
				for(int y = 0; y < ySize; ++ y) {
					
					int maxNeighborsValue = Integer.MIN_VALUE;
					
					for(int zNeighbor = z - zRadius; zNeighbor <= z + zRadius; zNeighbor ++) {
						if(zNeighbor >= 0 && zNeighbor < zSize) {
							
							for(int xNeighbor = x - xRadius; xNeighbor <= x + xRadius; xNeighbor ++) {
								if(xNeighbor >= 0 && xNeighbor < xSize) {
									
									for(int yNeighbor = y - yRadius; yNeighbor <= y + xRadius; yNeighbor ++) {
										if(yNeighbor >= 0 && yNeighbor < ySize) {
											
											if(imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor] > maxNeighborsValue)
												maxNeighborsValue = imageDataBeforeProcessing[xNeighbor][yNeighbor][zNeighbor];
										}
									}
								}
								
							}
						}
						
					}
					
					if(maxNeighborsValue > Integer.MIN_VALUE) 
						imgStack.setVoxel(x, y, z, maxNeighborsValue);
				}
			}
		}
	}

	
}
