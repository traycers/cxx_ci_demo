#include <vecutils/vecutils.hpp>

namespace vecutils
{
    Vector2 add(Vector2 a, Vector2 b)
    {
        return Vector2{a.x + b.x, a.y + b.y};
    }
}
