package com.github.ignaciotcrespo.ags;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URL;

public class GradleSwitchesToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        PropertiesPresenter presenter = new PropertiesPresenter();

        listenForFileChanges(project, presenter);

        JPanel panel = createPanel();

        createRefreshButton(project, presenter, panel);
        createTable(project, presenter, panel);

        showContent(toolWindow, panel);

        presenter.refreshPropertiesData(project);

        listenForQuickReferenceLoaded(presenter, panel);
        presenter.refreshHtmlQuickReference();
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
//        JToolBar toolBar = new JToolBar();
        JButton button = new JButton();
        button.setText("Refresh");
        button.setAlignmentX(JButton.CENTER_ALIGNMENT);
        button.addActionListener(__ -> presenter.refreshPropertiesData(project));
//        toolBar.add(button);
//        toolBar.setFloatable(false);
        panel.add(button);
    }

    private void listenForQuickReferenceLoaded(PropertiesPresenter presenter, JPanel panel) {
        presenter.getPresenterEvents()
                .ofType(QuickReferenceLoadedPresenterEvent.class)
                .subscribe(ev -> {
                    JEditorPane label = new JEditorPane();
                    label.setContentType("text/html");
                    label.setEditable(false);
                    label.setEnabled(true);
                    label.addHyperlinkListener(hyperlinkEvent -> {
                        if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            URL url = hyperlinkEvent.getURL();
                            if (url != null) {
                                BrowserUtil.browse(url);
                            }
                        }
                    });
                    label.setText(ev.html);
                    JScrollPane htmlContainer = new JBScrollPane(label) {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(panel.getWidth(), (int) (panel.getHeight() / 2.5));
                        }
                    };
                    htmlContainer.setLayout(new ScrollPaneLayout());
                    htmlContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                    separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
                    Utils.runInUiThread(() -> {
                        panel.add(separator);
                        panel.add(htmlContainer);
                    });
                });
    }

}
