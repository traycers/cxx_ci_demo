option(
    ENABLE_ASAN
    "Build with AddressSanitizer"
    OFF
)

option(
    ENABLE_UBSAN
    "Build with UndefinedBehaviorSanitizer"
    OFF
)

option(
    ENABLE_TSAN
    "Build with ThreadSanitizer"
    OFF
)

if(ENABLE_ASAN AND ENABLE_TSAN)
    message(FATAL_ERROR
        "ENABLE_ASAN and ENABLE_TSAN "
        "cannot be enabled together: AddressSanitizer "
        "and ThreadSanitizer are mutually incompatible."
    )
endif()


add_library(sanitizer INTERFACE)
add_library(${PACKAGE_NAME}::sanitizer ALIAS sanitizer)


if(ENABLE_ASAN)
    target_compile_options(
        sanitizer
        INTERFACE
            -fsanitize=address
            -fno-omit-frame-pointer
    )

    target_link_options(
        sanitizer
        INTERFACE
            -fsanitize=address
    )
endif()

if(ENABLE_UBSAN)
    target_compile_options(
        sanitizer
        INTERFACE
            -fsanitize=undefined
            -fno-sanitize-recover=undefined
            -fno-omit-frame-pointer
    )

    target_link_options(
        sanitizer
        INTERFACE
            -fsanitize=undefined
    )
endif()

if(ENABLE_TSAN)
    target_compile_options(
        sanitizer
        INTERFACE
            -fsanitize=thread
            -fno-omit-frame-pointer
    )

    target_link_options(
        sanitizer
        INTERFACE
            -fsanitize=thread
    )
endif()
