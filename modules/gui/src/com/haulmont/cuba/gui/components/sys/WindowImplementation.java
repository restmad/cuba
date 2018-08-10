package com.haulmont.cuba.gui.components.sys;

import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.UiServices;

/**
 * Internal. Provides API for WindowManager implementations.
 */
public interface WindowImplementation extends Window {
    void setFrameOwner(Screen screen);

    void setUiServices(UiServices uiServices);
    UiServices getUiServices();
}