#include <gtest/gtest.h>

#include "mathutils.h"

TEST(AppA, UsesMathutilsFromArtifactDependency) {
    EXPECT_EQ(mathutils::add(2, 3), 5);
}
