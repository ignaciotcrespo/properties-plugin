package com.github.ignaciotcrespo.ags;

class QuickReferenceLoadedPresenterEvent implements PresenterEvent {
    final String html;

    QuickReferenceLoadedPresenterEvent(String html) {
        this.html = html;
    }
}
