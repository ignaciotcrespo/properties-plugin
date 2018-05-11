package com.github.ignaciotcrespo.ags;

import java.io.File;

class ValidFile {
    File file;
    String projectRootFolder;

    public ValidFile(File file, String projectRootFolder) {
        this.file = file;
        this.projectRootFolder = projectRootFolder;
    }

    public String getRelativePath() {
        return file.toString().substring(projectRootFolder.length());
    }
}
