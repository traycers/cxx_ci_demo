give_dirs(
    VAR test_dirs
    DIRS "${CMAKE_CURRENT_SOURCE_DIR}"
    NO_RECURSE)
foreach(_test_dir IN LISTS test_dirs)
    get_filename_component(
        _test_dir_name
        "${_test_dir}"
        NAME)
    if(
        _test_dir_name MATCHES "^t_" AND
        EXISTS "${_test_dir}/CMakeLists.txt")
        add_subdirectory("${_test_dir}")
    endif()
endforeach()
