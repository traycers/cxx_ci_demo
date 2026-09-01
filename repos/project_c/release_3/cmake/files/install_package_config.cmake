include_guard(GLOBAL)


set(
    _install_package_config_current_dir
    ${CMAKE_CURRENT_LIST_DIR}/install_package_config)

function(install_package_config)
    include(CMakePackageConfigHelpers)

    get_package_id(_pkg)

    find_deps(PROJECT_DEPS FIND_DEPS)

    configure_package_config_file(
        "${_install_package_config_current_dir}/config.cmake.in"
        "${CMAKE_CURRENT_BINARY_DIR}/${_pkg}-config.cmake"
        INSTALL_DESTINATION "lib/cmake/${_pkg}"
    )

    install(
        FILES       "${CMAKE_CURRENT_BINARY_DIR}/${_pkg}-config.cmake"
        DESTINATION "lib/cmake/${_pkg}"
    )
endfunction()
