package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;

public class GradleSwitchesToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        PropertiesPresenter presenter = new PropertiesPresenter();

        listenForFileChanges(project, presenter);

        JPanel panel = createPanel();

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        createRefreshButton(project, presenter, buttonsPanel);
        createDuplicatesButton(presenter, buttonsPanel);
        JCheckBox showAllFilesCheckBox = new JCheckBox("Show all files");
        showAllFilesCheckBox.setVisible(false);
        showAllFilesCheckBox.addActionListener(__ -> presenter.getTableModel().setShowAllFiles(showAllFilesCheckBox.isSelected()));
        buttonsPanel.add(showAllFilesCheckBox);
        buttonsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, buttonsPanel.getPreferredSize().height));
        panel.add(buttonsPanel);
        createSearchField(presenter, panel);
        createTable(project, presenter, panel);

        showContent(toolWindow, panel);

        listenForEditorChanges(project, presenter, showAllFilesCheckBox);
        presenter.refreshPropertiesData(project);

    }

    private void listenForEditorChanges(@NotNull Project project, PropertiesPresenter presenter, JCheckBox showAllFilesCheckBox) {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                VirtualFile file = event.getNewFile();
                if (file != null && file.getName().endsWith(".properties")) {
                    String basePath = project.getBasePath();
                    if (basePath != null) {
                        String relativePath = file.getPath().substring(basePath.length());
                        presenter.getTableModel().setFocusedFile(relativePath);
                        showAllFilesCheckBox.setVisible(true);
                    }
                } else {
                    presenter.getTableModel().setFocusedFile(null);
                    showAllFilesCheckBox.setVisible(false);
                }
            }
        });
    }

    private void listenForFileChanges(@NotNull Project project, PropertiesPresenter presenter) {
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                presenter.onVFileModified(events, project);
            }
        });
    }

    @NotNull
    private JPanel createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }

    private void showContent(@NotNull ToolWindow toolWindow, JPanel panel) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void createTable(@NotNull Project project, PropertiesPresenter presenter, JPanel panel) {
        JBTable table = new JBTable();
        table.setExpandableItemsEnabled(true);
        table.setModel(presenter.getTableModel());
        table.setAutoCreateRowSorter(false);
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int col = table.columnAtPoint(evt.getPoint());
                if (col >= 0) {
                    presenter.getTableModel().sortBy(col);
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                    presenter.itemClicked(row, project);
                }
            }
        });
        JScrollPane tableContainer = new JBScrollPane(table);
        tableContainer.setLayout(new ScrollPaneLayout());

        panel.add(tableContainer);
    }

    private void createRefreshButton(@NotNull Project project, PropertiesPresenter presenter, JPanel panel) {
        JButton button = new JButton("Refresh");
        button.addActionListener(__ -> presenter.refreshPropertiesData(project));
        panel.add(button);
    }

    private void createSearchField(PropertiesPresenter presenter, JPanel panel) {
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));

        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search by key name or value");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() { presenter.getTableModel().setSearchText(searchField.getText()); }
        });

        JButton clearButton = new JButton("X");
        clearButton.setToolTipText("Clear search");
        clearButton.addActionListener(__ -> {
            searchField.setText("");
            presenter.getTableModel().setSearchText("");
        });

        searchPanel.add(new JLabel(" Search: "));
        searchPanel.add(searchField);
        searchPanel.add(clearButton);
        searchPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, searchPanel.getPreferredSize().height));
        panel.add(searchPanel);
    }

    private void createDuplicatesButton(PropertiesPresenter presenter, JPanel panel) {
        JCheckBox checkBox = new JCheckBox("Duplicates only");
        checkBox.addActionListener(__ -> {
            if (checkBox.isSelected()) {
                presenter.getTableModel().showDuplicatesOnly();
            } else {
                presenter.getTableModel().showAll();
            }
        });
        panel.add(checkBox);
    }


}
