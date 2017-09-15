% This software was developed by employees of the National Institute of
% Standards and Technology (NIST), an agency of the Federal Government.
% Pursuant to title 17 United States Code Section 105, works of NIST
% employees are not subject to copyright protection in the United States
% and are considered to be in the public domain. Permission to freely use,
% copy, modify, and distribute this software and its documentation without
% fee is hereby granted, provided that this notice and disclaimer of
% warranty appears in all copies.
% THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
% EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY
% WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED
% WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND
% FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL
% CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR
% FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT
% NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES,
% ARISING OUT OF, RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS
% SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY, CONTRACT, TORT, OR
% OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR PROPERTY OR
% OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT OF
% THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.


%%% Cell-scaffold contact point estimation %%%

I. Crop Z-stacks

a. Main
	|- main_CropZStacks.m

b. Functions
	|- CropZStacks.m
		|- Input Data
		(1) TIFS image files for cell (ch00) and scaffold (ch01)
		(2) Cell segmentation information (/Segmentations/[DB-Name]-offset2000_MinError/[DB-Name]_segmentation.csv)
		(3) Background image information (either the first or last slice of z-stack)
		|- Parameters
		(1) xyMargin: 100*[xyMargin]-percentage margin for x and y bounds
		(2) smoothFilterSize: Size of the Gaussian filter to locate cell along z-axis
		(3) smoothFilterSTD: Standard deviation of the Gaussian filter
		|- Outputs
		(1) Cropped z-stacks for cell and scaffold channels in FITS (int16)
		(2) Cropped background images for two channels
		(3) Bounding box information in CSV file


		
II. Cell-Scaffold Contact Point Estimation: Statistical Models

a. Main
	|- main_StatisticalModels.m

b. Functions
	|- KmeansClustering
		|- Input Data
		(1) Cell and scaffold z-stacks in FITS (int16)
		|- Outputs
		(1) Probability maps for cell and scaffold in FITS (double)

	|- EstimateContactPointProb
		|- Input Data
		(1) Cell and scaffold z-stacks in FITS (int16)
		(2) Background images for two channels in TIFS
		(3) Probability maps for cell and scaffold in FITS (double)
		|- Parameters
		(1) algorithm_method: Selection of algorithm number (1, 2, ..., 5) for contact point estimation
		(2) threshold_percentile: Intensity value corresponding to this percentile of image histogram will be selected
		(3) threshold_method: Thresholding method; {NormalThresholding, PercentileThresholding}
		(4) threshold_morphType: 'Open' operation with this structuring element
		(5) threshold_morphSize: Size of the structuring element
		(6) neighborhoodSize: Neighborhood size to determine contact point
		(7) contactPointProbMethod: Method for computing contact point probability {Max, Mean}
		(8) flatFieldCorrectionThreshold: Threshold value for flat field correction
		|- Outputs
		(1) Probability map for contact points in FITS (int16)

	|- ShapeDescription_PCA
		|- Input Data
		(1) Binary map for contact points in FITS (uint8)
		|- Parameters
		(1) CDELT1: Unit size along x-axistest_shapeDescription_WeightedInertia.xml
		(2) CDELT2: Unit size along y-axis
		(3) CDELT3: Unit size along z-axis
		(4) unit: Unit of CDELT1, CDELT2, and CDELT3
		|- Outputs
		(1) PCA results in CSV file
			'ImageName': Image name
			'CentroidX','CentroidY','CentroidZ': Centroid
				'PrincipalAxesEigenValue1','PrincipalAxesEigenValue2','PrincipalAxesEigenValue3': Eigenvalues
				'PrincipalAxesEigenVector1X','PrincipalAxesEigenVector1Y','PrincipalAxesEigenVector1Z': Eigenvector corresponding to PrincipalAxesEigenValue1
				'PrincipalAxesEigenVector2X','PrincipalAxesEigenVector2Y','PrincipalAxesEigenVector2Z': Eigenvector corresponding to PrincipalAxesEigenValue2
				'PrincipalAxesEigenVector3X','PrincipalAxesEigenVector3Y','PrincipalAxesEigenVector3Z': Eigenvector corresponding to PrincipalAxesEigenValue3
				'Unit',cell(nD,1));

	|- ShapeDescription_WeightedInertia
		|- Input Data
		(1) Probability map for contact points in FITS (double)
		|- Parameters
		(1) CDELT1: Unit size along x-axis
		(2) CDELT2: Unit size along y-axis
		(3) CDELT3: Unit size along z-axis
		(4) unit: Unit of CDELT1, CDELT2, and CDELT3
		|- Outputs
		(1) Weighted inertia computation results in CSV file
			'ImageName': Image name
			'CentroidX','CentroidY','CentroidZ': Centroid
				'EigenValue1','EigenValue2','EigenValue3': Eigenvalues
				'EigenVector1X','EigenVector1Y','EigenVector1Z': Eigenvector corresponding to PrincipalAxesEigenValue1
				'EigenVector2X','EigenVector2Y','EigenVector2Z': Eigenvector corresponding to PrincipalAxesEigenValue2
				'EigenVector3X','EigenVector3Y','EigenVector3Z': Eigenvector corresponding to PrincipalAxesEigenValue3
				'Unit',cell(nD,1));

	|- BinaryAggregation
		|- Input Data
		(1) Binary maps for contact points in FITS (uint8); multiple directories can be used
		(2) Corresponding PCA shape information in CSV
		|- Parameters
		(1) scaffoldType: Name of the scaffold type
		(2) sizeWidth: Size of the aggregated occurrence map (width; dimension 3)
		(3) sizeHeight: Size of the aggregated occurrence map (height; dimension 2)
		(4) sizeLayer: Size of the aggregated occurrence map (layer; dimension 1)
		|- Outputs
		(1) Aggregated occurrence map
			'P_total': Occurrence map; each pixel value ranges from 0 to nD
			'nD': Total number of z-stacks aggregated
			'CtrX','CtrY','CtrZ': Center point location in P_total


			
III. Cell-Scaffold Contact Point Estimation: Planar Geometrical Models for Spun Coat

a. Main
	|- main_PlanarGeometricalModel.m
	
b. Functions
	|- FitPlane
		|- Input Data
		(1) Cropped scaffold z-stack in FITS (int16)
		|- Parameters
		(1) dx: Length of each pixel along axis 1
		(2) dy: Length of each pixel along axis 2
		(3) dz: Length of each pixel along axis 3
		(4) du: Quantization unit
		(5) DoF: Degree of freedom to get relative standard deviation of planar fitting result
		(6) unit: Unit of measurements
		|- Outputs
		(1) Coefficient of hyperplane (ax + by + cz + d = 0), upper and lower z-intercepts of spun coat (z_lb and z_ub), thickness of spun coat, and statistics in CSV file

	|- FitPlane_CellIntersection
		|- Input Data
		(1) Binary cell z-stack in FITS (uint8)
		(2) Coefficient of hyperplane (ax + by + cz + d = 0), upper and lower z-intercepts of spun coat (z_lb and z_ub), thickness of spun coat, and statistics in CSV file
		|- Parameter
		(1) ez: Distance from the 3D plane to determine cell-scaffold intersection voxel
		|- Output data
		(1) Binary cell-scaffold intersection in FITS (uint8)
			

			
IV. XML Files

All inputs to a function are contained in an xml file.

The m-files to generate sample xml files are:
main_CreateXML_BinaryAggregation.m
main_CreateXML_CropZStacks.m
main_CreateXML_EstimateContactPointProb.m
main_CreateXML_KmeansClustering.m
main_CreateXML_ShapeDescription_PCA.m
main_CreateXML_ShapeDescription_WeightedInertia.m
main_CreateXML_FitPlane.m
main_CreateXML_FitPlane_CellIntersection.m

The sample xml files are:
test_binaryAggregation.xml
test_contact.xml  
test_crop.xml
test_shapeAggregation.xml
test_shapeDescription_PCA.xml
test_shapeDescription_WeightedInertia.xml
test_fitPlane.xml
test_fitPlane_CellIntersection.xml


V. Test Data

A set of test data is included. Its data structure is:

|-- test_data
	|-- BackgroundImageFileNames
		|-- SpunCoat-06252015.txt
	|-- ContactPointEstimation
		|-- Alg1
			|-- SpunCoat-06252015_ZProfile
				|-- 061115_SC_1_63x_Pos045.fits
				|-- 061115_SC_1_63x_Pos046.fits
		|-- Alg2
			|-- Aggregated
				|-- SpunCoat_P_total.fits
				|-- Center_nD.csv
			|-- SpunCoat-06252015_ZProfile
				|-- 061115_SC_1_63x_Pos045.fits
				|-- 061115_SC_1_63x_Pos046.fits
				|-- BinaryContact
					|-- 061115_SC_1_63x_Pos045_OutA2.fits
					|-- 061115_SC_1_63x_Pos046_OutA2.fits
				|-- Shape
					|-- ImageUnit
						|-- PCA.csv
					|-- PhysicalUnit
						|-- PCA.csv
						|-- WeightedInertia.csv
			|-- SpunCoat-07072015_ZProfile
				|-- 070215_SC_2_63x_Pos001.fits
				|-- 070215_SC_2_63x_Pos002.fits
				|-- BinaryContact
					|-- 070215_SC_2_63x_Pos001_OutA2.fits
					|-- 070215_SC_2_63x_Pos002_OutA2.fits
				|-- Shape
					|-- ImageUnit
						|-- PCA.csv
					|-- PhysicalUnit
						|-- PCA.csv
						|-- WeightedInertia.csv
		|-- Alg3
			|-- SpunCoat-06252015_ZProfile
				|-- 061115_SC_1_63x_Pos045.fits
				|-- 061115_SC_1_63x_Pos046.fits
		|-- Alg4
			|-- SpunCoat-06252015_ZProfile
				|-- 061115_SC_1_63x_Pos045.fits
				|-- 061115_SC_1_63x_Pos046.fits
		|-- Alg5
			|-- SpunCoat-06252015_ZProfile
				|-- 061115_SC_1_63x_Pos045.fits
				|-- 061115_SC_1_63x_Pos046.fits
		|-- Kmeans
			|-- SpunCoat-06252015_ZProfile
				|-- FITS
					|-- 061115_SC_1_63x_Pos045_ch00.fits
					|-- 061115_SC_1_63x_Pos045_ch01.fits
					|-- 061115_SC_1_63x_Pos046_ch00.fits
					|-- 061115_SC_1_63x_Pos046_ch01.fits
	|-- ExtractedTiffs06252015
		|-- Spun Coat
			|-- 061115_SC_1_63x_Pos045
				|-- 061115_SC_1_63x_Pos045_S001ch0LUT.tif
				|-- 061115_SC_1_63x_Pos045_S001ch1LUT.tif
				|-- 061115_SC_1_63x_Pos045_S001_Properties.xml
				|-- 061115_SC_1_63x_Pos045_S001.xml
				|-- 061115_SC_1_63x_Pos045_S001_z***_ch00.tif
				|-- 061115_SC_1_63x_Pos045_S001_z***_ch01.tif
			|-- 061115_SC_1_63x_Pos046
				|-- 061115_SC_1_63x_Pos046_S001ch0LUT.tif
				|-- 061115_SC_1_63x_Pos046_S001ch1LUT.tif
				|-- 061115_SC_1_63x_Pos046_S001_Properties.xml
				|-- 061115_SC_1_63x_Pos046_S001.xml
				|-- 061115_SC_1_63x_Pos046_S001_z***_ch00.tif
				|-- 061115_SC_1_63x_Pos046_S001_z***_ch01.tif
	|-- Segmentations
		|-- SpunCoat-06252015-offset2000_MinError
			|-- 061115_SC_1_63x_Pos045.fits
			|-- 061115_SC_1_63x_Pos046.fits
			|-- SpunCoat-06252015_segmentation.csv
		|-- SpunCoat-06252015_ZProfile (expected results)
			|-- BackgroundImages
				|-- 061115_SC_1_63x_Pos045_ch00.tif
				|-- 061115_SC_1_63x_Pos045_ch01.tif
				|-- 061115_SC_1_63x_Pos046_ch00.tif
				|-- 061115_SC_1_63x_Pos046_ch01.tif
			|-- FITS
				|-- 061115_SC_1_63x_Pos045_ch00.fits
				|-- 061115_SC_1_63x_Pos045_ch01.fits
				|-- 061115_SC_1_63x_Pos046_ch00.fits
				|-- 061115_SC_1_63x_Pos046_ch01.fits
	|-- PlanarGeoModel
		|-- Hyperplane_Coefficients.csv
		|-- Hyperplane_Coefficients_PhysicalUnit.csv
		|-- CellBW
			|-- 061115_SC_1_63x_Pos045.fits
			|-- 061115_SC_1_63x_Pos046.fits
		|-- CellIntersection
			|-- 061115_SC_1_63x_Pos045.fits
			|-- 061115_SC_1_63x_Pos046.fits