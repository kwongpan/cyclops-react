package com.aol.cyclops2.internal.stream.spliterators.push;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by johnmcclean on 12/01/2017.
 */
public class FilterOperator<T> extends BaseOperator<T,T> {


    Predicate<? super T> predicate;

    public FilterOperator(Operator<T> source, Predicate<? super T> predicate){
        super(source);
        this.predicate = predicate;


    }


    @Override
    public StreamSubscription subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        return  source.subscribe(e-> {
                    try {
                        if(predicate.test(e))
                            onNext.accept(e);
                    } catch (Throwable t) {

                        onError.accept(t);
                    }
                }
                ,onError,onComplete);
    }

    @Override
    public void subscribeAll(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onCompleteDs) {

        source.subscribeAll(e-> {
                    try {
                        if(predicate.test(e))
                            onNext.accept(e);
                    } catch (Throwable t) {

                        onError.accept(t);
                    }
                }
                ,onError,onCompleteDs);
    }
}
