package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
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
}
