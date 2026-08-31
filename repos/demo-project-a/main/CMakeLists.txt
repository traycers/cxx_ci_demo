cmake_minimum_required (VERSION 3.25)
set(PACKAGE_NAMESPACE cxx-ci-demo)
set(PACKAGE_NAME project_a)
project(${PACKAGE_NAME})
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
list(
    APPEND
    CMAKE_MODULE_PATH
    "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
include(cmake_utils)


option(USE_TEST "use tests" ON)


set(
    PROJECT_DEPS
    PKG cxx-ci-demo-project_c
)
find_pkgs(PROJECT_DEPS)
if(USE_TEST)
    enable_testing()
    find_package(GTest REQUIRED)
endif()



handle_version()
add_subdirectory(app_a_core)
add_subdirectory(app_a)



if(USE_TEST)
    add_subdirectory(tests)
endif()


install_package_config()


if(CMAKE_EXPORT_COMPILE_COMMANDS)
    file(
        CREATE_LINK
        "${CMAKE_BINARY_DIR}/compile_commands.json"
        "${CMAKE_SOURCE_DIR}/compile_commands.json"
        SYMBOLIC)
endif()
