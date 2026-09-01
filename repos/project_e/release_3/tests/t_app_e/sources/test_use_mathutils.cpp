#include <gtest/gtest.h>
#include <app_e_core/app_e_core.hpp>

TEST(AppE, UsesMathutilsViaAppECore) {
    EXPECT_EQ(app_e_core::add(2, 3), 5);
}
