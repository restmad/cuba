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

import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.web.AppUI;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component(Notifications.NAME)
@Scope(UIScope.NAME)
public class WebNotifications implements Notifications {

    protected AppUI ui;

    protected BackgroundWorker backgroundWorker;

    public WebNotifications(AppUI ui) {
        this.ui = ui;
    }

    @Inject
    public void setBackgroundWorker(BackgroundWorker backgroundWorker) {
        this.backgroundWorker = backgroundWorker;
    }

    @Override
    public Notification createNotification() {
        backgroundWorker.checkUIAccess();

        return new NotificationImpl();
    }

    // todo
    public class NotificationImpl implements Notification {
        @Override
        public Notification setCaption(String caption) {

            return this;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public Notification setDescription(String description) {

            return this;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public Notification setType(NotificationType notificationType) {

            return this;
        }

        @Override
        public NotificationType getType() {
            return null;
        }

        @Override
        public Notification setContentMode(ContentMode contentMode) {

            return this;
        }

        @Override
        public ContentMode getContentMode() {
            return null;
        }
    }
}