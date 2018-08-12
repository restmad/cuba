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
package com.haulmont.cuba.web.app.ui.demo.main;

import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.components.mainwindow.FoldersPane;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.events.AfterShowEvent;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.theme.HaloTheme;
import com.haulmont.cuba.web.widgets.CubaHorizontalSplitPanel;
import com.vaadin.server.Sizeable;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;

@UiController("demo-mainWindow")
@UiDescriptor("demo-main.xml")
public class DemoMainScreen extends Screen implements Window.HasWorkArea {
    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected Image logoImage;
    @Inject
    protected Messages messages;
    @Inject
    protected WebConfig webConfig;

    @Inject
    protected BoxLayout titleBar;
    @Inject
    protected SplitPanel foldersSplit;
    @Inject
    protected FoldersPane foldersPane;
    @Inject
    protected AppWorkArea workArea;

    @Subscribe
    public void init(InitEvent event) {
        Label<String> label = componentsFactory.createComponent(Label.NAME);
        label.setValue("IT IS ALIVE!");
        label.setIconFromSet(CubaIcon.MONEY);
        label.addStyleName(HaloTheme.LABEL_H2);

//        getWindow().add(label, 0);

        String logoImagePath = messages.getMainMessage("application.logoImage");
        if (StringUtils.isNotBlank(logoImagePath) && !"application.logoImage".equals(logoImagePath)) {
            logoImage.setSource(ThemeResource.class).setPath(logoImagePath);
        }

        if (webConfig.getUseInverseHeader()) {
            titleBar.setStyleName("c-app-menubar c-inverse-header");
        }

        if (webConfig.getFoldersPaneEnabled()) {
            if (webConfig.getFoldersPaneVisibleByDefault()) {
                foldersSplit.setSplitPosition(webConfig.getFoldersPaneDefaultWidth(), SizeUnit.PIXELS);
            } else {
                foldersSplit.setSplitPosition(0);
            }

            CubaHorizontalSplitPanel vSplitPanel = foldersSplit.unwrap(CubaHorizontalSplitPanel.class);
            vSplitPanel.setDefaultPosition(webConfig.getFoldersPaneDefaultWidth() + "px");
            vSplitPanel.setMaxSplitPosition(50, Sizeable.Unit.PERCENTAGE);
            vSplitPanel.setDockable(true);
        } else {
            foldersPane.setEnabled(false);
            foldersPane.setVisible(false);

            foldersSplit.remove(workArea);

            int foldersSplitIndex = getWindow().indexOf(foldersSplit);

            getWindow().remove(foldersSplit);
            getWindow().add(workArea, foldersSplitIndex);

            getWindow().expand(workArea);
        }
    }

    @Subscribe
    public void afterShow(AfterShowEvent event) {
        Label<String> label = componentsFactory.createComponent(Label.NAME);
        label.setValue("It is shown");

//        getWindow().add(label);
    }

    @Nullable
    @Override
    public AppWorkArea getWorkArea() {
        return workArea;
    }
}