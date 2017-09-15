/*
 * VesselTree - MRI image segmentation and characterization
 *
 * Copyright 2014  Piotr M. Szczypiński <piotr.szczypinski@p.lodz.pl>
 *                 Adam Sankowski <sankowski@gmail.com>
 *                 Grzegorz Dwojakowski <grzegorz.dwojakowski@gmail.com>
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


#ifndef BUILDTREE_H
#define BUILDTREE_H

#include <itkImage.h>
#include <itkNeighborhoodIterator.h>
#include <itkImageRegionIterator.h>
#include <itkConstantBoundaryCondition.h>
#include <itkImageDuplicator.h>
#include <limits>
#include "tree.h"


const int neighborhoodScanSequence[27][3] =
{
    {0,0,0}, //0 center
    {0,0,1},{0,0,-1},{0,1,0},{0,-1,0},{1,0,0},{-1,0,0}, //1-6 facet neighbor
    {0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},{1,1,0},{1,-1,0},{-1,1,0},{-1,-1,0},{1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1}, //7-18 edge neighbor
    {1,1,1},{1,1,-1},{1,-1,1},{1,-1,-1},{-1,1,1},{-1,1,-1},{-1,-1,1},{-1,-1,-1} //19-26 vertex neighbor
};

//----------------------------------------------------------------------------------------
/** \classt BuildTree
 *  \brief BuildTree jest klasą funkcji budowania drzewa o opisie wektorowym na podstawie rastrowego obrazu szkiletu
 */


template <class T> class BuildTree
{
public:
    typedef itk::Image< T, 3 > ImageType;

    struct Index3DNeighbor
    {
        typename ImageType::IndexType i;
        int n;
    };


    /**
     * @brief skeletonToTree analizuje obraz rastrowy ze szkieletem i buduje na jego podstawie drzewo
     * @param image wejściowy obraz rastrowy, woksele szkieletu muszą być oznaczone liczbami większymi od zera pozostałe muszą być wyzeorwane
     * @return zwrace obiekt klasy drzewa ze współrzędnymi będącymi indeksami wokseli
     */
/*
    static Tree skeletonToTreeIntSpaceBuffer(typename ImageType::Pointer input_image)
    {
        typedef itk::ConstantBoundaryCondition< ImageType > BoundaryConditionType;
        typedef itk::NeighborhoodIterator< ImageType, BoundaryConditionType> NeighborhoodIteratorType;
        typename NeighborhoodIteratorType::RadiusType radius;
        radius[0]=1; radius[1]=1; radius[2]=1;
        typename ImageType::IndexType regionIndex;
        regionIndex[0]=0;regionIndex[1]=0;regionIndex[2]=0;
        typename ImageType::SizeType regionSize = input_image->GetRequestedRegion().GetSize();
        typename ImageType::RegionType region;
        region.SetSize(regionSize);
        region.SetIndex(regionIndex);
        itk::NeighborhoodIterator<ImageType> iteratorStart(radius, input_image, region);

        int sc = iteratorStart.Size() / 2;
        int sy = iteratorStart.GetStride(1);
        int sz = iteratorStart.GetStride(2);

        for(iteratorStart.GoToBegin(); !iteratorStart.IsAtEnd(); ++iteratorStart)
        {
            int neighbors = 0;
            unsigned int i = 1;
            if(iteratorStart.GetCenterPixel() <= 0) continue;
            for(; i < 7; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1) break;
            neighbors = 0;
            for(; i < 19; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1) break;
            neighbors = 0;
            for(; i < 27; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1) break;
        }
        typename ImageType::IndexType startNodeIndex = iteratorStart.GetIndex();
    // Now branch tip is available from startNodeIndex
    //    std::cout << startNodeIndex << std::endl;

    // Copy image to a buffer
        typedef itk::ImageDuplicator< ImageType > DuplicatorType;
        typename DuplicatorType::Pointer duplicator = DuplicatorType::New();
        duplicator->SetInputImage(input_image);
        duplicator->Update();
        typename ImageType::Pointer bufferImage = duplicator->GetOutput();

    // Branch tracking based on flood-fill algorithm with stack
        Tree treeToCreate;
        std::vector<NodeIn3D> newBranch;
        std::vector<Index3DNeighbor> bifurStack;
        NodeIn3D newNode;
        Index3DNeighbor newNodeIndex;
        newNodeIndex.i = startNodeIndex;
        newNodeIndex.n = 1;
        newNode.connections = 0;
        newNode.diameter = 0;
        bifurStack.push_back(newNodeIndex);
        bufferImage->SetPixel(newNodeIndex.i, 0);

        while(bifurStack.size() > 0)
        {
            if(bifurStack.back().n >= 27)
            {
                bifurStack.pop_back();
                if(newBranch.size() > 0)
                {
                    treeToCreate.addBranchLoops(newBranch);
                    newBranch.clear();
                }
            }
            else
            {
                regionIndex = bifurStack.back().i;
                region.SetIndex(regionIndex);
                itk::NeighborhoodIterator<ImageType> iteratorN(radius, bufferImage, region);

                for(; bifurStack.back().n < 27; bifurStack.back().n++)
                {
                    const int* nss = neighborhoodScanSequence[bifurStack.back().n];
                    int offset = sc+nss[0]+nss[1]*sy+nss[2]*sz;
                    if(iteratorN.GetPixel(offset) > 0)
                    {
                        if(newBranch.size() <= 0)
                        {
                            newNodeIndex.i = iteratorN.GetIndex(sc);
                            newNode.x = newNodeIndex.i[0];
                            newNode.y = newNodeIndex.i[1];
                            newNode.z = newNodeIndex.i[2];
                            newBranch.push_back(newNode);
    //                        std::cout << newNodeIndex.i << std::endl;
                        }

                        newNodeIndex.i = iteratorN.GetIndex(offset);
                        newNodeIndex.n = 1;
                        bifurStack.push_back(newNodeIndex);

                        typename ImageType::IndexType pindex = iteratorN.GetIndex(offset);
                        bufferImage->SetPixel(pindex, 0);
                        newNode.x = pindex[0];
                        newNode.y = pindex[1];
                        newNode.z = pindex[2];
                        newBranch.push_back(newNode);
    //                    std::cout << newNodeIndex.i << std::endl;
                        break;
                    }
                }
            }
        }
        return treeToCreate;
    }
*/



//pms_nist
    /**
     * @brief findAllNodes find nonzero values in image and creates list of their coordinates
     * @param bufferImage image with nodes indicated by nonzero values
     * @param seedlist list of coordinates to return
     */
    static void findAllNodes(typename ImageType::Pointer bufferImage, std::vector<typename ImageType::IndexType>* seedlist)
    {
        itk::ImageRegionIterator< ImageType > iterator;
        iterator = itk::ImageRegionIterator< ImageType >(bufferImage, bufferImage->GetRequestedRegion());

        iterator.GoToBegin();
        while (!iterator.IsAtEnd())
        {
            if (iterator.Get() > 0)
            {
                seedlist->push_back(iterator.GetIndex());
            }
            ++iterator;
        }
    }
//pms_nist
    /**
     * @brief skeletonToTreeIntSpace builds a single-connected tree starting from the seed
     * @param bufferImage
     * @param treeToCreate
     * @param seedlist
     */
    static void skeletonToTreeIntSpace(typename ImageType::Pointer bufferImage,
                                       TreeStructure* treeToCreate,
                                       typename ImageType::IndexType seedToBegin)
    {
        unsigned int i;
        // Stack of bifurcation points to go back and check for connected voxels

        typedef itk::ConstantBoundaryCondition< ImageType > BoundaryConditionType;
        typedef itk::NeighborhoodIterator< ImageType, BoundaryConditionType> NeighborhoodIteratorType;
        typename NeighborhoodIteratorType::RadiusType radius;
        typename ImageType::IndexType region_index;
        typename ImageType::SizeType region_size;
        typename ImageType::RegionType region;

        for (i = 0; i < ImageType::ImageDimension; ++i)
        {
            radius[i] = 1;
            region_index[i] = 0;
        }
        region_size = bufferImage->GetRequestedRegion().GetSize();
        region.SetSize(region_size);
        region.SetIndex(region_index);

        itk::NeighborhoodIterator<ImageType> image_iterator(radius, bufferImage, region);
        int sc = image_iterator.Size() / 2;
        int sy = image_iterator.GetStride(1);
        int sz = image_iterator.GetStride(2);

        struct StackElement
        {
            unsigned int index;
            unsigned int n;
        };
        StackElement stack_element;
        NodeIn3D new_node;
        std::vector<StackElement> bifur_node_index_stack;
        //std::vector<unsigned int>
        BasicBranch new_branch;

        // Initialize stack with the new node (seed) index
        unsigned int new_node_index = treeToCreate->nodes.size();
        stack_element.n = 1;
        stack_element.index = new_node_index;
        bifur_node_index_stack.push_back(stack_element);
        // Create new branch to be added to tree when complete
        new_branch.nodeIndex.push_back(new_node_index);
        // Add a seed to the tree nodes vector
        new_node.x = seedToBegin[0];
        new_node.y = seedToBegin[1];
        new_node.z = seedToBegin[2];
        new_node.connections = 0;
        new_node.radius = 0;
        treeToCreate->nodes.push_back(new_node);
        bufferImage->SetPixel(seedToBegin, 0);

        while(bifur_node_index_stack.size() > 0)
        {
            if(bifur_node_index_stack.back().n >= 27)
            {
                //This is the last node in branch with no more connections to verify
                bifur_node_index_stack.pop_back();
                if(new_branch.nodeIndex.size() > 0)
                {
                    if(new_branch.nodeIndex.size() > 1 || treeToCreate->nodes[new_branch.nodeIndex.back()].connections < 1)
                    {
                        treeToCreate->branches.push_back(new_branch);
                        new_branch.nodeIndex.clear();
                    }
                    else new_branch.nodeIndex.clear();
                }
            }
            else
            {
                new_node_index = bifur_node_index_stack.back().index;
                new_node = treeToCreate->nodes[new_node_index];
                region_index[0] = new_node.x;
                region_index[1] = new_node.y;
                region_index[2] = new_node.z;
                region.SetIndex(region_index);

                if(new_branch.nodeIndex.size() <= 0)
                {
                    new_branch.nodeIndex.push_back(new_node_index);
                }

                itk::NeighborhoodIterator<ImageType> iterator(radius, bufferImage, region);
                for(; bifur_node_index_stack.back().n < 27; bifur_node_index_stack.back().n++)
                {
                    const int* nss = neighborhoodScanSequence[bifur_node_index_stack.back().n];
                    int offset = sc+nss[0]+nss[1]*sy+nss[2]*sz;

                    if(iterator.GetPixel(offset) > 0)
                    {
                        // Set voxel to zero
                        bufferImage->SetPixel(iterator.GetIndex(offset), 0);
                        // Next node to add to branch was found
                        typename ImageType::IndexType image_index = iterator.GetIndex(offset);
                        new_node.x = image_index[0];
                        new_node.y = image_index[1];
                        new_node.z = image_index[2];
                        new_node.radius = 0;
                        new_node.connections = 1;
                        treeToCreate->nodes[new_branch.nodeIndex.back()].connections++;
                        new_node_index = treeToCreate->nodes.size();
                        treeToCreate->nodes.push_back(new_node);
                        stack_element.n = 1;
                        stack_element.index = new_node_index;
                        bifur_node_index_stack.push_back(stack_element);
                        new_branch.nodeIndex.push_back(new_node_index);
                        break;
                    }
                }
            }
        }
    }



    static TreeStructure skeletonToTreeIntSpace(typename ImageType::Pointer imageBuffer, typename ImageType::IndexType* seedLocation = NULL)
    {
        unsigned int iimax, ii;
        std::vector<typename ImageType::IndexType> seedlist;
        findAllNodes(imageBuffer, &seedlist);

        // We force the tree will grow from only one seed.
        // Result must be single-connected.
        // Unconnected branches will be disregarded.
        if(seedLocation != NULL)
        {
            iimax = seedlist.size();
            double distance = std::numeric_limits<double>::max();
            unsigned int indseed = 0;
            for(ii = 0; ii < iimax; ii++)
            {
                double x = (*seedLocation)[0] - seedlist[ii][0];
                double y = (*seedLocation)[1] - seedlist[ii][1];
                double z = (*seedLocation)[2] - seedlist[ii][2];
                double d = x*x + y*y + z*z;
                if(d < distance)
                {
                    distance = d;
                    indseed = ii;
                }
            }
            typename ImageType::IndexType seed = seedlist[indseed];
            seedlist.clear();
            seedlist.push_back(seed);
        }

        // Building trees.
        // For loop to create disconnected trees.
        TreeStructure tree_struct;
        iimax = seedlist.size();
        for(ii = 0; ii < iimax; ++ii)
        {
            if(imageBuffer->GetPixel(seedlist[ii]) > 0)
            {
                // If seed has not been used for building tree it is used for building a new one.
                skeletonToTreeIntSpace(imageBuffer, &tree_struct, seedlist[ii]);
            }
        }
        return tree_struct;
    }


///////////////
    static void treeIntSpaceToDistance(typename ImageType::Pointer imageBuffer, TreeStructure* tree_struct)
    {
        unsigned int iimax, ii;
        // Computation of tree nodes' coordinates in image (metric) space
        iimax = tree_struct->nodes.size();
        typename ImageType::SpacingType spacing = imageBuffer->GetSpacing();
        for(ii = 0; ii<iimax; ii++)
        {
            tree_struct->nodes[ii].x *= spacing[0];
            tree_struct->nodes[ii].y *= spacing[1];
            tree_struct->nodes[ii].z *= spacing[2];
            tree_struct->nodes[ii].radius = 0;
        }
    }



//pms_nist
    /**
     * @brief skeletonToTree analizuje obraz rastrowy ze szkieletem i buduje na jego podstawie drzewo
     * @param image wejściowy obraz rastrowy, woksele szkieletu muszą być oznaczone liczbami większymi od zera pozostałe muszą być wyzeorwane
     * @return zwrace obiekt klasy drzewa ze współrzędnymi rzeczywistymi
     */
    static Tree skeletonToTree(typename ImageType::Pointer imageBuffer, typename ImageType::IndexType* seedLocation = NULL)
    {
        TreeStructure tree_struct = skeletonToTreeIntSpace(imageBuffer, seedLocation);
        treeIntSpaceToDistance(imageBuffer, &tree_struct);
        Tree tree(tree_struct);
        return tree;
    }




/*
    //=================================================
    //=================================================
    //=================================================
    //=================================================
    //=================================================
    //=================================================
    static void findSeedsForTree(typename ImageType::Pointer bufferImage, std::vector<typename ImageType::IndexType>* seedlist)
    {
        typedef itk::ConstantBoundaryCondition< ImageType > BoundaryConditionType;
        typedef itk::NeighborhoodIterator< ImageType, BoundaryConditionType> NeighborhoodIteratorType;
        typename NeighborhoodIteratorType::RadiusType radius;
        radius[0]=1; radius[1]=1; radius[2]=1;
        typename ImageType::IndexType regionIndex;
        regionIndex[0]=0;regionIndex[1]=0;regionIndex[2]=0;
        typename ImageType::SizeType regionSize = bufferImage->GetRequestedRegion().GetSize();
        typename ImageType::RegionType region;
        region.SetSize(regionSize);
        region.SetIndex(regionIndex);
        itk::NeighborhoodIterator<ImageType> iteratorStart(radius, bufferImage, region);

        int sc = iteratorStart.Size() / 2;
        int sy = iteratorStart.GetStride(1);
        int sz = iteratorStart.GetStride(2);

        for(iteratorStart.GoToBegin(); !iteratorStart.IsAtEnd(); ++iteratorStart)
        {
            typename ImageType::IndexType sni;
            int neighbors = 0;
            unsigned int i = 1;

            typename ImageType::PixelType pixv = iteratorStart.GetCenterPixel();

            if(pixv <= 0) continue;

            iteratorStart.SetCenterPixel(2);

            for(; i < 7; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1)
            {
                sni = iteratorStart.GetIndex();
                seedlist->push_back(sni);
                continue;
            }
            neighbors = 0;
            for(; i < 19; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1)
            {
                sni = iteratorStart.GetIndex();
                seedlist->push_back(sni);
                continue;
            }
            neighbors = 0;
            for(; i < 27; i++)
            {
                const int* nss = neighborhoodScanSequence[i];
                if(iteratorStart.GetPixel(sc+nss[0]+nss[1]*sy+nss[2]*sz) > 0) neighbors++;
            }
            if(neighbors == 1)
            {
                sni = iteratorStart.GetIndex();
                seedlist->push_back(sni);
                continue;
            }
        }
    }

    static void skeletonToTreeIntSpace(typename ImageType::Pointer bufferImage, Tree* treeToCreate,
                                       std::vector<typename ImageType::IndexType>* seedlist,
                                       typename ImageType::IndexType seedLocation)
    {
        typedef itk::ConstantBoundaryCondition< ImageType > BoundaryConditionType;
        typedef itk::NeighborhoodIterator< ImageType, BoundaryConditionType> NeighborhoodIteratorType;
        typename NeighborhoodIteratorType::RadiusType radius;
        radius[0]=1; radius[1]=1; radius[2]=1;
        typename ImageType::IndexType regionIndex;
        regionIndex[0]=0;regionIndex[1]=0;regionIndex[2]=0;
        typename ImageType::SizeType regionSize = bufferImage->GetRequestedRegion().GetSize();
        typename ImageType::RegionType region;
        region.SetSize(regionSize);
        region.SetIndex(regionIndex);
        itk::NeighborhoodIterator<ImageType> iteratorStart(radius, bufferImage, region);

        int sc = iteratorStart.Size() / 2;
        int sy = iteratorStart.GetStride(1);
        int sz = iteratorStart.GetStride(2);

        double distance = 100000000000000000;
        unsigned int seednow = -1;
        unsigned int ssmax = seedlist->size();
        for(unsigned int ss = 0; ss < ssmax; ++ss)
        {
            typename ImageType::IndexType sni;
            sni = (*seedlist)[ss];
            //std::numeric_limits<double>::quiet_NaN();
            //if(!(sni[0] == sni[0])) continue;
            if(sni[0] < 0 || sni[0] > regionSize[0]) continue;

            if(bufferImage->GetPixel(sni) <= 0)
            {
                (*seedlist)[ss][0] = -1;
                continue;
            }

//pms_nist
            printf("Using seed %i: %i, %i, %i\n", ss, (*seedlist)[ss][0], (*seedlist)[ss][1], (*seedlist)[ss][2]);

            double x = sni[0] - seedLocation[0];
            double y = sni[1] - seedLocation[1];
            double z = sni[2] - seedLocation[2];
            double d = x*x+y*y+z*z;
            if(d < distance)
            {
                distance = d;
                seednow = ss;
            }
        }
        //typename ImageType::IndexType startNodeIndex = iteratorStart.GetIndex();
        if(distance >= 10000000000000000) return;

        typename ImageType::IndexType startNodeIndex = (*seedlist)[seednow];
        (*seedlist)[seednow][0] = -1;

        std::vector<NodeIn3D> newBranch;
        std::vector<Index3DNeighbor> bifurStack;
        NodeIn3D newNode;
        Index3DNeighbor newNodeIndex;
        newNodeIndex.i = startNodeIndex;
        newNodeIndex.n = 1;
        newNode.connections = 0;
        newNode.radius = 0;
        bifurStack.push_back(newNodeIndex);
        bufferImage->SetPixel(newNodeIndex.i, 0);  ////////////

        while(bifurStack.size() > 0)
        {
            if(bifurStack.back().n >= 27)
            {
                bifurStack.pop_back();
                unsigned int nbs = newBranch.size();
                if(nbs > 0)
                {
                    unsigned int n;
                    typename ImageType::IndexType pindex;
                    typename ImageType::IndexType pr;

                    pindex[0] = newBranch[nbs-1].x;
                    pindex[1] = newBranch[nbs-1].y;
                    pindex[2] = newBranch[nbs-1].z;

                    for(n = 0; n < 27; n++)
                    {
                        const int* nss = neighborhoodScanSequence[n];
                        pr[0] = pindex[0] + nss[0];
                        pr[1] = pindex[1] + nss[1];
                        pr[2] = pindex[2] + nss[2];

                        if(pr[0] < 0 && pr[0] >= regionSize[0]) continue;
                        if(pr[1] < 0 && pr[1] >= regionSize[1]) continue;
                        if(pr[2] < 0 && pr[2] >= regionSize[2]) continue;

                        if(bufferImage->GetPixel(pr) > 0)
                        {
                            newNode.x = pr[0];
                            newNode.y = pr[1];
                            newNode.z = pr[2];
                            newBranch.push_back(newNode);
                            break;
                        }
                    }

                    for(n = 0; n < nbs; n++)
                    {
                        pindex[0] = newBranch[n].x;
                        pindex[1] = newBranch[n].y;
                        pindex[2] = newBranch[n].z;
                        bufferImage->SetPixel(pindex, 1);
                    }

                    treeToCreate->addBranch(newBranch, false);
                    newBranch.clear();
                }
            }
            else
            {
                regionIndex = bifurStack.back().i;
                region.SetIndex(regionIndex);
                itk::NeighborhoodIterator<ImageType> iteratorN(radius, bufferImage, region);

                for(; bifurStack.back().n < 27; bifurStack.back().n++)
                {
                    const int* nss = neighborhoodScanSequence[bifurStack.back().n];
                    int offset = sc+nss[0]+nss[1]*sy+nss[2]*sz;
                    if(iteratorN.GetPixel(offset) > 1)
                    {
                        if(newBranch.size() <= 0)
                        {
                            newNodeIndex.i = iteratorN.GetIndex(sc);
                            newNode.x = newNodeIndex.i[0];
                            newNode.y = newNodeIndex.i[1];
                            newNode.z = newNodeIndex.i[2];
                            newBranch.push_back(newNode);
    //                        std::cout << newNodeIndex.i << std::endl;
                        }

                        newNodeIndex.i = iteratorN.GetIndex(offset);
                        newNodeIndex.n = 1;
                        bifurStack.push_back(newNodeIndex);

                        typename ImageType::IndexType pindex = iteratorN.GetIndex(offset);
                        bufferImage->SetPixel(pindex, 0);  //////////////
                        newNode.x = pindex[0];
                        newNode.y = pindex[1];
                        newNode.z = pindex[2];
                        newBranch.push_back(newNode);
    //                    std::cout << newNodeIndex.i << std::endl;
                        break;
                    }
                }
            }
        }
        treeToCreate->correctConnectivity();
    }



    static Tree skeletonToTree(typename ImageType::Pointer image, typename ImageType::IndexType seedLocation)
    {
        std::vector<typename ImageType::IndexType> seedlist;

        int i, imax, previous;
        typename ImageType::SpacingType spacing;

        imax = image->GetLargestPossibleRegion().GetImageDimension();
        for(i = 0; i<imax; i++)
        {
            spacing[i] = image->GetSpacing()[i];
        }
        Tree tree;

        findSeedsForTree(image, &seedlist);



//pms_nist for debug
        printf("No of seeds = %i\n", seedlist.size());
        for(int ii = 0; ii < seedlist.size(); ii++)
        {
            printf("  %i: %i, %i, %i\n", ii, seedlist[ii][0], seedlist[ii][1], seedlist[ii][2]);
        }




        imax = 0;
        do{
            previous = imax;
            skeletonToTreeIntSpace(image, &tree, &seedlist, seedLocation);
            imax = tree.nodeCount();
        }while(imax > previous);

        for(i = 0; i<imax; i++)
        {
            NodeIn3D n = tree.node(i);
            n.x *= spacing[0];
            n.y *= spacing[1];
            n.z *= spacing[2];
            tree.setNode(n, i);
        }
        return tree;
    }
    //=================================================
    //=================================================
    //=================================================
    //=================================================
    //=================================================
    //=================================================
*/













/*
    static Tree skeletonToTree(typename ImageType::Pointer image)
    {
        int i, imax;
        typename ImageType::SpacingType spacing;

        imax = image->GetLargestPossibleRegion().GetImageDimension();
        for(i = 0; i<imax; i++)
        {
            spacing[i] = image->GetSpacing()[i];
        }
        Tree tree = skeletonToTreeIntSpace(image);
        imax = tree.nodeCount();
        for(i = 0; i<imax; i++)
        {
            NodeIn3D n = tree.node(i);
            n.x *= spacing[0];
            n.y *= spacing[1];
            n.z *= spacing[2];
            tree.setNode(n, i);
        }
        return tree;
    }
*/

    /**
     * @brief estimateDiameters szacuje średnicę gałęzi drzewa w jego węzłach
     * @param tree drzewo
     * @param image binarny obraz rastrowy drzewa
     */
    static void estimateDiameters(Tree* tree, typename ImageType::Pointer image)
    {
        typedef typename itk::ImageRegionConstIterator< ImageType > ConstIteratorType;
        int i, imax;
        imax = tree->nodeCount();
        double* radius = new double[imax];
        for(i = 0; i<imax; i++) radius[i] = std::numeric_limits<double>::max();
        typename ImageType::SpacingType spacing = image->GetSpacing();
        ConstIteratorType inputIterator(image, image->GetRequestedRegion());
        for(inputIterator.GoToBegin(); !inputIterator.IsAtEnd(); ++inputIterator)
        {
            if(inputIterator.Get() <= 0)
            {
                typename ImageType::IndexType coords = inputIterator.GetIndex();
                for(i = 0; i<imax; i++)
                {
                    NodeIn3D n = tree->node(i);
                    double dx, dy, dz, d;
                    dx = (n.x - coords[0]) * spacing[0];
                    dy = (n.y - coords[1]) * spacing[2];
                    dz = (n.z - coords[2]) * spacing[2];
                    d = dx*dx+dy*dy+dz*dz;
                    if(radius[i] > d) radius[i] = d;
                }
            }
        }
        for(i = 0; i<imax; i++)
        {
            NodeIn3D n = tree->node(i);
            if(radius[i] >= std::numeric_limits<double>::max()) n.radius = -1;
            else n.radius = sqrt(radius[i])*2.0;
            tree->setNode(n, i);
        }
        delete[] radius;
    }

};
#endif // BUILDTREE_H
