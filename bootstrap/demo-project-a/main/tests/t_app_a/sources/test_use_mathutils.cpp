#include <gtest/gtest.h>
#include <mathutils/mathutils.hpp>

TEST(AppA, UsesMathutilsFromArtifactDependency) {
    EXPECT_EQ(mathutils::add(2, 3), 5);
}
