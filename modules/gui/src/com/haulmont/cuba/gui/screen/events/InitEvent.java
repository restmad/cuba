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

package com.haulmont.cuba.gui.screen.events;

import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.ScreenOptions;

import java.util.EventObject;

/**
 * JavaDoc
 */
public class InitEvent extends EventObject {
    protected final ScreenOptions options;

    public InitEvent(Screen source, ScreenOptions options) {
        super(source);
        this.options = options;
    }

    @Override
    public Screen getSource() {
        return (Screen) super.getSource();
    }

    public ScreenOptions getOptions() {
        return options;
    }
}