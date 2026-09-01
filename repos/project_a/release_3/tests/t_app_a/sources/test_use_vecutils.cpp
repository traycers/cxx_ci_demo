#include <gtest/gtest.h>
#include <app_a_core/app_a_core.hpp>

TEST(AppA, ScaleUsesVecOpScaleChain) {
    auto r = app_a_core::scale({2, 3}, 2);
    EXPECT_EQ(r.x, 4);
    EXPECT_EQ(r.y, 6);
}
