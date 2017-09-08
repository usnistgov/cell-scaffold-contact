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

import java.net.URL;

import junit.framework.Assert;
import ij.ImagePlus;

import org.junit.Before;
import org.junit.Test;

import segment3D.Segment3DImage;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class Segment3DImageTest {

	private ImagePlus sampleCollagenGel;
	private ImagePlus sampleSpunCoat;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		URL sampleCollagenGelPath = Segment3DImageTest.class
				.getClassLoader()
				.getResource(
						"test/resources/Segment3DImageTest/050214_SJF_CollagenGel_1d_63x_12.fits");
		
		URL sampleSpunCoatPath = Segment3DImageTest.class
				.getClassLoader()
				.getResource(
						"test/resources/Segment3DImageTest/080613_SS_SC2_d1_63x_20.fits");
		
		sampleCollagenGel = new ImagePlus(sampleCollagenGelPath.getPath());
		sampleSpunCoat = new ImagePlus(sampleSpunCoatPath.getPath());
	}

	/**
	 * Test method for {@link segment3D.Segment3DImage#segmentImage(int, int, int)}.
	 */
	@Test
	public void testSegmentImage() {
		Segment3DImage segment3DImageCG = new Segment3DImage(sampleCollagenGel);
		segment3DImageCG.segmentImage(1, 2, 1);
		assertEquals(180280, segment3DImageCG.getFRGCount());
		
		Segment3DImage segment3DImageSC = new Segment3DImage(sampleSpunCoat);
		segment3DImageSC.segmentImage(8, 2, 1);
		assertEquals(286084, segment3DImageSC.getFRGCount());
	}

	/**
	 * Test method for {@link segment3D.Segment3DImage#thresholdImage(int)}.
	 */
	@Test
	public void testThresholdImage() {
		Segment3DImage segment3DImageCG = new Segment3DImage(sampleCollagenGel);
		segment3DImageCG.thresholdImage(1);
		assertEquals(1963339, segment3DImageCG.getFRGCount());
		
		Segment3DImage segment3DImageSC = new Segment3DImage(sampleSpunCoat);
		segment3DImageSC.thresholdImage(8);
		assertEquals(853180, segment3DImageSC.getFRGCount());
	}

}
