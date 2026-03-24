package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class PropsTableModel extends DefaultTableModel {

    private List<Object> items = new CopyOnWriteArrayList<>();
    private List<Object> displayItems = new CopyOnWriteArrayList<>();
    private List<ValidFile> validFiles = new CopyOnWriteArrayList<>();

    private Set<String> filterKeys = null;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    void addItem(Object item) {
        items.add(item);
        if (item instanceof ValidFile) {
            validFiles.add((ValidFile) item);
        }
        if (sortColumn >= 0) {
            rebuildRows();
        } else {
            appendRow(item);
        }
    }

    private void appendRow(Object item) {
        if (item instanceof ValidFile) {
            if (filterKeys != null) return;
            displayItems.add(item);
            addRow(new Object[]{((ValidFile) item).getRelativePath()});
            fireTableDataChanged();
        } else if (item instanceof Item) {
            Item it = (Item) item;
            if (filterKeys == null || filterKeys.contains(it.name)) {
                displayItems.add(item);
                addRow(new Object[]{"", it.name, it.value});
                fireTableDataChanged();
            }
        }
    }

    void clear() {
        items.clear();
        displayItems.clear();
        validFiles.clear();
        sortColumn = -1;
        sortAscending = true;
        while (getRowCount() > 0) {
            removeRow(0);
        }
        fireTableDataChanged();
    }

    Object getItem(int row) {
        if (row >= 0 && row < displayItems.size()) {
            return displayItems.get(row);
        }
        return null;
    }

    boolean containsFile(VirtualFile file) {
        for (ValidFile item : validFiles) {
            if (file.equals(item.virtualFile)) {
                return true;
            }
        }
        return false;
    }

    void showDuplicatesOnly() {
        Map<String, Set<String>> keyToFiles = new HashMap<>();
        for (Object obj : items) {
            if (obj instanceof Item) {
                Item it = (Item) obj;
                keyToFiles.computeIfAbsent(it.name, k -> new HashSet<>()).add(it.path);
            }
        }
        filterKeys = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : keyToFiles.entrySet()) {
            if (entry.getValue().size() > 1) {
                filterKeys.add(entry.getKey());
            }
        }
        rebuildRows();
    }

    void showAll() {
        filterKeys = null;
        rebuildRows();
    }

    void sortBy(int column) {
        if (sortColumn == column) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = column;
            sortAscending = true;
        }
        rebuildRows();
    }

    private void rebuildRows() {
        while (getRowCount() > 0) {
            removeRow(0);
        }
        displayItems.clear();

        if (sortColumn <= 0) {
            rebuildGroupedByFile();
        } else {
            rebuildFlat();
        }
        fireTableDataChanged();
    }

    private void rebuildGroupedByFile() {
        // Collect file->items mapping preserving order
        List<Map.Entry<ValidFile, List<Item>>> groups = buildFileGroups();

        if (sortColumn == 0) {
            groups.sort((a, b) -> {
                int cmp = a.getKey().getRelativePath().compareToIgnoreCase(b.getKey().getRelativePath());
                return sortAscending ? cmp : -cmp;
            });
        }

        for (Map.Entry<ValidFile, List<Item>> entry : groups) {
            ValidFile vf = entry.getKey();
            List<Item> fileItems = entry.getValue();
            if (!fileItems.isEmpty()) {
                displayItems.add(vf);
                addRow(new Object[]{vf.getRelativePath()});
                for (Item it : fileItems) {
                    displayItems.add(it);
                    addRow(new Object[]{"", it.name, it.value});
                }
            }
        }
    }

    private void rebuildFlat() {
        List<Item> flatItems = new ArrayList<>();
        for (Object obj : items) {
            if (obj instanceof Item) {
                Item it = (Item) obj;
                if (filterKeys == null || filterKeys.contains(it.name)) {
                    flatItems.add(it);
                }
            }
        }

        Comparator<Item> cmp;
        if (sortColumn == 1) {
            cmp = Comparator.comparing(it -> it.name, String.CASE_INSENSITIVE_ORDER);
        } else {
            cmp = Comparator.comparing(it -> it.value, String.CASE_INSENSITIVE_ORDER);
        }
        if (!sortAscending) {
            cmp = cmp.reversed();
        }
        flatItems.sort(cmp);

        for (Item it : flatItems) {
            displayItems.add(it);
            addRow(new Object[]{it.path, it.name, it.value});
        }
    }

    private List<Map.Entry<ValidFile, List<Item>>> buildFileGroups() {
        List<Map.Entry<ValidFile, List<Item>>> groups = new ArrayList<>();
        ValidFile currentFile = null;
        List<Item> currentItems = null;

        for (Object obj : items) {
            if (obj instanceof ValidFile) {
                if (currentFile != null) {
                    groups.add(new AbstractMap.SimpleEntry<>(currentFile, currentItems));
                }
                currentFile = (ValidFile) obj;
                currentItems = new ArrayList<>();
            } else if (obj instanceof Item) {
                Item it = (Item) obj;
                if (currentItems != null && (filterKeys == null || filterKeys.contains(it.name))) {
                    currentItems.add(it);
                }
            }
        }
        if (currentFile != null) {
            groups.add(new AbstractMap.SimpleEntry<>(currentFile, currentItems));
        }
        return groups;
    }
}
