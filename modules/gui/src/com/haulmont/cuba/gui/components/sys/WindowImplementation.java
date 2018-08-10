package com.haulmont.cuba.gui.components.sys;

import com.haulmont.cuba.gui.Screens.LaunchMode;
import com.haulmont.cuba.gui.screen.Screen;

/**
 * Internal. Provides API for WindowManager implementations.
 */
public interface WindowImplementation {
    void setFrameOwner(Screen screen);

    void setLaunchMode(LaunchMode launchMode);
    LaunchMode getLaunchMode();
}