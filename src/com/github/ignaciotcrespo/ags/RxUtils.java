package com.github.ignaciotcrespo.ags;


import io.reactivex.Observable;

import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

public class RxUtils {

    public static <T>Observable<T> doubleClick(Observable<T> obs){
        Observable<T> shared = obs.share();
        return shared
                .buffer(obs.debounce(200, TimeUnit.MILLISECONDS))
                .filter(list -> list.size()>1)
                .map(list -> list.get(0));

    }
}
