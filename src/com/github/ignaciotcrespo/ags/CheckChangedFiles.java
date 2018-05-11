package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import java.util.List;

class CheckChangedFiles implements UiEvent {
    final List<? extends VFileEvent> events;
    final Project project;

    CheckChangedFiles(List<? extends VFileEvent> events, Project project) {
        this.events = events;
        this.project = project;
    }
}
