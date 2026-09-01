#include <iostream>
#include <app_a_core/app_a_core.hpp>

int main()
{
    vecutils::Vector2 v{2, 3};
    auto r = app_a_core::scale(v, 2);
    std::cout                                                             //
        << "scale((" << v.x << ", " << v.y << "), x2) = "                 //
        << "(" << r.x << ", " << r.y << ")"                               //
        << "\n";
    return 0;
}
