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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
Disclaimer:  IMPORTANT:  This software was developed at the National Institute of Standards and Technology by employees of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the United States Code this software is not subject to copyright protection and is in the public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties, and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic. We would appreciate acknowledgement if the software is used. This software can be redistributed and/or modified freely provided that any derivative works bear some notice that they are derived from it, and any modified versions bear some notice that they have been modified.
*/
/**
 * This is a class for dir+file operations 
 *  
 * @author peter bajcsy
 * 
 */
public class FileOper {

	private static Log _logger = LogFactory.getLog(FileOper.class);
	public static final int SORT_ASCENDING = 0, SORT_DESCENDING = 1;

	public FileOper() {

	}

	/**
	 * This method selects files with the given suffix from a collection of
	 * image file names represented by Strings
	 * 
	 * @param c
	 *            - input collection of image file names
	 * @param suffix
	 *            - image suffix, i.e., ".tif"
	 * @return - output collection containing only selected image file names
	 */
	static public Collection<String> selectFileType(Collection<String> c,
			String suffix) {
		// sanity check
		if (c == null || c.isEmpty()) {
			_logger.error("ERROR: collection is null or empty");
			return null;
		}
		if (suffix == null) {
			_logger.error("ERROR: suffix is null ");
			return null;
		}
		Collection<String> retCollection = new ArrayList<String>();
		String temp = new String();
		for (Iterator<String> i = c.iterator(); i.hasNext();) {
			temp = i.next();
			if (temp.endsWith(suffix)) {
				retCollection.add(temp);
			}
		}
		return retCollection;
	}

	/**
	 * This method reads all files in a directory
	 * 
	 * @param dirPath
	 *            - input directory path
	 * @return - collection of file names
	 */
	static public Collection<String> readFileDirectory(String dirPath) {
		// sanity check
		if (dirPath == null || dirPath.length() < 1) {
			_logger.error("ERROR: directory  is null or empty");
			return null;
		}
		if (!DoesFileDirExist(dirPath)) {
			_logger.error("ERROR: directory  does not exist");
			return null;
		}
		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				list.add(dirPath + File.separatorChar + listOfFiles[i].getName());
			}
		}
		return list;
	}

	/**
	 * This method reads all sub-directories in a directory
	 * 
	 * @param dirPath
	 *            - input directory path
	 * @return - collection of directory names
	 */
	static public Collection<String> readSubDirectories(String dirPath) {
		// sanity check
		if (dirPath == null || dirPath.length() < 1) {
			_logger.error("ERROR: directory  is null or empty");
			return null;
		}
		if (!DoesFileDirExist(dirPath)) {
			_logger.error("ERROR: directory  does not exist");
			return null;
		}
		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				list.add(dirPath + File.separatorChar + listOfFiles[i].getName());
			}
		}
		return list;
	}
	
	static public Collection<String> sort(Collection<String> dirPathandName,
			int sortMethod) {
		// sanity check
		if (dirPathandName == null || dirPathandName.isEmpty()) {
			_logger.error("ERROR: collection of dirPathandName is null or empty");
			return null;
		}

		List<String> result = new ArrayList<String>(dirPathandName);
		
		switch (sortMethod) {
		case SORT_ASCENDING:
			java.util.Collections.sort(result); // ascending alphabetical order
			break;
		case SORT_DESCENDING:
			java.util.Collections.reverse(result); // descending alphabetical
													// order
			break;
				default:
			java.util.Collections.sort(result); // ascending alphabetical order
			break;
		}
		// test
/*		Object[] obj = result.toArray();
		System.out.println("TEST: result");
		for (int i = 0; i < obj.length; i++) {
			System.out.println("i=" + i + ",string=" + obj[i]);
		}
*/		return result;
	}

	static public Collection<String> label(Collection<String> dirPathandName,
			Collection<String> labelSubStrings) {
		// sanity check
		if (dirPathandName == null || dirPathandName.isEmpty()) {
			_logger.error("ERROR: collection of dirPathandName is null or empty");
			return null;
		}
		if (labelSubStrings == null || labelSubStrings.isEmpty()) {
			_logger.error("ERROR: collection of labelSubStrings is null or empty");
			return null;
		}

		List<String> result = new ArrayList<String>();
		List<String> subString = new ArrayList<String>(labelSubStrings);

		String src = new String();
		String sub = new String();
		boolean foundMatch = false;
		for (Iterator<String> i = dirPathandName.iterator(); i.hasNext();) {
			src = i.next();
			for (Iterator<String> j = subString.iterator(); j.hasNext();) {
				sub = j.next();

				if (src.contains(sub)) {
					result.add(sub);
					foundMatch = true;
				}
			}
			if (!foundMatch) {
				result.add("NoMatch");
			}
			foundMatch = false;
		}

		// test
/*		Object[] obj = result.toArray();
		System.out.println("TEST: result");
		for (int i = 0; i < obj.length; i++) {
			System.out.println("i=" + i + ",string=" + obj[i]);
		}
*/		return result;
	}


	/**
	 * this method returns an array of File objects that correspond to all file
	 * sin a directory
	 * 
	 * @param dirPath
	 *            - input directory path
	 * @return
	 */
	static public File[] listOfFiles(String dirPath) {

		String files;
		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				files = listOfFiles[i].getName();
				System.out.println(files);
			}
		}
		return listOfFiles;
	}

	/**
	 * this method returns an array of File objects that correspond to all sub-directories
	 * in a directory
	 * 
	 * @param dirPath
	 *            - input directory path
	 * @return
	 */
	static public File[] listOfDirectories(String dirPath) {

		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();
		File[] listOfDirs = null;
		
		int count = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				count++;
			}
		}
		if(count > 0){
			listOfDirs = new File[count];
			for (int j=0, i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isDirectory()) {
					listOfDirs[j] = listOfFiles[i];
					j++;
				}
			}			
		}
		return listOfDirs;
	}

	/**
	 * This method checks whether a file or a directory exists
	 * 
	 * @param dir
	 *            - input directory path
	 * @return - true/false outcome of the check
	 */
	static public boolean DoesFileDirExist(String dir) {
		File file = new File(dir);
		boolean exists = file.exists();
		if (!exists) {
			System.out.println("the file or directory does not exist=" + dir);
			return false;
		} else {
			System.out.println("the file or directory does exist=" + dir);
			return true;
		}
	}

	/**
	 * This method removes any element of the collection that contains a
	 * sub-string defined by eleiminate
	 * 
	 * @param c
	 *            - input collection
	 * @param eliminate
	 *            - input string to match and then eliminate the item in a
	 *            collection
	 * @return false/true
	 */
	static public boolean removeFile(Collection<String> c, String eliminate) {
		// sanity test
		if (c == null) {
			_logger.error("ERROR: missing input collection");
			return false;
		}
		if (eliminate == null) {
			_logger.error("ERROR: missing string to match in order to eliminate an element");
			return false;
		}
		for (Iterator<String> iter = c.iterator(); iter.hasNext();) {
			String temp = iter.next();
			if (temp.contains(eliminate)) {
				iter.remove();
				// test
				// System.out.println("Found match="+ temp);
				// System.out.println("size="+ c.size());
			}
		}
		return true;

	}

	/**
	 * This is a generic java method for printing objects to a console
	 * 
	 * @param c
	 *            - input Collection of any type
	 */
	static public void printCollection(Collection<?> c) {
		for (Object e : c) {
			System.out.println("object=" + e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public <T> Collection<T> copyCollection(Collection<? extends T> c) {

		List backup = new ArrayList(c.size());

		// deep copy
		for (Iterator<? extends T> i = c.iterator(); i.hasNext();) {
			T temp = i.next();
			backup.add(temp);
		}

		// shallow copy
		// backup.addAll(c);
		// test
		// System.out.println("original=");
		// printCollection(c);
		// System.out.println("backup=");
		// printCollection(backup);

		return backup;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 1)) {
			System.out.println("Please, specify the directory name");
			return;
		}

		// FileOper mytest = new FileOper();
		Collection<String> dirfiles = FileOper.readFileDirectory(args[0]);

		System.out.println("Directory Collection Size=" + dirfiles.size());
		FileOper.printCollection(dirfiles);
		System.out.println();
		System.out.println();

		Collection<String> onlyimages = FileOper.selectFileType(dirfiles,
				".tif");

		System.out.println("filtered Collection Size=" + onlyimages.size());
		FileOper.printCollection(onlyimages);

		Collection<String> backup = FileOper.copyCollection(onlyimages);

		FileOper.removeFile(backup, "white");

		System.out.println("after removal Collection Size=" + backup.size());
		FileOper.printCollection(backup);

		System.out.println("copy without removal Collection Size="
				+ onlyimages.size());
		FileOper.printCollection(onlyimages);

		Collection<String> sorted = FileOper.sort(onlyimages,
				FileOper.SORT_DESCENDING);
		System.out.println("sorted in descending order=" + sorted.size());
		FileOper.printCollection(sorted);

		List<String> labelSubStrings = new ArrayList<String>();
		labelSubStrings.add("band0");
		labelSubStrings.add("band1");
		labelSubStrings.add("band2");
		
		Collection<String> labeled = FileOper.label(sorted, labelSubStrings);
		System.out.println("labeled =" + labeled.size());
		FileOper.printCollection(labeled);

	}

}
