#include <vecopscale/vecopscale.hpp>

namespace vecopscale
{
    vecutils::Vector2 scale(vecutils::Vector2 v, int factor)
    {
        vecutils::Vector2 acc = v;
        for (int i = 1; i < factor; ++i)
        {
            acc = vecutils::add(acc, v);
        }
        return acc;
    }
}
