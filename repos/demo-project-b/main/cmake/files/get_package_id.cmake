include_guard(GLOBAL)


function(get_package_id out_var)
    validate_package_identity()
    set(${out_var} "${PACKAGE_NAMESPACE}-${PACKAGE_NAME}" PARENT_SCOPE)
endfunction()
