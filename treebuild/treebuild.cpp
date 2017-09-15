/*
 * Optical microscopy of fiber scaffolds and bacteria image preprocessing.
 * Intended for segmentation of two channel data. One channel is for cell
 * the other for fiber scaffold. Program requires three threshold values.
 * Requires Insight Toolkit (ITK) version 4.20 or higher.
 *
 * Copyright (c) 2016 by Piotr M. Szczypiński
 * and NIST (National Institute of Standards and Technology)
 *
 * This software was developed by employees of the National Institute 
 * of Standards and Technology (NIST), an agency of the Federal 
 * Government. Pursuant to title 17 United States Code Section 105, 
 * works of NIST employees are not subject to copyright protection 
 * in the United States and are considered to be in the public domain. 
 * Permission to freely use, copy, modify, and distribute this software 
 * and its documentation without fee is hereby granted, provided that 
 * this notice and disclaimer of warranty appears in all copies.
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, 
 * EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED 
 * TO, ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, 
 * ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
 * DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT 
 * THE SOFTWARE WILL BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE 
 * FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT, INDIRECT, 
 * SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM, OR 
 * IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON 
 * WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS 
 * SUSTAINED BY PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT 
 * LOSS WAS SUSTAINED FROM, OR AROSE OUT OF THE RESULTS OF, OR USE OF, 
 * THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */

#include <itkImage.h>
#include <itkImageFileReader.h>
#include <itkImageFileWriter.h>
#include "getoption.h"

#include "treediameters.h"
#include "imagefilters.h"
#include "buildtree.h"
#include "treesmooth.h"
#include "smoothdiameter.h"

char* input_image = NULL;
char* median_image = NULL;
char* skeleton_image = NULL;
char* index_tree = NULL;
char* distance_tree = NULL;
char* smooth_tree = (char*)"tree.nii";
bool isprinthelp = false;

const int divider = 2;
const float rho = 1.4142136;
float gamma_parameter = 4;
int radius = 2;

typedef itk::Image<unsigned char, 3>  ImageType;
typedef itk::ImageFileReader<ImageType> ReaderType;
typedef itk::ImageFileWriter<ImageType> WriterType;

//=============================================================================

int scan_parameters(int argc, char* argv[])
{
    int argi = 1;
    while(argi < argc)
    {
        GET_STRING_OPTION("-i", "--input-image", input_image)
                else GET_STRING_OPTION("-m", "--median-image", median_image)
                else GET_STRING_OPTION("-k", "--skeleton-image", skeleton_image)
                else GET_STRING_OPTION("-x", "--index-tree", index_tree)
                else GET_STRING_OPTION("-d", "--distance-tree", distance_tree)
                else GET_STRING_OPTION("-s", "--smoothed-tree", smooth_tree)
                else GET_INT_OPTION("-r", "--median-radius", radius)
                else GET_FLOAT_OPTION("-g", "--gamma", gamma_parameter)
                else GET_NOARG_OPTION("/?", "--help", isprinthelp, true)
                else return argi;
        argi++;
    }
    return 0;
}

void printhelp(void)
{
    //------------------------------------------------------------------------------------------v
    printf("Usage: treebuild [OPTIONS]...\n");
    printf("Skeletonize binary image, builds tree of connections and estimates diameters.\n");
    printf("2016.09.09 by Piotr M. Szczypinski\n");
    printf("Compilation date and time: %s %s\n", __DATE__, __TIME__);
    printf("Options:\n");
    printf("  -i, --input-image <file>    loads binary image from <file>\n");
    printf("  -m, --median-image <file>   save median filtered image to <file>\n");
    printf("  -k, --skeleton-image <file> save skeleton image to <file>\n");
    printf("  -x, --index-tree <file>     save tree with index coordinates to <file>\n");
    printf("  -d, --distance-tree <file>  save tree with distaance coordinates and radius estimations to <file>\n");
    printf("  -s, --smoothed-tree <file>  save smoothed tree to <file>\n");
    printf("  -r, --median-radius         median filter radius (default 2)\n");
    printf("  -g, --gamma                 for gamma higher then 1, the smaller radius of ellipse is favored (dafault 4)\n");
    printf("  /?, --help                  Display this help and exit.\n\n");
}


//=============================================================================
// Program starts in main()
int main(int argc, char *argv[])
{
    ImageType::Pointer input;
    ImageType::Pointer median;
    ImageType::Pointer filled;
    ImageType::Pointer skeleton;

    ReaderType::Pointer reader = ReaderType::New();

    int ret = scan_parameters(argc, argv);
    if(isprinthelp || argc <= 1 || input_image == NULL)
    {
        printhelp();
        return ret;
    }
    if(ret != 0)
    {
        if(ret < argc) fprintf(stderr, "Incorrect operand: %s\n", argv[ret]);
        fprintf(stderr, "Try 'treebuild --help' for more information.\n");
        fflush(stdout);
        return ret;
    }

    reader->SetFileName(input_image);
    reader->Update();
    input = reader->GetOutput();

    if(input.IsNull())
    {
        printf("Cannot load from %s\n", input_image); fflush(stdout);
        return -1;
    }

    printf("------ Input image %s\n", input_image); fflush(stdout);

    median = ImageFilters<unsigned char>::median(input, radius);
    if(median_image != NULL)
    {
        WriterType::Pointer writer = WriterType::New();
        writer->SetFileName(median_image);
        writer->SetInput(median);
        writer->Update();
    }
    printf("  ---- Median filtration completed with radius = %i\n", radius); fflush(stdout);

    unsigned char foreground = ImageFilters< unsigned char>::maxIntensity(median);
    filled = ImageFilters<unsigned char>::holeFill(median, foreground);
    printf("  ---- Holes filled, maximum intensity = %i\n", (int)foreground); fflush(stdout);
    skeleton = ImageFilters<unsigned char>::skeletonFromBinary(filled);
    if(skeleton_image != NULL)
    {
        WriterType::Pointer writer = WriterType::New();
        writer->SetFileName(skeleton_image);
        writer->SetInput(skeleton);
        writer->Update();
    }
    printf("  ---- Skeleton computed\n"); fflush(stdout);

    TreeStructure tree_structure = BuildTree<unsigned char>::skeletonToTreeIntSpace(skeleton, NULL);
    if(index_tree != NULL)
    {
        Tree(tree_structure).save(index_tree, 0);
    }
    BuildTree<unsigned char>::treeIntSpaceToDistance(skeleton, &tree_structure);
    printf("  ---- Tree connectivity computed\n"); fflush(stdout);

    Tree tree_i = Tree(tree_structure);

    TreeDiameters diams(&tree_i, filled, gamma_parameter, divider, rho);
    Tree tree_d = diams.getResult();
    if(distance_tree != NULL)
    {
        tree_d.save(distance_tree, 0);
    }
    printf("  ---- Diameters estimated with gamma = %f\n", gamma_parameter); fflush(stdout);

    float d1 = 0.5;
    float d2 = 0.2;
    float fa = 0.01;
    float faaa = 0.01;
    int iterations = 10;
    int chillout = 5;

    TreeSmooth smoothing(&tree_d, d1, d2, fa, faaa);
    Tree tree_s1 = smoothing.getResult(iterations, chillout);
    SmoothDiameter smoothingd(&tree_s1, d1, fa, faaa);
    Tree tree_s2 = smoothingd.getResult(iterations, chillout);

    if(smooth_tree != NULL)
    {
        tree_s2.save(smooth_tree, 0);
    }
    printf("  ---- Centerlines and diameters smoothed\n"); fflush(stdout);
    printf("------ Finished\n"); fflush(stdout);
    return EXIT_SUCCESS;
}


