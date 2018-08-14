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

package com.haulmont.cuba.gui.sys;

import com.haulmont.bali.util.URLEncodeUtils;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.annotation.TrackEditScreenHistory;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.ScreenHistoryEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Class that encapsulates screen opening history functionality. It is used by WindowManagerImpl and should not be invoked
 * from application code.
 *
 * todo convert to bean
 */
public class ScreenHistorySupport {

    protected Set<String> screenIds = new HashSet<>();

    protected Metadata metadata;
    protected Messages messages;
    protected Configuration configuration;

    public ScreenHistorySupport() {
        metadata = AppBeans.get(Metadata.NAME);
        messages = AppBeans.get(Messages.NAME);
        configuration = AppBeans.get(Configuration.NAME);

        String property = configuration.getConfig(ClientConfig.class).getScreenIdsToSaveHistory();
        if (StringUtils.isNotBlank(property)) {
            screenIds.addAll(Arrays.asList(StringUtils.split(property, ',')));
        }

        WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
        for (MetaClass metaClass : metadata.getTools().getAllPersistentMetaClasses()) {
            Map<String, Object> attributes = metadata.getTools().getMetaAnnotationAttributes(metaClass.getAnnotations(),
                    TrackEditScreenHistory.class);
            if (Boolean.TRUE.equals(attributes.get("value"))) {
                screenIds.add(windowConfig.getEditorScreenId(metaClass));
            }
        }
    }

    public void saveScreenHistory(Window window, OpenMode openMode) {
        Security security = AppBeans.get(Security.NAME);
        if (security.isEntityOpPermitted(ScreenHistoryEntity.class, EntityOp.CREATE)
                && window.getFrame() != null // todo rewrite with Screen instead
                && (window.getFrame() instanceof Window.Editor)
                && openMode != OpenMode.DIALOG
                && (screenIds == null || screenIds.contains(window.getId())))
        {
            String caption = window.getCaption();
            UUID entityId = null;
            Frame frame = window.getFrame();
            Entity entity = null;
            if (frame instanceof Window.Editor) {
                entity = ((Window.Editor) frame).getItem();
                if (entity != null) {
                    if (PersistenceHelper.isNew(entity)) {
                        return;
                    }
                    if (StringUtils.isBlank(caption))
                        caption = messages.getTools().getEntityCaption(entity.getMetaClass()) + " " +
                                metadata.getTools().getInstanceName(entity);
                    entityId = (UUID) entity.getId();
                }
            }
            ScreenHistoryEntity screenHistoryEntity = metadata.create(ScreenHistoryEntity.class);
            screenHistoryEntity.setCaption(StringUtils.abbreviate(caption, 255));
            screenHistoryEntity.setUrl(makeLink(window));
            screenHistoryEntity.setEntityId(entityId);
            addAdditionalFields(screenHistoryEntity, entity);

            CommitContext cc = new CommitContext(Collections.singleton(screenHistoryEntity));
            DataService dataService = AppBeans.get(DataService.NAME);
            dataService.commit(cc);
        }
    }

    protected void addAdditionalFields(ScreenHistoryEntity screenHistoryEntity, Entity entity) {

    }

    protected String makeLink(Window window) {
        Entity entity = null;
        if (window.getFrame() instanceof Window.Editor)
            entity = ((Window.Editor) window.getFrame()).getItem();
        String url = configuration.getConfig(GlobalConfig.class).getWebAppUrl() + "/open?" +
                "screen=" + window.getFrame().getId();
        if (entity != null) {
            String item = metadata.getSession().getClassNN(entity.getClass()).getName() + "-" + entity.getId();
            url += "&" + "item=" + item + "&" + "params=item:" + item;
        }
        Map<String, Object> params = getWindowParams(window);
        StringBuilder sb = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                Object value = param.getValue();
                if (value instanceof String /*|| value instanceof Integer || value instanceof Double*/
                        || value instanceof Boolean) {
                    sb.append(",").append(param.getKey()).append(":")
                            .append(URLEncodeUtils.encodeUtf8(value.toString()));
                }
            }
        }
        if (sb.length() > 0) {
            if (entity != null) {
                url += sb.toString();
            } else {
                url += "&params=" + sb.deleteCharAt(0).toString();
            }
        }

        return url;
    }

    protected Map<String, Object> getWindowParams(Window window) {
        return window.getContext().getParams();
    }
}