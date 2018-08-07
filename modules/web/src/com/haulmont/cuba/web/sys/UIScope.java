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
 *
 */

package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.web.AppUI;
import com.vaadin.server.VaadinSession;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@Component(UIScope.NAME)
public class UIScope implements Scope {

    public static final String NAME = "uiScope";

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || !session.hasLock()) {
            throw new IllegalStateException("Unable to use UIScope from non-Vaadin thread");
        }

        AppUI ui = AppUI.getCurrent();

        switch (name) {
            case Screens.NAME:
                return ui.getScreens();

            case Dialogs.NAME:
                return ui.getDialogs();

            case Notifications.NAME:
                return ui.getNotifications();

            default:
                throw new UnsupportedOperationException("Unknown UI scoped bean " + name);
        }
    }

    @Override
    public Object remove(String name) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || !session.hasLock()) {
            throw new IllegalStateException("Unable to use UIScope from non-Vaadin thread");
        }

        throw new UnsupportedOperationException("Remove is not supported for UI scope");
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }
}