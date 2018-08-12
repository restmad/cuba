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

import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.components.SizeUnit;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.web.AppUI;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component(Dialogs.NAME)
@Scope(UIScope.NAME)
public class WebDialogs implements Dialogs {

    protected AppUI ui;

    @Inject
    protected Messages messages;
    @Inject
    protected BackgroundWorker backgroundWorker;

    public WebDialogs(AppUI ui) {
        this.ui = ui;
    }

    @Override
    public OptionDialog createOptionDialog() {
        backgroundWorker.checkUIAccess();

        return new OptionDialogImpl();
    }

    @Override
    public MessageDialog createMessageDialog() {
        backgroundWorker.checkUIAccess();

        return new MessageDialogImpl();
    }

    @Override
    public ExceptionDialog createExceptionDialog() {
        backgroundWorker.checkUIAccess();

        return new ExceptionDialogImpl();
    }

    // todo
    public class OptionDialogImpl implements OptionDialog {
        protected MessageType type = MessageType.CONFIRMATION;

        @Override
        public OptionDialog setCaption(String caption) {
            return this;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public OptionDialog setMessage(String message) {
            return this;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public OptionDialog setType(MessageType messageType) {
            return this;
        }

        @Override
        public MessageType getType() {
            return type;
        }

        @Override
        public OptionDialog setContentMode(ContentMode contentMode) {
            return this;
        }

        @Override
        public ContentMode getContentMode() {
            return null;
        }

        @Override
        public OptionDialog setActions(Action... actions) {
            return this;
        }

        @Override
        public Action[] getActions() {
            return new Action[0];
        }

        @Override
        public OptionDialog setWidth(String width) {
            return this;
        }

        @Override
        public float getWidth() {
            return 0;
        }

        @Override
        public SizeUnit getWidthSizeUnit() {
            return null;
        }

        @Override
        public OptionDialog setHeight(String height) {
            return this;
        }

        @Override
        public float getHeight() {
            return 0;
        }

        @Override
        public SizeUnit getHeightSizeUnit() {
            return null;
        }

        @Override
        public void show() {

        }
    }

    // todo
    public class MessageDialogImpl implements MessageDialog {
        protected MessageType type = MessageType.CONFIRMATION;

        @Override
        public MessageDialog setCaption(String caption) {
            return this;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public MessageDialog setMessage(String message) {
            return this;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public MessageDialog setType(MessageType messageType) {
            return this;
        }

        @Override
        public MessageType getType() {
            return type;
        }

        @Override
        public MessageDialog setContentMode(ContentMode contentMode) {
            return this;
        }

        @Override
        public ContentMode getContentMode() {
            return null;
        }

        @Override
        public MessageDialog setWidth(String width) {
            return this;
        }

        @Override
        public float getWidth() {
            return 0;
        }

        @Override
        public SizeUnit getWidthSizeUnit() {
            return null;
        }

        @Override
        public MessageDialog setHeight(String height) {
            return this;
        }

        @Override
        public float getHeight() {
            return 0;
        }

        @Override
        public SizeUnit getHeightSizeUnit() {
            return null;
        }

        @Override
        public void show() {

        }
    }

    // todo
    public class ExceptionDialogImpl implements ExceptionDialog {
        @Override
        public ExceptionDialog setThrowable(Throwable throwable) {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        public ExceptionDialog setCaption(String caption) {
            return null;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public ExceptionDialog setDescription(String description) {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}