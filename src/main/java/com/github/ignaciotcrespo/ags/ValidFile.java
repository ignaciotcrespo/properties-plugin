package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

class ValidFile {
    final VirtualFile virtualFile;
    final File file;
    final String projectRootFolder;

    ValidFile(File file, String projectRootFolder) {
        this.file = file;
        this.projectRootFolder = projectRootFolder;
        this.virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
    }

    String getRelativePath() {
        return file.toString().substring(projectRootFolder.length());
    }
}
