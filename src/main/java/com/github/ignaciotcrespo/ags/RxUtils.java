package com.github.ignaciotcrespo.ags;


import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class RxUtils {

    @NotNull
    public static  <T> ObservableTransformer<T, T> doubleClick() {
        return upstream -> {
            Observable<T> shared = upstream.share();
            return shared
                    .buffer(upstream.debounce(350, TimeUnit.MILLISECONDS))
                    .filter(list -> list.size()>1)
                    .map(list -> list.get(0));

        };
    }

    public static <R> ObservableTransformer<R, R> avoidFastClicks() {
        return upstream -> upstream.throttleFirst(1, TimeUnit.SECONDS);
    }

}
