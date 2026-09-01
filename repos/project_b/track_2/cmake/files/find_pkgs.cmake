include_guard(GLOBAL)


function(find_pkgs deps_list)
    set(_pkg   "")
    set(_comps "")
    set(_state "WAITING")

    set(_items ${${deps_list}})
    list(APPEND _items "PKG")

    foreach(_item IN LISTS _items)
        if(_item STREQUAL "PKG")
            if(_pkg)
                find_package(${_pkg} REQUIRED COMPONENTS ${_comps})
                foreach(_comp IN LISTS _comps)
                    if(NOT TARGET ${_pkg}::${_comp})
                        message(FATAL_ERROR
                            "[from 'find_pkgs' function]: package "
                            "'${_pkg}' has no CMake component "
                            "'${_comp}' -- check package's "
                            "generated Config.cmake for "
                            "actual exported components list.")
                    endif()
                endforeach()
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
endfunction()
