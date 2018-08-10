/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.gui.screen;

import com.haulmont.bali.events.EventHub;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowInfo;

/**
 * Internal methods used in WindowManager implementations.
 */
public final class ScreenUtils {

    public static void setWindowId(Screen screen, String id) {
        screen.setId(id);
    }

    public static void setWindow(Screen screen, Window window) {
        screen.setWindow(window);
    }

    public static <E> void fireEvent(Screen screen, Class<E> eventType, E event) {
        screen.fireEvent(eventType, event);
    }

    public static EventHub getEventHub(Screen screen) {
        return screen.getEventHub();
    }

    public static void setWindowInfo(Screen screen, WindowInfo windowInfo) {
        screen.setWindowInfo(windowInfo);
    }

    public static WindowInfo getWindowInfo(Screen screen) {
        return screen.getWindowInfo();
    }

    public static ScreenOptions getScreenOptions(Screen screen) {
        return screen.getScreenOptions();
    }

    public static void setScreenOptions(Screen screen, ScreenOptions screenOptions) {
        screen.setScreenOptions(screenOptions);
    }
}