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

#include "treesmooth.h"

TreeSmooth::TreeSmooth(Tree *tree, float d1, float d2, float fa, float faaa)
{
    this->tree = tree;
    this->d1 = d1;
    this->d2 = d2;
    this->fa = fa;
    this->faaa = faaa;
}

void TreeSmooth::initialize(void)
{
    iteration = 0;

    int nodecount = tree->nodeCount();
    mtree.resize(nodecount);
    for(int i = 0; i < nodecount; i++)
    {
        mtree[i].x = tree->node(i).x;
        mtree[i].y = tree->node(i).y;
        mtree[i].z = tree->node(i).z;
    }

    int branchcount = tree->count();
    for(int b = 0; b < branchcount; b++)
    {
        int nodecount = tree->count(b);
        for(int k = 1; k < nodecount; k++)
        {
            unsigned int a1 = tree->nodeIndex(b, k);
            unsigned int a2 = tree->nodeIndex(b, k-1);
            mtree[a1].n.push_back(a2);
            mtree[a2].n.push_back(a1);
        }
    }
}

void TreeSmooth::compute(int iterations, int chillout)
{
    int i;
    float md1, md2, mfa, mfaaa;
    chillout++;
    md1 = d1 / chillout;
    md2 = d2 / chillout;
    mfa = fa / chillout;
    mfaaa = faaa / chillout;
    chillout--;
    for(i = 0; i < iterations; i++, iteration++)
    {
        step();
    }
    for(i = 0; i < chillout; i++, iteration++)
    {
        d1 -= md1;
        d2 -= md2;
        fa -= mfa;
        faaa -= mfaaa;
        step();
    }
}

Tree TreeSmooth::getResult(void)
{
    Tree tree = *this->tree;
    int nodecount = tree.nodeCount();
    for(int i = 0; i < nodecount; i++)
    {
        tree.setNode(i, mtree[i].x, mtree[i].y, mtree[i].z);
    }
    return tree;
}

Tree TreeSmooth::getResult(int iterations, int chillout)
{
    initialize();
    compute(iterations, chillout);
    return getResult();
}


void TreeSmooth::step(void)
{
    int ni;
    int nsize = mtree.size();
    for(ni = 0; ni < nsize; ni++)
    {
        mtree[ni].dx = 0.0;
        mtree[ni].dy = 0.0;
        mtree[ni].dz = 0.0;
        computeF(ni);
        computeD(ni);
    }
    for(ni = 0; ni < nsize; ni++)
    {
        mtree[ni].x += mtree[ni].dx;
        mtree[ni].y += mtree[ni].dy;
        mtree[ni].z += mtree[ni].dz;
    }
}

inline void TreeSmooth::computeF(int ni)
{
    float ddx = tree->node(ni).x - mtree[ni].x;
    float ddy = tree->node(ni).y - mtree[ni].y;
    float ddz = tree->node(ni).z - mtree[ni].z;
    mtree[ni].dx += (fa*ddx + faaa*ddx*ddx*ddx);
    mtree[ni].dy += (fa*ddy + faaa*ddy*ddy*ddy);
    mtree[ni].dz += (fa*ddz + faaa*ddz*ddz*ddz);
}

inline void TreeSmooth::computeD(int ni)
{
    int i;
    float ax;
    float ay;
    float az;
    int count = mtree[ni].n.size();
    if(count > 1)
    {
        ax = 0;
        ay = 0;
        az = 0;
// Obliczanie sredniej wspolrzednych dwoch sasiadow bliskich
        for(i = 0; i < count; i++)
        {
            int n = mtree[ni].n[i];
            ax += mtree[n].x;
            ay += mtree[n].y;
            az += mtree[n].z;
        }
        ax /= count;
        ay /= count;
        az /= count;

        mtree[ni].dx += d1*(ax - mtree[ni].x);
        mtree[ni].dy += d1*(ay - mtree[ni].y);
        mtree[ni].dz += d1*(az - mtree[ni].z);
    }
    int div = 0;
    ax = 0;
    ay = 0;
    az = 0;
    for(i = 0; i < count; i++)
    {
        int n = mtree[ni].n[i];
        if(mtree[n].n.size() == 2)
        {
            int nn = mtree[n].n[0];
            if(nn == ni) nn = mtree[n].n[1];
            //div++;
// Obliczanie rzutu punktu na prosta wyznaczona przez dwoch sasiadow: bliskiego i kolejnego
            float x_n_nn = mtree[n].x - mtree[nn].x;
            float x_ni_nn = mtree[ni].x - mtree[nn].x;
            float y_n_nn = mtree[n].y - mtree[nn].y;
            float y_ni_nn = mtree[ni].y - mtree[nn].y;
            float z_n_nn = mtree[n].z - mtree[nn].z;
            float z_ni_nn = mtree[ni].z - mtree[nn].z;
            float uu = (x_n_nn*x_n_nn + y_n_nn*y_n_nn + z_n_nn*z_n_nn);
            if(uu != 0.0)
            {
                float u = (x_n_nn*x_ni_nn + y_n_nn*y_ni_nn + z_n_nn*z_ni_nn)/uu;
                ax += (x_n_nn*u + mtree[nn].x);
                ay += (y_n_nn*u + mtree[nn].y);
                az += (z_n_nn*u + mtree[nn].z);
                div++;
            }
            else
            {
                ax += mtree[nn].x;
                ay += mtree[nn].y;
                az += mtree[nn].z;
                div++;
            }

//            ax += (2*mtree[n].x - mtree[nn].x);
//            ay += (2*mtree[n].y - mtree[nn].y);
//            az += (2*mtree[n].z - mtree[nn].z);
        }
    }
    if(div > 0)
    {
        ax /= div;
        ay /= div;
        az /= div;
        mtree[ni].dx += d2*(ax - mtree[ni].x);
        mtree[ni].dy += d2*(ay - mtree[ni].y);
        mtree[ni].dz += d2*(az - mtree[ni].z);
    }
}
