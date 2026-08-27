function(install_component_validate)
    if((NOT DEFINED PACKAGE_NAME)
        OR (NOT PACKAGE_NAME))
        message(FATAL_ERROR [==[

    Variable 'PACKAGE_NAME' is empty or not set.

    This variable is required by install_component() to correctly
    export targets and generate CMake config files.

    Define it in your root CMakeLists.txt:

        set(PACKAGE_NAME <your_project_name>)
        project(${PACKAGE_NAME})

    ]==])
    endif()

    if((NOT DEFINED PACKAGE_NAMESPACE)
        OR (NOT PACKAGE_NAMESPACE))
        message(FATAL_ERROR [==[

    Variable 'PACKAGE_NAMESPACE' is empty or not set.

    This variable is required by install_component() to correctly
    namespace exported targets.

    Define it in your root CMakeLists.txt:

        set(PACKAGE_NAMESPACE <your_namespace_prefix>)

    ]==])
    endif()
endfunction()

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
    install_component_validate()
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
        EXPORT ${PACKAGE_NAMESPACE}-${PACKAGE_NAME}Config
        COMPONENT ${PARAMS_TARGET_NAME})
    install(
        EXPORT ${PACKAGE_NAMESPACE}-${PACKAGE_NAME}Config
        NAMESPACE ${PACKAGE_NAMESPACE_COLON}::${PACKAGE_NAME}::
        DESTINATION lib/cmake/${PACKAGE_NAMESPACE}-${PACKAGE_NAME}
        COMPONENT ${PARAMS_TARGET_NAME})
endfunction()
