#include <gtest/gtest.h>
#include <vecutils/vecutils.hpp>

TEST(VecUtils, AddPositive)
{
    auto r = vecutils::add({2, 3}, {1, 1});
    EXPECT_EQ(r.x, 3);
    EXPECT_EQ(r.y, 4);
}

TEST(VecUtils, AddNegative)
{
    auto r = vecutils::add({-1, -1}, {1, 1});
    EXPECT_EQ(r.x, 0);
    EXPECT_EQ(r.y, 0);
}

TEST(VecUtils, AddZero)
{
    auto r = vecutils::add({0, 0}, {0, 0});
    EXPECT_EQ(r.x, 0);
    EXPECT_EQ(r.y, 0);
}
