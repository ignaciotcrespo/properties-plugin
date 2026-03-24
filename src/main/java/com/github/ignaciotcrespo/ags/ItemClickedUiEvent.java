package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.project.Project;

class ItemClickedUiEvent implements UiEvent {
    final int row;
    final Project project;

    ItemClickedUiEvent(int row, Project project) {
        this.row = row;
        this.project = project;
    }
}
