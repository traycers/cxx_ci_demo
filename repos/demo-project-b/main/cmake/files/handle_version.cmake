#
# https://jonathanhamberg.com/post/cmake-embedding-git-hash/
#

set(
    _handle_version_current_dir
    ${CMAKE_CURRENT_LIST_DIR}/handle_version)

function(handle_version)
    set(options)
    set(oneValueArgs
        NAME
        TARGET_NAME)
    set(multiValueArgs)
    cmake_parse_arguments(
        PARAM
        "${options}"
        "${oneValueArgs}"
        "${multiValueArgs}"
        ${ARGN})
    string(
        TIMESTAMP
        build_date
        "%Y-%m-%dT%H:%M:%SZ"
        UTC)
    set(
        build_branch
        $ENV{rigel_build_branch})
    set(
        build_commit
        $ENV{rigel_build_commit})
    set(
        build_number
        $ENV{rigel_build_number})
    set(
        build_version
        $ENV{rigel_build_version})
    if(NOT DEFINED PARAM_NAME)
        set(PARAM_NAME ${PACKAGE_NAME})
    endif()
    if(NOT DEFINED PARAM_TARGET_NAME)
        set(PARAM_TARGET_NAME version)
    endif()
    set(tdir "${CMAKE_CURRENT_BINARY_DIR}/generated/${tname}")
    file(MAKE_DIRECTORY "${tdir}/include/${PARAM_NAME}")
    file(MAKE_DIRECTORY "${tdir}/sources")
    configure_file(
        ${_handle_version_current_dir}/header.hpp.in
        ${tdir}/include/${PARAM_NAME}/version.hpp
        @ONLY)
    configure_file(
        ${_handle_version_current_dir}/thin.cpp.in
        ${tdir}/sources/version_thin.cpp
        @ONLY)
    configure_file(
        ${_handle_version_current_dir}/wide.cpp.in
        ${tdir}/sources/version_wide.cpp
        @ONLY)
    configure_file(
        ${_handle_version_current_dir}/u8.cpp.in
        ${tdir}/sources/version_u8.cpp
        @ONLY)
    add_library(
        ${PARAM_TARGET_NAME}
        STATIC
        "${tdir}/include/${PARAM_NAME}/version.hpp"
        "${tdir}/sources/version_thin.cpp"
        "${tdir}/sources/version_wide.cpp"
        "${tdir}/sources/version_u8.cpp")
    add_library(
       ${PACKAGE_NAME}::${PARAM_TARGET_NAME}
       ALIAS ${PARAM_TARGET_NAME})
    target_include_directories(
        ${PARAM_TARGET_NAME}
        PUBLIC
            ${tdir}/include)
endfunction()
