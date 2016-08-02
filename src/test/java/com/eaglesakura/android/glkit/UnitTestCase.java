package com.eaglesakura.android.glkit;


import com.eaglesakura.android.AndroidSupportTestCase;
import com.eaglesakura.android.egl.BuildConfig;

import org.junit.runner.RunWith;

import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, packageName = BuildConfig.APPLICATION_ID, sdk = 21)
public abstract class UnitTestCase extends AndroidSupportTestCase {
}
