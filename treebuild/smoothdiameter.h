#ifndef SMOOTHDIAMETER_H
#define SMOOTHDIAMETER_H

#include "tree.h"
#include <vector>

struct ForSmooth1D
{
    float x;
    float dx;
    std::vector<unsigned int> n;
};

class SmoothDiameter
{
public:
    /**
     * @brief SmoothDiameter class to smooth tree branches
     * @param tree input tree
     * @param d1 first derivate minimization parameter
     * @param d2 second derivate minimization parameter
     * @param fa anchore to original location parameter - linear relationship
     * @param faaa anchore to original location parameter - third power relationship
     */
    SmoothDiameter(Tree* tree, float d1, float fa, float faaa);
    Tree getResult(int iterations, int chillout);

private:
    float d1, fa, faaa;
    Tree* tree;
    std::vector<ForSmooth1D> mtree;
    int iteration;

    void initialize(void);
    void compute(int iterations, int chillout);
    Tree getResult(void);

    void step(void);
    inline void computeF(int ni);
    inline void computeD(int ni);
};

#endif // SMOOTHDIAMETER_H
