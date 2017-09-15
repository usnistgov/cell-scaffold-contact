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

//typedef unsigned int internalDataType;

typedef short int internalDataType;
typedef short int outputDataType;

//typedef unsigned char internalDataType;
//typedef unsigned char outputDataType;

void reverseEndianness(internalDataType *data, int numElements);

//void readMetaFile(const char *metaFileName, int *numVoxels,
//		  double *voxelDims, char *units);

//void readRawFile(const char *rawFileName, int totalNumVoxels,
//		 unsigned char *voxels);

int readFitsFile(const char *inputFileName, internalDataType **data,
		  int *numVoxels, double *voxelDims, char *units);

int writeFitsFile(const char *outputFileName, const outputDataType *data,
		   const int *numVoxels, const double *voxelDims,
		   const char *units);
