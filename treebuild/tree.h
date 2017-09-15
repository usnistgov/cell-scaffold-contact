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

#ifndef TREE_H
#define TREE_H

#include <vector>
#include <string>
#include <sstream>
#include <iostream>
#include <fstream>
#include <math.h>

/* \struct Coordinates3d
 *  \brief Coordinates3d stores coordinates x, y and z.
 *
 * This is simply a vector of coordinates.
 */
/** \struct Coordinates3d
 *  \brief struktura Coordinates3d przechowuje współrzędne (x, y, z) przestrzeni trójwymiarowej.
 */
struct Coordinates3d
{
    double x;
    double y;
    double z;
};

/* \struct NodeIn3D
 *  \brief NodeIn3D stores node coordinates, number
 *  of neighbors and local diameter of a vessel.
 *
 * This structure derives from the Coordinates3d.
 * It stores coordinates of the node, number of nodes connected to
 * it and diameter of a vessel arround the node.
 */
/** \struct NodeIn3D
 *  \brief struktura NodeIn3D przechowuje współrzędne węzła, liczbę węzłów połączonych i średnicę naczynia.
 *
 * Struktura dziedziczy z Coordinates3d i dodatkowo deklaruje zmienne
 * \param connections do przechowywania liczby połączeń z węzłami sąsiednimi
 * oraz \param diameter określający średnicę naczynia krwionośnego
 * w miejscu położenia węzła.
 */
struct NodeIn3D:Coordinates3d
{
    /** Liczba sąziednich węzłów połączonych*/
    unsigned int connections;
    /** Średnica naczynia krwionośnego w położeniu węzła*/
    float radius;
};

/* \struct BasicBranch
 *  \brief BasicBranch stores indexes of nodes building the branch.
 */
/** \struct BasicBranch
 *  \brief struktura BasicBranch zawiera wektor indeksów węzłów tworzących
 *  gałąź drzewa.
 *
 * Struktura zawiera wektor indeksów węzłów tworzących gałąź drzewa.
 * Współrzędne i inne informacje o węzłach muszą być przechowywane
 * w innym wektorze danych. Przykładem zastosowania jest struktura
 * TreeSkeletonStructure.
 */
struct BasicBranch
{
    std::vector<unsigned int> nodeIndex;
};

/* \struct TreeStructure
 *  \brief TreeStructure stores information on a vessel tree.
 *
 * TreeStructure stores indexes of nodes building individual
 * branches in a vector of BasicBranch structures.
 * The corresponding nodes coordinate, connectivity and diameter are stored
 * in a vector of NodeIn3D structures.
 * User should not operate directly on the TreeStructure.
 * Instead it is preferred to use functions of Tree class
 * to add or remove branches.
 */
/** \struct TreeSkeletonStructure
 *  \brief struktura TreeSkeletonStructure przechowuje informację o budowie drzewa.
 *
 * TreeSkeletonStructure przechowuje informację o budowie drzewa.
 * wszystkie węzły tworzące drzewo przechowywane są w wektorze nodes
 * natomiast to, które z tych węzłów tworzą poszczególne gałęzie
 * zapisywane jest w wektorze branches.
 * Użytkownik nie powinien bezpośrednio korzystać z pól tej struktury.
 * Zamiast tego należy korzystać z klasy TreeSkeleton implementującej
 * odpowiednie funkcje.
 */
struct TreeStructure
{
    /** Lista węzłów*/
    std::vector <NodeIn3D> nodes;
    /** Lista gałęzi*/
    std::vector <BasicBranch> branches;
};

//-------------------------------------------------------------------------------------
/* \classt Tree
 *  \brief Tree derives from TreeStructure.
 *
 * Tree defines functions to safely add and remove branches
 * in Tree structure, query the number of nodes and branches,
 * to save and load data, etc.
 * See example createtree.cpp: @include createtree.cpp
 */
/** \class TreeSkeleton
 *  \brief TreeSkeleton dziedziczy z TreeSkeletonStructure i implementuje
 *  funkcje dostępu.
 *
 * TreeSkeleton definiuje funkcje do bezpiecznego korzystania ze struktury
 * TreeSkeletonStructure. Między innymi są to funkcje dodawania i usuwania
 * gałęzi, zapisu do pliku i odczytu z pliku.
 *
 * \author Piotr M. Szczypiński
 *
 * Przykład createtree.cpp: \include createtree.cpp
 */
class Tree:TreeStructure
{
public:
    Tree();

//pms_nist
    Tree(TreeStructure tree);

    bool save(const char *fileName, unsigned int format);
    bool load(const char *fileName);

    unsigned int count(void);
    int count(unsigned int ib);
    unsigned int nodeCount(void);

    std::vector<NodeIn3D> branch(unsigned int ib);
    NodeIn3D node(unsigned int ib, unsigned int in);
    unsigned int nodeIndex(unsigned int ib, unsigned int in);

    NodeIn3D node(unsigned int i);

    bool setNode(NodeIn3D node, unsigned int i);
    bool setNode(unsigned int i, double x, double y, double z);
    bool setDiameter(unsigned int i, double d);

    bool addBranch(std::vector<NodeIn3D> branch, bool correct = true);
    //bool addBranchLoops(std::vector<NodeIn3D> branch);
//    void cutBranches(void);
    //bool expandBranch(unsigned int ib, unsigned int in, std::vector<NodeIn3D> branch);

    bool removeBranch(unsigned int ib, bool correct = true);
    bool removeNode(unsigned int in);
    bool splitNode(unsigned int in);
    bool splitBranch(unsigned int ib, unsigned int in);
    int disconnectedNumber(int *branch_membership = NULL);
    bool correctConnectivity(double epsilon = 1e-6);
    bool rebuildTreeLongBranches(void);
    bool rebuildTreeShortBranches(void);
};

#endif // TREE_H
