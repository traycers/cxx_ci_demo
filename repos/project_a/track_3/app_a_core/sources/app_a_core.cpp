#include <app_a_core/app_a_core.hpp>
#include <vecopscale/vecopscale.hpp>

namespace app_a_core
{
    vecutils::Vector2 scale(vecutils::Vector2 v, int factor)
    {
        return vecopscale::scale(v, factor);
    }
}
