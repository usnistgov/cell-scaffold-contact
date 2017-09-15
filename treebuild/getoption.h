/*
 * qMaZda - Image Analysis and Pattern Recognition
 *
 * Copyright 2013  Piotr M. Szczypiński <piotr.szczypinski@p.lodz.pl>
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

#ifndef GETOPTION_H
#define GETOPTION_H
//=============================================================================
// Macros for scanning arguments

// A - short form of switch eg. "-m"
// AA - long form of switch eg. "--mode-of-sth"
// I - integer variable to set
// S - string pointer to set
// SS - bool value: true if string was set or false if there was no string

#define GET_INT_OPTION(A, AA, I) \
    if(strcmp(argv[argi], A)==0 || strcmp(argv[argi], AA)==0)\
    {\
        argi++;\
        if(argi < argc) {if(sscanf(argv[argi], "%i", &I) != 1) return argi;}\
        else return argi;\
    }\
    else if(strncmp(argv[argi], A, strlen(A))==0 || strncmp(argv[argi], AA, strlen(AA))==0)\
    {\
        char* _token = strtok(argv[argi], "=");\
        if(_token != NULL) _token = strtok(NULL, "=");\
        if(sscanf(_token, "%i", &I) != 1) return argi;\
    }

#define GET_STRING_OPTION(A, AA, S) \
    if(strcmp(argv[argi], A)==0 || strcmp(argv[argi], AA)==0)\
    {\
        argi++;\
        if(argi < argc) S = argv[argi];\
        else return argi;\
    }\
    else if(strncmp(argv[argi], A, strlen(A))==0 || strncmp(argv[argi], AA, strlen(AA))==0)\
    {\
        char* _token = strtok(argv[argi], "=");\
        if(_token != NULL) _token = strtok(NULL, "=");\
        S = _token;\
    }

#define GET_STRINGORNOARG_OPTION(A, AA, S, SS) \
    if(strcmp(argv[argi], A)==0 || strcmp(argv[argi], AA)==0)\
    {\
        SS = true;\
        argi++;\
        if(argi < argc)\
        {\
            if(argv[argi][0] == '-') {argi--;}\
            else {S = argv[argi]; }\
        }\
        else {return 0;}\
    }\
    else if(strncmp(argv[argi], A, strlen(A))==0 || strncmp(argv[argi], AA, strlen(AA))==0)\
    {\
        char* _token = strtok(argv[argi], "=");\
        if(_token != NULL) _token = strtok(NULL, "=");\
        S = _token; SS = true;\
    }

#define GET_NOARG_OPTION(A, AA, B, V) \
    if(strcmp(argv[argi], A)==0 || strcmp(argv[argi], AA)==0)\
    {\
        B = V;\
    }

#define GET_FLOAT_OPTION(A, AA, F) \
    if(strcmp(argv[argi], A) == 0 || strcmp(argv[argi], AA)==0)\
    {\
        argi++;\
        if(argi < argc) {if(sscanf(argv[argi], "%f", &F) != 1) return argi;}\
        else return argi;\
    }\
    else if(strncmp(argv[argi], A, strlen(A))==0 || strncmp(argv[argi], AA, strlen(AA))==0)\
    {\
        char* _token = strtok(argv[argi], "=");\
        if(_token != NULL) _token = strtok(NULL, "=");\
        if(sscanf(_token, "%f", &F) != 1) return argi;\
    }

#endif // GETOPTION_H
