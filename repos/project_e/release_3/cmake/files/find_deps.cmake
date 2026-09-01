include_guard(GLOBAL)


function(find_deps deps_list out_var)
    set(_pkg    "")
    set(_comps  "")
    set(_state  "WAITING")
    set(_result "")

    foreach(_item IN LISTS ${deps_list})
        if(_item STREQUAL "PKG")
            if(_pkg)
                list(JOIN _comps " " _comps_str)
                if(_comps_str)
                    string(APPEND _result
                        "find_dependency(${_pkg} REQUIRED COMPONENTS ${_comps_str})\n"
                    )
                else()
                    string(APPEND _result "find_dependency(${_pkg} REQUIRED)\n")
                endif()
            endif()
            set(_state "PKG_NAME")
            set(_pkg   "")
            set(_comps "")
        elseif(_state STREQUAL "PKG_NAME")
            set(_pkg   "${_item}")
            set(_state "MAYBE_COMPONENTS")
        elseif(_item STREQUAL "COMPONENTS")
            set(_state "COLLECTING")
        elseif(_state STREQUAL "COLLECTING")
            list(APPEND _comps "${_item}")
        endif()
    endforeach()

    if(_pkg)
        list(JOIN _comps " " _comps_str)
        if(_comps_str)
            string(APPEND _result
                "find_dependency(${_pkg} REQUIRED COMPONENTS ${_comps_str})\n"
            )
        else()
            string(APPEND _result "find_dependency(${_pkg} REQUIRED)\n")
        endif()
    endif()

    set(${out_var} "${_result}" PARENT_SCOPE)
endfunction()
