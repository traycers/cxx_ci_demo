cmake_minimum_required(VERSION 3.20)
project(app_a CXX)

# mathutils is NOT vendored here — TeamCity's artifact dependency on demo-project-b unpacks
# sdk.zip (%deps_unpack_all%) into %deps_dir%, which the base_build template passes as
# -DCMAKE_PREFIX_PATH to this configure step (see docs/build.sh). Not present in git;
# add_subdirectory is deliberately not used — this is the point of the artifact-dependency
# wiring, not a copy/submodule. For local/manual builds, pass -DCMAKE_PREFIX_PATH=<path to an
# installed mathutils> yourself.
set(MATHUTILS_DIR "${CMAKE_PREFIX_PATH}")

add_library(mathutils STATIC IMPORTED)
set_target_properties(mathutils PROPERTIES
    IMPORTED_LOCATION "${MATHUTILS_DIR}/lib/libmathutils.a"
    INTERFACE_INCLUDE_DIRECTORIES "${MATHUTILS_DIR}/include"
)

add_executable(app_a src/main.cpp)
target_link_libraries(app_a PRIVATE mathutils)
install(TARGETS app_a RUNTIME DESTINATION bin)

enable_testing()
find_package(GTest REQUIRED)
add_executable(test_app_a tests/test_app_a.cpp)
target_link_libraries(test_app_a PRIVATE mathutils GTest::gtest GTest::gtest_main)
add_test(NAME app_a_tests COMMAND test_app_a)
