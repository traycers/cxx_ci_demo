#include <gtest/gtest.h>
#include <vecopscale/vecopscale.hpp>

TEST(VecOpScale, ScaleByTwo)
{
    auto r = vecopscale::scale({2, 3}, 2);
    EXPECT_EQ(r.x, 4);
    EXPECT_EQ(r.y, 6);
}

TEST(VecOpScale, ScaleByOneIsIdentity)
{
    auto r = vecopscale::scale({2, 3}, 1);
    EXPECT_EQ(r.x, 2);
    EXPECT_EQ(r.y, 3);
}

TEST(VecOpScale, ScaleByThree)
{
    auto r = vecopscale::scale({1, 2}, 3);
    EXPECT_EQ(r.x, 3);
    EXPECT_EQ(r.y, 6);
}
