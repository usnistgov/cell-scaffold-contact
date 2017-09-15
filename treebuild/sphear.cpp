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


#include "sphear.h"
#include <math.h>
#include <stddef.h>


Sphear::Sphear(int divisions)
{
    vertices = NULL;
    Create6neighbor(divisions);
}

Sphear::~Sphear()
{
    if(vertices != NULL) delete[] vertices;
}

void Sphear::Create6neighbor(int divisions)
{
  int w, v, k, l, i, j;
  int ostatni;

  verticesNumb = 12+30*divisions+10*(divisions*divisions-divisions);

//  if(wezly != NULL)  delete[] wezly;
//  wezly = new WEZEL3W[liczbawezlow];
  vertices = new Vertex6N[verticesNumb];

// Przepisanie 12tu punktow z dwudziestoscianu
  for(w=0; w<12; w++)
  {
    vertices[w].x = wsp12[w][0];
    vertices[w].y = wsp12[w][1];
    vertices[w].z = wsp12[w][2];
    vertices[w].neighbors[5] = -1;
    for(k=0; k<5; k++) vertices[w].neighbors[k] = pol12[w][k];
  }
  ostatni = 12;

  if(divisions < 1) return;

// Dodanie punktów na krawedziach
  for(w=0; w<11; w++)
  {
    for(k=0; k<5; k++)
    {
      if(pol12[w][k] > w)
      {
        v = pol12[w][k];
        for(l=0; l<5; l++)
        {
          if(pol12[v][l] == w) break;
        }

        vertices[w].neighbors[k] = ostatni;
        for(j=1; j<=divisions; j++)
        {
          vertices[ostatni].neighbors[0] = ostatni-1;
          vertices[ostatni].neighbors[1] = ostatni+1;
          vertices[ostatni].neighbors[2] = vertices[ostatni].neighbors[3] =
          vertices[ostatni].neighbors[4] = vertices[ostatni].neighbors[5] = -1;

          vertices[ostatni].x =
          (j*vertices[v].x + (divisions+1-j)*vertices[w].x)/(divisions+1);
          vertices[ostatni].y =
          (j*vertices[v].y + (divisions+1-j)*vertices[w].y)/(divisions+1);
          vertices[ostatni].z =
          (j*vertices[v].z + (divisions+1-j)*vertices[w].z)/(divisions+1);

          ostatni++;
        }
        vertices[ostatni-divisions].neighbors[0] = w;
        vertices[ostatni-1].neighbors[1] = v;
        vertices[v].neighbors[l] = ostatni-1;
      }
    }
  }


// Dodanie punktów we wnetrzu trójkata
  for(w=0; w<11; w++)
  {
    for(k=0; k<5; k++)
    {
      if(pol12[w][k] > w)
      {
        v = pol12[w][k];
        if(pol12[w][(k+1)%5] > w)
        {
          int wl, wv, vl;
          l = pol12[w][(k+1)%5];

          if(l < v) {int ll = l; l = v; v = ll; wl = k; wv = (k+1)%5;}
          else {wv = k; wl = (k+1)%5;};

          for(vl=0; vl<5; vl++)
          {
            if(pol12[v][vl] == l) break;
          }

          // Laczenie skrajnych dodanych punktów na krawedziach
          for(i=2; i<6; i++)
            if(vertices[vertices[w].neighbors[wl]].neighbors[i]<0)
              {vertices[vertices[w].neighbors[wl]].neighbors[i] = vertices[w].neighbors[wv]; break;}
          for(i=2; i<6; i++)
            if(vertices[vertices[w].neighbors[wv]].neighbors[i]<0)
              {vertices[vertices[w].neighbors[wv]].neighbors[i] = vertices[w].neighbors[wl]; break;}
          for(i=2; i<6; i++)
            if(vertices[vertices[v].neighbors[vl]].neighbors[i]<0)
              {vertices[vertices[v].neighbors[vl]].neighbors[i] = vertices[w].neighbors[wv]+divisions-1; break;}
          for(i=2; i<6; i++)
            if(vertices[vertices[w].neighbors[wv]+divisions-1].neighbors[i]<0)
              {vertices[vertices[w].neighbors[wv]+divisions-1].neighbors[i] = vertices[v].neighbors[vl]; break;}
          for(i=2; i<6; i++)
            if(vertices[vertices[w].neighbors[wl]+divisions-1].neighbors[i]<0)
              {vertices[vertices[w].neighbors[wl]+divisions-1].neighbors[i] = vertices[v].neighbors[vl]+divisions-1; break;}
          for(i=2; i<6; i++)
            if(vertices[vertices[v].neighbors[vl]+divisions-1].neighbors[i]<0)
              {vertices[vertices[v].neighbors[vl]+divisions-1].neighbors[i] = vertices[w].neighbors[wl]+divisions-1; break;}

          // Dodawanie i laczenie punktów na sciankach
          // ze soba i punktami na krawedziach
          for(int jj=1; jj<divisions; jj++)
          {
            for(j=1; j<=jj; j++)
            {
              if(j<=1)
              {
                vertices[ostatni].neighbors[0] = vertices[w].neighbors[wv]+jj;
                for(i=2; i<6; i++)
                  if(vertices[vertices[w].neighbors[wv]+jj].neighbors[i]<0)
                    {vertices[vertices[w].neighbors[wv]+jj].neighbors[i] = ostatni; break;}
                vertices[ostatni].neighbors[1] = vertices[w].neighbors[wv]+jj-1;
                for(i=2; i<6; i++)
                  if(vertices[vertices[w].neighbors[wv]+jj-1].neighbors[i]<0)
                    {vertices[vertices[w].neighbors[wv]+jj-1].neighbors[i] = ostatni; break;}
              }
              else
              {
                vertices[ostatni].neighbors[0] = ostatni-1;
                vertices[ostatni].neighbors[1] = ostatni-jj;
              }

              if(j>=jj)
              {
                vertices[ostatni].neighbors[2] = vertices[w].neighbors[wl]+jj;
                for(i=2; i<6; i++)
                  if(vertices[vertices[w].neighbors[wl]+jj].neighbors[i]<0)
                    {vertices[vertices[w].neighbors[wl]+jj].neighbors[i] = ostatni; break;}
                vertices[ostatni].neighbors[3] = vertices[w].neighbors[wl]+jj-1;
                for(i=2; i<6; i++)
                  if(vertices[vertices[w].neighbors[wl]+jj-1].neighbors[i]<0)
                    {vertices[vertices[w].neighbors[wl]+jj-1].neighbors[i] = ostatni; break;}
              }
              else
              {
                vertices[ostatni].neighbors[2] = ostatni+1;
                vertices[ostatni].neighbors[3] = ostatni-jj+1;
              }

              if(jj>=divisions-1)
              {
                vertices[ostatni].neighbors[4] = vertices[v].neighbors[vl]+j-1;
                for(i=2; i<6; i++)
                  if(vertices[vertices[v].neighbors[vl]+j-1].neighbors[i]<0)
                    {vertices[vertices[v].neighbors[vl]+j-1].neighbors[i] = ostatni; break;}
                vertices[ostatni].neighbors[5] = vertices[v].neighbors[vl]+j;
                for(i=2; i<6; i++)
                  if(vertices[vertices[v].neighbors[vl]+j].neighbors[i]<0)
                    {vertices[vertices[v].neighbors[vl]+j].neighbors[i] = ostatni; break;}
              }
              else
              {
                vertices[ostatni].neighbors[4] = ostatni+jj;
                vertices[ostatni].neighbors[5] = ostatni+jj+1;
              }
              // Obliczanie wspolrzednych poczatkowych
              vertices[ostatni].x =
                ((jj+1-j)*vertices[vertices[w].neighbors[wv]+jj].x
                + (j)*vertices[vertices[w].neighbors[wl]+jj].x)/(jj+1);
              vertices[ostatni].y =
                ((jj+1-j)*vertices[vertices[w].neighbors[wv]+jj].y
                + (j)*vertices[vertices[w].neighbors[wl]+jj].y)/(jj+1);
              vertices[ostatni].z =
                ((jj+1-j)*vertices[vertices[w].neighbors[wv]+jj].z
                + (j)*vertices[vertices[w].neighbors[wl]+jj].z)/(jj+1);

              ostatni++;
            }
          }
        }
      }
    }
  }

// Sferowanie (normalizacja wektorow [xp,yp,zp])
  for(w = 12; w<verticesNumb; w++)
  {
    double norm;
    norm = vertices[w].x*vertices[w].x
          +vertices[w].y*vertices[w].y
          +vertices[w].z*vertices[w].z;
    norm = sqrt(norm);

    vertices[w].x = vertices[w].x / norm;
    vertices[w].y = vertices[w].y / norm;
    vertices[w].z = vertices[w].z / norm;
  }

// Sortowanie sasiadow w kolejnosci prawoskretnej
  for(w = 0; w<verticesNumb; w++)
  {
    int kl[6];
    double d1, d2, d;
    int km, kp;
    int k1, k2;

    int max = 6;
    if(w < 12) max = 5;
//przepisanie
    for(k = 0; k < max; k++) kl[k] = vertices[w].neighbors[k];

//wylawianie pierwszego
    km = kl[0]; kp = 0;
    for(k = 1; k < max; k++)
      if(kl[k]<km) {km = kl[k]; kp = k;}

//wylawainie dwoch najblizszych do pierwszego
    d1=12; d2=12;
    k1 = k2 = 0;
    for(k = 0; k < max; k++)
    if(k!=kp)
    {
      double dx, dy, dz;
      dx = vertices[kl[k]].x - vertices[kl[kp]].x;
      dy = vertices[kl[k]].y - vertices[kl[kp]].y;
      dz = vertices[kl[k]].z - vertices[kl[kp]].z;
      d = dx*dx + dy*dy + dz*dz;

      if(d1 > d)
      {
        if(d2 > d1)
        {
          d2 = d1;
          k2 = k1;
        }
        d1 = d;
        k1 = k;
      }
      else
      {
        if(d2 > d)
        {
          d2 = d;
          k2 = k;
        }
      }

    }
    // Wyznaczone kolejne wezly sasiednie kp, k1 i k2, km=kl[kp]
    // Wyznaczany kierunek w prawo
    if(0 < ((vertices[w].y-vertices[km].y) * (vertices[w].z-vertices[kl[k1]].z)
           -(vertices[w].z-vertices[km].z) * (vertices[w].y-vertices[kl[k1]].y))
           * vertices[w].x +
           ((vertices[w].z-vertices[km].z) * (vertices[w].x-vertices[kl[k1]].x)
           -(vertices[w].x-vertices[km].x) * (vertices[w].z-vertices[kl[k1]].z))
           * vertices[w].y +
           ((vertices[w].x-vertices[km].x) * (vertices[w].y-vertices[kl[k1]].y)
           -(vertices[w].y-vertices[km].y) * (vertices[w].x-vertices[kl[k1]].x))
           * vertices[w].z)
    {
      vertices[w].neighbors[0] = km; kl[kp] = -1;
      km = kl[k1];
      vertices[w].neighbors[1] = kl[k1]; kl[k1] = -1;
      vertices[w].neighbors[max-1] = kl[k2]; kl[k2] = -1;
    }
    else
    {
      vertices[w].neighbors[0] = km; kl[kp] = -1;
      km = kl[k2];
      vertices[w].neighbors[1] = kl[k2]; kl[k2] = -1;
      vertices[w].neighbors[max-1] = kl[k1]; kl[k1] = -1;
    }

    for(v = 2; v < max-1; v++)
    {
      d1=12;
      for(k = 0; k < max; k++)
      if(kl[k]>=0)
      {
        double dx, dy, dz;
        dx = vertices[kl[k]].x - vertices[km].x;
        dy = vertices[kl[k]].y - vertices[km].y;
        dz = vertices[kl[k]].z - vertices[km].z;
        d = dx*dx + dy*dy + dz*dz;

        if(d1 > d)
        {
          d1 = d;
          k1 = k;
        }
      }
      vertices[w].neighbors[v] = km = kl[k1];
      kl[k1] = -1;
    }
  }
}

unsigned int Sphear::trianglesNumber()
{
    return 2*verticesNumb - 4;
}

unsigned int Sphear::edgesNumber()
{
    return 3*verticesNumb - 6;
}

unsigned int Sphear::verticesNumber()
{
    return verticesNumb;
}

void Sphear::vertex(unsigned int index, double V[3])
{
    Vertex6N v = vertices[index];
    V[0] = v.x;
    V[1] = v.y;
    V[2] = v.z;
}

void Sphear::triangles(unsigned int *triangles)
{
    unsigned int* t = triangles;
    int k;
    bool *lcz;
    lcz = new bool[verticesNumb];
    for(k=0; k<verticesNumb; k++) lcz[k]=true;
    for(k=verticesNumb-1; k>=0; k--)
    {
        int i, j, s;
        lcz[k] = false;
        if(k<12) s=5;
        else s = 6;
        for(i=0; i<s; i++)
        {
            j = (i+1) % s;

            if(lcz[vertices[k].neighbors[i]] && lcz[vertices[k].neighbors[j]])
            {
                *t = k; t++;
                *t = vertices[k].neighbors[j]; t++;
                *t = vertices[k].neighbors[i]; t++;
            }
        }
    }
    delete[] lcz;
}



void Sphear::edges(unsigned int* edges)
{
    unsigned int* t = edges;
    for(int k=verticesNumb-1; k>=0; k--)
    {
        int s;
        if(k<12) s = 5;
        else s = 6;

        for(int i=0; i<s; i++)
        {
            if(vertices[k].neighbors[i]>k)
            {
                *t = k; t++;
                *t = vertices[k].neighbors[i]; t++;

            }
        }
    }
}

