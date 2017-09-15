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


//#include <stdio.h>
#include <itkImage.h>
#include <itkImageFileReader.h>
#include <itkImageFileWriter.h>
#include "getoption.h"

extern "C" {
#include "FitsIO.h"
}

bool isprinthelp = false;
char* input_image = (char*)"input.nii";
char* output_image = (char*)"output.fits";
float x_spacing = -1.0;
float y_spacing = -1.0;
float z_spacing = -1.0;

//=============================================================================

int scan_parameters(int argc, char* argv[])
{
    int argi = 1;
    while(argi < argc)
    {
        GET_STRING_OPTION("-i", "--input", input_image)
        else GET_STRING_OPTION("-o", "--output", output_image)
        else GET_FLOAT_OPTION("-x", "--x_spacing", x_spacing)
        else GET_FLOAT_OPTION("-y", "--y_spacing", y_spacing)
        else GET_FLOAT_OPTION("-z", "--z_spacing", z_spacing)
        else GET_NOARG_OPTION("/?", "--help", isprinthelp, true)
        else return argi;
        argi++;
    }
    return 0;
}

void printhelp(void)
{
//------------------------------------------------------------------------------------------v
    printf("Usage: itk2fits -i input -o output [OPTIONS]...\n");
    printf("Converts FITS files to one of the ITK available formats or the other way.\n");
    printf("2016.08.28 by Piotr M. Szczypinski\n");
    printf("Compilation date and time: %s %s\n", __DATE__, __TIME__);
    printf("Options:\n");
    printf("  -i, --input <file>      Load 3D image from <file>,\n");
    printf("  -o, --output <file>     Save results to <file>,\n");
    printf("  -x, --x-spacing <float> Force voxel spacing in X direction,\n");
    printf("  -y, --y-spacing <float> Force voxel spacing in Y direction,\n");
    printf("  -z, --z-spacing <float> Force voxel spacing in Z direction,\n");
    printf("  /?, --help              Display this help and exit\n\n");
    printf("Example: itk2fits -i input.nii -o output.fits\n");
    printf("         itk2fits -i input.fits -o output.nii -x 0.12 -y 0.12 -z 0.462\n\n");
}

//=============================================================================

int main(int argc, char *argv[])
{
    int ret = scan_parameters(argc, argv);
    if(isprinthelp || argc <= 1)
    {
        printhelp();
        return ret;
    }
    if(ret != 0)
    {
        if(ret < argc) fprintf(stderr, "Incorrect operand: %s\n", argv[ret]);
        fprintf(stderr, "Try itk2fits --help for more information.\n");
        fflush(stdout);
        return ret;
    }

    internalDataType *data;
    int numVoxels[3];
    double voxelDims[3];
    char units[3*81];
    typedef itk::Image< internalDataType, 3 > Image3DType;
    Image3DType::Pointer output = Image3DType::New();
    Image3DType::RegionType Region;
    Image3DType::RegionType::IndexType Start;
    Image3DType::RegionType::SizeType Size;

    if (readFitsFile(input_image, &data, numVoxels, voxelDims, units) == 0)
    {
        double spac[3], orgi[3];
        for(int i = 0; i < 3; i++)
        {
            Start[i] = 0;
            Size[i]  = numVoxels[i];
            spac[i] = voxelDims[i];
            orgi[i] = 0;
        }

        if(x_spacing > 0) spac[0] = x_spacing;
        if(y_spacing > 0) spac[1] = y_spacing;
        if(z_spacing > 0) spac[2] = z_spacing;

        Region.SetIndex( Start );
        Region.SetSize( Size );

        output->SetRegions( Region );
        output->SetOrigin(orgi);
        output->SetSpacing(spac);

        size_t numberOfPixels = output->GetLargestPossibleRegion().GetNumberOfPixels();

        output->GetPixelContainer()->SetImportPointer(data, numberOfPixels, true); //true - dealocate data on delete, false - no date dealocation

        typedef itk::ImageFileWriter< Image3DType > WriterType;
        WriterType::Pointer writer = WriterType::New();
        writer->SetFileName(output_image);
        writer->SetInput(output);
        writer->Update();

//        printf("Units: %s, Size: %i %i %i, Spacing: %f %f %f\n",
//               units,
//               numVoxels[2], numVoxels[1], numVoxels[0],
//               (float)voxelDims[2], (float)voxelDims[1], (float)voxelDims[0]);

        return 0;
    }
    else
    {
        typedef itk::ImageFileReader<Image3DType> ReaderType;
        ReaderType::Pointer reader = ReaderType::New();

        reader->SetFileName(input_image);
        reader->Update();
        Image3DType::Pointer input = reader->GetOutput();

        for(int i = 0; i < 3; i++)
        {
            numVoxels[i] = input->GetLargestPossibleRegion().GetSize()[i];
            voxelDims[i] = input->GetSpacing()[i];
        }
        if(x_spacing > 0) voxelDims[0] = x_spacing;
        if(y_spacing > 0) voxelDims[1] = y_spacing;
        if(z_spacing > 0) voxelDims[2] = z_spacing;

//        printf("Units: %s, Size: %i %i %i, Spacing: %f %f %f\n",
//               units,
//               numVoxels[2], numVoxels[1], numVoxels[0],
//               (float)voxelDims[2], (float)voxelDims[1], (float)voxelDims[0]);

        size_t numberOfPixels = input->GetLargestPossibleRegion().GetNumberOfPixels();
        reverseEndianness(input->GetPixelContainer()->GetImportPointer(), numberOfPixels);

        if(writeFitsFile(output_image, input->GetPixelContainer()->GetImportPointer(), numVoxels, voxelDims, units) == 0)
            return 0;
    }
    printf("itk2fits: error converting files.\n");
    return 1;
}
