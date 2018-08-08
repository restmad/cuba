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
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Dialogs.MessageType;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.DialogAction;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.util.OperationResult;
import com.haulmont.cuba.gui.util.UnknownOperationResult;

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

    protected WindowInfo getWindowInfo() {
        return windowInfo;
    }

    protected void setWindowInfo(WindowInfo windowInfo) {
        this.windowInfo = windowInfo;
    }

    protected ScreenOptions getScreenOptions() {
        return screenOptions;
    }

    protected void setScreenOptions(ScreenOptions screenOptions) {
        this.screenOptions = screenOptions;
    }

    protected <E> void fireEvent(Class<E> eventType, E event) {
        eventHub.publish(eventType, event);
    }

    public Window getWindow() {
        return window;
    }

    protected void setWindow(Window window) {
        checkNotNullArgument(window);

        if (this.window != null) {
            throw new IllegalStateException("Screen already has Window");
        }
        this.window = window;
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
     * @param listener listener
     * @return
     */
    public Subscription addAfterCloseListener(Consumer<AfterCloseEvent> listener) {
        return eventHub.subscribe(AfterCloseEvent.class, listener);
    }

    private Screens getScreens() {
        return getWindow().getUiServices().getScreens();
    }

    private Dialogs getDialogs() {
        return getWindow().getUiServices().getDialogs();
    }

    private Messages getMessages() {
        return beanLocator.get(Messages.NAME);
    }

    private Icons getIcons() {
        return beanLocator.get(Icons.NAME);
    }

    protected OperationResult showUnsavedChangesDialog() {
        UnknownOperationResult result = new UnknownOperationResult();
        Messages messages = getMessages();

        getDialogs().createOptionDialog()
                .setCaption(messages.getMainMessage("closeUnsaved.caption"))
                .setMessage(messages.getMainMessage("saveUnsaved"))
                .setType(MessageType.WARNING)
                .setActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {
                                    discardAndClose()
                                            .then(result::success)
                                            .otherwise(result::fail);
                                }),
                        new DialogAction(DialogAction.Type.NO, Action.Status.PRIMARY)
                                .withHandler(e -> {
                                    // todo try to move focus back
                                    // findAndFocusChildComponent();
                                })
                )
                .show();

        return result;
    }

    protected OperationResult showSaveConfirmationDialog() {
        UnknownOperationResult result = new UnknownOperationResult();
        Messages messages = getMessages();

        getDialogs().createOptionDialog()
                .setCaption(messages.getMainMessage("closeUnsaved.caption"))
                .setMessage(messages.getMainMessage("saveUnsaved"))
                .setActions(
                        new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY)
                                .withCaption(messages.getMainMessage("closeUnsaved.save"))
                                .withHandler(e -> {
                                    commitAndClose()
                                            .then(result::success)
                                            .otherwise(result::fail);
                                }),
                        new BaseAction("discard")
                                .withIcon(getIcons().get(CubaIcon.DIALOG_CANCEL))
                                .withCaption(messages.getMainMessage("closeUnsaved.discard"))
                                .withHandler(e -> {
                                    discardAndClose()
                                            .then(result::success)
                                            .otherwise(result::fail);
                                }),
                        new DialogAction(DialogAction.Type.CANCEL)
                                .withIcon(null)
                                .withHandler(e -> {
                                    // todo try to move focus back
                                    // findAndFocusChildComponent();

                                    result.fail();
                                })
                )
                .show();

        return result;
    }

    /**
     * JavaDoc
     *
     * @param action
     * @return
     */
    public OperationResult close(CloseAction action) {
        BeforeCloseEvent beforeCloseEvent = new BeforeCloseEvent(this, action);
        fireEvent(BeforeCloseEvent.class, beforeCloseEvent);

        if (beforeCloseEvent.isClosePrevented()) {
            return OperationResult.fail();
        }

        Configuration configuration = beanLocator.get(Configuration.NAME);
        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);

        if (action.isCheckForUnsavedChanges() && isModified()) {

            if (clientConfig.getUseSaveConfirmation()) {
                return showSaveConfirmationDialog();
            } else {
                return showUnsavedChangesDialog();
            }
        }

        // save settings right before removing
        if (!clientConfig.getManualScreenSettingsSaving()) {
            saveSettings();
        }

        getScreens().remove(this);

        AfterCloseEvent afterCloseEvent = new AfterCloseEvent(this, action);
        fireEvent(AfterCloseEvent.class, afterCloseEvent);

        return OperationResult.success();
    }

    /**
     * JavaDoc
     *
     * @return
     */
    public OperationResult close() {
        return close(WINDOW_CLOSE_ACTION);
    }

    /**
     * JavaDoc
     *
     * @return
     */
    public OperationResult commitAndClose() {
        // todo commit

        return close(WINDOW_COMMIT_AND_CLOSE_ACTION);
    }

    /**
     * JavaDoc
     */
    public OperationResult discardAndClose() {
        // todo commit

        return close(WINDOW_DISCARD_AND_CLOSE_ACTION);
    }

    /**
     * JavaDoc
     */
    public void saveSettings() {

    }

    /**
     * JavaDoc
     */
    public boolean isModified() {
        return false;
    }
}