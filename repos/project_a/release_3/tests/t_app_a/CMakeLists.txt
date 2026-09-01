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
    VAR sources_headers
    DIRS "${PROJECT_SOURCE_DIR}/sources"
    FILTERS "*.h" "*.hpp"
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
    ${sources_headers}
    ${sources_units}
)


target_include_directories(
    ${PROJECT_NAME}
    PRIVATE
        ${sources_include_dirs}
)
target_link_libraries(
    ${PROJECT_NAME}
    PRIVATE
        GTest::GTest
        GTest::Main
        ${PACKAGE_NAME}::sanitizer
        ${PACKAGE_NAME}::app_a_core
)


add_test(${PROJECT_NAME} ${PROJECT_NAME})
