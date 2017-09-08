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

import java.util.Arrays;

import ij.ImagePlus;
import ij.process.ByteProcessor;

import org.junit.Before;
import org.junit.Test;

import validation.SimilarityMetricsExtractor;
import validation.SimilarityMetricsExtractor.VennDiagram;

/**
 * Class used for testing SimilarityMetricsExtractor
 * 
 * @author Mylene Simon
 *
 */
public class SimilarityMetricsExtractorTest {

	private SimilarityMetricsExtractor similarityMetricsExtractor;
	
	private ImagePlus refImage;
	private ImagePlus simImage;
	private ImagePlus overlappingImage;
	private ImagePlus nonOverlappingImage;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		// instantiate SimilarityMetricsExtractor class
		similarityMetricsExtractor = new SimilarityMetricsExtractor();
		
		// create images for tests:
		int numrows = 1024;
		int numcols = 1024;
		int totalPixels = 1024 * 1024;
		
		// refImage : 2D ImagePlus with ByteProcessor, 1024x1024, 
		// foreground = 200x200 square with top left corner at (411,411)
		refImage = new ImagePlus();
		refImage.setTitle("refImage");
		byte[] refImgData = new byte[totalPixels];
		
		for(int x = 411; x < 611; ++ x)
			for(int y = 411; y < 611; ++ y)
				refImgData[y * numcols + x] = (byte) 255;
		
		ByteProcessor refImgProc = new ByteProcessor(numcols, numrows,
				refImgData);
		refImage.setProcessor(refImgProc);
		
		// simImage : 2D ImagePlus with ByteProcessor, 1024x1024, 
		// foreground = 200x200 square with top left corner at (411,411)
		simImage = new ImagePlus();
		simImage.setTitle("simImage");
		byte[] simImgData = new byte[totalPixels];
		Arrays.fill(simImgData, (byte) 0);
		
		for(int x = 411; x < 611; ++ x)
			for(int y = 411; y < 611; ++ y)
				simImgData[y * numcols + x] = (byte) 255;
		
		ByteProcessor simImgProc = new ByteProcessor(numcols, numrows,
				simImgData);
		simImage.setProcessor(simImgProc);
		
		// overlappingImage : 2D ImagePlus with ByteProcessor, 1024x1024, 
		// foreground = 200x200 square with top left corner at (451,451)
		overlappingImage = new ImagePlus();
		overlappingImage.setTitle("overlappingImage");
		byte[] overlappingImgData = new byte[totalPixels];
		Arrays.fill(overlappingImgData, (byte) 0);
		
		for(int x = 451; x < 651; ++ x)
			for(int y = 451; y < 651; ++ y)
				overlappingImgData[y * numcols + x] = (byte) 255;
		
		ByteProcessor overlappingImgProc = new ByteProcessor(numcols, numrows,
				overlappingImgData);
		overlappingImage.setProcessor(overlappingImgProc);
		
		// nonOverlappingImage : 2D ImagePlus with ByteProcessor, 1024x1024, 
		// foreground = 200x200 square with top left corner at (800,800)
		nonOverlappingImage = new ImagePlus();
		nonOverlappingImage.setTitle("nonOverlappingImage");
		byte[] nonOverlappingImgData = new byte[totalPixels];
		Arrays.fill(nonOverlappingImgData, (byte) 0);
		
		for(int x = 800; x < 1000; ++ x)
			for(int y = 800; y < 1000; ++ y)
				nonOverlappingImgData[y * numcols + x] = (byte) 255;
		
		ByteProcessor nonOverlappingImgProc = new ByteProcessor(numcols, numrows,
				nonOverlappingImgData);
		nonOverlappingImage.setProcessor(nonOverlappingImgProc);
		
	}

	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#extractVennDiagramQuantities(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testExtractVennDiagramQuantitiesSimilarImages() {
		
		int totalPixels = 1024 * 1024;

		// Test Venn Diagram similar images
		VennDiagram vennDiagramEquals = similarityMetricsExtractor
				.extractVennDiagramQuantities(refImage, simImage, 255);
		
		double proportionSimPixels = ((200.0 * 200.0) / ((double) totalPixels)) 
									* 100.0;
		
		assertEquals(
				"Number of different frg pixels in image should be "
				+ "0 for similar images.",
				0, vennDiagramEquals.onlyImgFrgPixels);
		
		assertEquals(
				"Number of different frg pixels in ref should be "
				+ "0 for similar images.",
				0, vennDiagramEquals.onlyRefFrgPixels);
		
		assertEquals(
				"Number of similar frg pixels should be "
				+ (200 * 200) + " for similar images.",
				200 * 200, vennDiagramEquals.similarFrgPixels);
		
		assertEquals(
				"Proportion of different frg pixels in image should be "
				+ "0.0% for similar images.",
				0.0, vennDiagramEquals.proportionOnlyImgFrgPixels, 0.001);
		
		assertEquals(
				"Proportion of different frg pixels in ref should be "
				+ "0.0% for similar images.",
				0.0, vennDiagramEquals.proportionOnlyRefFrgPixels, 0.001);
		
		assertEquals(
				"Proportion of similar frg pixels should be "
				+ proportionSimPixels
				+ "% for similar images.",
				proportionSimPixels, 
				vennDiagramEquals.proportionSimilarFrgPixels, 
				0.001);
	
	}
	
	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#extractVennDiagramQuantities(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testExtractVennDiagramQuantitiesOverlappingImages() {
		
		int totalPixels = 1024 * 1024;
		
		// Test Venn Diagram overlapping images
		VennDiagram vennDiagramOverlapping = similarityMetricsExtractor
				.extractVennDiagramQuantities(refImage, overlappingImage, 255);
		
		int nonOverlappingPixels = 200 * 40 + (200 - 40) * 40;
		int overlappingPixels = (200 - 40) * (200 - 40);
		double proportionNonOverlappingPixels = 
				((double) nonOverlappingPixels / (double) totalPixels) * 100.0;
		double proportionOverlappingPixels = 
				((double) overlappingPixels / (double) totalPixels) * 100.0;
		
		assertEquals(
				"Number of different frg pixels in image should be "
				+ nonOverlappingPixels + " for overlapping images.",
				nonOverlappingPixels, vennDiagramOverlapping.onlyImgFrgPixels);
		
		assertEquals(
				"Number of different frg pixels in ref should be "
				+ nonOverlappingPixels + " for overlapping images.",
				nonOverlappingPixels, vennDiagramOverlapping.onlyRefFrgPixels);
		
		assertEquals(
				"Number of similar frg pixels should be "
				+ overlappingPixels + " for overlapping images.",
				overlappingPixels, vennDiagramOverlapping.similarFrgPixels);
		
		assertEquals("Proportion of different frg pixels in image should be "
				+ proportionNonOverlappingPixels + " for overlapping images.",
				proportionNonOverlappingPixels,
				vennDiagramOverlapping.proportionOnlyImgFrgPixels, 
				0.001);
		
		assertEquals(
				"Proportion of different frg pixels in ref should be "
				+ proportionNonOverlappingPixels + " for overlapping images.",
				proportionNonOverlappingPixels, 
				vennDiagramOverlapping.proportionOnlyRefFrgPixels, 
				0.001);
		
		assertEquals(
				"Proportion of similar frg pixels should be "
				+ proportionOverlappingPixels + " for overlapping images.",
				proportionOverlappingPixels, 
				vennDiagramOverlapping.proportionSimilarFrgPixels, 
				0.001);
	}
	
	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#extractVennDiagramQuantities(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testExtractVennDiagramQuantitiesNonOverlappingImages() {
		
		int totalPixels = 1024 * 1024;
		
		// Test Venn Diagram non overlapping images
		VennDiagram vennDiagramNonOverlapping = similarityMetricsExtractor
				.extractVennDiagramQuantities(refImage, nonOverlappingImage, 255);
		
		double proportionDiffPixels = ((200.0 * 200.0) / ((double) totalPixels)) 
				* 100.0;
		
		assertEquals(
				"Number of different frg pixels in image should be "
				+ (200 * 200) + " for non overlapping images.",
				(200 * 200), vennDiagramNonOverlapping.onlyImgFrgPixels);
		
		assertEquals(
				"Number of different frg pixels in ref should be "
				+ (200 * 200) + " for overlapping images.",
				(200 * 200), vennDiagramNonOverlapping.onlyRefFrgPixels);
		
		assertEquals(
				"Number of similar frg pixels should be "
				+ 0 + " for non overlapping images.",
				0, vennDiagramNonOverlapping.similarFrgPixels);
		
		assertEquals("Proportion of different frg pixels in image should be "
				+ proportionDiffPixels +" % for non overlapping images.",
				proportionDiffPixels,
				vennDiagramNonOverlapping.proportionOnlyImgFrgPixels, 
				0.001);
		
		assertEquals(
				"Proportion of different frg pixels in ref should be "
				+ proportionDiffPixels + " % for non overlapping images.",
				proportionDiffPixels, 
				vennDiagramNonOverlapping.proportionOnlyRefFrgPixels, 
				0.001);
		
		assertEquals(
				"Proportion of similar frg pixels should be "
				+ "0.0 % for non overlapping images.",
				0.0, 
				vennDiagramNonOverlapping.proportionSimilarFrgPixels, 
				0.001);
	}

	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#computeDiceIndex(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testComputeDiceIndexSimilarImages() {
		
		// Test Dice index for similar images
		double diceIndex = SimilarityMetricsExtractor.computeDiceIndex(
				refImage, simImage, 255);
		
		assertEquals("Dice index should be 1.0 for similar images.",
				1.0, diceIndex, 0.001);
	}
	
	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#computeDiceIndex(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testComputeDiceIndexOverlappingImages() {
		
		// Test Dice index for overlapping images
		double diceIndex = SimilarityMetricsExtractor.computeDiceIndex(
				refImage, overlappingImage, 255);
		
		double overlappingPixels = (200.0 - 40.0) * (200.0 - 40.0);
		double expectedDice = (2.0 * overlappingPixels) / (200 * 200 + 200 * 200);
		
		assertEquals("Dice index should be " + expectedDice + " for overlapping images.",
				expectedDice, diceIndex, 0.001);
	}
	
	/**
	 * Test method for {@link validation.SimilarityMetricsExtractor#computeDiceIndex(ij.ImagePlus, ij.ImagePlus, int)}.
	 */
	@Test
	public void testComputeDiceIndexNonOverlappingImages() {
		
		// Test Dice index for non overlapping images
		double diceIndex = SimilarityMetricsExtractor.computeDiceIndex(
				refImage, nonOverlappingImage, 255);
		
		assertEquals("Dice index should be 0.0 for non overlapping images.",
				0.0, diceIndex, 0.001);
	}

}
