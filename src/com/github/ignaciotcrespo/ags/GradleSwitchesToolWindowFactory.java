package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GradleSwitchesToolWindowFactory implements ToolWindowFactory {


    static class Item {
        String name;
        String value;
        String path;

        public Item(String name, String value, String path) {
            this.name = name;
            this.value = value;
            this.path = path;
        }
    }

    static class ValidFile {
        File file;
        String projectRootFolder;

        public ValidFile(File file, String projectRootFolder) {
            this.file = file;
            this.projectRootFolder = projectRootFolder;
        }

        public String getRelativePath() {
            return file.toString().substring(projectRootFolder.length());
        }
    }


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        List<Object> items = new ArrayList<>();

        PublishSubject<MouseEvent> clicks = PublishSubject.create();

        //JTable table = new TreeTable(new ListTreeTableModel());
        JBTable table = new JBTable();
        table.setExpandableItemsEnabled(true);
        DefaultTableModel tableModel = new PropsTableModel();
        tableModel.setColumnIdentifiers(new Object[]{"File", "Name", "Value"});

        table.setModel(tableModel);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clicks.onNext(evt);
            }
        });
        clicks
                .throttleLast(250, TimeUnit.MILLISECONDS)
                .subscribe(evt -> {
                    int row = table.rowAtPoint(evt.getPoint());
                    int col = table.columnAtPoint(evt.getPoint());
                    if (row >= 0 && col >= 0) {
//                    Object value = table.getModel().getValueAt(row, col);
                        Object item = items.get(row);
                        if (item instanceof ValidFile) {
                            openValidFile(project, table, (ValidFile) item);
                        } else {
                            while (--row >= 0) {
                                item = items.get(row);
                                if (item instanceof ValidFile) {
                                    openValidFile(project, table, (ValidFile) item);
                                    break;
                                }
                            }
                        }

                    }
                });


        JScrollPane tableContainer = new JBScrollPane(table);
        tableContainer.setLayout(new ScrollPaneLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        JButton button = new JButton();
        button.setText("Refresh");
        button.addActionListener(actionEvent -> {
            refresh(project, tableModel, items);
        });
        toolBar.add(button);
        toolBar.setFloatable(false);
        panel.add(toolBar, BorderLayout.NORTH);

        panel.add(tableContainer, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "", false);


        toolWindow.getContentManager().addContent(content);


        refresh(project, tableModel, items);


    }

    private void openValidFile(@NotNull Project project, JBTable table, ValidFile item) {
        VirtualFile fileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(item.file);
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(fileByIoFile, true, true);
        }, ModalityState.stateForComponent(table));
    }

    private void refresh(@NotNull Project project, DefaultTableModel tableModel, List<Object> items) {
        getValidFilesObservable(project)
                .subscribeOn(Schedulers.io())
                .doOnNext(f -> {
                    items.add(f);
                    tableModel.addRow(new Object[]{f.getRelativePath()});
                })
                .flatMap(this::getItemsObservable)
                .doOnNext(item -> {
                    items.add(item);
                    tableModel.addRow(new Object[]{"", item.name, item.value});
                    tableModel.fireTableDataChanged();
                }).doOnSubscribe(disposable -> {
            items.clear();
            clearTable(tableModel);
        })
                .subscribe();
    }

    private void clearTable(DefaultTableModel tableModel) {
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        tableModel.fireTableDataChanged();
    }

    private Observable<Item> getItemsObservable(ValidFile f) {
        return Observable.create(emitter -> {
            searchProperties(emitter, f);
            emitter.onComplete();
        });
    }

    private Observable<ValidFile> getValidFilesObservable(@NotNull Project project) {
        return Observable.create((ObservableEmitter<ValidFile> emitter) -> {
            try {
                Module[] modules = ModuleManager.getInstance(project).getModules();
                VirtualFile file = ModuleRootManager.getInstance(modules[0]).getContentRoots()[0];
                List<VirtualFile> allExcludedRoots = new ArrayList<>();
                for (Module module : modules) {
                    VirtualFile[] excludedRoots = ModuleRootManager.getInstance(module).getExcludeRoots();
                    allExcludedRoots.addAll(Arrays.asList(excludedRoots));
                }
                String projectRootFolder = file.getCanonicalPath();
                File file1 = new File(projectRootFolder);
                searchFiles(emitter, file1, projectRootFolder, allExcludedRoots);
            } finally {
                emitter.onComplete();
            }
        });
    }

    private void searchProperties(Emitter<Item> emitter, ValidFile file) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(file.file));
            Enumeration<?> propertyNames = props.propertyNames();
            Map<String, Item> map = new TreeMap<>();
            while (propertyNames.hasMoreElements()) {
                String key = propertyNames.nextElement().toString();
                Item item = new Item(key, props.getProperty(key), file.file.toString().substring(file.projectRootFolder.length()));
                map.put(key, item);
            }
            for (Item item : map.values()) {
                emitter.onNext(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            emitter.onComplete();
        }
    }

    private void searchFiles(ObservableEmitter<ValidFile> emitter, File folder, String projectRootFolder, List<VirtualFile> excludedRoots) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    VirtualFile fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f);
                    boolean isExcluded = false;
                    for (VirtualFile excluded : excludedRoots) {
                        if (excluded.equals(fVirtual)) {
                            isExcluded = true;
                            break;
                        }
                    }
                    if (!isExcluded) {
                        searchFiles(emitter, f, projectRootFolder, excludedRoots);
                    }
                } else if (f.toString().endsWith(".properties") || f.getName().equals("settings.gradle")) {
                    emitter.onNext(new ValidFile(f, projectRootFolder));
                }
            }
        }
    }


    private static class PropsTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
//                if (col == 0) return false;
//                return super.isCellEditable(row, col);
        }
    }
}
