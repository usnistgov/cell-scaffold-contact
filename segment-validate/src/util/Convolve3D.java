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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

/**
 * This class came from imageJ/Fiji plugin
 * URL: https://github.com/fiji/VIB-lib/blob/master/src/main/java/process3d/Convolve3d.java
 * 
 * @author pnb
 *
 */
public class Convolve3D {

	public static ImagePlus convolve(ImagePlus image,
					float[] H_x, float[] H_y, float[] H_z) {
		ImagePlus tmp1 = convolveX(image, H_x);
		ImagePlus tmp2 = convolveY(tmp1, H_y);
		return convolveZ(tmp2, H_z);
	}

	public static ImagePlus convolveX(ImagePlus image, float[] H_x) {
		float[][][] H = new float[1][1][H_x.length];
		for(int i = 0; i < H_x.length; i++) {
			H[0][0][i] = H_x[i];
		}
		return convolve(image, H);
	}

	public static ImagePlus convolveY(ImagePlus image, float[] H_y) {
		float[][][] H = new float[1][H_y.length][1];
		for(int i = 0; i < H_y.length; i++) {
			H[0][i][0] = H_y[i];
		}
		return convolve(image, H);
	}

	public static ImagePlus convolveZ(ImagePlus image, float[] H_z) {
		float[][][] H = new float[H_z.length][1][1];
		for(int i = 0; i < H_z.length; i++) {
			H[i][0][0] = H_z[i];
		}
		return convolve(image, H);
	}

	private static int w, h, d;
	private static int r_x, r_y, r_z;
	private static Object[] slices_in;
	private static boolean isByte, isShort, isFloat;
	private static float[][][] H;

	public static ImagePlus convolve(ImagePlus image, float[][][] kernel) {
		H = kernel;
		// Determine dimensions of the filter
		r_z = H.length;
		r_y = H[0].length;
		r_x = H[0][0].length;

		// Determine dimensions of the image
		w = image.getWidth(); h = image.getHeight();
		d = image.getStackSize();

		// Adjust minimum and maximum indices (because of filter size)
		int min_x = r_x/2, max_x = w - r_x/2;
		int min_y = r_y/2, max_y = h - r_y/2;
		int min_z = r_z/2, max_z = d - r_z/2;

		// initialize slices_in and slices_out
		slices_in = new Object[d];
		float[][] slices_out = new float[d][];
		for(int i = 0; i < d; i++) {
			slices_in[i] = image.getStack().
						getProcessor(i+1).getPixels();
			slices_out[i] = new float[w*h];
		}

		// determine image type
		isByte = slices_in[0] instanceof byte[];
		isShort = slices_in[0] instanceof short[];
		isFloat = slices_in[0] instanceof float[];

		// convolve
// 		for(int z = min_z; z < max_z; z++) {
// 			IJ.showProgress(z, max_z);
// 			for(int y = min_y; y < max_y; y++) {
// 				for(int x = min_x; x < max_x; x++) {
		for(int z = 0; z < d; z++) {
			IJ.showProgress(z, d);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					slices_out[z][y*w+x] =
						convolvePoint(z,y,x);
				}
			}
		}

		// create output image
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("",
				new FloatProcessor(w, h, slices_out[z], null));
		}
		ImagePlus result = new ImagePlus("", stack);
		result.setCalibration(image.getCalibration());
		return result;
	}

	private static float convolvePoint(int z, int y, int x) {
		float sum = 0f;
		for(int k=-r_z/2; k<=+r_z/2; k++) {
			for(int j=-r_y/2; j<=+r_y/2; j++) {
				for(int i=-r_x/2; i<=+r_x/2; i++) {
					sum += getValue(x+i, y+j, z+k) *
						H[k+r_z/2][j+r_y/2][i+r_x/2];
				}
			}
		}
		return sum;
	}

	private static float getValue(int x, int y, int z) {
		if(x < 0) return 0f;
		if(x > w-1) return 0f;
		if(y < 0) return 0f;
		if(y > h-1) return 0f;
		if(z < 0) return 0f;
		if(z > d-1) return 0f;
		int index = y * w + x;
		if(isByte)
			return ((byte[])slices_in[z])[index] & 0xff;
		else if(isShort)
			return ((short[])slices_in[z])[index];
		else if(isFloat)
			return ((float[])slices_in[z])[index];
		IJ.error("Neither byte nor float image");
		return -1f;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
