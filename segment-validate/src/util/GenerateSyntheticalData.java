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
package util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import io.Fits3DWriter;

/**
 * This is a util class to generate synthetic data for testing purposes
 * 
 * @author mhs1
 *
 */
public class GenerateSyntheticalData {

	/**
	 * Generate a 3D FITS file representing the 3D image of a sphere of ~30um
	 * @param outputFolder Output folder
	 */
	public static void generateSphere30Micrometers(String outputFolder) {

		ImagePlus img3DSphere;
		
		// create images for tests:
		int numrows = 200;
		int numcols = 200;
		int totalPixels = 200 * 200;
		
		// Create 3D sphere image
		img3DSphere = new ImagePlus();
		img3DSphere.setTitle("img3DSphere30um");
		ImageStack img3DSphereStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 200; ++ z) {
			byte[] img3DSphereData = new byte[totalPixels];
			
			for(int x = 0; x < 200; ++ x) {
				for(int y = 0; y < 200; ++y) {
					if (Math.pow(((double) x * 0.240 - 24.0), 2)
							+ Math.pow(((double) y * 0.240 - 24.0), 2)
							+ Math.pow(((double) z * 0.713 - 71.3), 2) 
						<= 225)
						img3DSphereData[y * numcols + x] = (byte) 255;
				}
			}
			
			ByteProcessor img3DSphereProc = new ByteProcessor(numcols, numrows,
					img3DSphereData);
			img3DSphereStack.addSlice(img3DSphereProc);
		}
		
		img3DSphere.setStack(img3DSphereStack);
		
		// Set the calibration for voxel dimensions
		Calibration newCalibration = img3DSphere.getCalibration();
		newCalibration.pixelWidth = 0.240;
		newCalibration.pixelHeight = 0.240;
		newCalibration.pixelDepth = 0.713;
		newCalibration.setXUnit("um");
		img3DSphere.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DSphere30um.fits", img3DSphere);

	}
	
	/**
	 * Generate a 3D FITS file representing the 3D image of an ellipsoid
	 * @param outputFolder Output folder
	 */
	public static void generateEllipsoid(String outputFolder) {

		ImagePlus img3DEllipsoid;
		
		// create images for tests:
		int numrows = 200;
		int numcols = 200;
		int totalPixels = 200 * 200;
		
		// Create 3D sphere image
		img3DEllipsoid = new ImagePlus();
		img3DEllipsoid.setTitle("img3DEllipsoid");
		ImageStack img3DEllipsoidStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 200; ++ z) {
			byte[] img3DEllipsoidData = new byte[totalPixels];
			
			for(int x = 0; x < 200; ++ x) {
				for(int y = 0; y < 200; ++y) {
					if (Math.pow(((double) x * 0.240- 24.0), 2)/Math.pow(15, 2)
							+ Math.pow(((double) y * 0.240 - 24.0), 2)/Math.pow(10, 2)
							+ Math.pow(((double) z * 0.713 - 71.3), 2)/Math.pow(3, 2)
						<= 1.0)
						img3DEllipsoidData[y * numcols + x] = (byte) 255;
				}
			}
			
			ByteProcessor img3DEllipsoidProc = new ByteProcessor(numcols, numrows,
					img3DEllipsoidData);
			img3DEllipsoidStack.addSlice(img3DEllipsoidProc);
		}
		
		img3DEllipsoid.setStack(img3DEllipsoidStack);
		
		// Set the calibration for voxel dimensions
		Calibration newCalibration = img3DEllipsoid.getCalibration();
		newCalibration.pixelWidth = 0.240;
		newCalibration.pixelHeight = 0.240;
		newCalibration.pixelDepth = 0.713;
		newCalibration.setXUnit("um");
		img3DEllipsoid.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DEllipsoid_seg.fits", img3DEllipsoid);

	}
	
	/**
	 * Generate a 3D FITS file representing the 3D image of a cylinder
	 * @param outputFolder Output folder
	 */
	public static void generateCylinder(String outputFolder) {

		ImagePlus img3DCylinder;
		
		// create images for tests:
		int numrows = 200;
		int numcols = 200;
		int totalPixels = 200 * 200;
		
		// Create 3D sphere image
		img3DCylinder = new ImagePlus();
		img3DCylinder.setTitle("img3DCylinder");
		ImageStack img3DCylinderStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 75; ++ z) {
			
			byte[] img3DCylinderData = new byte[totalPixels];
			ByteProcessor img3DEllipsoidProc = new ByteProcessor(numcols, numrows,
					img3DCylinderData);
			img3DCylinderStack.addSlice(img3DEllipsoidProc);
		}
		
		for(int z = 75; z < 125; ++ z) {
			byte[] img3DCylinderData = new byte[totalPixels];
			
			for(int x = 0; x < 200; ++ x) {
				for(int y = 0; y < 200; ++y) {
					if (Math.pow(((double) x * 0.240- 24.0), 2)/Math.pow(7.5, 2)
							+ Math.pow(((double) y * 0.240 - 24.0), 2)/Math.pow(7.5, 2)
						<= 1.0)
						img3DCylinderData[y * numcols + x] = (byte) 255;
				}
			}
			
			ByteProcessor img3DEllipsoidProc = new ByteProcessor(numcols, numrows,
					img3DCylinderData);
			img3DCylinderStack.addSlice(img3DEllipsoidProc);
		}
		
		for(int z = 125; z < 200; ++ z) {
			
			byte[] img3DCylinderData = new byte[totalPixels];
			ByteProcessor img3DEllipsoidProc = new ByteProcessor(numcols, numrows,
					img3DCylinderData);
			img3DCylinderStack.addSlice(img3DEllipsoidProc);
		}
		
		img3DCylinder.setStack(img3DCylinderStack);
		
		// Set the calibration for voxel dimensions
		Calibration newCalibration = img3DCylinder.getCalibration();
		newCalibration.pixelWidth = 0.240;
		newCalibration.pixelHeight = 0.240;
		newCalibration.pixelDepth = 0.713;
		newCalibration.setXUnit("um");
		img3DCylinder.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DCylinder_seg.fits", img3DCylinder);

	}
	
	/**
	 * Generate a 3D FITS file representing the 3D image of a cube of 
	 * ~100x100x100 voxels
	 * @param outputFolder Output folder
	 */
	public static void generateCube100voxels(String outputFolder) {

		ImagePlus img3DCube;
		
		// create images for tests:
		int numrows = 200;
		int numcols = 200;
		int depth = 200;
		int totalPixels = 200 * 200;
		
		// Create 3D sphere image
		img3DCube = new ImagePlus();
		img3DCube.setTitle("img3DCube100voxels");
		ImageStack img3DCubeStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 29; ++ z) {
			
			byte[] img3DCubeData = new byte[totalPixels];
			ByteProcessor img3DCubeProc = new ByteProcessor(numcols, numrows,
					img3DCubeData);
			img3DCubeStack.addSlice(img3DCubeProc);
		}
		
		for(int z = 29; z < 129; ++ z) {
			
			byte[] img3DCubeData = new byte[totalPixels];
			
			for(int x = 29; x < 129; ++ x)
				for(int y = 29; y < 129; ++ y)
					img3DCubeData[y * numcols + x] = (byte) 255;
			
			ByteProcessor img3DCubeProc = new ByteProcessor(numcols, numrows,
					img3DCubeData);
			img3DCubeStack.addSlice(img3DCubeProc);
		}
		
		for(int z = 129; z < depth; ++ z) {
			
			byte[] img3DCubeData = new byte[totalPixels];
			ByteProcessor img3DCubeProc = new ByteProcessor(numcols, numrows,
					img3DCubeData);
			img3DCubeStack.addSlice(img3DCubeProc);
		}
		
		img3DCube.setStack(img3DCubeStack);
		
		
		// Set the calibration for voxel dimensions
		Calibration newCalibration = img3DCube.getCalibration();
		newCalibration.pixelWidth = 0.240;
		newCalibration.pixelHeight = 0.240;
		newCalibration.pixelDepth = 0.713;
		newCalibration.setXUnit("um");
		img3DCube.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DCube100voxels_seg.fits", img3DCube);

		

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//generateSphere30Micrometers("/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/Calibration/Sphere30umCalibration/");
		//generateCube100voxels("/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/Calibration/Cube100voxelsCalibration/");
		//generateEllipsoid("/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/Calibration/EllipsoidCalibration/");
		generateCylinder("/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/Calibration/CylinderCalibration/");

	}

}
