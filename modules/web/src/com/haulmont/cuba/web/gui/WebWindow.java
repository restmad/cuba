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
package com.haulmont.cuba.web.gui;

import com.haulmont.bali.events.EventRouter;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.LookupComponent.LookupSelectionChangeNotifier;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.security.ActionsPermissions;
import com.haulmont.cuba.gui.components.sys.FrameImplementation;
import com.haulmont.cuba.gui.components.sys.WindowImplementation;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.ScreenUtils;
import com.haulmont.cuba.gui.sys.TestIdManager;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.gui.components.WebFrameActionsHolder;
import com.haulmont.cuba.web.gui.components.WebWrapperUtils;
import com.haulmont.cuba.web.widgets.CubaSingleModeContainer;
import com.haulmont.cuba.web.widgets.CubaVerticalActionsLayout;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

public class WebWindow implements Window, Component.Wrapper,
                                  Component.HasXmlDescriptor, WrappedWindow, Component.Disposable,
                                  SecuredActionsHolder, Component.HasIcon,
        FrameImplementation, WindowImplementation {

    protected static final String C_WINDOW_LAYOUT = "c-window-layout";

    private static final Logger log = LoggerFactory.getLogger(WebWindow.class);

    protected String id;
    protected String debugId;

    protected List<Component> ownComponents = new ArrayList<>();
    protected Map<String, Component> allComponents = new HashMap<>();

    protected List<Timer> timers = null; // lazy initialized timers list

    protected String focusComponentId;

    protected com.vaadin.ui.Component component;

    protected Screen frameOwner;

    protected Element element;

    protected WindowContext context;

    protected String icon;
    protected String caption;
    protected String description;

    protected WebFrameActionsHolder actionsHolder = new WebFrameActionsHolder(this);
    protected ActionsPermissions actionsPermissions = new ActionsPermissions(this);

    protected Messages messages;
    protected Icons icons;

    protected boolean disposed = false;
    protected DialogOptions dialogOptions = new WebDialogOptions();
    protected ContentSwitchMode contentSwitchMode = ContentSwitchMode.DEFAULT;
    protected boolean closeable = true;
    // todo remove
    private EventRouter eventRouter;

    public WebWindow() {
        component = createLayout();
        if (component instanceof com.vaadin.event.Action.Container) {
            ((com.vaadin.event.Action.Container) component).addActionHandler(actionsHolder);
        }

        setupEventListeners();
    }

    @Inject
    protected void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Inject
    protected void setIcons(Icons icons) {
        this.icons = icons;
    }

    protected void setupEventListeners() {
        component.addAttachListener(event -> enableEventListeners());
        component.addDetachListener(event -> disableEventListeners());
    }

    protected void disableEventListeners() {
        // todo
        /*Frame wrapper = delegate.getWrapper();
        if (wrapper != null) {
            List<ApplicationListener> uiEventListeners = ((AbstractFrame) wrapper).getUiEventListeners();
            if (uiEventListeners != null) {
                AppUI ui = AppUI.getCurrent();
                UiEventsMulticaster multicaster = ui.getUiEventsMulticaster();

                for (ApplicationListener listener : uiEventListeners) {
                    multicaster.removeApplicationListener(listener);
                }
            }
        }*/
    }

    protected void enableEventListeners() {
        // todo
        /*Frame wrapper = delegate.getWrapper();
        if (wrapper != null) {
            List<ApplicationListener> uiEventListeners = ((AbstractFrame) wrapper).getUiEventListeners();
            if (uiEventListeners != null) {
                AppUI ui = AppUI.getCurrent();
                UiEventsMulticaster multicaster = ui.getUiEventsMulticaster();

                for (ApplicationListener listener : uiEventListeners) {
                    multicaster.addApplicationListener(listener);
                }
            }
        }*/
    }

    protected com.vaadin.ui.ComponentContainer createLayout() {
        CubaVerticalActionsLayout layout = new CubaVerticalActionsLayout();
        layout.setStyleName(C_WINDOW_LAYOUT);
        layout.setSizeFull();
        return layout;
    }

    protected com.vaadin.ui.ComponentContainer getContainer() {
        return (com.vaadin.ui.ComponentContainer) component;
    }

    @Nullable
    protected com.vaadin.ui.Window asDialogWindow() {
        if (component.isAttached()) {
            com.vaadin.ui.Component parent = component;
            while (parent != null) {
                if (parent instanceof com.vaadin.ui.Window) {
                    return (com.vaadin.ui.Window) parent;
                }

                parent = parent.getParent();
            }
        }
        return null;
    }

    @Override
    public void registerComponent(Component component) {
        if (component.getId() != null) {
            allComponents.put(component.getId(), component);
        }
    }

    @Override
    public void unregisterComponent(Component component) {
        if (component.getId() != null) {
            allComponents.remove(component.getId());
        }
    }

    @Nullable
    @Override
    public Component getRegisteredComponent(String id) {
        return allComponents.get(id);
    }

    @Override
    public String getStyleName() {
        return StringUtils.normalizeSpace(component.getStyleName().replace(C_WINDOW_LAYOUT, ""));
    }

    @Override
    public void setStyleName(String name) {
        getContainer().setStyleName(name);

        getContainer().addStyleName(C_WINDOW_LAYOUT);
    }

    @Override
    public void addStyleName(String styleName) {
        getContainer().addStyleName(styleName);
    }

    @Override
    public void removeStyleName(String styleName) {
        getContainer().removeStyleName(styleName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(Class<X> internalComponentClass) {
        return (X) getComponent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrapComposition(Class<X> internalCompositionClass) {
        return (X) getComposition();
    }

    @Override
    public boolean getSpacing() {
        if (getContainer() instanceof Layout.SpacingHandler) {
            return ((Layout.SpacingHandler) getContainer()).isSpacing();
        }
        return false;
    }

    @Override
    public void setSpacing(boolean enabled) {
        if (getContainer() instanceof Layout.SpacingHandler) {
            ((Layout.SpacingHandler) getContainer()).setSpacing(true);
        }
    }

    @Override
    public com.haulmont.cuba.gui.components.MarginInfo getMargin() {
        if (getContainer() instanceof Layout.MarginHandler) {
            MarginInfo vMargin = ((Layout.MarginHandler) getContainer()).getMargin();
            return new com.haulmont.cuba.gui.components.MarginInfo(vMargin.hasTop(), vMargin.hasRight(), vMargin.hasBottom(), vMargin.hasLeft());
        }
        return new com.haulmont.cuba.gui.components.MarginInfo(false);
    }

    @Override
    public void setMargin(com.haulmont.cuba.gui.components.MarginInfo marginInfo) {
        if (getContainer() instanceof Layout.MarginHandler) {
            MarginInfo vMargin = new MarginInfo(marginInfo.hasTop(), marginInfo.hasRight(), marginInfo.hasBottom(),
                    marginInfo.hasLeft());
            ((Layout.MarginHandler) getContainer()).setMargin(vMargin);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void addAction(Action action) {
        checkNotNullArgument(action, "action must be non null");

        actionsHolder.addAction(action);
        actionsPermissions.apply(action);

        // force update of actions on client side
        if (action.getShortcutCombination() != null) {
            component.markAsDirty();
        }
    }

    @Override
    public void addAction(Action action, int index) {
        checkNotNullArgument(action, "action must be non null");

        actionsHolder.addAction(action, index);
        actionsPermissions.apply(action);

        // force update of actions on client side
        if (action.getShortcutCombination() != null) {
            component.markAsDirty();
        }
    }

    @Override
    public void removeAction(@Nullable com.haulmont.cuba.gui.components.Action action) {
        actionsHolder.removeAction(action);
    }

    @Override
    public void removeAction(@Nullable String id) {
        actionsHolder.removeAction(id);
    }

    @Override
    public void removeAllActions() {
        actionsHolder.removeAllActions();
    }

    @Override
    public Collection<com.haulmont.cuba.gui.components.Action> getActions() {
        return actionsHolder.getActions();
    }

    @Override
    @Nullable
    public com.haulmont.cuba.gui.components.Action getAction(String id) {
        return actionsHolder.getAction(id);
    }

    @Override
    public boolean isValid() {
        Collection<Component> components = ComponentsHelper.getComponents(this);
        for (Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit() && !validatable.isValid())
                    return false;
            }
        }
        return true;
    }

    @Override
    public void validate() throws ValidationException {
        Collection<Component> components = ComponentsHelper.getComponents(this);
        for (Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit()) {
                    validatable.validate();
                }
            }
        }
    }

    @Override
    public boolean validate(List<Validatable> fields) {
        ValidationErrors errors = new ValidationErrors();

        for (Validatable field : fields) {
            try {
                field.validate();
            } catch (ValidationException e) {
                if (log.isTraceEnabled())
                    log.trace("Validation failed", e);
                else if (log.isDebugEnabled())
                    log.debug("Validation failed: " + e);

                ComponentsHelper.fillErrorMessages(field, e, errors);
            }
        }

        return handleValidationErrors(errors);
    }

    @Override
    public boolean validateAll() {
        ValidationErrors errors = new ValidationErrors();

        Collection<Component> components = ComponentsHelper.getComponents(this);
        for (Component component : components) {
            if (component instanceof Validatable) {
                Validatable validatable = (Validatable) component;
                if (validatable.isValidateOnCommit()) {
                    try {
                        validatable.validate();
                    } catch (ValidationException e) {
                        if (log.isTraceEnabled()) {
                            log.trace("Validation failed", e);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Validation failed: " + e);
                        }
                        ComponentsHelper.fillErrorMessages(validatable, e, errors);
                    }
                }
            }
        }

        validateAdditionalRules(errors);

        return handleValidationErrors(errors);
    }

    protected void validateAdditionalRules(ValidationErrors errors) {
    }

    protected boolean handleValidationErrors(ValidationErrors errors) {
//        delegate.postValidate(errors); todo

        if (errors.isEmpty())
            return true;

//        delegate.showValidationErrors(errors); todo

        WebComponentsHelper.focusProblemComponent(errors);

        return false;
    }

    @Override
    public DialogOptions getDialogOptions() {
        return dialogOptions;
    }

    @Override
    public boolean isCloseable() {
        return closeable;
    }

    @Override
    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

    @Override
    public Screen getFrameOwner() {
        return frameOwner;
    }

    @Override
    public void setFrameOwner(Screen controller) {
        this.frameOwner = controller;
    }

    @Override
    public WindowContext getContext() {
        return context;
    }

    @Override
    public void setContext(FrameContext ctx) {
        this.context = (WindowContext) ctx;
    }

    protected com.vaadin.ui.Component.Focusable getComponentToFocus(com.vaadin.ui.ComponentContainer container) {
        for (com.vaadin.ui.Component child : container) {
            if (child instanceof Panel) {
                child = ((Panel) child).getContent();
            }
            if (child instanceof TabSheet) {
                // #PL-3176
                // we don't know about selected tab after request
                // may be focused component lays on not selected tab
                // it may break component tree
                continue;
            }
            if (child instanceof com.vaadin.ui.ComponentContainer) {
                com.vaadin.ui.Component.Focusable result = getComponentToFocus((com.vaadin.ui.ComponentContainer) child);
                if (result != null) {
                    return result;
                }
            } else {
                if (child instanceof com.vaadin.ui.Component.Focusable
//                        vaadin8 implement
//                        && !child.isReadOnly()
                        && WebComponentsHelper.isComponentVisible(child)
                        && WebComponentsHelper.isComponentEnabled(child)
                        && !(child instanceof Button)) {

                    return (com.vaadin.ui.Component.Focusable) child;
                }
            }
        }
        return null;
    }

    @Override
    public String getFocusComponent() {
        return focusComponentId;
    }

    @Override
    public void setFocusComponent(String componentId) {
        this.focusComponentId = componentId;

        if (componentId != null) {
            Component focusComponent = getComponent(componentId);
            if (focusComponent instanceof Focusable) {
                ((Focusable) focusComponent).focus();
            } else {
                log.error("Can't find focus component: {}", componentId);
            }
        } else {
            findAndFocusChildComponent();
        }
    }

    /**
     * Use EventRouter for listeners instead of fields with listeners List.
     *
     * @return lazily initialized {@link EventRouter} instance.
     * @see EventRouter
     */
    protected EventRouter getEventRouter() {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        return eventRouter;
    }

    @Override
    public void addBeforeCloseWithShortcutListener(BeforeCloseWithShortcutListener listener) {
        getEventRouter().addListener(BeforeCloseWithShortcutListener.class, listener);
    }

    @Override
    public void removeBeforeCloseWithShortcutListener(BeforeCloseWithShortcutListener listener) {
        getEventRouter().removeListener(BeforeCloseWithShortcutListener.class, listener);
    }

    public void fireBeforeCloseWithShortcut(BeforeCloseWithShortcutEvent event) {
        getEventRouter().fireEvent(BeforeCloseWithShortcutListener.class,
                BeforeCloseWithShortcutListener::beforeCloseWithShortcut, event);
    }

    @Override
    public void addBeforeCloseWithCloseButtonListener(BeforeCloseWithCloseButtonListener listener) {
        getEventRouter().addListener(BeforeCloseWithCloseButtonListener.class, listener);
    }

    @Override
    public void removeBeforeCloseWithCloseButtonListener(BeforeCloseWithCloseButtonListener listener) {
        getEventRouter().removeListener(BeforeCloseWithCloseButtonListener.class, listener);
    }

    public void fireBeforeCloseWithCloseButton(BeforeCloseWithCloseButtonEvent event) {
        getEventRouter().fireEvent(BeforeCloseWithCloseButtonListener.class,
                BeforeCloseWithCloseButtonListener::beforeCloseWithCloseButton, event);
    }

    @Override
    public void addTimer(Timer timer) {
        if (component.isAttached()) {
            attachTimerToUi((WebTimer) timer);
        } else {
            component.addAttachListener(new ClientConnector.AttachListener() {
                @Override
                public void attach(ClientConnector.AttachEvent event) {
                    if (timers.contains(timer)) {
                        attachTimerToUi((WebTimer) timer);
                    }
                    // execute attach listener only once
                    component.removeAttachListener(this);
                }
            });
        }

        if (timers == null) {
            timers = new ArrayList<>(2);
        }
        timers.add(timer);
    }

    protected void attachTimerToUi(WebTimer timer) {
        AppUI appUI = (AppUI) component.getUI();
        appUI.addTimer(timer.getTimerImpl());
    }

    @Override
    public Timer getTimer(String id) {
        if (timers == null) {
            return null;
        }

        return timers.stream()
                .filter(timer -> Objects.equals(timer.getId(), id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Completely stop and remove timers of the window.
     */
    public void stopTimers() {
        AppUI appUI = AppUI.getCurrent();
        if (timers != null) {
            for (Timer timer : timers) {
                timer.stop();
                WebTimer webTimer = (WebTimer) timer;
                appUI.removeTimer(webTimer.getTimerImpl());
            }
            timers.clear();
        }
    }

    @Override
    public Element getXmlDescriptor() {
        return element;
    }

    @Override
    public void setXmlDescriptor(Element element) {
        this.element = element;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public WindowManager getWindowManager() {
        return (WindowManager) ScreenUtils.getScreenContext(getFrameOwner()).getScreens();
    }

    @Override
    public void add(Component childComponent) {
        add(childComponent, ownComponents.size());
    }

    @Override
    public void add(Component childComponent, int index) {
        if (childComponent.getParent() != null && childComponent.getParent() != this) {
            throw new IllegalStateException("Component already has parent");
        }

        if (ownComponents.contains(childComponent)) {
            com.vaadin.ui.Component composition = WebComponentsHelper.getComposition(childComponent);
            int existingIndex = ((AbstractOrderedLayout)getContainer()).getComponentIndex(composition);
            if (index > existingIndex) {
                index--;
            }

            remove(childComponent);
        }

        com.vaadin.ui.ComponentContainer container = getContainer();
        com.vaadin.ui.Component vComponent = WebComponentsHelper.getComposition(childComponent);
        ((AbstractOrderedLayout)container).addComponent(vComponent, index);

        com.vaadin.ui.Alignment alignment = WebWrapperUtils.toVaadinAlignment(childComponent.getAlignment());
        ((AbstractOrderedLayout) container).setComponentAlignment(vComponent, alignment);

        if (childComponent instanceof BelongToFrame
                && ((BelongToFrame) childComponent).getFrame() == null) {
            ((BelongToFrame) childComponent).setFrame(this);
        } else {
            registerComponent(childComponent);
        }

        if (index == ownComponents.size()) {
            ownComponents.add(childComponent);
        } else {
            ownComponents.add(index, childComponent);
        }

        childComponent.setParent(this);
    }

    @Override
    public int indexOf(Component component) {
        return ownComponents.indexOf(component);
    }

    @Nullable
    @Override
    public Component getComponent(int index) {
        return ownComponents.get(index);
    }

    @Override
    public void remove(Component childComponent) {
        getContainer().removeComponent(WebComponentsHelper.getComposition(childComponent));
        ownComponents.remove(childComponent);

        childComponent.setParent(null);
    }

    @Override
    public void removeAll() {
        getContainer().removeAllComponents();
        for (Component childComponent : ownComponents) {
            if (childComponent.getId() != null) {
                allComponents.remove(childComponent.getId());
            }
        }

        Component[] childComponents = ownComponents.toArray(new Component[0]);
        ownComponents.clear();

        for (Component ownComponent : childComponents) {
            ownComponent.setParent(null);
        }
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return Collections.unmodifiableCollection(ownComponents);
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;

        AppUI ui = AppUI.getCurrent();
        if (ui != null
                && ui.isTestMode()
                && StringUtils.isEmpty(debugId)) {
            getComponent().setId(ui.getTestIdManager().getTestId("window_" + id));
        }
    }

    @Override
    public Component getParent() {
        return null;
    }

    @Override
    public void setParent(Component parent) {
    }

    @Override
    public boolean isEnabled() {
        return component.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        component.setEnabled(enabled);
    }

    @Override
    public boolean isResponsive() {
        com.vaadin.ui.ComponentContainer container = getContainer();

        return container instanceof AbstractComponent
                && ((AbstractComponent) container).isResponsive();
    }

    @Override
    public void setResponsive(boolean responsive) {
        com.vaadin.ui.ComponentContainer container = getContainer();

        if (container instanceof AbstractComponent) {
            ((AbstractComponent) container).setResponsive(responsive);
        }
    }

    @Override
    public boolean isVisible() {
        return getComposition().isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisibleRecursive() {
        return isVisible(); // vaadin8 is this correct?
    }

    @Override
    public boolean isEnabledRecursive() {
        return isEnabled(); // vaadin8 is this correct?
    }

    @Override
    public float getHeight() {
        return component.getHeight();
    }

    @Override
    public void setHeight(String height) {
        component.setHeight(height);
    }

    @Override
    public SizeUnit getHeightSizeUnit() {
        return WebWrapperUtils.toSizeUnit(component.getHeightUnits());
    }

    @Override
    public float getWidth() {
        return component.getWidth();
    }

    @Override
    public void setWidth(String width) {
        component.setWidth(width);
    }

    @Override
    public SizeUnit getWidthSizeUnit() {
        return WebWrapperUtils.toSizeUnit(component.getWidthUnits());
    }

    @Override
    public Component getOwnComponent(String id) {
        Component nestedComponent = allComponents.get(id);
        if (ownComponents.contains(nestedComponent)) {
            return nestedComponent;
        }

        return null;
    }

    @Nullable
    @Override
    public Component getComponent(String id) {
        return ComponentsHelper.getWindowComponent(this, id);
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.MIDDLE_CENTER;
    }

    @Override
    public void setAlignment(Alignment alignment) {
    }

    @Override
    public void expand(Component component, String height, String width) {
        final com.vaadin.ui.Component expandedComponent = WebComponentsHelper.getComposition(component);
        if (getContainer() instanceof AbstractOrderedLayout) {
            WebComponentsHelper.expand((AbstractOrderedLayout) getContainer(), expandedComponent, height, width);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void expand(Component component) {
        expand(component, "", "");
    }

    @Override
    public void resetExpanded() {
        if (getContainer() instanceof AbstractOrderedLayout) {
            AbstractOrderedLayout container = (AbstractOrderedLayout) getContainer();

            for (com.vaadin.ui.Component child : container) {
                container.setExpandRatio(child, 0.0f);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isExpanded(Component component) {
        return ownComponents.contains(component) && WebComponentsHelper.isComponentExpanded(component);
    }

    @Override
    public ExpandDirection getExpandDirection() {
        return ExpandDirection.VERTICAL;
    }

    @Override
    public com.vaadin.ui.Component getComponent() {
        return component;
    }

    @Override
    public com.vaadin.ui.Component getComposition() {
        return component;
    }

    public boolean findAndFocusChildComponent() {
        com.vaadin.ui.Component.Focusable focusComponent = getComponentToFocus(getContainer());
        if (focusComponent != null) {
            focusComponent.focus();
            return true;
        }
        return false;
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public void setCaption(String caption) {
        this.caption = caption;

        if (component.isAttached()) {
            TabSheet.Tab tabWindow = asTabWindow();
            if (tabWindow != null) {
                setTabCaptionAndDescription(tabWindow);
                // todo
                // windowManagerImpl.getBreadCrumbs((com.vaadin.ui.ComponentContainer) tabWindow.getComponent()).update();
            } else {
                Layout singleModeWindow = asSingleWindow();
                if (singleModeWindow != null) {
                    // todo
                    // windowManagerImpl.getBreadCrumbs(singleModeWindow).update();
                }
            }
        }
    }

    @Nullable
    protected TabSheet.Tab asTabWindow() {
        if (component.isAttached()) {
            com.vaadin.ui.Component parent = component;
            while (parent != null) {
                if (parent.getParent() instanceof TabSheet) {
                    return ((TabSheet) parent.getParent()).getTab(parent);
                }

                parent = parent.getParent();
            }
        }
        return null;
    }

    @Nullable
    protected Layout asSingleWindow() {
        if (component.isAttached()) {
            com.vaadin.ui.Component parent = component;
            while (parent != null) {
                if (parent.getParent() instanceof CubaSingleModeContainer) {
                    return (Layout) parent;
                }

                parent = parent.getParent();
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;

        if (component.isAttached()) {
            TabSheet.Tab tabWindow = asTabWindow();
            if (tabWindow != null) {
                setTabCaptionAndDescription(tabWindow);

                // todo
                // windowManagerImpl.getBreadCrumbs((com.vaadin.ui.ComponentContainer) tabWindow.getComponent()).update();
            }
        }
    }

    // todo move to WebTabWindow
    protected void setTabCaptionAndDescription(TabSheet.Tab tabWindow) {
        //
    }

    @Override
    public Frame getFrame() {
        return this;
    }

    @Override
    public void setFrame(Frame frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Window getWrapper() {
        return ((Window) frameOwner);
    }

    @Override
    public void dispose() {
        stopTimers();

        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public ActionsPermissions getActionsPermissions() {
        return actionsPermissions;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public void setIconFromSet(Icons.Icon icon) {
        setIcon(icons.get(icon));
    }

    @Override
    public ContentSwitchMode getContentSwitchMode() {
        return contentSwitchMode;
    }

    @Override
    public void setContentSwitchMode(ContentSwitchMode mode) {
        Preconditions.checkNotNullArgument(mode, "Content switch mode can't be null. " +
                "Use ContentSwitchMode.DEFAULT option instead");

        MainTabSheetMode tabSheetMode = AppBeans.get(Configuration.class)
                .getConfig(WebConfig.class)
                .getMainTabSheetMode();
        if (tabSheetMode != MainTabSheetMode.MANAGED) {
            log.debug("Content switch mode can be set only for the managed main TabSheet. Current invocation will be ignored.");
        }

        this.contentSwitchMode = mode;
    }

    // todo remove
    /*public static class Editor extends WebWindow implements Window.Editor {
        @Override
        public Entity getItem() {
            return null; // todo
            // return ((EditorWindowDelegate) delegate).getItem();
        }

        @Override
        public void setItem(Entity item) {
//            ((EditorWindowDelegate) delegate).setItem(item);
            // todo
        }

        @Override
        protected boolean onClose(String actionId) {
            releaseLock();
            return super.onClose(actionId);
        }

        public void releaseLock() {
            // todo
//            ((EditorWindowDelegate) delegate).releaseLock();
        }

        @Nullable
        @Override
        public Datasource getParentDs() {
            return ((EditorWindowDelegate) delegate).getParentDs();
        }

        @Override
        public void setParentDs(Datasource parentDs) {
            ((EditorWindowDelegate) delegate).setParentDs(parentDs);
        }

        protected MetaClass getMetaClass() {
            return getDatasource().getMetaClass();
        }

        protected Datasource getDatasource() {
            return delegate.getDatasource();
        }

        protected MetaClass getMetaClass(Object item) {
            final MetaClass metaClass;
            if (item instanceof Datasource) {
                metaClass = ((Datasource) item).getMetaClass();
            } else {
                metaClass = ((Instance) item).getMetaClass();
            }
            return metaClass;
        }

        protected Instance getInstance(Object item) {
            if (item instanceof Datasource) {
                return ((Datasource) item).getItem();
            } else {
                return (Instance) item;
            }
        }

        @Override
        public boolean commit() {
            return commit(true);
        }

        @Override
        public boolean commit(boolean validate) {
            if (validate && !getWrapper().validateAll())
                return false;

            return ((EditorWindowDelegate) delegate).commit(false);
        }

        @Override
        public void commitAndClose() {
            if (!getWrapper().validateAll())
                return;

            if (((EditorWindowDelegate) delegate).commit(true))
                close(COMMIT_ACTION_ID);
        }

        @Override
        public boolean isLocked() {
            return ((EditorWindowDelegate) delegate).isLocked();
        }

        @Override
        public boolean isCrossFieldValidate() {
            return ((EditorWindowDelegate) delegate).isCrossFieldValidate();
        }

        @Override
        public void setCrossFieldValidate(boolean crossFieldValidate) {
            ((EditorWindowDelegate) delegate).setCrossFieldValidate(crossFieldValidate);
        }

        @Override
        protected void validateAdditionalRules(ValidationErrors errors) {
            ((EditorWindowDelegate) delegate).validateAdditionalRules(errors);
        }
    }*/

    // todo remove
    public static class Lookup extends WebWindow implements Window.Lookup, LookupComponent.LookupSelectionChangeListener {

        protected Handler handler;

        protected Validator validator;

        protected Component lookupComponent;
        protected VerticalLayout container;
        protected CubaVerticalActionsLayout rootLayout;

        public Lookup() {
            addAction(new SelectAction((AbstractLookup) this.getFrameOwner())); // todo

            // todo use BaseAction instead
            addAction(new AbstractAction(WindowDelegate.LOOKUP_CANCEL_ACTION_ID) {
                @Override
                public void actionPerform(Component component) {
                    close("cancel");
                }
            });
        }

        @Nullable
        protected Action getSelectAction() {
            return getAction(WindowDelegate.LOOKUP_SELECT_ACTION_ID);
        }

        @Nullable
        protected Action getCancelAction() {
            return getAction(WindowDelegate.LOOKUP_CANCEL_ACTION_ID);
        }

        @Override
        public com.haulmont.cuba.gui.components.Component getLookupComponent() {
            return lookupComponent;
        }

        @Override
        public void setLookupComponent(Component lookupComponent) {
            Action selectAction = getSelectAction();
            if (this.lookupComponent instanceof LookupSelectionChangeNotifier) {
                LookupSelectionChangeNotifier lvChangeNotifier = (LookupSelectionChangeNotifier) this.lookupComponent;
                lvChangeNotifier.removeLookupValueChangeListener(this);

                if (selectAction != null)
                    selectAction.setEnabled(true);
            }

            this.lookupComponent = lookupComponent;

            if (lookupComponent instanceof LookupComponent) {
                ((LookupComponent) lookupComponent).setLookupSelectHandler(() -> {
                    if (selectAction != null)
                        selectAction.actionPerform(null);
                });

                if (lookupComponent instanceof LookupSelectionChangeNotifier) {
                    LookupSelectionChangeNotifier lvChangeNotifier =
                            (LookupSelectionChangeNotifier) lookupComponent;
                    lvChangeNotifier.addLookupValueChangeListener(this);

                    if (selectAction != null)
                        selectAction.setEnabled(!lvChangeNotifier.getLookupSelectedItems().isEmpty());
                }
            }
        }

        @Override
        public void lookupValueChanged(LookupComponent.LookupSelectionChangeEvent event) {
            Action selectAction = getSelectAction();
            if (selectAction != null)
                selectAction.setEnabled(!event.getSource().getLookupSelectedItems().isEmpty());
        }

        @Override
        public Handler getLookupHandler() {
            return handler;
        }

        @Override
        public void setLookupHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        protected com.vaadin.ui.ComponentContainer getContainer() {
            return container;
        }

        @Override
        public void initLookupLayout() {
            // todo reimplement
            /*Action selectAction = getAction(WindowDelegate.LOOKUP_SELECT_ACTION_ID);
            if (selectAction == null || selectAction.getOwner() == null) {
                Frame frame = openFrame(null, "lookupWindowActions");
                com.vaadin.ui.Component vFrame = frame.unwrapComposition(com.vaadin.ui.Component.class);
                rootLayout.addComponent(vFrame);

                if (AppUI.getCurrent().isTestMode()) {
                    Component selectButton = frame.getComponent("selectButton");
                    if (selectButton instanceof com.haulmont.cuba.gui.components.Button) {
                        selectButton.unwrap(Button.class).setCubaId("selectButton");
                    }
                    Component cancelButton = frame.getComponent("cancelButton");
                    if (cancelButton instanceof com.haulmont.cuba.gui.components.Button) {
                        cancelButton.unwrap(Button.class).setCubaId("cancelButton");
                    }
                }
            }*/
        }

        @Override
        public Validator getLookupValidator() {
            return validator;
        }

        @Override
        public void setLookupValidator(Validator validator) {
            this.validator = validator;
        }

        @Override
        public String getStyleName() {
            return StringUtils.normalizeSpace(container.getStyleName().replace(C_WINDOW_LAYOUT, ""));
        }

        @Override
        protected com.vaadin.ui.ComponentContainer createLayout() {
            rootLayout = new CubaVerticalActionsLayout();
            rootLayout.setStyleName("c-lookup-window-wrapper");
            rootLayout.setSizeFull();
            rootLayout.setSpacing(true);

            container = new VerticalLayout();
            container.setMargin(false);
            container.setSpacing(false);
            container.setStyleName(C_WINDOW_LAYOUT);
            container.setSizeFull();

            rootLayout.addComponent(container);
            rootLayout.setExpandRatio(container, 1);

            return rootLayout;
        }

        @Override
        public void setId(String id) {
            super.setId(id);

            if (debugId != null) {
                AppUI ui = AppUI.getCurrent();
                if (ui.isTestMode()) {
                    TestIdManager testIdManager = ui.getTestIdManager();
                    Action selectAction = getSelectAction();
                    if (selectAction != null) {
                        ActionOwner selectActionOwner = selectAction.getOwner();
                        if (selectActionOwner instanceof com.haulmont.cuba.gui.components.Button) {
                            ((com.haulmont.cuba.gui.components.Button) selectActionOwner).setId(testIdManager.getTestId(debugId + "_selectButton"));
                        }
                    }
                    Action cancelAction = getCancelAction();
                    if (cancelAction != null) {
                        ActionOwner cancelActionOwner = cancelAction.getOwner();
                        if (cancelActionOwner instanceof com.haulmont.cuba.gui.components.Button) {
                            ((com.haulmont.cuba.gui.components.Button) cancelActionOwner).setId(testIdManager.getTestId(debugId + "_cancelButton"));
                        }
                    }
                }
            }
        }
    }

    protected class WebDialogOptions extends DialogOptions {
        @Override
        public Float getWidth() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.getWidth();
            }

            return super.getWidth();
        }

        @Override
        public SizeUnit getWidthUnit() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return WebWrapperUtils.toSizeUnit(dialogWindow.getWidthUnits());
            }

            return super.getWidthUnit();
        }

        @Override
        protected DialogOptions setWidth(Float width, SizeUnit sizeUnit) {
            super.setWidth(width, sizeUnit);

            if (width != null) {
                com.vaadin.ui.Window dialogWindow = asDialogWindow();
                if (dialogWindow != null) {
                    if (width < 0) {
                        dialogWindow.setWidthUndefined();
                        component.setWidthUndefined();
                        getContainer().setWidthUndefined();
                    } else {
                        Unit unit = sizeUnit != null
                                ? WebWrapperUtils.toVaadinUnit(sizeUnit)
                                : Unit.PIXELS;
                        dialogWindow.setWidth(width, unit);
                        component.setWidth(100, Unit.PERCENTAGE);
                    }
                }
            }

            return this;
        }

        @Override
        public Float getHeight() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.getHeight();
            }

            return super.getHeight();
        }

        @Override
        public SizeUnit getHeightUnit() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return WebWrapperUtils.toSizeUnit(dialogWindow.getHeightUnits());
            }

            return super.getHeightUnit();
        }

        @Override
        protected DialogOptions setHeight(Float height, SizeUnit sizeUnit) {
            super.setHeight(height, sizeUnit);

            if (height != null) {
                com.vaadin.ui.Window dialogWindow = asDialogWindow();
                if (dialogWindow != null) {
                    if (height < 0) {
                        dialogWindow.setHeightUndefined();
                        component.setHeightUndefined();
                        getContainer().setHeightUndefined();
                    } else {
                        Unit unit = sizeUnit != null
                                ? WebWrapperUtils.toVaadinUnit(sizeUnit)
                                : Unit.PIXELS;
                        dialogWindow.setHeight(height, unit);
                        component.setHeight(100, Unit.PERCENTAGE);
                    }
                }
            }

            return this;
        }

        @Override
        public Boolean getModal() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.isModal();
            }

            return super.getModal();
        }

        @Override
        public DialogOptions setModal(Boolean modal) {
            super.setModal(modal);

            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.setModal(BooleanUtils.isTrue(modal));
            }

            return this;
        }

        @Override
        public Boolean getResizable() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.isResizable();
            }

            return super.getResizable();
        }

        @Override
        public DialogOptions setResizable(Boolean resizable) {
            super.setResizable(resizable);

            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.setResizable(BooleanUtils.isTrue(resizable));
            }

            return this;
        }

        @Override
        public Boolean getCloseable() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.isClosable();
            }

            return super.getCloseable();
        }

        @Override
        public DialogOptions setCloseable(Boolean closeable) {
            super.setCloseable(closeable);

            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.setClosable(BooleanUtils.isTrue(closeable));
            }

            return this;
        }

        @Override
        public DialogOptions center() {
            super.center();

            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.center();
            }

            return this;
        }

        @Override
        public Integer getPositionX() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.getPositionX();
            }

            return super.getPositionX();
        }

        @Override
        public DialogOptions setPositionX(Integer positionX) {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.setPositionX(positionX != null ? positionX : 0);
            }

            return super.setPositionX(positionX);
        }

        @Override
        public Integer getPositionY() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                return dialogWindow.getPositionY();
            }

            return super.getPositionY();
        }

        @Override
        public DialogOptions setPositionY(Integer positionY) {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                dialogWindow.setPositionY(positionY != null ? positionY : 0);
            }

            return super.setPositionY(positionY);
        }

        @Override
        public DialogOptions setMaximized(Boolean maximized) {
            super.setMaximized(maximized);

            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                if (BooleanUtils.isTrue(maximized)) {
                    dialogWindow.setWindowMode(WindowMode.MAXIMIZED);
                } else {
                    dialogWindow.setWindowMode(WindowMode.NORMAL);
                }
            }
            return this;
        }

        @Override
        public Boolean getMaximized() {
            com.vaadin.ui.Window dialogWindow = asDialogWindow();
            if (dialogWindow != null) {
                if (dialogWindow.getWindowMode() == WindowMode.MAXIMIZED) {
                    return true;
                }
                return false;
            }

            return super.getMaximized();
        }
    }
}