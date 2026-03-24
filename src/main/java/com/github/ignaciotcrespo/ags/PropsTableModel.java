package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class PropsTableModel extends DefaultTableModel {

    private List<Object> items = new CopyOnWriteArrayList<>();
    private List<ValidFile> validFiles = new CopyOnWriteArrayList<>();

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
//                if (col == 0) return false;
//                return super.isCellEditable(row, col);
    }

    void addItem(Object item){
        items.add(item);
        if(item instanceof ValidFile){
            validFiles.add((ValidFile) item);
            addRow(new Object[]{((ValidFile) item).getRelativePath()});
            fireTableDataChanged();
        } else if (item instanceof Item) {
            Item it = (Item) item;
            addRow(new Object[]{"", it.name, it.value});
            fireTableDataChanged();
        }
    }

    void clear() {
        items.clear();
        validFiles.clear();
        while (getRowCount() > 0) {
            removeRow(0);
        }
        fireTableDataChanged();
    }

    Object getItem(int row) {
        return items.get(row);
    }

    boolean containsFile(VirtualFile file) {
        for (ValidFile item: validFiles) {
                if(file.equals(item.virtualFile)){
                    return true;
                }
        }
        return false;
    }

    void showDuplicatesOnly() {
        // Find property keys that appear in more than one file
        Map<String, Set<String>> keyToFiles = new HashMap<>();
        for (Object obj : items) {
            if (obj instanceof Item) {
                Item it = (Item) obj;
                keyToFiles.computeIfAbsent(it.name, k -> new HashSet<>()).add(it.path);
            }
        }
        Set<String> duplicateKeys = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : keyToFiles.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateKeys.add(entry.getKey());
            }
        }

        rebuildRows(duplicateKeys);
    }

    void showAll() {
        rebuildRows(null);
    }

    private void rebuildRows(Set<String> filterKeys) {
        while (getRowCount() > 0) {
            removeRow(0);
        }

        ValidFile lastFile = null;
        boolean fileHeaderAdded = false;

        for (Object obj : items) {
            if (obj instanceof ValidFile) {
                lastFile = (ValidFile) obj;
                fileHeaderAdded = false;
            } else if (obj instanceof Item) {
                Item it = (Item) obj;
                if (filterKeys == null || filterKeys.contains(it.name)) {
                    if (!fileHeaderAdded && lastFile != null) {
                        addRow(new Object[]{lastFile.getRelativePath()});
                        fileHeaderAdded = true;
                    }
                    addRow(new Object[]{"", it.name, it.value});
                }
            }
        }
        fireTableDataChanged();
    }
}
