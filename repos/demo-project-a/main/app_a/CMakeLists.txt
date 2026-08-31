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


add_executable(
    ${PROJECT_NAME}
    ${sources_headers_public}
    ${sources_headers_private}
    ${sources_units}
    ${sources_version}
)


target_include_directories(
    ${PROJECT_NAME}
    PRIVATE
        ${sources_include_dirs}
)
target_link_libraries(
    ${PROJECT_NAME}
    PRIVATE
        $<BUILD_INTERFACE:${PACKAGE_NAME}::sanitizer>
        ${PACKAGE_NAME}::app_a_core
    PUBLIC
)


install_component(
    TARGET_NAME ${PROJECT_NAME}
    NO_HEADERS
)
