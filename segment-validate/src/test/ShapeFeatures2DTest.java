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
package test;

import static org.junit.Assert.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import io.Fits2DWriter;
import io.Fits3DWriter;

import org.junit.Before;
import org.junit.Test;

import shapeFeatures2D.ShapeFeatures2D;
import validation.OrthogonalProjection;

/**
 * @author mhs1
 *
 */
public class ShapeFeatures2DTest {

	private ImagePlus img3DCube;
	private ImagePlus img3DRectanglePrism;
	private ImagePlus img3DSphere;
	
	private ImagePlus img2DCubeXY;
	private ImagePlus img2DCubeXZ;
	private ImagePlus img2DCubeZY;
	
	private ImagePlus img2DRectangleXY;
	private ImagePlus img2DRectangleXZ;
	private ImagePlus img2DRectangleZY;
	
	final private String outputFolder = "/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/ShapeMetricsAnalysis/OutputJUnitTest/";
	
	final private double EPSILON = 0.00001;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		// create images for tests:
		int numrows = 100;
		int numcols = 100;
		int depth = 100;
		int totalPixels = 100 * 100;
		
		// Initialization of 3D images
		img3DCube = new ImagePlus();
		img3DCube.setTitle("img3DCube");
		ImageStack img3DCubeStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 29; ++ z) {
			
			byte[] img3DCubeData = new byte[totalPixels];
			ByteProcessor img3DCubeProc = new ByteProcessor(numcols, numrows,
					img3DCubeData);
			img3DCubeStack.addSlice(img3DCubeProc);
		}
		
		for(int z = 29; z < 69; ++ z) {
			
			byte[] img3DCubeData = new byte[totalPixels];
			
			for(int x = 29; x < 69; ++ x)
				for(int y = 29; y < 69; ++ y)
					img3DCubeData[y * numcols + x] = (byte) 255;
			
			ByteProcessor img3DCubeProc = new ByteProcessor(numcols, numrows,
					img3DCubeData);
			img3DCubeStack.addSlice(img3DCubeProc);
		}
		
		for(int z = 69; z < depth; ++ z) {
			
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
		
		Fits3DWriter.write(outputFolder + "img3DCube.fits", img3DCube);
		
		img3DRectanglePrism = new ImagePlus();
		img3DRectanglePrism.setTitle("img3DRectanglePrism");
		ImageStack img3DRectanglePrismStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 9; ++ z) {
			
			byte[] img3DRectanglePrismData = new byte[totalPixels];
			ByteProcessor img3DRectanglePrismProc = new ByteProcessor(numcols,
					numrows, img3DRectanglePrismData);
			img3DRectanglePrismStack.addSlice(img3DRectanglePrismProc);
		}
		
		for(int z = 9; z < 89; ++ z) {
			
			byte[] img3DRectanglePrismData = new byte[totalPixels];
			
			for(int x = 19; x < 79; ++ x)
				for(int y = 29; y < 69; ++ y)
					img3DRectanglePrismData[y * numcols + x] = (byte) 255;
			
			ByteProcessor img3DRectanglePrismProc = new ByteProcessor(numcols,
					numrows, img3DRectanglePrismData);
			img3DRectanglePrismStack.addSlice(img3DRectanglePrismProc);
		}
		
		for(int z = 89; z < depth; ++ z) {
			
			byte[] img3DRectanglePrismData = new byte[totalPixels];
			ByteProcessor img3DRectanglePrismProc = new ByteProcessor(numcols,
					numrows, img3DRectanglePrismData);
			img3DRectanglePrismStack.addSlice(img3DRectanglePrismProc);
		}
		
		img3DRectanglePrism.setStack(img3DRectanglePrismStack);
		// Set the calibration for voxel dimensions
		img3DRectanglePrism.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DRectanglePrism.fits",
				img3DRectanglePrism);
		
		// Create 3D sphere image
		img3DSphere = new ImagePlus();
		img3DSphere.setTitle("img3DSphere");
		ImageStack img3DSphereStack = new ImageStack(numcols, numrows);
		
		for(int z = 0; z < 100; ++ z) {
			byte[] img3DSphereData = new byte[totalPixels];
			
			for(int x = 0; x < 100; ++ x) {
				for(int y = 0; y < 100; ++y) {
					if (Math.pow(((double) x * 0.240 - 12.0), 2)
							+ Math.pow(((double) y * 0.240 - 12.0), 2)
							+ Math.pow(((double) z * 0.713 - 35.65), 2) 
						<= 56.25)
						img3DSphereData[y * numcols + x] = (byte) 255;
				}
			}
			
			ByteProcessor img3DSphereProc = new ByteProcessor(numcols, numrows,
					img3DSphereData);
			img3DSphereStack.addSlice(img3DSphereProc);
		}
		
		img3DSphere.setStack(img3DSphereStack);
		
		// Set the calibration for voxel dimensions
		img3DSphere.setCalibration(newCalibration);
		
		Fits3DWriter.write(outputFolder + "img3DSphere.fits", img3DSphere);

		// Cube 2D orthogonal projections
		img2DCubeXY = OrthogonalProjection.projectionXY(img3DCube,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DCubeXY.fits", img2DCubeXY);

		img2DCubeXZ = OrthogonalProjection.projectionXZ(img3DCube,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DCubeXZ.fits", img2DCubeXZ);

		img2DCubeZY = OrthogonalProjection.projectionZY(img3DCube,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DCubeZY.fits", img2DCubeZY);
		
		// Rectangle prism 2D orthogonal projections
		img2DRectangleXY = OrthogonalProjection.projectionXY(img3DRectanglePrism,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DRectangleXY.fits", img2DRectangleXY);

		img2DRectangleXZ = OrthogonalProjection.projectionXZ(img3DRectanglePrism,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DRectangleXZ.fits", img2DRectangleXZ);

		img2DRectangleZY = OrthogonalProjection.projectionZY(img3DRectanglePrism,
				OrthogonalProjection.projectionType_Max);
		Fits2DWriter.write(outputFolder + "img2DRectangleYZ.fits", img2DRectangleZY);
	}

	/**
	 * Test method for {@link shapeFeatures2D.ShapeFeatures2D#area2D(ij.ImagePlus, int)}.
	 */
	@Test
	public void testArea2D() {
		
		// Cube 2D orthogonal projections
		assertEquals("Area of 2D max XY projection cube ", 92.16,
				ShapeFeatures2D.area2D(img2DCubeXY, 255), EPSILON);
		
		assertEquals("Area of 2D max XZ projection cube ", 273.792,
				ShapeFeatures2D.area2D(img2DCubeXZ, 255), EPSILON);
		
		assertEquals("Area of 2D max ZY projection cube ", 273.792,
				ShapeFeatures2D.area2D(img2DCubeZY, 255), EPSILON);
		
		// Rectangle prism 2D orthogonal projections
		assertEquals("Area of 2D max XY projection rectangle ", 138.24,
				ShapeFeatures2D.area2D(img2DRectangleXY, 255), EPSILON);
		
		assertEquals("Area of 2D max XZ projection rectangle ", 821.376,
				ShapeFeatures2D.area2D(img2DRectangleXZ, 255), EPSILON);
		
		assertEquals("Area of 2D max ZY projection rectangle ", 547.584,
				ShapeFeatures2D.area2D(img2DRectangleZY, 255), EPSILON);
		
	}

	/**
	 * Test method for {@link shapeFeatures2D.ShapeFeatures2D#aspectRatio2D(ij.ImagePlus, int)}.
	 */
	@Test
	public void testAspectRatio2D() {
		
		// Cube 2D orthogonal projections
		assertEquals("Aspect ratio of 2D max XY projection cube ", 1.0,
				ShapeFeatures2D.aspectRatio2D(img2DCubeXY, 255), EPSILON);
		
		assertEquals("Aspect ratio of 2D max XZ projection cube ", 2.9708333333,
				ShapeFeatures2D.aspectRatio2D(img2DCubeXZ, 255), EPSILON);
		
		assertEquals("Aspect ratio of 2D max ZY projection cube ", 2.9708333333,
				ShapeFeatures2D.aspectRatio2D(img2DCubeZY, 255), EPSILON);
		
		// Rectangle prism 2D orthogonal projections
		assertEquals("Aspect ratio of 2D max XY projection rectangle ", 1.5,
				ShapeFeatures2D.aspectRatio2D(img2DRectangleXY, 255), EPSILON);
		
		assertEquals("Aspect ratio of 2D max XZ projection rectangle ", 3.9611111111,
				ShapeFeatures2D.aspectRatio2D(img2DRectangleXZ, 255), EPSILON);
		
		assertEquals("Aspect ratio of 2D max ZY projection rectangle ", 5.94166666666,
				ShapeFeatures2D.aspectRatio2D(img2DRectangleZY, 255), EPSILON);
		
	}

	/**
	 * Test method for {@link shapeFeatures2D.ShapeFeatures2D#perimeter2D(ij.ImagePlus, int)}.
	 */
	@Test
	public void testPerimeter2D() {
		
		// Cube 2D orthogonal projections
		assertEquals("Perimeter of 2D max XY projection cube ", 36.136684,
				ShapeFeatures2D.perimeter2D(img2DCubeXY, 255), EPSILON);
		
		assertEquals("Perimeter of 2D max XZ projection cube ", 71.80798,
				ShapeFeatures2D.perimeter2D(img2DCubeXZ, 255), EPSILON);
		
		assertEquals("Perimeter of 2D max ZY projection cube ", 71.80798,
				ShapeFeatures2D.perimeter2D(img2DCubeZY, 255), EPSILON);
		
	}

	/**
	 * Test method for {@link shapeFeatures2D.ShapeFeatures2D#maxProjectionLDirection(ij.ImagePlus, double, double, double, double, double, double)}.
	 */
	@Test
	public void testMaxProjectionLDirection() {
		
		// test XY Projection cube
		ImagePlus maxXYProjectionCube = ShapeFeatures2D
				.maxProjectionLDirection(img3DCube, 1, 0, 0, 0, 1, 0);
		Fits2DWriter.write(outputFolder + "maxXYProjectionCube.fits", maxXYProjectionCube);
		
		// test XZ projection cube
		ImagePlus maxXZProjectionCube = ShapeFeatures2D
				.maxProjectionLDirection(img3DCube, 1, 0, 0, 0, 0, 1);
		Fits2DWriter.write(outputFolder + "maxXZProjectionCube.fits", maxXZProjectionCube);
		
		// test YZ projection cube
		ImagePlus maxYZProjectionCube = ShapeFeatures2D
				.maxProjectionLDirection(img3DCube, 0, 1, 0, 0, 0, 1);
		Fits2DWriter.write(outputFolder + "maxYZProjectionCube.fits", maxYZProjectionCube);
		
		// test Projection from top left front corner to top right back corner
		ImagePlus maxFromTopLeftFrontToTopRightBackCornersProjectionCube = ShapeFeatures2D
				.maxProjectionLDirection(img3DCube, 1, 0, -1, 0, 1, 0);
		Fits2DWriter
				.write(outputFolder
						+ "maxFromTopLeftFrontToTopRightBackCornersProjectionCube.fits",
						maxFromTopLeftFrontToTopRightBackCornersProjectionCube);
		
		// test Projection from top left front corner to top right back corner
		ImagePlus maxFromTopLeftFrontToBottomRightBackCornersProjectionCube = ShapeFeatures2D
				.maxProjectionLDirection(img3DCube, 1, -1, 0, 0, 1, -1);
		Fits2DWriter
				.write(outputFolder
						+ "maxFromTopLeftFrontToBottomRightBackCornersProjectionCube.fits",
						maxFromTopLeftFrontToBottomRightBackCornersProjectionCube);

		// test XY Projection rectangle
		ImagePlus maxXYProjectionRectangle = ShapeFeatures2D
				.maxProjectionLDirection(img3DRectanglePrism, 1, 0, 0, 0, 1, 0);
		Fits2DWriter.write(outputFolder + "maxXYProjectionRectangle.fits", maxXYProjectionRectangle);
		
		// test XZ projection rectangle
		ImagePlus maxXZProjectionRectangle = ShapeFeatures2D
				.maxProjectionLDirection(img3DRectanglePrism, 1, 0, 0, 0, 0, 1);
		Fits2DWriter.write(outputFolder + "maxXZProjectionRectangle.fits", maxXZProjectionRectangle);
		
		// test YZ projection rectangle
		ImagePlus maxYZProjectionRectangle = ShapeFeatures2D
				.maxProjectionLDirection(img3DRectanglePrism, 0, 1, 0, 0, 0, 1);
		Fits2DWriter.write(outputFolder + "maxYZProjectionRectangle.fits", maxYZProjectionRectangle);
	}

	/**
	 * Test method for {@link shapeFeatures2D.ShapeFeatures2D#computeCoordinateInProjected2DPlane(int, int, int, double, double, double, double)}.
	 */
	@Test
	public void testComputeCoordinateInProjected2DPlane() {
		
		// orthogonal XY projection, top left front corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, top left front corner, x coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 68,
						29, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, top left front corner, y coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 68,
						29, 0, 1, 0, 1));
		
		// orthogonal XY projection, top right front corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, top right front corner, x coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 68,
						29, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, top left front corner, y coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 68,
						29, 0, 1, 0, 1));
		
		// orthogonal XY projection, bottom left front corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, bottom left front corner, x coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 29,
						29, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, bottom left front corner, y coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 29,
						29, 0, 1, 0, 1));
		
		// orthogonal XY projection, bottom right front corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, bottom right front corner, x coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 29,
						29, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, bottom right front corner, y coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 29,
						29, 0, 1, 0, 1));
		
		// orthogonal XY projection, top left back corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, top left back corner, x coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 68,
						68, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, top left back corner, y coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 68,
						68, 0, 1, 0, 1));
		
		// orthogonal XY projection, top right back corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, top right back corner, x coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 68,
						68, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, top left back corner, y coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 68,
						68, 0, 1, 0, 1));
		
		// orthogonal XY projection, bottom left back corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, bottom left back corner, x coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 29,
						68, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, bottom left back corner, y coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(29, 29,
						68, 0, 1, 0, 1));
		
		// orthogonal XY projection, bottom right back corner, x and y coordinate
		assertEquals(
				"Orthogonal XY projection, bottom right back corner, x coordinate",
				68, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 29,
						68, 1, 0, 0, 1));
		assertEquals(
				"Orthogonal XY projection, bottom right back corner, y coordinate",
				29, ShapeFeatures2D.computeCoordinateInProjected2DPlane(68, 29,
						68, 0, 1, 0, 1));
	}

}
