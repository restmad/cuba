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
import com.haulmont.bali.events.Subscription;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.util.OperationResult;

import javax.inject.Inject;
import java.util.function.Consumer;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

/**
 * JavaDoc
 */
public abstract class Screen implements FrameOwner {

    private String id;

    private WindowInfo windowInfo;
    private ScreenOptions screenOptions;

    private Window window;

    private EventHub eventHub = new EventHub();

    private BeanLocator beanLocator;

    @Inject
    protected void setBeanLocator(BeanLocator beanLocator) {
        this.beanLocator = beanLocator;
    }

    protected EventHub getEventHub() {
        return eventHub;
    }

    public String getId() {
        return id;
    }

    /**
     * JavaDoc
     *
     * @param id
     */
    protected void setId(String id) {
        this.id = id;
    }

    protected void setWindow(Window window) {
        checkNotNullArgument(window);

        if (this.window != null) {
            throw new IllegalStateException("Screen already has Window");
        }
        this.window = window;
    }

    protected void setWindowInfo(WindowInfo windowInfo) {
        this.windowInfo = windowInfo;
    }

    protected WindowInfo getWindowInfo() {
        return windowInfo;
    }

    protected void setScreenOptions(ScreenOptions screenOptions) {
        this.screenOptions = screenOptions;
    }

    protected ScreenOptions getScreenOptions() {
        return screenOptions;
    }

    protected <E> void fireEvent(Class<E> eventType, E event) {
        eventHub.publish(eventType, event);
    }

    public Window getWindow() {
        return window;
    }

    protected Subscription addInitListener(Consumer<InitEvent> listener) {
        return eventHub.subscribe(InitEvent.class, listener);
    }

    protected Subscription addAfterInitListener(Consumer<AfterInitEvent> listener) {
        return eventHub.subscribe(AfterInitEvent.class, listener);
    }

    protected Subscription addBeforeCloseListener(Consumer<BeforeCloseEvent> listener) {
        return eventHub.subscribe(BeforeCloseEvent.class, listener);
    }

    /**
     * JavaDoc
     *
     * @param action
     * @return
     */
    public OperationResult close(CloseAction action) {
        BeforeCloseEvent event = new BeforeCloseEvent(this, action);
        fireEvent(BeforeCloseEvent.class, event);

        if (event.isClosePrevented()) {
            return OperationResult.fail();
        }

        Configuration configuration = beanLocator.get(Configuration.NAME);
        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);

        if (isModified()) {
            if (clientConfig.getUseSaveConfirmation()) {
                // todo show dialog
            } else {
                // todo show dialog
            }
        }

        // save settings right before removing
        saveSettings();

        getScreens().remove(this);

        return OperationResult.success();
    }

    private Screens getScreens() {
        return getWindow().getUiServices().getScreens();
    }

    private Dialogs getDialogs() {
        return getWindow().getUiServices().getDialogs();
    }

    /**
     * JavaDoc
     *
     * @return
     */
    public OperationResult close() {
        return close(WINDOW_CLOSE_ACTION);
    }

    public void saveSettings() {
        // todo implement

        /*
        if (!clientConfig.getManualScreenSettingsSaving()) {
            if (getWrapper() != null) {
                getWrapper().saveSettings();
            } else {
                saveSettings();
            }
        }
        */
    }

    public boolean isModified() {
        return false; // todo implement
    }
}