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

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.gui.screen.LegacyFrame;
import com.haulmont.cuba.gui.settings.Settings;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

// todo get rid of it
public class WindowDelegate {

    public static final String LOOKUP_ITEM_CLICK_ACTION_ID = "lookupItemClickAction";
    public static final String LOOKUP_ENTER_PRESSED_ACTION_ID = "lookupEnterPressed";
    public static final String LOOKUP_SELECT_ACTION_ID = "lookupSelectAction";
    public static final String LOOKUP_CANCEL_ACTION_ID = "lookupCancelAction";

    protected Window window;
    protected Window wrapper;
    protected Settings settings;

    protected WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);

    private final Logger log = LoggerFactory.getLogger(WindowDelegate.class);

    public WindowDelegate(Window window) {
        this.window = window;
    }

    // fixme move to WindowManager
    public Window wrapBy(Class<?> wrapperClass) {
        Constructor<?> constructor;
        try {
            constructor = wrapperClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to get constructor for screen class " + wrapperClass);
        }

        try {
            wrapper = (Window) constructor.newInstance();
            ((AbstractFrame) wrapper).setWrappedFrame(window);
            return wrapper;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to init window controller", e);
        }
    }

    public Window getWrapper() {
        return wrapper;
    }

    @Deprecated
    public Datasource getDatasource() {
        Datasource ds = null;
        Element element = ((Component.HasXmlDescriptor) window).getXmlDescriptor();
        String datasourceName = element.attributeValue("datasource");
        if (!StringUtils.isEmpty(datasourceName)) {
            DsContext context = LegacyFrame.of(window).getDsContext();
            if (context != null) {
                ds = context.get(datasourceName);
            }
        }

        if (ds == null) {
            throw new GuiDevelopmentException("Can't find main datasource", window.getId());
        }

        return ds;
    }

    public Settings getSettings() {
        return settings;
    }

    public void saveSettings() {
        if (settings != null) {
            ComponentsHelper.walkComponents(
                    window,
                    (component, name) -> {
                        if (component.getId() != null
                                && component instanceof HasSettings) {
                            log.trace("Saving settings for : {} : {}", name, component);

                            Element e = WindowDelegate.this.settings.get(name);
                            boolean modified = ((HasSettings) component).saveSettings(e);

                            if (component instanceof HasPresentations
                                    && ((HasPresentations) component).isUsePresentations()) {
                                Object def = ((HasPresentations) component).getDefaultPresentationId();
                                e.addAttribute("presentation", def != null ? def.toString() : "");
                                Presentations presentations = ((HasPresentations) component).getPresentations();
                                if (presentations != null) {
                                    presentations.commit();
                                }
                            }
                            WindowDelegate.this.settings.setModified(modified);
                        }
                    }
            );
            settings.commit();
        }
    }

    public void deleteSettings() {
        settings.delete();
    }

    public void applySettings(Settings settings) {
        this.settings = settings;
        ComponentsHelper.walkComponents(
                window,
                (component, name) -> {
                    if (component.getId() != null
                            && component instanceof HasSettings) {
                        log.trace("Applying settings for : {} : {} ", name, component);

                        Element e = WindowDelegate.this.settings.get(name);
                        ((HasSettings) component).applySettings(e);

                        if (component instanceof HasPresentations
                                && e.attributeValue("presentation") != null) {
                            final String def = e.attributeValue("presentation");
                            if (!StringUtils.isEmpty(def)) {
                                UUID defaultId = UUID.fromString(def);
                                ((HasPresentations) component).applyPresentationAsDefault(defaultId);
                            }
                        }
                    }
                }
        );
    }

    public void disposeComponents() {
        ComponentsHelper.walkComponents(
                window,
                (component, name) -> {
                    if (component instanceof Component.Disposable) {
                        ((Component.Disposable) component).dispose();
                    }
                }
        );
    }

    public boolean isValid() {
        Collection<Component> components = ComponentsHelper.getComponents(window);
        for (Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit() && !validatable.isValid())
                    return false;
            }
        }
        return true;
    }

    public void validate() throws ValidationException {
        Collection<Component> components = ComponentsHelper.getComponents(window);
        for (Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit()) {
                    validatable.validate();
                }
            }
        }
    }

    public void postValidate(ValidationErrors errors) {
        if (wrapper instanceof AbstractWindow) {
            ((AbstractWindow) wrapper).postValidate(errors);
        }
    }

    public void showValidationErrors(ValidationErrors errors) {
        if (wrapper instanceof AbstractWindow) {
            ((AbstractWindow) wrapper).showValidationErrors(errors);
        }
    }

    public boolean preClose(String actionId) {
        if (wrapper instanceof AbstractWindow) {
            return ((AbstractWindow) wrapper).preClose(actionId);
        }

        return true;
    }

    public boolean isModified() {
        if (wrapper instanceof Window.Committable)
            return ((Window.Committable) wrapper).isModified();
        else
            return LegacyFrame.of(window).getDsContext() != null && LegacyFrame.of(window).getDsContext().isModified();
    }

    public Window openWindow(String windowAlias, OpenType openType, Map<String, Object> params) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openWindow(windowInfo, openType, params);
    }

    public Window openWindow(String windowAlias, OpenType openType) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openWindow(windowInfo, openType);
    }

    public Window.Editor openEditor(Entity item, OpenType openType) {
        WindowInfo editorScreen = windowConfig.getEditorScreen(item);
        return window.getWindowManager().openEditor(editorScreen, item, openType);
    }

    public Window.Editor openEditor(Entity item, OpenType openType, Map<String, Object> params) {
        WindowInfo editorScreen = windowConfig.getEditorScreen(item);
        return window.getWindowManager().openEditor(editorScreen, item, openType, params);
    }

    public Window.Editor openEditor(Entity item, OpenType openType, Map<String, Object> params, Datasource parentDs) {
        WindowInfo editorScreen = windowConfig.getEditorScreen(item);
        return window.getWindowManager().openEditor(editorScreen, item, openType, params, parentDs);
    }

    public Window.Editor openEditor(String windowAlias, Entity item, OpenType openType, Map<String, Object> params, Datasource parentDs) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openEditor(windowInfo, item, openType, params, parentDs);
    }

    public Window.Editor openEditor(String windowAlias, Entity item, OpenType openType, Map<String, Object> params) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openEditor(windowInfo, item, openType, params);
    }

    public Window.Editor openEditor(String windowAlias, Entity item, OpenType openType, Datasource parentDs) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openEditor(windowInfo, item, openType, parentDs);
    }

    public Window.Editor openEditor(String windowAlias, Entity item, OpenType openType) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openEditor(windowInfo, item, openType);
    }

    public Window.Lookup openLookup(Class<? extends Entity>  entityClass, Window.Lookup.Handler handler, OpenType openType) {
        WindowInfo lookupScreen = windowConfig.getLookupScreen(entityClass);
        return window.getWindowManager().openLookup(lookupScreen, handler, openType);
    }

    public Window.Lookup openLookup(Class<? extends Entity>  entityClass, Window.Lookup.Handler handler, OpenType openType, Map<String, Object> params) {
        WindowInfo lookupScreen = windowConfig.getLookupScreen(entityClass);
        return window.getWindowManager().openLookup(lookupScreen, handler, openType, params);
    }

    public Window.Lookup openLookup(String windowAlias, Window.Lookup.Handler handler, OpenType openType, Map<String, Object> params) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openLookup(windowInfo, handler, openType, params);
    }

    public Window.Lookup openLookup(String windowAlias, Window.Lookup.Handler handler, OpenType openType) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        return window.getWindowManager().openLookup(windowInfo, handler, openType);
    }

    public Frame openFrame(Component parent, String windowAlias) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        Frame wrappedFrame = ((Frame.Wrapper) wrapper).getWrappedFrame();
        return window.getWindowManager().openFrame(wrappedFrame, parent, windowInfo);
    }

    public Frame openFrame(Component parent, String windowAlias, Map<String, Object> params) {
        WindowInfo windowInfo = windowConfig.getWindowInfo(windowAlias);
        Frame wrappedFrame = ((Frame.Wrapper) wrapper).getWrappedFrame();
        return window.getWindowManager().openFrame(wrappedFrame, parent, windowInfo, params);
    }
}