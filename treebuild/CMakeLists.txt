
cmake_minimum_required(VERSION 2.8)

project(treebuild)

find_package(ITK REQUIRED)
include(${ITK_USE_FILE})
if (ITKVtkGlue_LOADED)
  find_package(VTK REQUIRED)
  include(${VTK_USE_FILE})
endif()

add_executable(treebuild treebuild.cpp treediameters.cpp tree.cpp treesmooth.cpp smoothdiameter.cpp sphear.cpp alglib/alglibinternal.cpp alglib/alglibmisc.cpp alglib/ap.cpp alglib/dataanalysis.cpp alglib/diffequations.cpp alglib/fasttransforms.cpp alglib/integration.cpp alglib/interpolation.cpp alglib/linalg.cpp alglib/optimization.cpp alglib/solvers.cpp alglib/specialfunctions.cpp alglib/statistics.cpp)

if( "${ITK_VERSION_MAJOR}" LESS 4 )
  target_link_libraries(treebuild ITKReview ${ITK_LIBRARIES})
else( "${ITK_VERSION_MAJOR}" LESS 4 )
  target_link_libraries(treebuild ${ITK_LIBRARIES})
endif( "${ITK_VERSION_MAJOR}" LESS 4 )
