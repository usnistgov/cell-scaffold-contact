// ================================================================
/// 
/// This software was developed by employees of the National Institute 
/// of Standards and Technology (NIST), an agency of the Federal 
/// Government. Pursuant to title 17 United States Code Section 105, 
/// works of NIST employees are not subject to copyright protection 
/// in the United States and are considered to be in the public domain. 
/// Permission to freely use, copy, modify, and distribute this software 
/// and its documentation without fee is hereby granted, provided that 
/// this notice and disclaimer of warranty appears in all copies.
/// THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, 
/// EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED 
/// TO, ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, 
/// ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
/// PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
/// DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT 
/// THE SOFTWARE WILL BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE 
/// FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT, INDIRECT, 
/// SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM, OR 
/// IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON 
/// WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS 
/// SUSTAINED BY PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT 
/// LOSS WAS SUSTAINED FROM, OR AROSE OUT OF THE RESULTS OF, OR USE OF, 
/// THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
/// 
/// ================================================================

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
