#include "smoothdiameter.h"

SmoothDiameter::SmoothDiameter(Tree *tree, float d1, float fa, float faaa)
{
    this->tree = tree;
    this->d1 = d1;
    this->fa = fa;
    this->faaa = faaa;
}

void SmoothDiameter::initialize(void)
{
    iteration = 0;

    int nodecount = tree->nodeCount();
    mtree.resize(nodecount);

    for(int i = 0; i < nodecount; i++)
    {
        mtree[i].x = tree->node(i).radius;
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

void SmoothDiameter::compute(int iterations, int chillout)
{
    int i;
    float md1, md2, mfa, mfaaa;
    chillout++;
    md1 = d1 / chillout;
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
        fa -= mfa;
        faaa -= mfaaa;
        step();
    }
}

Tree SmoothDiameter::getResult(void)
{
    Tree tree = *this->tree;
    int nodecount = tree.nodeCount();

    for(int i = 0; i < nodecount; i++)
    {
        tree.setDiameter(i, mtree[i].x);
    }
    return tree;
}

Tree SmoothDiameter::getResult(int iterations, int chillout)
{
    initialize();
    compute(iterations, chillout);
    return getResult();
}


void SmoothDiameter::step(void)
{
    int ni;
    int nsize = mtree.size();
    for(ni = 0; ni < nsize; ni++)
    {
        mtree[ni].dx = 0.0;
        computeF(ni);
        computeD(ni);
    }
    for(ni = 0; ni < nsize; ni++)
    {
        mtree[ni].x += mtree[ni].dx;
    }
}

inline void SmoothDiameter::computeF(int ni)
{
    float ddx = tree->node(ni).radius - mtree[ni].x;
    mtree[ni].dx += (fa*ddx + faaa*ddx*ddx*ddx);
}

inline void SmoothDiameter::computeD(int ni)
{
    int i;
    float ax;
    int count = mtree[ni].n.size();
    if(count > 1)
    {
        ax = 0;
        for(i = 0; i < count; i++)
        {
            int n = mtree[ni].n[i];
            ax += mtree[n].x;
        }
        ax /= count;
        mtree[ni].dx += d1*(ax - mtree[ni].x);
    }
}
