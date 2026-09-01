#include <gtest/gtest.h>

#include "mathutils.h"

TEST(MathUtils, AddPositive) {
    EXPECT_EQ(mathutils::add(2, 3), 5);
}

TEST(MathUtils, AddNegative) {
    EXPECT_EQ(mathutils::add(-1, 1), 0);
}

TEST(MathUtils, AddZero) {
    EXPECT_EQ(mathutils::add(0, 0), 0);
}
