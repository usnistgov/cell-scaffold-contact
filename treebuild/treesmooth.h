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

#ifndef TREESMOOTH_H
#define TREESMOOTH_H
#include "tree.h"
#include <vector>

struct NodeForSmooth
{
    float x, y, z;
    float dx, dy, dz;
    std::vector<unsigned int> n;
};

class TreeSmooth
{
public:
    /**
     * @brief treeSmooth class to smooth tree branches
     * @param tree input tree
     * @param d1 first derivate minimization parameter
     * @param d2 second derivate minimization parameter
     * @param fa anchore to original location parameter - linear relationship
     * @param faaa anchore to original location parameter - third power relationship
     */
    TreeSmooth(Tree* tree, float d1, float d2, float fa, float faaa);
    Tree getResult(int iterations, int chillout);

private:
    float d1, d2, fa, faaa;
    Tree* tree;
    std::vector<NodeForSmooth> mtree;
    int iteration;

    void initialize(void);
    void compute(int iterations, int chillout);
    Tree getResult(void);

    void step(void);
    inline void computeF(int ni);
    inline void computeD(int ni);
};

#endif // TREESMOOTH_H
