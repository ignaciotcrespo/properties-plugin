package com.github.ignaciotcrespo.ags;

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
import java.util.concurrent.TimeUnit;

import static com.github.ignaciotcrespo.ags.RxUtils.avoidFastClicks;
import static com.github.ignaciotcrespo.ags.RxUtils.doubleClick;

class PropertiesPresenter {

    private PublishSubject<UiEvent> uiEvents = PublishSubject.create();
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

    private void tryToOpenFile(ItemClickedUiEvent ui) {
        int row = ui.row;
        Object item = tableModel.getItem(row);
        if (item instanceof ValidFile) {
            openValidFile(ui.project, (ValidFile) item);
        } else if (item instanceof Item) {
            openItemFile(ui.project, (Item) item);
        }
    }

    private void openItemFile(@NotNull Project project, Item item) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            File file = new File(basePath + item.path);
            VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
            if (vf != null) {
                Utils.runInUiThread(() -> FileEditorManager.getInstance(project).openFile(vf, true, true));
            }
        }
    }

    void refreshPropertiesData(Project project) {
        uiEvents.onNext(new RefreshUiEvent(project));
    }

    private void refresh(@NotNull Project project, PropsTableModel tableModel) {
        getValidFilesObservable(project)
                .delay(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .doOnNext(item -> Utils.runInUiThread(() -> tableModel.addItem(item)))
                .flatMap(this::getItemsObservable)
                .doOnNext(item -> Utils.runInUiThread(() -> tableModel.addItem(item)))
                .doOnSubscribe(disposable -> tableModel.clear())
                .subscribe();
    }

    private Observable<ValidFile> getValidFilesObservable(@NotNull Project project) {
        return Observable.create((ObservableEmitter<ValidFile> emitter) -> {
            try {
                String basePath = project.getBasePath();
                if (basePath != null) {
                    List<VirtualFile> allExcludedRoots = new ArrayList<>();
                    Module[] modules = ModuleManager.getInstance(project).getModules();
                    for (Module module : modules) {
                        VirtualFile[] excludedRoots = ModuleRootManager.getInstance(module).getExcludeRoots();
                        allExcludedRoots.addAll(Arrays.asList(excludedRoots));
                    }
                    File file1 = new File(basePath);
                    searchFiles(emitter, file1, basePath, allExcludedRoots);
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
                    if(".gradle".equals(f.getName())){
                        continue;
                    }
                    if(".idea".equals(f.getName())){
                        continue;
                    }
                    if(f.getAbsolutePath().contains("build") && f.getAbsolutePath().contains("generated")){
                        continue;
                    }
                    if(f.getAbsolutePath().contains("build") && f.getAbsolutePath().contains("intermediates")){
                        continue;
                    }
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
            Utils.runInUiThread(() -> FileEditorManager.getInstance(project).openFile(fileByIoFile, true, true));
        }
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
