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

import java.io.File;

import segment3D.Segment3DImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import io.Fits3DWriter;
import io.Nifti_Writer;

/**
 * This class came from imageJ/Fiji plugin
 * URL: https://github.com/fiji/VIB-lib/blob/master/src/main/java/process3d/
 * 
 * @author pnb
 *
 */

public class Gradient3D {

	public static ImagePlus calculateGrad(ImagePlus imp, boolean useCalib) {
		
		//IJ.showStatus("Calculating gradient");
		System.out.println("Computing 3D gradient");

		Calibration c = imp.getCalibration();
		float dx = useCalib ? 2*(float)c.pixelWidth : 2;
		float dy = useCalib ? 2*(float)c.pixelHeight : 2;
		float dz = useCalib ? 2*(float)c.pixelDepth : 2;

		float[] H_x = new float[] {-1/dx, 0, 1/dx};
		ImagePlus g_x = Convolve3D.convolveX(imp, H_x);

		float[] H_y = new float[] {-1/dy, 0, 1/dy};
		ImagePlus g_y = Convolve3D.convolveY(imp, H_y);

		float[] H_z = new float[] {-1/dz, 0, 1/dz};
		ImagePlus g_z = Convolve3D.convolveZ(imp, H_z);

		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		ImageStack grad = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			FloatProcessor res = new FloatProcessor(w, h);
			grad.addSlice("", res);
			float[] values = (float[])res.getPixels();
			float[] x_ = (float[])g_x.getStack().
						getProcessor(z+1).getPixels();
			float[] y_ = (float[])g_y.getStack().
						getProcessor(z+1).getPixels();
			float[] z_ = (float[])g_z.getStack().
						getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++) {
				values[i] = (float)Math.sqrt(
				x_[i]*x_[i] + y_[i]*y_[i] + z_[i]*z_[i]);
			}
		}
		ImagePlus ret = new ImagePlus("Gradient", grad);
		ret.setCalibration(c);
		return ret;
	}

	public static void main(String[] args) {
		
		String inputImageFilename = "C:/PeterB/Projects/CarlSimon/ContactProb-test/FiberScaffoldSegmentation/Results/070515_MMF_1_63x_Pos003_OutA2.fits"; 
		String outputImageFilenameNII = "C:/PeterB/Projects/CarlSimon/ContactProb-test/FiberScaffoldSegmentation/Results/070515_MMF_1_63x_Pos003_OutA2Gradient.nii";
		String outputImageFilenameFITS = "C:/PeterB/Projects/CarlSimon/ContactProb-test/FiberScaffoldSegmentation/Results/070515_MMF_1_63x_Pos003_OutA2Binary.fits";

		// load
		ImagePlus img3D = new ImagePlus(inputImageFilename); // input one fits file per zstack
		// calculate the gradient
		ImagePlus resImage = Gradient3D.calculateGrad(img3D,false);
		// try to call GC to free unused memory
		img3D = null;
		System.gc();
		// write the 32 bit per pixel gradient image
		Nifti_Writer nifti = new Nifti_Writer();
		String outputType = new String("::NIFTI_FILE:");
		nifti.write(resImage, outputImageFilenameNII, outputType);

		// threshold by zero (any gradient larger than 0 will become 1 else 0
		Segment3DImage segment3DImage = new Segment3DImage(resImage);
		segment3DImage.thresholdImage(0);		
		ImagePlus segmentedImage = segment3DImage.generateSegmentedImagePlus();
		// try to call GC to free unused memory
		resImage = null;
		System.gc();
		
		Fits3DWriter.write(outputImageFilenameFITS , segmentedImage);
		

	}

}
