/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.sys.layout.BoxLayoutAdapter;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopHBox extends DesktopAbstractBox implements AutoExpanding {

    public DesktopHBox() {
        layoutAdapter.setFlowDirection(BoxLayoutAdapter.FlowDirection.X);
        setHeight("-1px"); // fix layout inside a scrollbox if the height is not set
    }

    @Override
    public boolean expandsWidth() {
        return false;
    }

    @Override
    public boolean expandsHeight() {
        return true;
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        layoutAdapter.setExpandLayout(!widthSize.isOwnSize()); // expand layout if width not -1
    }
}
