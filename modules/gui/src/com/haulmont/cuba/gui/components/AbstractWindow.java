/*
 * Copyright (c) 2008-2016 Haulmont.
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
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.DialogOptions;
import com.haulmont.cuba.gui.FrameContext;
import com.haulmont.cuba.gui.WindowContext;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.security.ActionsPermissions;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.gui.sys.UiServices;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.springframework.context.ApplicationListener;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * Base class for simple screen controllers.
 */
public class AbstractWindow extends Screen implements Window, LegacyFrame, Component.HasXmlDescriptor, Window.Wrapper, SecuredActionsHolder {

    protected Frame frame;
    private Object _companion;

    private Component parent;
    private List<ApplicationListener> uiEventListeners;

    private DsContext dsContext;

    @Inject
    protected Messages messages;

    public AbstractWindow() {
    }

    public UiServices getUiServices() {
        throw new UnsupportedOperationException("TODO"); // todo
    }

    /** INTERNAL. Don't call from application code. */
    public void setWrappedFrame(Frame frame) {
        this.frame = frame;
    }

    @Override
    public WindowManager getWindowManager() {
        return frame.getWindowManager();
    }

    @Override
    public Frame getWrappedFrame() {
        return frame;
    }

    @Subscribe
    protected void init(InitEvent initEvent) {
        init(Collections.emptyMap()); // todo
    }

    /**
     * Called by the framework after creation of all components and before showing the screen.
     * <br> Override this method and put initialization logic here.
     *
     * @param params parameters passed from caller's code, usually from
     *               {@link #openWindow(String, WindowManager.OpenType)} and similar methods, or set in
     *               {@code screens.xml} for this registered screen
     */
    public void init(Map<String, Object> params) {
    }

    @Override
    public String getId() {
        return frame.getId();
    }

    @Override
    public void setId(String id) {
        String currentId = getId();

        if (!Objects.equals(currentId, id)) {
            if (getFrame() != null) {
                getFrame().unregisterComponent(this);
            }
        }

        frame.setId(id);

        // register this wrapper instead of underlying frame
        if (!Objects.equals(currentId, id)) {
            if (getFrame() != null) {
                getFrame().registerComponent(this);
            }
        }
    }

    @Override
    public Component getParent() {
        return parent;
    }

    @Override
    public void setParent(Component parent) {
        this.parent = parent;
    }

    @Override
    public boolean isEnabled() {
        return frame.isEnabled();
    }

    @Override
    public boolean isEnabledRecursive() {
        return frame.isEnabledRecursive();
    }

    @Override
    public void setEnabled(boolean enabled) {
        frame.setEnabled(enabled);
    }

    @Override
    public boolean isVisible() {
        return frame.isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    @Override
    public boolean isVisibleRecursive() {
        return frame.isVisibleRecursive();
    }

    @Override
    public float getHeight() {
        return frame.getHeight();
    }

    @Override
    public int getHeightUnits() {
        return frame.getHeightUnits();
    }

    @Override
    public SizeUnit getHeightSizeUnit() {
        return frame.getHeightSizeUnit();
    }

    @Override
    public void setHeight(String height) {
        frame.setHeight(height);
    }

    @Override
    public float getWidth() {
        return frame.getWidth();
    }

    @Override
    public int getWidthUnits() {
        return frame.getWidthUnits();
    }

    @Override
    public SizeUnit getWidthSizeUnit() {
        return frame.getWidthSizeUnit();
    }

    @Override
    public void setWidth(String width) {
        frame.setWidth(width);
    }

    @Override
    public Alignment getAlignment() {
        return frame.getAlignment();
    }

    @Override
    public void setAlignment(Alignment alignment) {
        frame.setAlignment(alignment);
    }

    @Override
    public boolean isResponsive() {
        return frame.isResponsive();
    }

    @Override
    public void setResponsive(boolean responsive) {
        frame.setResponsive(responsive);
    }

    @Override
    public String getIcon() {
        return frame.getIcon();
    }

    @Override
    public void setIcon(String icon) {
        frame.setIcon(icon);
    }

    @Override
    public void setIconFromSet(Icons.Icon icon) {
        frame.setIconFromSet(icon);
    }

    @Override
    public void add(Component component) {
        frame.add(component);
    }

    @Override
    public void remove(Component component) {
        frame.remove(component);
    }

    @Override
    public void removeAll() {
        frame.removeAll();
    }

    @Override
    public Component getOwnComponent(String id) {
        return frame.getOwnComponent(id);
    }

    @Nullable
    @Override
    public Component getComponent(String id) {
        return frame.getComponent(id);
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return frame.getOwnComponents();
    }

    @Override
    public Collection<Component> getComponents() {
        return frame.getComponents();
    }

    @Override
    public Object getComponent() {
        return frame;
    }

    @Override
    public Object getComposition() {
        return frame;
    }

    @Override
    public void expand(Component component, String height, String width) {
        frame.expand(component, height, width);
    }

    @Override
    public void expand(Component component) {
        frame.expand(component);
    }

    @Override
    public void resetExpanded() {
        frame.resetExpanded();
    }

    @Override
    public boolean isExpanded(Component component) {
        return frame.isExpanded(component);
    }

    @Override
    public ExpandDirection getExpandDirection() {
        return ExpandDirection.VERTICAL;
    }

    @Override
    public void setContext(FrameContext ctx) {
        frame.setContext(ctx);
    }

    @Override
    public String getMessagesPack() {
        return frame.getMessagesPack();
    }

    @Override
    public void setMessagesPack(String name) {
        frame.setMessagesPack(name);
    }

    /**
     * Get localized message from the message pack associated with this frame or window.
     * @param key   message key
     * @return      localized message
     * @see Messages#getMessage(String, String)
     */
    protected String getMessage(String key) {
        String msgPack = getMessagesPack();
        if (StringUtils.isEmpty(msgPack)) {
            throw new DevelopmentException("MessagePack is not set");
        }

        return messages.getMessage(msgPack, key);
    }

    /**
     * Get localized message from the message pack associated with this frame or window, and use it as a format
     * string for parameters provided.
     * @param key       message key
     * @param params    parameter values
     * @return          formatted string or the key in case of IllegalFormatException
     * @see Messages#formatMessage(String, String, Object...)
     */
    protected String formatMessage(String key, Object... params) {
        String msgPack = getMessagesPack();
        if (StringUtils.isEmpty(msgPack)) {
            throw new DevelopmentException("MessagePack is not set");
        }

        return messages.formatMessage(msgPack, key, params);
    }

    @Override
    public void registerComponent(Component component) {
        frame.registerComponent(component);
    }

    @Override
    public void unregisterComponent(Component component) {
        frame.unregisterComponent(component);
    }

    @Nullable
    @Override
    public Component getRegisteredComponent(String id) {
        return frame.getRegisteredComponent(id);
    }

    @Override
    public boolean isValid() {
        return frame.isValid();
    }

    @Override
    public void validate() throws ValidationException {
        frame.validate();
    }

    /**
     * Show validation errors alert. Can be overriden in subclasses.
     *
     * @param errors the list of validation errors. Caller fills it by errors found during the default validation.
     */
    public void showValidationErrors(ValidationErrors errors) {
        StringBuilder buffer = new StringBuilder();
        for (ValidationErrors.Item error : errors.getAll()) {
            buffer.append(error.description).append("\n");
        }

        Configuration configuration = AppBeans.get(Configuration.NAME);
        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);

        NotificationType notificationType = NotificationType.valueOf(clientConfig.getValidationNotificationType());
        showNotification(messages.getMainMessage("validationFail.caption"), buffer.toString(), notificationType);
    }

    @Override
    public void add(Component childComponent, int index) {
        frame.add(childComponent, index);
    }

    @Override
    public int indexOf(Component component) {
        return frame.indexOf(component);
    }

    @Nullable
    @Override
    public Component getComponent(int index) {
        return frame.getComponent(index);
    }

    /**
     * @return a companion implementation, specific for the current client type
     */
    @Nullable
    public <T> T getCompanion() {
        //noinspection unchecked
        return (T) _companion;
    }

    /** INTERNAL. Don't call from application code. */
    public void setCompanion(Object companion) {
        this._companion = companion;
    }

    /** INTERNAL. Don't call from application code. */
    @Nullable
    public List<ApplicationListener> getUiEventListeners() {
        return uiEventListeners;
    }

    /** INTERNAL. Don't call from application code. */
    public void setUiEventListeners(List<ApplicationListener> uiEventListeners) {
        this.uiEventListeners = uiEventListeners;
    }

    @Override
    public Frame getFrame() {
        return this.frame.getFrame();
    }

    @Override
    public void setFrame(Frame frame) {
        this.frame.setFrame(frame);
        // register this wrapper instead of underlying frame
        frame.registerComponent(this);
    }

    @Override
    public String getStyleName() {
        return frame.getStyleName();
    }

    @Override
    public void setStyleName(String styleName) {
        frame.setStyleName(styleName);
    }

    @Override
    public void addStyleName(String styleName) {
        frame.addStyleName(styleName);
    }

    @Override
    public void removeStyleName(String styleName) {
        frame.removeStyleName(styleName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(Class<X> internalComponentClass) {
        if (getComponent() instanceof Component.Wrapper) {
            return (X) ((Component.Wrapper) frame).getComponent();
        }

        return (X) frame;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrapComposition(Class<X> internalCompositionClass) {
        if (getComposition() instanceof Component.Wrapper) {
            return (X) ((Component.Wrapper) frame).getComposition();
        }

        return (X) frame;
    }

    @Override
    public void setSpacing(boolean enabled) {
        frame.setSpacing(enabled);
    }

    @Override
    public boolean getSpacing() {
        return frame.getSpacing();
    }

    @Override
    public void setMargin(MarginInfo marginInfo) {
        frame.setMargin(marginInfo);
    }

    @Override
    public MarginInfo getMargin() {
        return frame.getMargin();
    }

    @Override
    public void addAction(Action action) {
        frame.addAction(action);
    }

    @Override
    public void addAction(Action action, int index) {
        frame.addAction(action, index);
    }

    @Override
    public void removeAction(@Nullable Action action) {
        frame.removeAction(action);
    }

    @Override
    public void removeAction(@Nullable String id) {
        frame.removeAction(id);
    }

    @Override
    public void removeAllActions() {
        frame.removeAllActions();
    }

    @Override
    public Collection<Action> getActions() {
        return frame.getActions();
    }

    @Override
    @Nullable
    public Action getAction(String id) {
        return frame.getAction(id);
    }

    @Override
    public Element getXmlDescriptor() {
        return ((HasXmlDescriptor) frame).getXmlDescriptor();
    }

    @Override
    public void setXmlDescriptor(Element element) {
        ((HasXmlDescriptor) frame).setXmlDescriptor(element);
    }

    @Override
    public void setCloseable(boolean closeable) {
        ((Window)this.frame).setCloseable(closeable);
    }

    @Override
    public boolean isCloseable() {
        return ((Window)this.frame).isCloseable();
    }

    @Override
    public Screen getFrameOwner() {
        return this;
    }

    @Override
    public WindowContext getContext() {
        return (WindowContext) frame.getContext();
    }

    @Override
    public DsContext getDsContext() {
        return dsContext;
    }

    @Override
    public void setDsContext(DsContext dsContext) {
        this.dsContext = dsContext;
    }

    /**
     * @deprecated Use {@link #addCloseListener(CloseListener)}
     */
    @Deprecated
    @Override
    public void addListener(CloseListener listener) {
        addCloseListener(listener);
    }

    /**
     * @deprecated Use {@link #removeCloseListener(CloseListener)}
     */
    @Deprecated
    @Override
    public void removeListener(CloseListener listener) {
        removeCloseListener(listener);
    }

    @Override
    public void addCloseListener(CloseListener listener) {
        ((Window) frame).addCloseListener(listener);
    }

    @Override
    public void removeCloseListener(CloseListener listener) {
        ((Window) frame).removeCloseListener(listener);
    }

    @Override
    public void addCloseWithCommitListener(CloseWithCommitListener listener) {
        ((Window) frame).addCloseWithCommitListener(listener);
    }

    @Override
    public void removeCloseWithCommitListener(CloseWithCommitListener listener) {
        ((Window) frame).removeCloseWithCommitListener(listener);
    }

    /**
     * @return screen caption which is set in XML or via {@link #setCaption(String)}
     */
    @Override
    public String getCaption() {
        return frame.getCaption();
    }

    /**
     * Set the screen caption. If called in {@link #init(java.util.Map)}, overrides the value from XML.
     * @param caption   caption
     */
    @Override
    public void setCaption(String caption) {
        frame.setCaption(caption);
    }

    /**
     * Screen description is used by the framework to show some specified information, e.g. current filter or folder
     * name. We don't recommend to use it in application code.
     */
    @Override
    public String getDescription() {
        return frame.getDescription();
    }

    /**
     * Screen description is used by the framework to show some specified information, e.g. current filter or folder
     * name. We don't recommend to use it in application code.
     */
    @Override
    public void setDescription(String description) {
        frame.setDescription(description);
    }

    /** INTERNAL. Don't call from application code. */
    @Override
    public Window getWrappedWindow() {
        return (Window) frame;
    }

    /**
     * This method is called when the screen is opened to restore settings saved in the database for the current user.
     * <p>You can override it to restore custom settings.
     * <p>For example:
     * <pre>
     * public void applySettings(Settings settings) {
     *     super.applySettings(settings);
     *     String visible = settings.get(hintBox.getId()).attributeValue("visible");
     *     if (visible != null)
     *         hintBox.setVisible(Boolean.valueOf(visible));
     * }
     * </pre>
     * @param settings settings object loaded from the database for the current user
     */
    @Override
    public void applySettings(Settings settings) {
        ((Window) frame).applySettings(settings);
    }

    /**
     * This method is called when the screen is closed to save the screen settings to the database.
     */
    @Override
    public void saveSettings() {
        ((Window) frame).saveSettings();
    }

    @Override
    public void deleteSettings() {
        ((Window) frame).deleteSettings();
    }

    @Override
    public void setFocusComponent(String componentId) {
        ((Window) frame).setFocusComponent(componentId);
    }

    @Override
    public String getFocusComponent() {
        return ((Window) frame).getFocusComponent();
    }

    /**
     * @return  the screen settings interface. Never null.
     */
    @Override
    public Settings getSettings() {
        return ((Window) frame).getSettings();
    }

    @Override
    public void addTimer(Timer timer) {
        ((Window) frame).addTimer(timer);
    }

    @Override
    public Timer getTimer(String id) {
        return ((Window) frame).getTimer(id);
    }

    @Override
    public boolean validate(List<Validatable> fields) {
        return frame.validate(fields);
    }

    /**
     * Check validity by invoking validators on all components which support them
     * and show validation result notification. This method also calls {@link #postValidate(ValidationErrors)} hook to
     * support additional validation.
     * <p>You should override this method in subclasses ONLY if you want to completely replace the validation process,
     * otherwise use {@link #postValidate(ValidationErrors)}.
     * @return true if the validation was successful, false if there were any problems
     */
    @Override
    public boolean validateAll() {
        return frame.validateAll();
    }

    @Override
    public DialogOptions getDialogOptions() {
        return ((Window) frame).getDialogOptions();
    }

    /**
     * Whether automatic applying of attribute access rules enabled. If you don't want to apply attribute access
     * rules to a screen, override this method and return false.
     */
    public boolean isAttributeAccessControlEnabled() {
        return true;
    }

    /**
     * Hook to be implemented in subclasses. <br>
     * Called by the framework after the screen is fully initialized and opened. <br>
     * Override this method and put custom initialization logic here.
     */
    public void ready() {
    }

    /**
     * Hook to be implemented in subclasses. Called by {@link #validateAll()} at the end of standard validation.
     * @param errors the list of validation errors. Caller fills it by errors found during the default validation.
     * Overridden method should add into it errors found by custom validation.
     */
    protected void postValidate(ValidationErrors errors) {
    }

    /**
     * Hook to be implemented in subclasses. Called by the framework before closing the screen.
     * @param actionId  a string that is passed to one of {@link #close} methods by calling code to identify itself.
     *                  Can be an {@link Action} ID, or a constant like {@link #COMMIT_ACTION_ID} or
     *                  {@link #CLOSE_ACTION_ID}.
     * @return          true to proceed with closing, false to interrupt and leave the screen open
     */
    protected boolean preClose(String actionId) {
        return true;
    }

    /**
     * Close the screen.
     * <br> If the screen has uncommitted changes in its {@link com.haulmont.cuba.gui.data.DsContext},
     * the confirmation dialog will be shown.
     * <br> Don't override this method in subclasses, use hook {@link #preClose(String)}
     *
     * @param actionId action ID that will be propagated to attached {@link CloseListener}s.
     *                 Use {@link #COMMIT_ACTION_ID} if some changes have just been committed, or
     *                 {@link #CLOSE_ACTION_ID} otherwise. These constants are recognized by various mechanisms of the
     *                 framework.
     */
    @Override
    public boolean close(String actionId) {
        return ((Window) frame).close(actionId);
    }

    /** 
     * Close the screen.
     * <br> If the window has uncommitted changes in its {@link com.haulmont.cuba.gui.data.DsContext},
     * and force=false, the confirmation dialog will be shown.
     * <br> Don't override this method in subclasses, use hook {@link #preClose(String)}
     *
     * @param actionId action ID that will be propagated to attached {@link CloseListener}s.
     *                 Use {@link #COMMIT_ACTION_ID} if some changes have just been committed, or
     *                 {@link #CLOSE_ACTION_ID} otherwise. These constants are recognized by various mechanisms of the
     *                 framework.
     * @param force    if true, no confirmation dialog will be shown even if the screen has uncommitted changes
     */
    @Override
    public boolean close(String actionId, boolean force) {
        return ((Window) frame).close(actionId, force);
    }

    /** INTERNAL. Don't call or override in application code. */
    @Override
    public void closeAndRun(String actionId, Runnable runnable) {
        ((Window) frame).closeAndRun(actionId, runnable);
    }

    @Override
    public ActionsPermissions getActionsPermissions() {
        if (frame instanceof SecuredActionsHolder) {
            return ((SecuredActionsHolder) frame).getActionsPermissions();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ContentSwitchMode getContentSwitchMode() {
        return ((Window) frame).getContentSwitchMode();
    }

    @Override
    public void setContentSwitchMode(ContentSwitchMode mode) {
        ((Window) frame).setContentSwitchMode(mode);
    }

    @Override
    public void addBeforeCloseWithShortcutListener(BeforeCloseWithShortcutListener listener) {
        ((Window) frame).addBeforeCloseWithShortcutListener(listener);
    }

    @Override
    public void removeBeforeCloseWithShortcutListener(BeforeCloseWithShortcutListener listener) {
        ((Window) frame).removeBeforeCloseWithShortcutListener(listener);
    }

    @Override
    public void addBeforeCloseWithCloseButtonListener(BeforeCloseWithCloseButtonListener listener) {
        ((Window) frame).addBeforeCloseWithCloseButtonListener(listener);
    }

    @Override
    public void removeBeforeCloseWithCloseButtonListener(BeforeCloseWithCloseButtonListener listener) {
        ((Window) frame).removeBeforeCloseWithCloseButtonListener(listener);
    }
}