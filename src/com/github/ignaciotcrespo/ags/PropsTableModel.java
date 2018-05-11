package com.github.ignaciotcrespo.ags;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

class PropsTableModel extends DefaultTableModel {

    List<Object> items = new ArrayList<>();

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
//                if (col == 0) return false;
//                return super.isCellEditable(row, col);
    }

    void addItem(Object item){
        items.add(item);
        if(item instanceof ValidFile){
            addRow(new Object[]{((ValidFile) item).getRelativePath()});
        } else if (item instanceof Item) {
            Item it = (Item) item;
            addRow(new Object[]{"", it.name, it.value});
            fireTableDataChanged();
        }
    }

    void clear() {
        items.clear();
        while (getRowCount() > 0) {
            removeRow(0);
        }
        fireTableDataChanged();
    }

    public Object getItem(int row) {
        return items.get(row);
    }
}
