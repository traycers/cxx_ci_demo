cmake_minimum_required(VERSION 3.20)
project(mathutils CXX)

add_library(mathutils STATIC src/mathutils.cpp)
target_include_directories(mathutils PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
)

install(TARGETS mathutils ARCHIVE DESTINATION lib)
install(FILES include/mathutils.h DESTINATION include)

enable_testing()
find_package(GTest REQUIRED)
add_executable(test_mathutils tests/test_mathutils.cpp)
target_link_libraries(test_mathutils PRIVATE mathutils GTest::gtest GTest::gtest_main)
add_test(NAME mathutils_tests COMMAND test_mathutils)
