/*
 * VesselKKnife - MRI image segmentation and characterization
 *
 * Copyright 2014-2016 Piotr M. Szczypiński <piotr.szczypinski@p.lodz.pl>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef TREEDIAMETERS_H
#define TREEDIAMETERS_H

#include "tree.h"
#include <vector>
#include "alglib/linalg.h"
#include "alglib/dataanalysis.h"
#include <itkImage.h>
#include <itkImageFileReader.h>
#include <itkImageFileWriter.h>
#include "getoption.h"

typedef itk::Image<unsigned char, 3>  ImageType;

class TreeDiameters
{
public:
    TreeDiameters(Tree* tree, ImageType* image, double gamma, int divider, double rho);
    Tree getResult();
    Tree getResult(int ooo);
    Tree getResult(unsigned int node[3]);

private:
    alglib::real_2d_array radially_spaced_directions;
    alglib::real_2d_array pts;
    alglib::real_2d_array cvm;
    alglib::real_1d_array w;
    alglib::real_2d_array u;
    alglib::real_2d_array vt;

    unsigned int radially_spaced_directions_number;
    double gamma;
    double rho;
    int divider;
    Tree *tree;
    ImageType *image;

    double diameterFromPca(void);
    void findVectors(double center[3]);
    void findBorderIntersection(double center[3], unsigned int direction_index);

};

#endif // TREEDIAMETERS_H
