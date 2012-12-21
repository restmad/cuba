/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.cuba.gui.data.DatasourceListener;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.core.entity.Entity;

/**
 * @param <T>
 * @author tulupov
 * @version $Id$
 */
public class DsListenerAdapter<T extends Entity> implements DatasourceListener<T> {

    @Override
    public void itemChanged(Datasource<T> ds, T prevItem, T item) {
    }

    @Override
    public void stateChanged(Datasource<T> ds, Datasource.State prevState, Datasource.State state) {
    }

    @Override
    public void valueChanged(T source, String property, Object prevValue, Object value) {
    }
}