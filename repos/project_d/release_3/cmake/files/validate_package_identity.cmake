include_guard(GLOBAL)


function(validate_package_identity)
    if((NOT DEFINED PACKAGE_NAME)
        OR (NOT PACKAGE_NAME))
        message(FATAL_ERROR [==[

    Variable 'PACKAGE_NAME' is empty or not set.

    This variable is required to correctly export targets and
    generate CMake config files.

    Define it in your root CMakeLists.txt:

        set(PACKAGE_NAME <your_project_name>)
        project(${PACKAGE_NAME})

    ]==])
    endif()

    if((NOT DEFINED PACKAGE_NAMESPACE)
        OR (NOT PACKAGE_NAMESPACE))
        message(FATAL_ERROR [==[

    Variable 'PACKAGE_NAMESPACE' is empty or not set.

    This variable is required to correctly namespace exported
    targets and generated CMake config files.

    Must be in kebab-case (hyphen-separated, e.g. 'my-namespace'):
    install_component() replaces '-' with '::' to build the C++
    alias namespace, so anything else will produce a malformed
    alias target.

    Define it in your root CMakeLists.txt:

        set(PACKAGE_NAMESPACE <your-namespace-prefix>)

    ]==])
    endif()
endfunction()
