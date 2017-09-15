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


#ifndef SPHEAR_H
#define SPHEAR_H

const double wsp12[12][3] =
{
  {0.0000000000, 0.0000000000, 1.0000000000},
  {0.0000000000, 0.8944271910, 0.4472135950},
  {0.8506508080, 0.2763932020, 0.4472135950},
  {0.5257311120, -0.7236067970, 0.4472135950},
  {-0.5257311120, -0.7236067970, 0.4472135950},
  {-0.8506508080, 0.2763932020, 0.4472135950},
  {-0.8506508080, -0.2763932020, -0.4472135950},
  {-0.5257311120, 0.7236067970, -0.4472135950},
  {0.5257311120, 0.7236067970, -0.4472135950},
  {0.8506508080, -0.2763932020, -0.4472135950},
  {0.0000000000, -0.8944271910, -0.4472135950},
  {0.0000000000, 0.0000000000, -1.0000000000},
};

const double pol12[12][6] =
{
  {1, 5, 4, 3, 2, -1},
  {0, 2, 8, 7, 5, -1},
  {0, 3, 9, 8, 1, -1},
  {0, 4, 10, 9, 2, -1},
  {0, 5, 6, 10, 3, -1},
  {0, 1, 7, 6, 4, -1},
  {4, 5, 7, 11, 10, -1},
  {1, 8, 11, 6, 5, -1},
  {1, 2, 9, 11, 7, -1},
  {2, 3, 10, 11, 8, -1},
  {3, 4, 6, 11, 9, -1},
  {6, 7, 8, 9, 10, -1},
};

struct Vertex6N
{
  double x;
  double y;
  double z;
  int neighbors[6];
};

class Sphear
{
public:
    Sphear(int divisions = 0);
    ~Sphear();

    void triangles(unsigned int* triangles);
    void edges(unsigned int *edges);
    void vertex(unsigned int index, double V[3]);
    unsigned int trianglesNumber();
    unsigned int edgesNumber();
    unsigned int verticesNumber();

private:
    int verticesNumb;
    Vertex6N *vertices;
    void Create6neighbor(int divisions);
};

#endif // SPHEAR_H
