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

#include "treediameters.h"
#include "sphear.h"


using namespace alglib;

TreeDiameters::TreeDiameters(Tree *tree, ImageType *image, double gamma, int divider, double rho)
{
    this->tree = tree;
    this->image = image;
    this->gamma = gamma;
    this->rho = rho;
    this->divider = divider;
    w.setlength(3);
    cvm.setlength(3, 3);
    u.setlength(3, 3);
    vt.setlength(3, 3);

    ImageType::SpacingType voxelsize = image->GetSpacing();

    if(divider >= 0)
    {
        Sphear sphear(divider);
        radially_spaced_directions_number = sphear.verticesNumber();
        radially_spaced_directions.setlength(radially_spaced_directions_number, 3);
        double vertex[3];
        for(unsigned int direction = 0; direction < radially_spaced_directions_number; direction++)
        {
            sphear.vertex(direction, vertex);
            radially_spaced_directions(direction, 0) = vertex[0] / voxelsize[0];
            radially_spaced_directions(direction, 1) = vertex[1] / voxelsize[1];
            radially_spaced_directions(direction, 2) = vertex[2] / voxelsize[2];
        }
    }
    else
    {
        Sphear sphear(-divider-1);
        radially_spaced_directions_number = sphear.trianglesNumber();
        radially_spaced_directions.setlength(radially_spaced_directions_number, 3);

        unsigned int* tr = new unsigned int[radially_spaced_directions_number*3];
        sphear.triangles(tr);
        double vertex[3];

        for(unsigned int direction = 0; direction < radially_spaced_directions_number; direction++)
        {
            vertex[0] = 0;
            vertex[1] = 0;
            vertex[2] = 0;
            for(unsigned int v = 0; v < 3; v++)
            {
                double vertext[3];
                sphear.vertex(tr[direction*3 + v], vertext);
                vertex[0] += vertext[0];
                vertex[1] += vertext[1];
                vertex[2] += vertext[2];
            }
            //double dist = sqrt(vertex[0]*vertex[0]+vertex[1]*vertex[1]+vertex[2]*vertex[2]);
            radially_spaced_directions(direction, 0) = vertex[0] / voxelsize[0];
            radially_spaced_directions(direction, 1) = vertex[1] / voxelsize[1];
            radially_spaced_directions(direction, 2) = vertex[2] / voxelsize[2];
        }
        delete[] tr;
    }
    pts.setlength(radially_spaced_directions_number, 3);
}

Tree TreeDiameters::getResult()
{
    ImageType::SpacingType voxelsize = image->GetSpacing();

    Tree tree = *this->tree;
    int nodecount = tree.nodeCount();
    for(int i = 0; i < nodecount; i++)
    {
        double center[3];
        center[0] = tree.node(i).x / voxelsize[0] + 0.5;
        center[1] = tree.node(i).y / voxelsize[1] + 0.5;
        center[2] = tree.node(i).z / voxelsize[2] + 0.5;
        findVectors(center);
        tree.setDiameter(i, diameterFromPca());
    }
    return tree;
}




double TreeDiameters::diameterFromPca(void)
{
    covm(pts, radially_spaced_directions_number, 3, cvm);

    if(rmatrixsvd(cvm, 3, 3, 0, 0, 2, w, u, vt))
    {
        double lamb2, lamb3;
        if(w(2) >= w(1) && w(2) >= w(0)) {lamb2 = w(1); lamb3 = w(0);}
        else if(w(0) >= w(2) && w(0) >= w(1)) {lamb2 = w(1); lamb3 = w(2);}
        else {lamb2 = w(2); lamb3 = w(0);}
        double radius = -gamma/2.0;
        radius = (pow(lamb2, radius) + pow(lamb3, radius))/2.0;
        radius = rho * pow(radius, -1/gamma);
        return radius;
    }
    else
        return 0;
}

void TreeDiameters::findVectors(double center[3])
{
    ImageType::IndexType node;
    node[0] = center[0];
    node[1] = center[1];
    node[2] = center[2];

    ImageType::RegionType region = image->GetLargestPossibleRegion();
    ImageType::SizeType imageSize = region.GetSize();

    if(node[0] >= imageSize[0] || node[1] >= imageSize[1] || node[2] >= imageSize[2])
    {
        for(unsigned int direction_index = 0; direction_index < radially_spaced_directions_number; direction_index++)
            for(int dd = 0; dd < 3; dd++)
                pts(direction_index, dd) = 0.0;
        return;
    }

    unsigned char pix = image->GetPixel(node);
    if(pix)
    {
        for(unsigned int direction_index = 0; direction_index < radially_spaced_directions_number; direction_index++)
        {
            findBorderIntersection(center, direction_index);
        }
    }
    else
    {
        for(unsigned int direction_index = 0; direction_index < radially_spaced_directions_number; direction_index++)
        {
            for(int dd = 0; dd < 3; dd++)
                pts(direction_index, dd) = 0.0;
        }
    }
}

/**
 * @brief TreeDiameters::findBorderIntersection
 * @param center center (node) location in voxel space
 * @param direction_index index of direction indicating pts() vector
 */
void TreeDiameters::findBorderIntersection(double center[3], unsigned int direction_index)
{
    const double almostzero = 0.000001;
    //double ctr[3]; // center location in voxel space
    double tmp[3]; // temporary distance
    double shift[3]; // shift for x, y, and z step
    double dist[3]; // distance for x, y, and z step
    double dir[3]; // direction in voxel space
    int d;

    ImageType::SpacingType voxelsize = image->GetSpacing();
    ImageType::RegionType region = image->GetLargestPossibleRegion();
    ImageType::SizeType imageSize = region.GetSize();
    // Initialization
    for(d = 0; d < 3; d++)
    {
        dist[d] = -1.0;
        dir[d] = radially_spaced_directions(direction_index, d); // voxelsize[d];
        shift[d] = center[d];

        if(dir[d] > 0) shift[d] = ceil(shift[d]);
        else shift[d] = floor(shift[d]);

        if(fabs(dir[d]) > almostzero)
        {
            tmp[d] = shift[d]-center[d];
            dist[d] = 0.0;
            for(int dd = 0; dd < 3; dd++)
            {
                if(dd != d) tmp[dd] = tmp[d]*dir[dd]/dir[d];
                dist[d] += tmp[dd]*tmp[dd];
            }
        }
    }

    // Computation of plane and line intersection point with the subsequent (from center point) planes
    do
    {
        ImageType::IndexType node; //center location in voxel index space

        for(d = 0; d < 3; d++)
        {
            if( dist[d] >= 0.0 &&
                (dist[d] <= dist[0] || dist[0] < 0) &&
                (dist[d] <= dist[1] || dist[1] < 0) &&
                (dist[d] <= dist[2] || dist[2] < 0) )

            {
                tmp[d] = shift[d]-center[d];
                if(dir[d] > 0) node[d] = shift[d] + 1.0;
                else node[d] = shift[d];
                for(int dd = 0; dd < 3; dd++)
                {
                    if(dd != d)
                    {
                        tmp[dd] = tmp[d]*dir[dd]/dir[d];
                        node[dd] = tmp[dd] + center[dd] + 0.5;
                    }
                }

                if(node[0] >= imageSize[0] || node[1] >= imageSize[1] || node[2] >= imageSize[2])
                {
                    for(int dd = 0; dd < 3; dd++)
                        pts(direction_index, dd) = tmp[dd]*voxelsize[dd];
                    return;
                }
                unsigned char pix = image->GetPixel(node);
                if(pix)
                {
                    if(dir[d] > 0) shift[d] += 1.0;
                    else shift[d] -= 1.0;

                    tmp[d] = shift[d]-center[d];
                    dist[d] = 0.0;
                    for(int dd = 0; dd < 3; dd++)
                    {
                        if(dd != d) tmp[dd] = tmp[d]*dir[dd]/dir[d];
                        dist[d] += tmp[dd]*tmp[dd];
                    }
                }
                else
                {
                    for(int dd = 0; dd < 3; dd++)
                        pts(direction_index, dd) = tmp[dd]*voxelsize[dd];
                    return;
                }
                break;
            }
        }
        if(d >= 3)
        {
            for(int dd = 0; dd < 3; dd++)
                pts(direction_index, dd) = 0.0;
            return;
        }
    }while(true);
}

