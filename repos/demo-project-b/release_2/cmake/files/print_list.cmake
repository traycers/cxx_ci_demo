
macro(print_list value)
    message("${value}  =  ")
    foreach(it IN LISTS ${value})
        message("    ${it}")
    endforeach()
endmacro()
