cmake_minimum_required(VERSION 2.8)

project(itk2fits)

find_package(ITK REQUIRED)
include(${ITK_USE_FILE})
if (ITKVtkGlue_LOADED)
  find_package(VTK REQUIRED)
  include(${VTK_USE_FILE})
endif()

add_executable(itk2fits itk2fits.cpp FitsIO.c)

if( "${ITK_VERSION_MAJOR}" LESS 4 )
  target_link_libraries(itk2fits ITKReview ${ITK_LIBRARIES})
else( "${ITK_VERSION_MAJOR}" LESS 4 )
  target_link_libraries(itk2fits ${ITK_LIBRARIES})
endif( "${ITK_VERSION_MAJOR}" LESS 4 )
