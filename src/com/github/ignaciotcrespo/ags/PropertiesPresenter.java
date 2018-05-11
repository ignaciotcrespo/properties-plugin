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
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.ignaciotcrespo.ags.RxUtils.avoidFastClicks;
import static com.github.ignaciotcrespo.ags.RxUtils.doubleClick;

class PropertiesPresenter {

    private PublishSubject<UiEvent> uiEvents = PublishSubject.create();
    private PublishSubject<PresenterEvent> presenterEvents = PublishSubject.create();
    private PropsTableModel tableModel = new PropsTableModel();

    PropertiesPresenter() {
        tableModel.setColumnIdentifiers(new Object[]{"File", "Name", "Value"});

        uiEvents
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .ofType(RefreshUiEvent.class)
                .compose(avoidFastClicks())
                .retry()
                .doOnError(System.out::println)
                .subscribe(ui -> refresh(ui.project, tableModel));

        uiEvents
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .ofType(ItemClickedUiEvent.class)
                .compose(doubleClick())
                .subscribe(this::tryToOpenFile);

        uiEvents
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .ofType(RefreshQuickReferenceUiEvent.class)
                .subscribe(this::refreshQuickReference);

        uiEvents
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .ofType(CheckChangedFiles.class)
                .subscribe(this::checkChangedFiles);
    }

    private void checkChangedFiles(CheckChangedFiles checkChangedFiles) {
        for (VFileEvent ev : checkChangedFiles.events) {
            VirtualFile file = ev.getFile();
            if (file != null) {
                if ((file.getName().endsWith(".properties") || file.getName().equals("settings.gradle"))) {
                    refreshPropertiesData(checkChangedFiles.project);
                    break;
                }
            }
        }
    }

    private void refreshQuickReference(RefreshQuickReferenceUiEvent __) {
        try {
            String txt = read(this.getClass().getResourceAsStream("android_gradle_plugin_reference.html"));
            if (txt != null && txt.length() > 0) {
                presenterEvents.onNext(new QuickReferenceLoadedPresenterEvent("<html>" + txt + "</html>"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    private void tryToOpenFile(ItemClickedUiEvent ui) {
        int row = ui.row;
        Object item = tableModel.getItem(row);
        if (item instanceof ValidFile) {
            openValidFile(ui.project, (ValidFile) item);
        } else {
            while (--row >= 0) {
                item = tableModel.getItem(row);
                if (item instanceof ValidFile) {
                    openValidFile(ui.project, (ValidFile) item);
                    break;
                }
            }
        }
    }

    void refreshPropertiesData(Project project) {
        uiEvents.onNext(new RefreshUiEvent(project));
    }

    private void refresh(@NotNull Project project, PropsTableModel tableModel) {
        getValidFilesObservable(project)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .doOnNext(tableModel::addItem)
                .flatMap(this::getItemsObservable)
                .doOnNext(tableModel::addItem)
                .doOnSubscribe(disposable -> tableModel.clear())
                .subscribe();
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
                if (projectRootFolder != null) {
                    File file1 = new File(projectRootFolder);
                    searchFiles(emitter, file1, projectRootFolder, allExcludedRoots);
                }
            } finally {
                emitter.onComplete();
            }
        });
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

    private Observable<Item> getItemsObservable(ValidFile f) {
        return Observable.create(emitter -> {
            searchProperties(emitter, f);
            emitter.onComplete();
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

    PropsTableModel getTableModel() {
        return tableModel;
    }

    void itemClicked(int row, Project project) {
        uiEvents.onNext(new ItemClickedUiEvent(row, project));
    }

    private void openValidFile(@NotNull Project project, ValidFile item) {
        VirtualFile fileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(item.file);
        if (fileByIoFile != null) {
            runInUiThread(() -> FileEditorManager.getInstance(project).openFile(fileByIoFile, true, true));
        }
    }

    private void runInUiThread(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.defaultModalityState());
    }

    void refreshHtmlQuickReference() {
        uiEvents.onNext(new RefreshQuickReferenceUiEvent());
    }

    Observable<PresenterEvent> getPresenterEvents() {
        return presenterEvents;
    }

    void onVFileModified(List<? extends VFileEvent> events, Project project) {
        boolean refreshed = false;
        for (VFileEvent ev : events) {
            if (tableModel.containsFile(ev.getFile())) {
                refreshPropertiesData(project);
                refreshed = true;
                break;
            }
        }
        if (!refreshed) {
            uiEvents.onNext(new CheckChangedFiles(events, project));
        }
    }

}
