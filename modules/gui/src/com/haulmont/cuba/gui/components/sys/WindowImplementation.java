package com.haulmont.cuba.gui.components.sys;

import com.haulmont.cuba.gui.Screens.LaunchMode;
import com.haulmont.cuba.gui.screen.Screen;

/**
 * Internal. Provides API for WindowManager implementations.
 */
public interface WindowImplementation {
    void setController(Screen screen);
    Screen getController();

    void setLaunchMode(LaunchMode launchMode);
    LaunchMode getLaunchMode();
}