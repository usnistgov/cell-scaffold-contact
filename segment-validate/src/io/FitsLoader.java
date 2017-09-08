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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/**
 * This is a class for loading 3D 8-bits FITS images
 * 
 * @author Mylene Simon
 * @see IJ.FITS_Reader
 *
 */
public class FitsLoader {

	/**
	 * Read the FITS file and load it into an ImagePlus object
	 * 
	 * @param fileName The path of the FITS file
	 * @return The ImagePlus object containing the FITS image
	 */
    public static ImagePlus read(String fileName) {
    	if (fileName==null)
            return null;
    	
    	// GET FILE
        File f = new File(fileName);
        String directory = f.getParent()+File.separator;
        String name = f.getName();
        ImagePlus imp;
        
        FitsDecoder fd = new FitsDecoder(directory, name);
        FileInfo fi = null;
        try {fi = fd.getInfo();}
        catch (IOException e) {}
        if (fi!=null && fi.width>0 && fi.height>0 && fi.offset>0) {
            FileOpener fo = new FileOpener(fi);
            imp = fo.open(false);
            if(fi.nImages==1) {
              ImageProcessor ip = imp.getProcessor();              
              ip.flipVertical(); // origin is at bottom left corner
            } else {
              ImageStack stack = imp.getStack(); // origin is at bottom left corner              
              for(int i=1; i<=stack.getSize(); i++)
                  stack.getProcessor(i).flipVertical();
            }
            Calibration cal = imp.getCalibration();
            if (fi.fileType==FileInfo.GRAY16_SIGNED && fd.bscale==1.0 && fd.bzero== -32768.0)
                cal.setFunction(Calibration.NONE, null, "Gray Value");
            
        } else {
            System.err.println("This does not appear to be a FITS file.");
            return null;
        }
        return imp;
    }

    /**
     * Main method for test
     * 
	 * @param args
	 */
	public static void main(String[] args) {
		//String filename = "/Users/mhs1/Documents/1000_z-stacks_project/CarlSimon/ExperimentAugust2013/FITS files/Spun Coat/061813_DC_SC1_d1_63x_1.fits";
		if(args == null || args.length<1){
			System.err.println("Missing argument: Input file name");
			return;
		}
		String filename = args[0];
		ImagePlus testImg = FitsLoader.read(filename);
		testImg.show(filename);
		System.out.println("Height : " + testImg.getHeight() + " Width : " + testImg.getWidth() + " Nb slices : " + testImg.getNSlices());
	}

}

/**
 * This class decodes the FITS header
 * 
 * @author ImageJ
 *
 */
class FitsDecoder {
    private String directory, fileName;
    private DataInputStream f;
    private StringBuffer info = new StringBuffer(512);
    double bscale, bzero;

    public FitsDecoder(String directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
    }

    FileInfo getInfo() throws IOException {
        FileInfo fi = new FileInfo();
        fi.fileFormat = FileInfo.FITS;
        fi.fileName = fileName;
        fi.directory = directory;
        fi.width = 0;
        fi.height = 0;
        fi.offset = 0;

        InputStream is = new FileInputStream(directory + fileName);
        if (fileName.toLowerCase().endsWith(".gz")) is = new GZIPInputStream(is);
        f = new DataInputStream(is);
        String line = getString(80);
        info.append(line+"\n");
        if (!line.startsWith("SIMPLE"))
            {f.close(); return null;}
        int count = 1;
        while ( true ) {
            count++;
            line = getString(80);
            info.append(line+"\n");
  
            // Cut the key/value pair
            int index = line.indexOf ( "=" );

            // Strip out comments
            int commentIndex = line.indexOf ( "/", index );
            if ( commentIndex < 0 )
                commentIndex = line.length ();
            
            // Split that values
            String key;
            String value;
            if ( index >= 0 ) {
                key = line.substring ( 0, index ).trim ();
                value = line.substring ( index + 1, commentIndex ).trim ();
            } else {
                key = line.trim ();
                value = "";
            }
            
            // Time to stop ?
            if (key.equals ("END") ) break;

            // Look for interesting information         
            if (key.equals("BITPIX")) {
                int bitsPerPixel = Integer.parseInt ( value );
               if (bitsPerPixel==8)
                    fi.fileType = FileInfo.GRAY8;
                else if (bitsPerPixel==16)
                    fi.fileType = FileInfo.GRAY16_SIGNED;
                else if (bitsPerPixel==32)
                    fi.fileType = FileInfo.GRAY32_INT;
                else if (bitsPerPixel==-32)
                    fi.fileType = FileInfo.GRAY32_FLOAT;
                else if (bitsPerPixel==-64)
                    fi.fileType = FileInfo.GRAY64_FLOAT;
                else {
                    //IJ.error("BITPIX must be 8, 16, 32, -32 (float) or -64 (double).");
                    f.close();
                    return null;
                }
            } else if (key.equals("NAXIS1"))
                fi.width = Integer.parseInt ( value );
            else if (key.equals("NAXIS2"))
                fi.height = Integer.parseInt( value );
            else if (key.equals("NAXIS3")) //for multi-frame fits
                fi.nImages = Integer.parseInt ( value );
            else if (key.equals("BSCALE"))
                bscale = parseDouble ( value );
            else if (key.equals("BZERO"))
                bzero = parseDouble ( value );
        else if (key.equals("CDELT1"))
                fi.pixelWidth = parseDouble ( value );
        else if (key.equals("CDELT2"))
                fi.pixelHeight = parseDouble ( value );
        else if (key.equals("CDELT3"))
                fi.pixelDepth = parseDouble ( value );
        else if (key.equals("CTYPE1"))
                fi.unit = value;

            if (count>360 && fi.width==0)
                {f.close(); return null;}
        }

        f.close();
        fi.offset = 2880+2880*(((count*80)-1)/2880);
        return fi;
    }

    String getString(int length) throws IOException {
        byte[] b = new byte[length];
        f.readFully(b);
        return new String(b, StandardCharsets.ISO_8859_1);
    }

    int getInteger(String s) {
        s = s.substring(10, 30);
        s = s.trim();
        return Integer.parseInt(s);
    }

    double parseDouble(String s) throws NumberFormatException {
        Double d = new Double(s);
        return d.doubleValue();
    }

    String getHeaderInfo() {
        return new String(info);
    }

}
	

