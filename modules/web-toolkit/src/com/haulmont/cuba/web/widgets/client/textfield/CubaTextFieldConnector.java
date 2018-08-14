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

package com.haulmont.cuba.web.widgets.client.textfield;

import com.haulmont.cuba.web.widgets.CubaTextField;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Paintable;
import com.vaadin.client.UIDL;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.textfield.TextFieldConnector;
import com.vaadin.shared.ui.Connect;

@Connect(CubaTextField.class)
public class CubaTextFieldConnector extends TextFieldConnector implements Paintable {

    @Override
    public CubaTextFieldState getState() {
        return (CubaTextFieldState) super.getState();
    }

    @Override
    public CubaTextFieldWidget getWidget() {
        return (CubaTextFieldWidget) super.getWidget();
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        // We may have actions attached to this text field
        if (uidl.getChildCount() > 0) {
            final int cnt = uidl.getChildCount();
            for (int i = 0; i < cnt; i++) {
                UIDL childUidl = uidl.getChildUIDL(i);
                if (childUidl.getTag().equals("actions")) {
                    if (getWidget().getShortcutActionHandler() == null) {
                        getWidget().setShortcutActionHandler(new ShortcutActionHandler(uidl.getId(), client));
                    }
                    getWidget().getShortcutActionHandler().updateActionMap(childUidl);
                }
            }
        }
    }

    @OnStateChange("readOnlyFocusable")
    void updateReadOnlyFocusable() {
        getWidget().setReadOnlyFocusable(getState().readOnlyFocusable);
    }

    @OnStateChange("caseConversion")
    void updateCaseConversion() {
        getWidget().setCaseConversion(getState().caseConversion);
    }
}