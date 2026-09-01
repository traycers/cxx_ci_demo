function(install_component)
    set(options
        NO_HEADERS)
    set(oneValueArgs
        INSTALL_DIR_REGEX
        TARGET_NAME)
    set(multiValueArgs)
    cmake_parse_arguments(
        PARAMS
        "${options}"
        "${oneValueArgs}"
        "${multiValueArgs}"
        ${ARGN})
    get_package_id(_pkg)
    string(
        REPLACE "-" "::"
        PACKAGE_NAMESPACE_COLON
        ${PACKAGE_NAMESPACE})
    if(NOT PARAMS_NO_HEADERS)
        if(EXISTS "${PROJECT_SOURCE_DIR}/include/")
            install(
                DIRECTORY "${PROJECT_SOURCE_DIR}/include/"
                DESTINATION include
                COMPONENT ${PARAMS_TARGET_NAME})
        endif()
        if(DEFINED PARAMS_INSTALL_DIR_REGEX)
            install(
                DIRECTORY "${PROJECT_SOURCE_DIR}/include/"
                DESTINATION include
                COMPONENT ${PARAMS_TARGET_NAME}
                REGEX ${PARAMS_INSTALL_DIR_REGEX}
                EXCLUDE)
        endif()
    endif()
    install(
        TARGETS ${PARAMS_TARGET_NAME}
        EXPORT ${_pkg}-config
        COMPONENT ${PARAMS_TARGET_NAME})
    install(
        EXPORT ${_pkg}-config
        FILE ${_pkg}-targets.cmake
        NAMESPACE ${PACKAGE_NAMESPACE_COLON}::${PACKAGE_NAME}::
        DESTINATION lib/cmake/${_pkg}
        COMPONENT ${PARAMS_TARGET_NAME})
endfunction()
