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
import java.awt.*;

public class GradleSwitchesToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        PropertiesPresenter presenter = new PropertiesPresenter();

        JPanel panel = createPanel();

        createRefreshButton(project, presenter, panel);
        createTable(project, presenter, panel);

        showContent(toolWindow, panel);

        presenter.refreshPropertiesData(project);

        listenForQuickReferenceLoaded(presenter, panel);
        presenter.refreshHtmlQuickReference();
    }

    @NotNull
    private JPanel createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }

    private void showContent(@NotNull ToolWindow toolWindow, JPanel panel) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
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
                if(row >=0) {
                    presenter.itemClicked(row, project);
                }
            }
        });
        JScrollPane tableContainer = new JBScrollPane(table);
        tableContainer.setLayout(new ScrollPaneLayout());

        panel.add(tableContainer);
    }

    private void createRefreshButton(@NotNull Project project, PropertiesPresenter presenter, JPanel panel) {
        JToolBar toolBar = new JToolBar();
        JButton button = new JButton();
        button.setText("Refresh");
        button.addActionListener(__ -> presenter.refreshPropertiesData(project));
        toolBar.add(button);
        toolBar.setFloatable(false);
        panel.add(toolBar);
    }

    private void listenForQuickReferenceLoaded(PropertiesPresenter presenter, JPanel panel) {
        presenter.getPresenterEvents()
                .ofType(QuickReferenceLoadedPresenterEvent.class)
                .subscribe(ev -> {
                    JTextPane label = new JTextPane();
                    label.setContentType("text/html");
                    label.setText(ev.html);
                    JScrollPane htmlContainer = new JBScrollPane(label) {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(panel.getWidth(), (int) (panel.getHeight() / 2.5));
                        }
                    };
                    htmlContainer.setLayout(new ScrollPaneLayout());
                    htmlContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    panel.add(htmlContainer);
                });
    }

}
