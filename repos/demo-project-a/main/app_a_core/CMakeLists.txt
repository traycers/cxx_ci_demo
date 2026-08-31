get_filename_component(
    PROJECT_NAME_TEMP
    ${CMAKE_CURRENT_SOURCE_DIR}
    NAME
)
project(
    ${PROJECT_NAME_TEMP}
    VERSION 1.0
    LANGUAGES CXX
)


give_files(
    VAR sources_headers_public
    DIRS "${PROJECT_SOURCE_DIR}/include"
    FILTERS "*.hpp"
)
give_files(
    VAR sources_headers_private
    DIRS "${PROJECT_SOURCE_DIR}/sources"
    FILTERS "*.hpp"
)
give_files(
    VAR sources_units
    DIRS "${PROJECT_SOURCE_DIR}/sources"
    FILTERS "*.cpp"
)
give_dirs(
    VAR sources_include_dirs
    DIRS "${PROJECT_SOURCE_DIR}/sources"
)


add_library(
    ${PROJECT_NAME}
    STATIC
    ${sources_headers_public}
    ${sources_headers_private}
    ${sources_units}
)
add_library(
    ${PACKAGE_NAME}::${PROJECT_NAME}
    ALIAS ${PROJECT_NAME}
)


target_include_directories(
    ${PROJECT_NAME}
    PRIVATE
        ${sources_include_dirs}
    PUBLIC
        $<BUILD_INTERFACE:${PROJECT_SOURCE_DIR}/include/>
        $<INSTALL_INTERFACE:include/>)
target_link_libraries(
    ${PROJECT_NAME}
    PRIVATE
        $<BUILD_INTERFACE:${PACKAGE_NAME}::sanitizer>
    PUBLIC
        cxx::ci::demo::project_c::vecopscale
)


install_component(
    TARGET_NAME ${PROJECT_NAME}
)
