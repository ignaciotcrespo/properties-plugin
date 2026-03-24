package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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
        buttonsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, buttonsPanel.getPreferredSize().height));
        panel.add(buttonsPanel);
        createTable(project, presenter, panel);

        showContent(toolWindow, panel);

        presenter.refreshPropertiesData(project);

    }

    private void listenForFileChanges(@NotNull Project project, PropertiesPresenter presenter) {
//        MessageBusConnection bus = project.getMessageBus().connect();
//        bus.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener(){
//            @Override
//            public void after(@NotNull List<? extends VFileEvent> events) {
//                presenter.onVFileModified(events, project);
//            }
//        });
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
        //JTable table = new TreeTable(new ListTreeTableModel());
        JBTable table = new JBTable();
        table.setExpandableItemsEnabled(true);
        table.setModel(presenter.getTableModel());
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
