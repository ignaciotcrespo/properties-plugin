package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RefreshUiEvent implements UiEvent {

    Project project;

    public RefreshUiEvent(@NotNull Project project) {
        this.project = project;
    }
}
