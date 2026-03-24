package com.github.ignaciotcrespo.ags;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

class Utils {
    static void runInUiThread(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.defaultModalityState());
    }
}
