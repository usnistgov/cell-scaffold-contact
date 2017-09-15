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

#include "tree.h"

/**
 * @brief Tree::Tree constructor
 * @param distance is a distance at which two nodes are combined into one
 */
Tree::Tree()
{
}

//pms_nist
Tree::Tree(TreeStructure tree)
{
    this->nodes = tree.nodes;
    this->branches = tree.branches;
}

/**
 * @brief Tree::correctConnectivity removes unused nodes, empty branches, and connects branches in no bifurcation points
 * @return true on success and false otherwise
 */
bool Tree::correctConnectivity(double epsilon)
{
    unsigned int bc = branches.size();
    if(bc == 0) return false;
    unsigned int nc = nodes.size();
    if(nc == 0) return false;

    for(unsigned int n = 0; n < nc; n++) nodes[n].connections = 0;

// Remove junk branches and recompute nodes connectivity
    for(unsigned int b = 0; b < bc; b++)
    {
        unsigned int bnc = branches[b].nodeIndex.size();
        if(bnc < 1)
        {
            branches.erase(branches.begin()+b);
            bc = branches.size();
            b--;
            continue;
        }
        if(bnc > 0) nodes[branches[b].nodeIndex[0]].connections ++;
        if(bnc > 1) nodes[branches[b].nodeIndex[bnc-1]].connections ++;
        for(unsigned int n = 1; n < bnc-1; n++)
        {
            nodes[branches[b].nodeIndex[n]].connections += 2;
        }
    }

// Remove junk nodes which do not belong to any branch
    for(unsigned int n = 0; n < nc; n++)
    {
        if(nodes[n].connections == 0)
        {
            nodes.erase(nodes.begin()+n);
            nc = nodes.size();
            for(unsigned int b = 0; b < bc; b++)
            {
                unsigned int bnc = branches[b].nodeIndex.size();
                for(unsigned int nn = 0; nn < bnc; nn++)
                {
                    if(branches[b].nodeIndex[nn] > n) branches[b].nodeIndex[nn]--;
                }
            }
            n--;
            continue;
        }
    }
    if(bc == 0) return false;
    if(nc == 0) return false;


// Remove nodes which are repeated or closer than epsilon

        for(unsigned int n = 0; n < nc-1; n++)
        {
            for(unsigned int nn = n+1; nn < nc; nn++)
            {
                if((fabs(nodes[n].x-nodes[nn].x) <= epsilon) &&
                   (fabs(nodes[n].y-nodes[nn].y) <= epsilon) &&
                   (fabs(nodes[n].z-nodes[nn].z) <= epsilon))
                {
                    nodes[n].connections += nodes[nn].connections;
                    nodes.erase(nodes.begin() + nn);
                    nc--;
                    for(unsigned int b = 0; b < bc; b++)
                    {
                        unsigned int bnc = branches[b].nodeIndex.size();
                        for(unsigned int nb = 0; nb < bnc; nb++)
                        {
                            if(branches[b].nodeIndex[nb] == nn) branches[b].nodeIndex[nb] = n;
                            else if(branches[b].nodeIndex[nb] > nn) branches[b].nodeIndex[nb]--;
                        }
                    }
                }
            }
        }
        if(nc == 0) return false;


// Remove repeated nodes
        for(unsigned int b = 0; b < bc; b++)
        {
            unsigned int bnc = branches[b].nodeIndex.size();
            for(unsigned int nb = 0; nb < bnc-1; nb++)
            {
                while(branches[b].nodeIndex[nb] == branches[b].nodeIndex[nb+1] && nb < bnc-2)
                {
                    branches[b].nodeIndex.erase(branches[b].nodeIndex.begin()+nb+1);
                    bnc--;
                }
            }
        }


// Unite branches in fake bifurcation
    for(unsigned int b = 0; b < bc; b++)
    {
        unsigned int bnc = branches[b].nodeIndex.size();
        unsigned int nodeind = branches[b].nodeIndex[0];
        if(nodes[nodeind].connections == 2)
        {
            for(unsigned int bb = b+1; bb < bc; bb++)
            {
                unsigned int bbnc = branches[bb].nodeIndex.size();
                if(branches[bb].nodeIndex[0] == nodeind)
                {
                    std::vector<unsigned int> branch;
                    unsigned int lnb = bbnc + bnc - 1;
                    branch.resize(lnb);
                    unsigned int q;
                    for(q = 0; q < bnc; q++)
                        branch[q] = branches[b].nodeIndex[bnc - q - 1];
                    for(; q < lnb; q++)
                        branch[q] = branches[bb].nodeIndex[q - bnc + 1];

                    branches[b].nodeIndex = branch;
                    branches.erase(branches.begin()+bb);
                    b--; bc--; bb = bc;
                    continue;
                }
                if(branches[bb].nodeIndex[bbnc-1] == nodeind)
                {
                    std::vector<unsigned int> branch;
                    unsigned int lnb = bbnc + bnc - 1;
                    branch.resize(lnb);
                    unsigned int q;
                    for(q = 0; q < bnc; q++)
                        branch[q] = branches[b].nodeIndex[bnc - q - 1];
                    for(; q < lnb; q++)
                        branch[q] = branches[bb].nodeIndex[lnb - q - 1];

                    branches[b].nodeIndex = branch;
                    branches.erase(branches.begin()+bb);
                    b--; bc--; bb = bc;
                    continue;
                }
            }
        }
        else
        {
            nodeind = branches[b].nodeIndex[bnc-1];
            if(nodes[nodeind].connections == 2)
            {
                for(unsigned int bb = b+1; bb < bc; bb++)
                {
                    unsigned int bbnc = branches[bb].nodeIndex.size();
                    if(branches[bb].nodeIndex[0] == nodeind)
                    {
                        std::vector<unsigned int> branch;
                        unsigned int lnb = bbnc + bnc - 1;
                        branch.resize(lnb);
                        unsigned int q;
                        for(q = 0; q < bnc; q++)
                            branch[q] = branches[b].nodeIndex[q];
                        for(; q < lnb; q++)
                            branch[q] = branches[bb].nodeIndex[q - bnc + 1];

                        branches[b].nodeIndex = branch;
                        branches.erase(branches.begin()+bb);
                        b--; bc--; bb = bc;
                        continue;
                    }
                    if(branches[bb].nodeIndex[bbnc-1] == nodeind)
                    {
                        std::vector<unsigned int> branch;
                        unsigned int lnb = bbnc + bnc - 1;
                        branch.resize(lnb);
                        unsigned int q;
                        for(q = 0; q < bnc; q++)
                            branch[q] = branches[b].nodeIndex[q];
                        for(; q < lnb; q++)
                            branch[q] = branches[bb].nodeIndex[lnb - q - 1];

                        branches[b].nodeIndex = branch;
                        branches.erase(branches.begin()+bb);
                        b--; bc--; bb = bc;
                        continue;
                    }
                }
            }
        }
    }
    if(bc == 0) return false;

//pms_nist
//    save("test__cc_.txt", 0);

    return true;
}

/**
 * @brief Tree::rebuildTreeShortBranches converts tree internal structure to have short branches that cannot have bifurcations in the midle
 * @return true on success and false otherwise
 */
bool Tree::rebuildTreeShortBranches(void)
{
    if(! correctConnectivity()) return false;
    unsigned int bc = branches.size();
    if(bc == 0) return false;
    unsigned int nc = nodes.size();
    if(nc == 0) return false;

    for(unsigned int b = 0; b < bc; b++)
    {
        unsigned int bnc = branches[b].nodeIndex.size();

        for(unsigned int q = 1; q < bnc-1; q++)
        {
            if(nodes[branches[b].nodeIndex[q]].connections > 2)
            {
                std::vector<unsigned int> newbranch;
                newbranch.resize(branches[b].nodeIndex.size() - q);
                for(unsigned int qq = q; qq < bnc; qq++)
                {
                    newbranch[qq - q] = branches[b].nodeIndex[qq];
                }
                branches[b].nodeIndex.resize(q+1);
                BasicBranch nnewbranch;
                nnewbranch.nodeIndex = newbranch;
                branches.push_back(nnewbranch);
                bc++;
                break;
            }
        }
    }

    return true;
}

/**
 * @brief Tree::rebuildTreeLongBranches converts tree internal structure to have long branches that can have bifurcations in the midle
 * @return true on success and false otherwise
 */
bool Tree::rebuildTreeLongBranches(void)
{
    if(! correctConnectivity()) return false;
    unsigned int bc = branches.size();
    if(bc == 0) return false;
    unsigned int nc = nodes.size();
    if(nc == 0) return false;

    std::vector <BasicBranch> newbranches;
    do
    {
//        printf("NewTree bc = %i\n", bc);

    // Find highest radius node to start
        double maxradi = 0;
        unsigned int firstindex = (unsigned int)-1;

        for(unsigned int b = 0; b < bc; b++)
        {
            unsigned int n;
            unsigned int bnc = branches[b].nodeIndex.size();
            if(bnc < 1) return false;
            n = branches[b].nodeIndex[0];

//            printf("           = %i %f\n", n, (float)nodes[n].radius);

            if(nodes[n].connections == 1)
            if(maxradi < nodes[n].radius || firstindex == (unsigned int)-1)
            {
                maxradi = nodes[n].radius;
                firstindex = n;
            }
            n = branches[b].nodeIndex[bnc-1];

//            printf("           = %i %f\n", n, (float)nodes[n].radius);

            if(nodes[n].connections == 1)
            if(maxradi < nodes[n].radius || firstindex == (unsigned int)-1)
            {
                maxradi = nodes[n].radius;
                firstindex = n;
            }
        }
        if(firstindex == (unsigned int)-1)
        {
            firstindex = branches[0].nodeIndex[0];
            maxradi = nodes[firstindex].radius;
        }
        unsigned int follow_branch = 0;
        unsigned int follow_node = 0;
        //unsigned int follow_added = 0;


//        printf("firstindex = %i\n", firstindex);
//        fflush(stdout);

        std::vector<unsigned int> newbranch;
        do
        {
            //follow_added = 0;
            newbranch.clear();

            unsigned int firstbranch = (unsigned int)-1;
            double direction[3];
            direction[0] = 0.0;
            direction[1] = 0.0;
            direction[2] = 0.0;
            double dotproduct;

//            printf("NewBranch start = %i\n", firstindex);
//            fflush(stdout);

            do
            {
        // Find branch to append
                dotproduct = 2.0;
                firstbranch = (unsigned int)-1;

//                printf("FindBranchIndex %i\n", firstindex);
//                fflush(stdout);

                for(unsigned int b = 0; b < bc; b++)
                {
                    unsigned int bnc = branches[b].nodeIndex.size();
                    if(bnc < 1) return false;
                    if(branches[b].nodeIndex[0] == firstindex)
                    {
                        if(bnc == 1)
                        {
                           firstbranch = b << 1;
                           dotproduct = -2.0;
                           break;
                        }
                        double dir[3];
                        dir[0] = nodes[branches[b].nodeIndex[0]].x;
                        dir[1] = nodes[branches[b].nodeIndex[0]].y;
                        dir[2] = nodes[branches[b].nodeIndex[0]].z;
                        dir[0] -= nodes[branches[b].nodeIndex[1]].x;
                        dir[1] -= nodes[branches[b].nodeIndex[1]].y;
                        dir[2] -= nodes[branches[b].nodeIndex[1]].z;
                        double dot = sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
                        if(dot > 0.0)
                        {
                            dir[0] /= dot;
                            dir[1] /= dot;
                            dir[2] /= dot;
                        }
                        dot = dir[0]*direction[0]+dir[1]*direction[1]+dir[2]*direction[2];
                        if(dotproduct > dot)
                        {
                            dotproduct = dot;
                            firstbranch = b << 1;
                        }
                    }
                    else if(branches[b].nodeIndex[bnc-1] == firstindex)
                    {
                        if(bnc == 1)
                        {
                           firstbranch = (b << 1) + 1;
                           dotproduct = -1.0;
                           break;
                        }
                        double dir[3];
                        dir[0] = nodes[branches[b].nodeIndex[bnc-1]].x;
                        dir[1] = nodes[branches[b].nodeIndex[bnc-1]].y;
                        dir[2] = nodes[branches[b].nodeIndex[bnc-1]].z;
                        dir[0] -= nodes[branches[b].nodeIndex[bnc-2]].x;
                        dir[1] -= nodes[branches[b].nodeIndex[bnc-2]].y;
                        dir[2] -= nodes[branches[b].nodeIndex[bnc-2]].z;
                        double dot = sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
                        if(dot > 0.0)
                        {
                            dir[0] /= dot;
                            dir[1] /= dot;
                            dir[2] /= dot;
                        }
                        dot = dir[0]*direction[0]+dir[1]*direction[1]+dir[2]*direction[2];
                        if(dotproduct > dot)
                        {
                            dotproduct = dot;
                            firstbranch = (b << 1) + 1;
                        }
                    }
                }

        // Add branches and move to newbranches
                if(firstbranch != (unsigned int)-1)
                {
                    unsigned int bnc = branches[firstbranch>>1].nodeIndex.size();
                    unsigned int prev_nbnc = newbranch.size();
                    if(prev_nbnc == 0)
                    {
                        newbranch.resize(bnc);
                        newbranch[0] = firstindex;
                    }
                    else
                    {
                        prev_nbnc--;
                        newbranch.resize(prev_nbnc + bnc);
                    }

                    unsigned int q;
                    if(firstbranch & 1)
                    {
                        for(q = 1; q < bnc; q++)
                            newbranch[prev_nbnc + q] = branches[firstbranch>>1].nodeIndex[bnc - q - 1];
                    }
                    else
                    {
                        for(q = 1; q < bnc; q++)
                            newbranch[prev_nbnc + q] = branches[firstbranch>>1].nodeIndex[q];
                    }
                    branches.erase(branches.begin() + (firstbranch>>1));
                    bc--;
                    //follow_added++;
                    firstindex = newbranch[prev_nbnc + bnc - 1];



//                    printf("Appended: ");
//                    for(int pr = 0; pr < newbranch.size(); pr++)
//                        printf("%i ", newbranch[pr]);
//                    printf("\n");
//                    fflush(stdout);
                }


//                printf("EndWhileBranch\n");
//                fflush(stdout);


            }
            while(firstbranch != (unsigned int)-1);



            if(newbranch.size() > 0)
            {
                BasicBranch nnewbranch;
                nnewbranch.nodeIndex = newbranch;
                newbranches.push_back(nnewbranch);




//                printf("Complete: ");
//                for(int pr = 0; pr < newbranch.size(); pr++)
//                    printf("%i ", newbranch[pr]);
//                printf("\n");
//                fflush(stdout);


                newbranch.clear();
            }














// Next bifurcation
            //if(follow_added == 0)


            firstindex = (unsigned int)-1;



            for(; follow_branch < newbranches.size(); follow_branch++)
            {
                unsigned int nbnc = newbranches[follow_branch].nodeIndex.size();
                for(; follow_node < nbnc; follow_node++)
                {
                    if(nodes[newbranches[follow_branch].nodeIndex[follow_node]].connections > 2)
                    {
                        firstindex = newbranches[follow_branch].nodeIndex[follow_node];

//                        printf("----b = %i n = %i\n", follow_branch, follow_node);
//                        fflush(stdout);
                        break;
                    }
                }
                if(firstindex != (unsigned int)-1)
                {
                    follow_node++;
                    break;
                }
                follow_node = 0;
            }
        }
        while(firstindex != (unsigned int)-1);
        bc = branches.size();
    }
    while(bc > 0);

    branches = newbranches;

//    save("test___.txt", 0);

    return true;
}

/**
 * @brief Tree::disconnectedNumber checks for number of separate branch sets, or a number of separate trees
 * @param branch_membership vector to store branch membership to a tree
 * @return true on success and false otherwise
 */
int Tree::disconnectedNumber(int* branch_membership)
{
//    /** Lista węzłów*/
//    std::vector <NodeIn3D> nodes;
//    double x;
//    double y;
//    double z;
//    unsigned int connections;
//    float diameter;


//    /** Lista gałęzi*/
//    std::vector <BasicBranch> branches;
//    std::vector<unsigned int> nodeIndex;

    unsigned int disconnected = 0;
    unsigned int i;
    unsigned int* node_label = new unsigned int[nodes.size()];
    for(i = 0; i < nodes.size(); i++)
    {
        node_label[i] = 0;
    }

    unsigned int* dis_label = new unsigned int[branches.size()];
    for(i = 0; i < branches.size(); i++)
    {
        dis_label[i] = 0;
    }
    for(i = 0; i < branches.size(); i++)
    {
        unsigned int a1 = branches[i].nodeIndex[0];
        unsigned int a2 = branches[i].nodeIndex.back();
        if(node_label[a1] == 0 && node_label[a2] == 0)
        {
            node_label[a1] = i+1;
            node_label[a2] = i+1;
        }
        else if(node_label[a1] == 0)
        {
            node_label[a1] = node_label[a2];
        }
        else if(node_label[a2] == 0)
        {
            node_label[a2] = node_label[a1];
        }
        else if(node_label[a2] != node_label[a1])
        {
            unsigned int b1 = node_label[a1];
            unsigned int b2 = node_label[a2];
            for(unsigned int ii = 0; ii < nodes.size(); ii++)
            {
                if(node_label[ii] == b1) node_label[ii] = b2;
            }
        }
    }
    for(i = 0; i < nodes.size(); i++)
    {
        if(node_label[i] > 0) dis_label[node_label[i]-1]++;
    }
    for(i = 0; i < branches.size(); i++)
    {
        if(dis_label[i] > 0) disconnected++;
    }

    if(branch_membership != NULL)
    {
        for(i = 0; i < branches.size(); i++)
        {
            int a1 = branches[i].nodeIndex[0];
            branch_membership[i] = -(int)(node_label[a1]+1);
        }

        bool again = true;
        int labeln = 0;
        while(again)
        {
            int labelo;
            again = false;
            for(i = 0; i < branches.size(); i++)
            {
                if(branch_membership[i] < 0)
                {
                    labelo = branch_membership[i];
                    branch_membership[i] = labeln;
                    again = true;
                    break;
                }
            }
            for(; i < branches.size(); i++)
            {
                if(branch_membership[i] == labelo)
                {
                    branch_membership[i] = labeln;
                }
            }
            labeln++;
        }
    }

    delete[] dis_label;
    delete[] node_label;
    return disconnected;
}



/**
 * @brief Tree::save saves tree to a text fole
 * @param fileName name of a file to create and save
 * @param format file format
 * @return true on success and false otherwise
 */
bool Tree::save(const char *fileName, unsigned int format)
{
    std::ofstream file;
    file.open(fileName);
    if (!file.is_open()) return false;
    if (!file.good()) return false;

    switch(format)
    {
    case 0:
    {
        file << "@TreeSkeleton2014_Internal"<< std::endl;
        file << "@NumberOfAllNodes " << nodes.size() << std::endl;
        for(std::vector<NodeIn3D>::iterator n = nodes.begin(); n != nodes.end(); ++n)
        {
            file << "\t" << n->x << " " << n->y << " " << n->z << " " << n->connections << " "<< n->radius << std::endl;
        }
        file << "@NumberOfBranches " << branches.size() << std::endl;
        for(std::vector<BasicBranch>::iterator b = branches.begin(); b != branches.end(); ++b)
        {
            file << "\t" << b->nodeIndex.size();
            for(std::vector<unsigned int>::iterator n = b->nodeIndex.begin(); n != b->nodeIndex.end(); ++n)
            {
                file << " " << *n;
            }
            file << std::endl;
        }
    } break;

    case 1:
    {
        file << "@TreeSkeleton2014_Simple"<< std::endl;
        file << "@NumberOfBranches " << branches.size() << std::endl;
        for(std::vector<BasicBranch>::iterator b = branches.begin(); b != branches.end(); ++b)
        {
            file << "@NumberOfNodes " << b->nodeIndex.size() << std::endl;
            for(std::vector<unsigned int>::iterator nn = b->nodeIndex.begin(); nn != b->nodeIndex.end(); ++nn)
            {
                NodeIn3D* n = &(nodes[*nn]);
                file << "\t" << n->x << " " << n->y << " " << n->z << " " << n->connections << " "<< n->radius << std::endl;
            }
        }
    } break;
    }
    file.close();
    return true;
    
}

/**
 * @brief Tree::load loads tree from a text file
 * @param fileName name of a file with a tree data
 * @return true on success and false otherwise
 */
bool Tree::load(const char *fileName)
{
    std::ifstream file;
    file.open(fileName);
    if (!file.is_open()) return false;
    if (!file.good()) return false;

    Tree newtree;
    std::string inputstring;
    file >> inputstring;

    if(inputstring == "@TreeSkeleton2014_Internal")
    {
        int NumberOfAllNodes;
        file>>inputstring; if(inputstring != "@NumberOfAllNodes") return false;
        file>>NumberOfAllNodes; if(NumberOfAllNodes <= 0) return false;
        for(int n = 0; n < NumberOfAllNodes; n++)
        {
            NodeIn3D newnode;
            file >> newnode.x >> newnode.y >> newnode.z >> newnode.connections >> newnode.radius;
            newtree.nodes.push_back(newnode);
        }

        int NumberOfBranches;
        file>>inputstring; if(inputstring != "@NumberOfBranches") return false;
        file>>NumberOfBranches; if(NumberOfBranches <= 0) return false;
        for(int b = 0; b < NumberOfBranches; b++)
        {
            BasicBranch newbranch;
            int NumberOfNodes;
            file>>NumberOfNodes; if(NumberOfNodes <= 0)
                return false;
            for(int n = 0; n < NumberOfNodes; n++)
            {
                unsigned int newindex;
                file>>newindex; if(newindex >= NumberOfAllNodes)
                    return false;
                newbranch.nodeIndex.push_back(newindex);
            }
            newtree.branches.push_back(newbranch);
        }
    }
    else if(inputstring == "@TreeSkeleton2014_Simple")
    {
        int NumberOfBranches;
        file>>inputstring; if(inputstring != "@NumberOfBranches")
            return false;
        file>>NumberOfBranches; if(NumberOfBranches <= 0)
            return false;
        for(int b = 0; b < NumberOfBranches; b++)
        {
            std::vector<NodeIn3D> newbranch;
            int NumberOfNodes;
            file>>inputstring; if(inputstring != "@NumberOfNodes")
                return false;
            file>>NumberOfNodes; if(NumberOfNodes <= 0)
                return false;
            for(int n = 0; n < NumberOfNodes; n++)
            {
                NodeIn3D newnode;
                file >> newnode.x >> newnode.y >> newnode.z >> newnode.connections >> newnode.radius;
                newbranch.push_back(newnode);
            }
            newtree.addBranch(newbranch);
        }
    }
    else return false;

    nodes = newtree.nodes;
    branches = newtree.branches;
    file.close();
    return correctConnectivity();
    return true;
}

/**
 * @brief Tree::addBranch
 * @param branch
 * @param correct
 * @return
 */
bool Tree::addBranch(std::vector<NodeIn3D> branch, bool correct)
{
    unsigned int bnc = branch.size();
    unsigned int nc = nodes.size();
    nodes.resize(bnc+nc);
    std::vector<unsigned int> newindex;
    newindex.resize(bnc);
    for(unsigned int n = 0; n < bnc; n++)
    {
        nodes[nc + n] = branch[n];
        newindex[n] = nc + n;
    }
    BasicBranch newBranch;
    newBranch.nodeIndex = newindex;
    branches.push_back(newBranch);

    if(correct) return correctConnectivity();
    return true;
}

/**
 * @brief Tree::removeBranch
 * @param ib
 * @param correct
 * @return
 */
bool Tree::removeBranch(unsigned int ib, bool correct)
{
    unsigned int bc = branches.size();
    if(ib >= bc) return false;
    branches.erase(branches.begin()+ib);
    if(correct) return correctConnectivity();
    return true;
}

/**
 * @brief Tree::removeNode
 * @param in
 * @return
 */
bool Tree::removeNode(unsigned int in)
{
    if(in >= nodes.size()) return false;
    nodes.erase(nodes.begin() + in);
    unsigned int bc = branches.size();
    for(unsigned int b = 0; b < bc; b++)
    {
        unsigned int bnc = branches[b].nodeIndex.size();
        for(unsigned int nb = bnc - 1; nb < bnc; nb--)
        {
            if(branches[b].nodeIndex[nb] > in)
            {
                branches[b].nodeIndex[nb]--;
            }
            else if(branches[b].nodeIndex[nb] == in)
            {
                branches[b].nodeIndex.erase(branches[b].nodeIndex.begin() + nb);
            }
        }
    }
    return true;
}

/**
 * @brief Tree::splitNode
 * @param in
 * @return
 */
bool Tree::splitNode(unsigned int in)
{
    if(in >= nodes.size()) return false;
    unsigned int bc = branches.size();
    for(unsigned int b = 0; b < bc; b++)
    {
        unsigned int bnc = branches[b].nodeIndex.size();
        for(unsigned int nb = 1; nb < bnc-1; nb++)
        {
            if(branches[b].nodeIndex[nb] == in)
            {
                std::vector<unsigned int> newbranch;
                newbranch.resize(bnc - nb);
                for(unsigned int nnb = nb; nnb < bnc; nnb++)
                {
                    newbranch[nnb - nb] = branches[b].nodeIndex[nnb];
                }
                branches[b].nodeIndex.resize(nb + 1);
                BasicBranch nnewbranch;
                nnewbranch.nodeIndex = newbranch;
                branches.push_back(nnewbranch);
            }
        }
    }
    return true;
}

/**
 * @brief Tree::splitBranch
 * @param ib
 * @param in
 * @return
 */
bool Tree::splitBranch(unsigned int ib, unsigned int in)
{
    if(ib >= branches.size()) return false;
    unsigned int bnc = branches[ib].nodeIndex.size();
    if(in >= bnc - 1 || in == 0) return false;

    std::vector<unsigned int> newbranch;
    newbranch.resize(bnc - in);
    for(unsigned int nnb = in; nnb < bnc; nnb++)
    {
        newbranch[nnb - in] = branches[ib].nodeIndex[nnb];
    }
    branches[ib].nodeIndex.resize(in + 1);
    BasicBranch nnewbranch;
    nnewbranch.nodeIndex = newbranch;
    branches.push_back(nnewbranch);

    return true;
}

/**
 * @brief Tree::nodeCount
 * @return
 */
unsigned int Tree::nodeCount(void)
{
    return nodes.size();
}

/**
 * @brief Tree::count
 * @return
 */
unsigned int Tree::count(void)
{
    return branches.size();
}

/**
 * @brief Tree::count
 * @param ib
 * @return
 */
int Tree::count(unsigned int ib)
{
    if(ib >= branches.size()) return -1;
    return branches[ib].nodeIndex.size();
}

/**
 * @brief Tree::branch
 * @param ib
 * @return
 */
std::vector<NodeIn3D> Tree::branch(unsigned int ib)
{
    std::vector<NodeIn3D> r;
    if(ib >= branches.size()) return r;

    for(std::vector<unsigned int>::iterator nn = branches[ib].nodeIndex.begin(); nn != branches[ib].nodeIndex.end(); ++nn)
    {
        r.push_back(nodes[*nn]);
    }
    return r;
}

/**
 * @brief Tree::node
 * @param ib
 * @param in
 * @return
 */
NodeIn3D Tree::node(unsigned int ib, unsigned int in)
{
    NodeIn3D r;
    if(ib >= branches.size()) return r;
    if(in >= branches[ib].nodeIndex.size()) return r;
    if(branches[ib].nodeIndex[in] >= nodes.size()) return r;
    r = nodes[branches[ib].nodeIndex[in]];
    return r;
}

/**
 * @brief Tree::nodeIndex
 * @param ib
 * @param in
 * @return
 */
unsigned int Tree::nodeIndex(unsigned int ib, unsigned int in)
{
    return branches[ib].nodeIndex[in];
}

/**
 * @brief Tree::node
 * @param i
 * @return
 */
NodeIn3D Tree::node(unsigned int i)
{
    NodeIn3D r;
    if(i >= nodes.size()) return r;
    r = nodes[i];
    return r;
}

/**
 * @brief Tree::setNode
 * @param node
 * @param i
 * @return
 */
bool Tree::setNode(NodeIn3D node, unsigned int i)
{
    if(i >= nodes.size()) return false;
    nodes[i] = node;
    return true;
}

/**
 * @brief Tree::setNode
 * @param i
 * @param x
 * @param y
 * @param z
 * @return
 */
bool Tree::setNode(unsigned int i, double x, double y, double z)
{
    if(i >= nodes.size()) return false;
    nodes[i].x = x;
    nodes[i].y = y;
    nodes[i].z = z;
    return true;
}

/**
 * @brief Tree::setDiameter
 * @param i
 * @param d
 * @return
 */
bool Tree::setDiameter(unsigned int i, double d)
{
    if(i >= nodes.size()) return false;
    nodes[i].radius = d;
    return true;
}

/*
bool Tree::addPoint(double x, double y, double z, unsigned int con, unsigned int dia)
{
    NodeIn3D input;
    input.x = x;
    input.y = y;
    input.z = z;
    input.connections = con;
    input.diameter = dia;
    nodes.push_back(input);
    return true;
}
*/
