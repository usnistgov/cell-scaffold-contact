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
package io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import ij.ImagePlus;

/**
 * This is a class for saving 2D 8-bits FITS images
 * 
 * @author Mylene Simon
 * @see IJ.FITS_Writer
 *
 */
public class Fits2DWriter {

	/**
	 * Write the 2D 8-bits ImagePlus in a FITS file
	 * 
	 * @param destFilePath Path for the destination file
	 * @param imp ImagePlus 2D image to save
	 */
	public static void write(String destFilePath, ImagePlus imp) {
		
		// Check if this is an 8-bits image
        int bitDepth = imp.getBitDepth();
        if (bitDepth != 8) {
            System.err.println("Only 2D 8-bits images are supported.");
            return;
        }

        // Create file, overwrite if already exists
        File f = new File(destFilePath);
        if (f.exists()) f.delete();
        
        // Compute filler length to put at the end of the image data 
        // (Fits data are blocks of 2880 bytes)
        int numBytes = imp.getBytesPerPixel();
        int fillerLength = 2880 - ( (numBytes * imp.getWidth() * imp.getHeight()) % 2880 );

        // Create header for the Fits file
        createHeader(destFilePath, imp, bitDepth);
        
        // Write data
        writeData2D(destFilePath, imp);
        char[] endFiller = new char[fillerLength];
        appendFile(endFiller, destFilePath);
	}
	
	/**
     * Creates a FITS header for the image
     */ 
    private static void createHeader(String path, ImagePlus imp, int bitPix) {
        
    	int numCards = 11;
        String bitperpix = " " + bitPix;
        double cdelt1 = imp.getCalibration().pixelWidth;
    	double cdelt2 = imp.getCalibration().pixelHeight;
    	String ctype1 = imp.getCalibration().getXUnit();
    	String ctype2 = imp.getCalibration().getYUnit();
    	
    	// Conversions for the micro symbol (avoid encoding problems)
    	if(ctype1.equals("µm")) ctype1 = "um";
    	if(ctype2.equals("µm")) ctype2 = "um";
     
        appendFile(writeCard("SIMPLE", " T", "Created by NIST with ImageJ"), path);
        appendFile(writeCard("BITPIX", bitperpix, "number of bits per data pixel"), path);
        appendFile(writeCard("NAXIS", " 2", "number of data axes"), path);
        appendFile(writeCard("NAXIS1", " " + imp.getWidth(), "length of data axis 1"), path);
        appendFile(writeCard("NAXIS2", " " + imp.getHeight(), "length of data axis 2"), path);

		appendFile(writeCard("CDELT1", " " + new DecimalFormat("#0.000").format(cdelt1), "length of each pixel along axis 1"), path);
    	appendFile(writeCard("CDELT2", " " + new DecimalFormat("#0.000").format(cdelt2), "length of each pixel along axis 2"), path);
    	appendFile(writeCard("CTYPE1", " " + ctype1, "units along axis 1"), path);
    	appendFile(writeCard("CTYPE2", " " + ctype2, "units along axis 2"), path);

        appendFile(writeCard("BZERO", " -128", "data range offset"), path);
        appendFile(writeCard("BSCALE", " 1", "default scaling factor"), path);

        int fillerSize = 2880 - ((numCards*80+3) % 2880);
        char[] end = new char[3];
        end[0] = 'E'; end[1] = 'N'; end[2] = 'D';
        char[] filler = new char[fillerSize];
        for (int i = 0; i < fillerSize; i++)
            filler[i] = ' ';
        appendFile(end, path);
        appendFile(filler, path);
    }
    
    /**
     * Writes one line of a FITS header
     */ 
    private static char[] writeCard(String title, String value, String comment) {
        char[] card = new char[80];
        for (int i = 0; i < 80; i++)
            card[i] = ' ';
        s2ch(title, card, 0);
        card[8] = '=';
        s2ch(value, card, 10);
        card[31] = '/';
        card[32] = ' ';
        s2ch(comment, card, 33);
        return card;
    }
            
    /**
     * Converts a String to a char[]
     */
    private static void s2ch (String str, char[] ch, int offset) {
        int j = 0;
        for (int i = offset; i < 80 && i < str.length()+offset; i++)
            ch[i] = str.charAt(j++);
    }
    
    /**
     * Appends 'line' to the end of the file specified by 'path'.
     */
    private static void appendFile(char[] line, String path) {
        try {
            FileWriter output = new FileWriter(path, true);
            output.write(line);
            output.close();
        }
        catch (IOException e) {
            System.err.println("Error writing Fits file.");
            e.printStackTrace();
            return;
        }
    }
    
    /**
     * Appends the data of the current 2D image to the end of the file specified by path.
     */
    private static void writeData2D(String path, ImagePlus imp) {
        int w = imp.getWidth();
        int h = imp.getHeight();
                
        byte[] pixels = (byte[])imp.getProcessor().getPixels();
        try {   
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path,true)));
            for (int i = h - 1; i >= 0; i-- )
                for (int j = i*w; j < w*(i+1); j++)
                    dos.write(pixels[j]&0xff);
            dos.close();
        }
        catch (IOException e) {
        	System.err.println("Error writing Fits file.");
        	e.printStackTrace();
            return;
        }
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
