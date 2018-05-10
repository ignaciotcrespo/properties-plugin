package com.github.ignaciotcrespo.ags;

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
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

public class GradleSwitchesToolWindowFactory implements ToolWindowFactory {

    private JPanel panel;

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

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

//        Observable.create(new ObservableOnSubscribe<Item>() {
//            @Override
//            public void subscribe(ObservableEmitter<Item> emitter) throws Exception {
//                Module[] modules = ModuleManager.getInstance(project).getModules();
//                VirtualFile file = ModuleRootManager.getInstance(modules[0]).getContentRoots()[0];
//                List<VirtualFile> allExcludedRoots = new ArrayList<>();
//                for (Module module: modules) {
//                    VirtualFile[] excludedRoots = ModuleRootManager.getInstance(module).getExcludeRoots();
//                    allExcludedRoots.addAll(Arrays.asList(excludedRoots));
//
////            for (VirtualFile f: excludedRoots) {
////                names.add(f.toString());
////                values.add("excluded");
////                filePaths.add("meh");
////            }
//                }
//                String projectRootFolder = file.getCanonicalPath();
//                File file1 = new File(projectRootFolder);
//                searchProperties(emitter, file1, projectRootFolder, allExcludedRoots);
//            }
//        });

        Observable.fromCallable(new Callable<JScrollPane>() {
            @Override
            public JScrollPane call() throws Exception {
                List<String> names = new ArrayList<>();
                List<String> values = new ArrayList<>();
                List<String> filePaths = new ArrayList<>();

                Module[] modules = ModuleManager.getInstance(project).getModules();
                VirtualFile file = ModuleRootManager.getInstance(modules[0]).getContentRoots()[0];
                List<VirtualFile> allExcludedRoots = new ArrayList<>();
                for (Module module: modules) {
                    VirtualFile[] excludedRoots = ModuleRootManager.getInstance(module).getExcludeRoots();
                    allExcludedRoots.addAll(Arrays.asList(excludedRoots));

//            for (VirtualFile f: excludedRoots) {
//                names.add(f.toString());
//                values.add("excluded");
//                filePaths.add("meh");
//            }
                }
                String projectRootFolder = file.getCanonicalPath();
                File file1 = new File(projectRootFolder);
                searchProperties(names, values, filePaths, file1, projectRootFolder, allExcludedRoots);

                String[] columnNames = {"Name", "Value", "File"};
                Object[][] data = new Object[names.size()][3];
                for (int i = 0; i < names.size(); i++) {
                    data[i] = new String[]{names.get(i), values.get(i), filePaths.get(i)};
                }
                //{{projectRootFolder, "Smith"},{"John", "Doe"}};
                JTable table = new JTable(data, columnNames);
                table.setFillsViewportHeight(true);



//        ListTableModel<String> data = new ListTableModel<>();
//        data.addRow("uno");
//        data.addRow("dos");
//        TableColumnModel columnNames = new DefaultTableColumnModel();
//        TableColumn tableColumn = new TableColumn();
//        tableColumn.setHeaderValue("titulazo!");
//        columnNames.addColumn(tableColumn);
//        JBTable table = new JBTable(data, columnNames);
//        table.setAutoResizeMode(JBTable.AUTO_RESIZE_ALL_COLUMNS);
//        table.setAutoCreateColumnsFromModel(true);
//        table.getEmptyText().setText("empty!!");
//        table.setFillsViewportHeight(true);

                JScrollPane tableContainer = new JBScrollPane(table);
                tableContainer.setLayout(new ScrollPaneLayout());

                panel = new JPanel();
                panel.add(tableContainer, BorderLayout.CENTER);
//        frame.getContentPane().add(panel);
//
//        frame.pack();
//        frame.setVisible(true);
                return tableContainer;
            }
        })
//                .observeOn(Schedulers.trampoline())
//                .subscribeOn(Schedulers.newThread())
        .subscribe(tableContainer -> {
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(tableContainer, "", false);


            toolWindow.getContentManager().addContent(content);

        }, System.out::println)
        ;




    }

    private void searchProperties(ObservableEmitter<Item> emitter, File folder, String projectRootFolder, List<VirtualFile> excludedRoots) {
        File[] files = folder.listFiles();
        if(files != null) {
            for (File f: files) {
                if(f.isDirectory()){
                    VirtualFile fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f);
                    boolean isExcluded = false;
                    for (VirtualFile excluded: excludedRoots) {
                        if(excluded.equals(fVirtual)){
                            isExcluded = true;
                            break;
                        }
                    }
                    if(!isExcluded) {
                        searchProperties(emitter, f, projectRootFolder, excludedRoots);
                    }
                } else if(f.toString().endsWith(".properties") || f.getName().equals("settings.gradle")){
                    Properties props = new Properties();
                    try {
                        props.load(new FileInputStream(f));
                        Enumeration<?> propertyNames = props.propertyNames();
                        while (propertyNames.hasMoreElements()) {
                            String key = propertyNames.nextElement().toString();
                            emitter.onNext(new Item(key, props.getProperty(key), f.toString().substring(projectRootFolder.length())));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void searchProperties(List<String> names, List<String> values, List<String> filePaths, File folder, String projectRootFolder, List<VirtualFile> excludedRoots) {
        File[] files = folder.listFiles();
        if(files != null) {
            for (File f: files) {
                if(f.isDirectory()){
                    VirtualFile fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f);
                    boolean isExcluded = false;
                    for (VirtualFile excluded: excludedRoots) {
                        if(excluded.equals(fVirtual)){
                            isExcluded = true;
                            break;
                        }
                    }
                    if(!isExcluded) {
                        searchProperties(names, values, filePaths, f, projectRootFolder, excludedRoots);
                    }
                } else if(f.toString().endsWith(".properties") || f.getName().equals("settings.gradle")){
                    Properties props = new Properties();
                    try {
                        props.load(new FileInputStream(f));
                        Enumeration<?> propertyNames = props.propertyNames();
                        while (propertyNames.hasMoreElements()) {
                            String key = propertyNames.nextElement().toString();
                            names.add(key);
                            values.add(props.getProperty(key));
                            filePaths.add(f.toString().substring(projectRootFolder.length()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
