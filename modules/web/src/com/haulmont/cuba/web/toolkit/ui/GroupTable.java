/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 13.11.2009 11:10:14
 *
 * $Id$
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.data.GroupTableContainer;
import com.haulmont.cuba.web.toolkit.data.util.GroupTableContainerWrapper;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.Action;
import com.vaadin.terminal.KeyMapper;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Component;

import java.util.*;

public class GroupTable extends Table implements GroupTableContainer {

    private KeyMapper groupIdMap = new KeyMapper();

    private GroupPropertyValueFormatter groupPropertyValueFormatter;

    public GroupTable() {
        super();
    }

    public GroupTable(String caption) {
        super(caption);
    }

    public GroupTable(String caption, Container dataSource) {
        super(caption, dataSource);
    }

    @Override
    public void setContainerDataSource(Container newDataSource) {

        disableContentRefreshing();

        if (newDataSource == null) {
            newDataSource = new IndexedContainer();
        }

        super.setContainerDataSource(
                new GroupTableContainerWrapper(newDataSource));

        initComponent();

        enableContentRefreshing(true);

    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        // The tab ordering number
        if (getTabIndex() > 0) {
            target.addAttribute("tabindex", getTabIndex());
        }

        // Initialize temps
        final Object[] colids = getVisibleColumns();
        final int cols = colids.length;
        final int first = getCurrentPageFirstItemIndex();
        int total = size();
        final int pagelen = getPageLength();
        final int colHeadMode = getColumnHeaderMode();
        final boolean colheads = colHeadMode != COLUMN_HEADER_MODE_HIDDEN;
        final boolean rowheads = getRowHeaderMode() != ROW_HEADER_MODE_HIDDEN;
        final Object[][] cells = getVisibleCells();
        int rows;
        if (reqRowsToPaint >= 0) {
            rows = reqRowsToPaint;
        } else {
            rows = cells[0].length;
            if (alwaysRecalculateColumnWidths) {
                // TODO experimental feature for now: tell the client to
                // recalculate column widths.
                // We'll only do this for paints that do not originate from
                // table scroll/cache requests (i.e when reqRowsToPaint<0)
                target.addAttribute("recalcWidths", true);
            }
        }

        if (!isNullSelectionAllowed() && getNullSelectionItemId() != null
                && containsId(getNullSelectionItemId())) {
            total--;
            rows--;
        }

        // selection support
        LinkedList selectedKeys = new LinkedList();
        if (isMultiSelect()) {
            // only paint selections that are currently visible in the client
            HashSet sel = new HashSet((Set) getValue());
            Collection vids = getVisibleItemIds();
            for (Iterator it = vids.iterator(); it.hasNext();) {
                Object id = it.next();
                if (sel.contains(id)) {
                    selectedKeys.add(itemIdMapper.key(id));
                }
            }
        } else {
            Object value = getValue();
            if (value == null) {
                value = getNullSelectionItemId();
            }
            if (value != null) {
                selectedKeys.add(itemIdMapper.key(value));
            }
        }

        // Table attributes
        if (isSelectable()) {
            target.addAttribute("selectmode", (isMultiSelect() ? "multi"
                    : "single"));
        } else {
            target.addAttribute("selectmode", "none");
        }

        if (clickListenerCount > 0) {
            target.addAttribute("listenClicks", true);
        }

        target.addAttribute("cols", cols);
        target.addAttribute("rows", rows);

        target.addAttribute("firstrow",
                (reqFirstRowToPaint >= 0 ? reqFirstRowToPaint
                        : firstToBeRenderedInClient));
        target.addAttribute("totalrows", total);
        if (pagelen > 0 && !allowMultiStringCells) {
            target.addAttribute("pagelength", pagelen);
        }
        if (colheads) {
            target.addAttribute("colheaders", true);
        }
        if (rowheads) {
            target.addAttribute("rowheaders", true);
        }
        if (allowMultiStringCells) {
            target.addAttribute("multistring", true);
        }

        if (isSelectable() && !isMultiSelect() && !isNullSelectionAllowed()) {
            target.addAttribute("nullSelectionDisallowed", true);
        }

        if (isStoreColWidth()) {
            target.addAttribute("storeColWidth", true);
        }

        if (pagingMode == PagingMode.PAGE) {
            target.addAttribute("pagescount", pagesCount);
            target.addAttribute("curpage", currentPage);
            paintPaging(target);
        }
        target.addAttribute("pagingMode", pagingMode.name());

        // Visible column order
        final List<String> visibleColOrder = new ArrayList<String>(visibleColumns.size());
        for (final Object columnId : visibleColumns) {
            if (!isColumnCollapsed(columnId)) {
                visibleColOrder.add(columnIdMap.key(columnId));
            }
        }
        target.addAttribute("vcolorder", visibleColOrder.toArray());

        boolean hasAggregation = items instanceof AggregationContainer && isAggregatable()
                && !((AggregationContainer) items).getAggregationPropertyIds().isEmpty();
        if (hasAggregation) {
            paintAggregationRow(target, ((AggregationContainer) items).aggregate(allItemIds()));
        }

        // Rows
        final Set actionSet = new LinkedHashSet();
        final boolean selectable = isSelectable();
        final boolean[] iscomponent = new boolean[visibleColumns.size()];
        int iscomponentIndex = 0;
        for (final Iterator it = visibleColumns.iterator(); it.hasNext()
                && iscomponentIndex < iscomponent.length;) {
            final Object columnId = it.next();
            if (columnGenerators.containsKey(columnId)) {
                iscomponent[iscomponentIndex++] = true;
            } else {
                final Class colType = getType(columnId);
                iscomponent[iscomponentIndex++] = colType != null
                        && Component.class.isAssignableFrom(colType);
            }
        }
        target.startTag("rows");
        // cells array contains all that are supposed to be visible on client,
        // but we'll start from the one requested by client
        int start = 0;
        if (reqFirstRowToPaint != -1 && firstToBeRenderedInClient != -1) {
            start = reqFirstRowToPaint - firstToBeRenderedInClient;
        }
        int end = cells[0].length;
        if (reqRowsToPaint != -1) {
            end = start + reqRowsToPaint;
        }
        // sanity check
        if (lastToBeRenderedInClient != -1 && lastToBeRenderedInClient < end) {
            end = lastToBeRenderedInClient + 1;
        }
        if (start > cells[CELL_ITEMID].length || start < 0) {
            start = 0;
        }
        // trying to fix ArrayIndexOutOfBoundsException
        if (end > cells[CELL_ITEMID].length) {
            end = cells[CELL_ITEMID].length;
        }

        boolean hasGroups = hasGroups();

        for (int i = start; i < end; i++) {
            final Object itemId = cells[CELL_ITEMID][i];

            if (!isNullSelectionAllowed() && getNullSelectionItemId() != null
                    && itemId == getNullSelectionItemId()) {
                // Remove null selection item if null selection is not allowed
                continue;
            }

            if (hasGroups && isGroup(itemId)) {
                target.startTag("gr");
            } else {
                target.startTag("tr");
            }

            // tr attributes
            if (rowheads) {
                if (cells[CELL_ICON][i] != null) {
                    target.addAttribute("icon", (Resource) cells[CELL_ICON][i]);
                }
                if (cells[CELL_HEADER][i] != null) {
                    target.addAttribute("caption",
                            (String) cells[CELL_HEADER][i]);
                }
            }
            target.addAttribute("key", Integer.parseInt(cells[CELL_KEY][i]
                    .toString()));
            if (actionHandlers != null || isSelectable()) {
                if (isSelected(itemId)) {
                    target.addAttribute("selected", true);
                }
            }

            paintRowActions(target, actionSet, itemId);

            paintCellStyleGenerator(target, itemId);

            //add group table attributes
            if (hasGroups) {
                final Collection groupProperties = getGroupProperties();

                if (isGroup(itemId)) {
                    target.addAttribute("colKey", columnIdMap.key(getGroupProperty(itemId)));
                    target.addAttribute("groupKey", groupIdMap.key(itemId));
                    if (isExpanded(itemId)) {
                        target.addAttribute("expanded", true);
                    }
                    final Object propertyValue = getGroupPropertyValue(itemId);
                    target.addAttribute("caption", formatGroupPropertyValue(itemId, propertyValue));

                    if (hasAggregation) {
                        paintGroupAggregation(target, itemId,
                                ((AggregationContainer) items).aggregate(getGroupItemIds(itemId)));
                    }
                } else {
                    // paint none groupped cells
                    int currentColumn = 0;
                    for (final Iterator it = visibleColumns.iterator(); it.hasNext(); currentColumn++) {
                        final Object columnId = it.next();
                        if (!groupProperties.contains(columnId)) {
                            paintCell(target, itemId, columnId, cells[CELL_FIRSTCOL + currentColumn][i],
                                    iscomponent[currentColumn]);
                        } else {
                            paintCell(target, itemId, columnId, "", false);
                        }
                    }
                }

            } else {
                // cells
                int currentColumn = 0;
                for (final Iterator it = visibleColumns.iterator(); it.hasNext(); currentColumn++) {
                    final Object columnId = it.next();
                    paintCell(target, itemId, columnId, cells[CELL_FIRSTCOL + currentColumn][i],
                            iscomponent[currentColumn]);
                }
            }

            if (hasGroups && isGroup(itemId)) {
                target.endTag("gr");
            } else {
                target.endTag("tr");
            }
        }
        target.endTag("rows");

        // The select variable is only enabled if selectable
        if (selectable && selectedKeys.size() > 0) {
            target.addVariable(this, "selected", (String[]) selectedKeys
                    .toArray(new String[selectedKeys.size()]));
        }

        // The cursors are only shown on pageable table
        if (first != 0 || getPageLength() > 0) {
            target.addVariable(this, "firstvisible", first);
        }

        // Sorting
        if (getContainerDataSource() instanceof Container.Sortable) {
            target.addVariable(this, "sortcolumn", columnIdMap
                    .key(getSortContainerPropertyId()));
            target.addVariable(this, "sortascending", isSortAscending());
        }

        // Resets and paints "to be painted next" variables. Also reset
        // pageBuffer
        reqFirstRowToPaint = -1;
        reqRowsToPaint = -1;
        containerChangeToBeRendered = false;
        target.addVariable(this, "reqrows", reqRowsToPaint);
        target.addVariable(this, "reqfirstrow", reqFirstRowToPaint);

        // Actions
        if (!actionSet.isEmpty()) {
            target.addVariable(this, "action", "");
            target.startTag("actions");
            for (final Iterator it = actionSet.iterator(); it.hasNext();) {
                final Action a = (Action) it.next();
                target.startTag("action");
                if (a.getCaption() != null) {
                    target.addAttribute("caption", a.getCaption());
                }
                if (a.getIcon() != null) {
                    target.addAttribute("icon", a.getIcon());
                }
                target.addAttribute("key", actionMapper.key(a));
                target.endTag("action");
            }
            target.endTag("actions");
        }
        if (isColumnReorderingAllowed()) {
            final String[] colorder = new String[visibleColumns.size()];
            int i = 0;
            for (final Iterator it = visibleColumns.iterator(); it.hasNext()
                    && i < colorder.length;) {
                colorder[i++] = columnIdMap.key(it.next());
            }
            target.addVariable(this, "columnorder", colorder);
        }
        // Available columns
        if (isColumnCollapsingAllowed()) {
            final HashSet ccs = new HashSet();
            for (final Object o : visibleColumns) {
                if (isColumnCollapsed(o)) {
                    ccs.add(o);
                }
            }
            final String[] collapsedkeys = new String[ccs.size()];
            int nextColumn = 0;
            for (final Iterator it = visibleColumns.iterator(); it.hasNext()
                    && nextColumn < collapsedkeys.length;) {
                final Object columnId = it.next();
                if (isColumnCollapsed(columnId)) {
                    collapsedkeys[nextColumn++] = columnIdMap.key(columnId);
                }
            }
            target.addVariable(this, "collapsedcolumns", collapsedkeys);
        }

        if (hasGroups) {
            final Collection groupProperties = getGroupProperties();
            final String[] groupColumns = new String[groupProperties.size()];

            int index = 0;
            for (final Object groupColumnId : groupProperties) {
                groupColumns[index++] = columnIdMap.key(groupColumnId);
            }
            target.addVariable(this, "groupColumns", groupColumns);
        }

        paintVisibleColumns(target, colheads);
    }

    private Collection<?> allItemIds() {
        if (hasGroups()) {
            List itemIds = new LinkedList();
            for (final Object groupId : rootGroups()) {
                itemIds.addAll(getGroupItemIds(groupId));
            }
            return itemIds;
        } else {
            return items.getItemIds();
        }
    }

    protected void paintGroupAggregation(PaintTarget target, Object groupId, Map<Object, String> aggregations)
            throws PaintException {

        boolean paintGroupProperty = false;

        final Collection groupProperties = getGroupProperties();
        final Object groupProperty = getGroupProperty(groupId);

        for (final Object columnId : visibleColumns) {
            if (columnId == null || isColumnCollapsed(columnId)) {
                continue;
            }

            if (groupProperties.contains(columnId) && !paintGroupProperty) {
                if (columnId.equals(groupProperty)) {
                    paintGroupProperty = true;
                }
                continue;
            }

            if (cellStyleGenerator != null) {
                String cellStyle = cellStyleGenerator.getStyle(null, columnId);
                if (cellStyle != null && !cellStyle.equals("")) {
                    target.addAttribute("style-"
                            + columnIdMap.key(columnId), cellStyle + "-ag");
                }
            }
            target.addText(aggregations.get(columnId));
        }
    }

    @Override
    protected void refreshRenderedCells() {
        if (getParent() == null) {
            return;
        }

        if (isContentRefreshesEnabled) {

            HashSet oldListenedProperties = listenedProperties;
            HashSet oldVisibleComponents = visibleComponents;

            // initialize the listener collections
            listenedProperties = new HashSet();
            visibleComponents = new HashSet();

            // Collects the basic facts about the table page
            final Object[] colids = getVisibleColumns();
            final int cols = colids.length;
            int rows, totalRows;
            rows = totalRows = size();
            int pagelen;
            if (allowMultiStringCells) {
                pagelen = totalRows;
            } else {
                pagelen = getPageLength();
            }

            int firstIndex = pagingMode == PagingMode.PAGE
                    ? currentPageFirstItemIndex() : getCurrentPageFirstItemIndex();

            if (rows > 0 && firstIndex >= 0) {
                rows -= firstIndex;
            }

            if (pagelen > 0 && pagelen < rows) {
                rows = pagelen;
            }

            if (pagingMode == PagingMode.SCROLLING) {
                // If "to be painted next" variables are set, use them
                if (lastToBeRenderedInClient - firstToBeRenderedInClient > 0) {
                    rows = lastToBeRenderedInClient - firstToBeRenderedInClient + 1;
                }
                if (firstToBeRenderedInClient >= 0) {
                    if (firstToBeRenderedInClient < totalRows) {
                        firstIndex = firstToBeRenderedInClient;
                    } else {
                        firstIndex = totalRows - 1;
                    }
                } else {
                    // initial load
                    firstToBeRenderedInClient = firstIndex;
                }
                if (totalRows > 0) {
                    if (rows + firstIndex > totalRows) {
                        rows = totalRows - firstIndex;
                    }
                } else {
                    rows = 0;
                }
            }

            if (rows < 0) {
                rows = 0;
            }

            Object[][] cells = new Object[cols + CELL_FIRSTCOL][rows];
            if (rows == 0) {
                pageBuffer = cells;
                unregisterPropertiesAndComponents(oldListenedProperties,
                        oldVisibleComponents);
                return;
            }

            if (pagingMode == PagingMode.PAGE) {
                pagesCount = totalRows % pagelen == 0
                        ? totalRows / pagelen
                        : totalRows / pagelen + 1;
                if (currentPage > pagesCount) currentPage = pagesCount;
            }

            Object id;
            // Gets the first item id
            if (items instanceof Container.Indexed) {
                id = ((Container.Indexed) items).getIdByIndex(firstIndex);
            } else {
                id = ((Container.Ordered) items).firstItemId();
                for (int i = 0; i < firstIndex; i++) {
                    id = ((Container.Ordered) items).nextItemId(id);
                }
            }

            final int headmode = getRowHeaderMode();
            final boolean[] iscomponent = new boolean[cols];
            for (int i = 0; i < cols; i++) {
                iscomponent[i] = columnGenerators.containsKey(colids[i])
                        || Component.class.isAssignableFrom(getType(colids[i]));
            }
            int firstIndexNotInCache;
            if (pageBuffer != null && pageBuffer[CELL_ITEMID].length > 0) {
                firstIndexNotInCache = pageBufferFirstIndex
                        + pageBuffer[CELL_ITEMID].length;
            } else {
                firstIndexNotInCache = -1;
            }

            // Creates the page contents
            int filledRows = 0;
            for (int i = 0; i < rows && id != null; i++) {
                cells[CELL_ITEMID][i] = id;
                cells[CELL_KEY][i] = itemIdMapper.key(id);
                if (headmode != ROW_HEADER_MODE_HIDDEN) {
                    switch (headmode) {
                    case ROW_HEADER_MODE_INDEX:
                        cells[CELL_HEADER][i] = String.valueOf(i + firstIndex
                                + 1);
                        break;
                    default:
                        cells[CELL_HEADER][i] = getItemCaption(id);
                    }
                    cells[CELL_ICON][i] = getItemIcon(id);
                }

                final GroupTableContainer items = (GroupTableContainer) this.items;
                final boolean groupped = items.hasGroups();

                if (cols > 0 && (!groupped || !items.isGroup(id)))
                {
                    for (int j = 0; j < cols; j++) {
                        if (isColumnCollapsed(colids[j]) || (groupped &&
                                getGroupProperties().contains(colids[j]))) {
                            continue;
                        }
                        Property p = null;
                        Object value = "";
                        boolean isGenerated = columnGenerators
                                .containsKey(colids[j]);

                        if (!isGenerated) {
                            p = getContainerProperty(id, colids[j]);
                        }

                        // check in current pageBuffer already has row
                        int index = firstIndex + i;
                        if (p != null || isGenerated) {
                            if (p instanceof Property.ValueChangeNotifier) {
                                if (oldListenedProperties == null
                                        || !oldListenedProperties.contains(p)) {
                                    ((Property.ValueChangeNotifier) p)
                                            .addListener(this);
                                }
                                listenedProperties.add(p);
                            }
                            if (index < firstIndexNotInCache
                                    && index >= pageBufferFirstIndex) {
                                // we have data already in our cache,
                                // recycle it instead of fetching it via
                                // getValue/getPropertyValue
                                int indexInOldBuffer = index
                                        - pageBufferFirstIndex;
                                value = pageBuffer[CELL_FIRSTCOL + j][indexInOldBuffer];
                            } else {
                                if (isGenerated) {
                                    final ColumnGenerator cg = columnGenerators.get(colids[j]);
                                    value = cg.generateCell(this, id, colids[j]);

                                } else if (iscomponent[j]) {
                                    value = p.getValue();
                                } else if (p != null) {
                                    value = getPropertyValue(id, colids[j], p);
                                } else {
                                    value = getPropertyValue(id, colids[j],
                                            null);
                                }
                            }
                        }

                        if (value instanceof Component) {
                            if (oldVisibleComponents == null
                                    || !oldVisibleComponents.contains(value)) {
                                ((Component) value).setParent(this);
                            }
                            visibleComponents.add((Component) value);
                        }
                        cells[CELL_FIRSTCOL + j][i] = value;
                    }
                }

                id = ((Container.Ordered) items).nextItemId(id);

                filledRows++;
            }

            // Assures that all the rows of the cell-buffer are valid
            if (filledRows != cells[0].length) {
                final Object[][] temp = new Object[cells.length][filledRows];
                for (int i = 0; i < cells.length; i++) {
                    System.arraycopy(cells[i], 0, temp[i], 0, filledRows);
                }
                cells = temp;
            }

            pageBufferFirstIndex = firstIndex;

            // Saves the results to internal buffer
            pageBuffer = cells;

            unregisterPropertiesAndComponents(oldListenedProperties,
                    oldVisibleComponents);

            requestRepaint();
        }
    }

    @Override
    protected boolean changeVariables(Map variables) {
        boolean clientNeedsContentRefresh = super.changeVariables(variables);

        boolean needsResetPageBuffer = false;
        Object[] newGroupProperties = null;

        if (variables.containsKey("columnorder") && !variables.containsKey("groupedcolumns")) {
            newGroupProperties = new Object[0];
        } else if (variables.containsKey("groupedcolumns")) {
            final Object[] ids = (Object[]) variables.get("groupedcolumns");
            final Object[] groupProperties = new Object[ids.length];
            for (int i = 0; i < ids.length; i++) {
                groupProperties[i] = columnIdMap.get(ids[i].toString());
            }
            newGroupProperties = groupProperties;
        }

        if (variables.containsKey("collapsedcolumns")) {
            boolean needToRegroup = false;
            final List<Object> groupProperties = new ArrayList<Object>(getGroupProperties());
            for (final Iterator it = groupProperties.iterator(); it.hasNext();) {
                final Object propertyId = it.next();
                if (collapsedColumns.contains(propertyId)) {
                    it.remove();
                    needToRegroup = true;
                }
            }
            if (needToRegroup) {
                newGroupProperties = groupProperties.toArray();
            }
        }

        if (variables.containsKey("expand")) {
            Object groupId = groupIdMap.get((String) variables.get("expand"));
            expand(groupId, false);
            clientNeedsContentRefresh = true;
            needsResetPageBuffer = true;
        }

        if (variables.containsKey("collapse")) {
            Object groupId = groupIdMap.get((String) variables.get("collapse"));
            collapse(groupId, false);
            clientNeedsContentRefresh = true;
            needsResetPageBuffer = true;
        }

        if (newGroupProperties != null) {
            groupBy(newGroupProperties, false);
        }

        if (needsResetPageBuffer) {
            resetPageBuffer();
        }

        return clientNeedsContentRefresh;
    }

    public void groupBy(Object[] properties) {
        groupBy(properties, true);
    }

    protected void groupBy(Object[] properties, boolean rerender) {
        ((GroupTableContainer) items).groupBy(properties);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            requestRepaint();
        }
    }

    protected String formatGroupPropertyValue(Object groupId, Object groupValue) {
        if (groupValue == null) {
            return "";
        }
        return groupPropertyValueFormatter != null
                ? groupPropertyValueFormatter.format(groupId, groupValue)
                : groupValue.toString();
    }

    public boolean hasGroups() {
        return ((GroupTableContainer) items).hasGroups();
    }

    public Collection<?> rootGroups() {
        return ((GroupTableContainer) items).rootGroups();
    }

    public boolean hasChildren(Object id) {
        return ((GroupTableContainer) items).hasChildren(id);
    }

    public Collection<?> getChildren(Object id) {
        return ((GroupTableContainer) items).getChildren(id);
    }

    public Object getGroupPropertyValue(Object itemId) {
        return ((GroupTableContainer) items).getGroupPropertyValue(itemId);
    }

    public Object getGroupProperty(Object itemId) {
        return ((GroupTableContainer) items).getGroupProperty(itemId);
    }

    public boolean isGroup(Object itemId) {
        return ((GroupTableContainer) items).isGroup(itemId);
    }

    public Collection<?> getGroupItemIds(Object itemId) {
        return ((GroupTableContainer) items).getGroupItemIds(itemId);
    }

    public int getGroupItemsCount(Object itemId) {
        return ((GroupTableContainer) items).getGroupItemsCount(itemId);
    }

    public Collection<?> getGroupProperties() {
        return ((GroupTableContainer) items).getGroupProperties();
    }

    public void expand(Object id) {
        expand(id, true);
    }

    protected void expand(Object id, boolean rerender) {
        ((GroupTableContainer) items).expand(id);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            requestRepaint();
        }
    }

    public boolean isExpanded(Object id) {
        return ((GroupTableContainer) items).isExpanded(id);
    }

    public void expandAll() {
        ((GroupTableContainer) items).expandAll();
        resetPageBuffer();
        refreshRenderedCells();
        requestRepaint();
    }

    public void collapseAll() {
        ((GroupTableContainer) items).collapseAll();
        resetPageBuffer();
        refreshRenderedCells();
        requestRepaint();
    }

    public void collapse(Object id) {
        collapse(id, true);
    }

    protected void collapse(Object id, boolean rerender) {
        ((GroupTableContainer) items).collapse(id);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            requestRepaint();
        }
    }

    public GroupPropertyValueFormatter getGroupPropertyValueFormatter() {
        return groupPropertyValueFormatter;
    }

    public void setGroupPropertyValueFormatter(GroupPropertyValueFormatter groupPropertyValueFormatter) {
        this.groupPropertyValueFormatter = groupPropertyValueFormatter;
    }

    @Override
    public String getTag() {
        return "grouptable";
    }

    public interface GroupPropertyValueFormatter {
        String format(Object groupId, Object value);
    }
}
