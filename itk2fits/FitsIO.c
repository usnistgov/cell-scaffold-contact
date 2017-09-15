// ================================================================
/// 
/// This software was developed by employees of the National Institute 
/// of Standards and Technology (NIST), an agency of the Federal 
/// Government. Pursuant to title 17 United States Code Section 105, 
/// works of NIST employees are not subject to copyright protection 
/// in the United States and are considered to be in the public domain. 
/// Permission to freely use, copy, modify, and distribute this software 
/// and its documentation without fee is hereby granted, provided that 
/// this notice and disclaimer of warranty appears in all copies.
/// THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, 
/// EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED 
/// TO, ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, 
/// ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
/// PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
/// DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT 
/// THE SOFTWARE WILL BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE 
/// FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT, INDIRECT, 
/// SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM, OR 
/// IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON 
/// WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS 
/// SUSTAINED BY PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT 
/// LOSS WAS SUSTAINED FROM, OR AROSE OUT OF THE RESULTS OF, OR USE OF, 
/// THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
/// 
/// ================================================================

// ================================================================
// 
// Authors: Derek Juba <derek.juba@nist.gov>
// Date:    2014-04-04
//
// ================================================================

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "FitsIO.h"



//void readMetaFile(const char *metaFileName, int *numVoxels,
//		  double *voxelDims, char *units) {
//  FILE *metaFile = fopen(metaFileName, "r");
//  if (metaFile == NULL) {
//    printf("Error opening meta file %s\n", metaFileName);
//    exit(1);
//  }

//  char unit[81];

//  int argsFilled = fscanf(metaFile, "%i %i %i %lf %lf %lf %80s",
//			  numVoxels+0, numVoxels+1, numVoxels+2,
//			  voxelDims+0, voxelDims+1, voxelDims+2,
//			  unit);

//  if (argsFilled != 7) {
//    printf("Error reading meta file %s\n", metaFileName);
//    exit(1);
//  }

//  strcpy(units + 0*81, unit);
//  strcpy(units + 1*81, unit);
//  strcpy(units + 2*81, unit);

//  fclose(metaFile);
//}

//void readRawFile(const char *rawFileName, int totalNumVoxels,
//		 unsigned char *voxels) {
//  FILE *rawFile = fopen(rawFileName, "rb");
//  if (rawFile == NULL) {
//    printf("Error opening raw file %s\n", rawFileName);
//    exit(1);
//  }

//  int elementsRead =
//    fread(voxels, sizeof(unsigned char), totalNumVoxels, rawFile);

//  if (elementsRead != totalNumVoxels) {
//    printf("Error reading from raw file\n");
//    exit(1);
//  }

//  fclose(rawFile);
//}

int readFitsFile(const char *inputFileName, internalDataType **data,
		  int *numVoxels, double *voxelDims, char *units) {
  FILE *inputFile = fopen(inputFileName, "rb");

  if (inputFile == NULL) {
    //printf("Error opening input fits file %s\n", inputFileName);
    return -1;
  }

  int bitsPerPixel = 0;

  int readError = 0;

  char fitsHeaderLine[81];
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "SIMPLE  = %*c");
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "BITPIX  = %d", &bitsPerPixel);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "NAXIS   = %*d");
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "NAXIS1  = %d", numVoxels + 0);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "NAXIS2  = %d", numVoxels + 1);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "NAXIS3  = %d", numVoxels + 2);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CDELT1  = %lf", voxelDims + 0);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CDELT2  = %lf", voxelDims + 1);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CDELT3  = %lf", voxelDims + 2);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CTYPE1  = %s", units + 0*81);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CTYPE2  = %s", units + 1*81);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "CTYPE3  = %s", units + 2*81);
  readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  sscanf(fitsHeaderLine, "END");

  for (int numHeaderLines = 13; numHeaderLines < 36; numHeaderLines++) {
    readError = readError || (fgets(fitsHeaderLine, 81, inputFile) == NULL);
  }

  if (readError) {
    //printf("Error reading input fits file %s\n", inputFileName);
    return -2;
  }

  int numElements = numVoxels[0] * numVoxels[1] * numVoxels[2];

  *data = (internalDataType *)malloc(numElements * sizeof(internalDataType));

  if (*data == NULL) {
    //printf("Error allocating memory for output data\n");
    return -3;
  }

  if (bitsPerPixel == 8) {
    unsigned char *inputData = 
      (unsigned char *)malloc(sizeof(unsigned char) * numElements);

    if (inputData == NULL) {
      //printf("Error allocating memory for input data\n");
      return -4;
    }

    int elementsRead = 
      fread(inputData, sizeof(unsigned char), numElements, inputFile);

    if (elementsRead != numElements) {
      //printf("Error reading input file %s\n", inputFileName);
      return -5;
    }

    for (int i = 0; i < numElements; i++) {
      (*data)[i] = inputData[i];
    }

    free(inputData);
  }
  else if (bitsPerPixel == 16) {
    unsigned short *inputData = 
      (unsigned short *)malloc(sizeof(unsigned short) * numElements);

    if (inputData == NULL) {
      //printf("Error allocating memory for input data\n");
      return -6;
    }

    int elementsRead = 
      fread(inputData, sizeof(unsigned short), numElements, inputFile);

    if (elementsRead != numElements) {
      //printf("Error reading input file %s\n", inputFileName);
      return -7;
    }

    for (int i = 0; i < numElements; i++) {
      (*data)[i] = inputData[i];
    }

    reverseEndianness(*data, numElements);

    free(inputData);
  }
  else {
    //printf("Error: invalid number of bits per pixel %d\n", bitsPerPixel);
    return -8;
  }

  fclose(inputFile);
  return 0;
}

int writeFitsFile(const char *outputFileName, const outputDataType *data,
		   const int *numVoxels, const double *voxelDims, 
		   const char *units) {
  FILE *outputFile = fopen(outputFileName, "wb");

  if (outputFile == NULL) {
    //printf("Error opening output fits file %s\n", outputFileName);
    return -1;
  }

  int bitsPerPixel = sizeof(outputDataType) * 8;

  char fitsHeaderLine[81];
  snprintf(fitsHeaderLine, 81, "SIMPLE  =   T / conforms to FITS standard");
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "BITPIX  = %d / number of bits per data pixel", 
	   bitsPerPixel);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "NAXIS   =   3 / number of data axes");
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "NAXIS1  = %d / length of data axis 1", 
	   numVoxels[0]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "NAXIS2  = %d / length of data axis 2", 
	   numVoxels[1]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "NAXIS3  = %d / length of data axis 3", 
	   numVoxels[2]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, 
	   "CDELT1  = %lf / length of each voxel along axis 1", voxelDims[0]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, 
	   "CDELT2  = %lf / length of each voxel along axis 2", voxelDims[1]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, 
	   "CDELT3  = %lf / length of each voxel along axis 3", voxelDims[2]);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "CTYPE1  = %s / units along axis 1", units+0*81);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "CTYPE2  = %s / units along axis 2", units+1*81);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "CTYPE3  = %s / units along axis 3", units+2*81);
  fprintf(outputFile, "%-80s", fitsHeaderLine);
  snprintf(fitsHeaderLine, 81, "END");
  fprintf(outputFile, "%-80s", fitsHeaderLine);

  for (int numHeaderLines = 13; numHeaderLines < 36; numHeaderLines++) {
    fprintf(outputFile, "%-80s", "");
  }

  int totalNumVoxels = numVoxels[0] * numVoxels[1] * numVoxels[2];

  int numVoxelsWritten = 
    fwrite(data, sizeof(outputDataType), totalNumVoxels, outputFile);

  if (numVoxelsWritten != totalNumVoxels) {
    //printf("Error writing to fits file %s\n", outputFileName);
    return -2;
  }

  fclose(outputFile);

  return 0;
}

void reverseEndianness(internalDataType *data, int numElements) {
  for (int i = 0; i < numElements; i++) {
    internalDataType itemToReverse = data[i];
    internalDataType reversedItem = 0;

    for (unsigned int byteIndex = 0; 
	 byteIndex < sizeof(itemToReverse); 
	 byteIndex++) {
      ((char *) &reversedItem)[byteIndex] = 
	((char *) &itemToReverse)[sizeof(itemToReverse) - 1 - byteIndex];
    }

    data[i] = reversedItem;
  }
}
