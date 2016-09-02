package com.aol.cyclops.types.applicative.zipping;

import java.util.Iterator;
import java.util.function.Function;

import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.types.Functor;
import com.aol.cyclops.types.IterableFunctor;

@FunctionalInterface
public interface ZippingApplicative2<T, T2, R, D extends IterableFunctor<R>> extends Functor<Function<? super T, Function<? super T2, ? extends R>>> {

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#map(java.util.function.Function)
     */
    @Override
    default <U> Functor<U> map(Function<? super Function<? super T, Function<? super T2, ? extends R>>, ? extends U> fn) {
        return delegate().map(fn);
    }

    //<U extends Functor<Function<? super T,? extends R>> & Convertable<Function<? super T,? extends R>>> U delegate();
    IterableFunctor<Function<? super T, Function<? super T2, ? extends R>>> delegate();

    default ZippingApplicative<T2, R, D> ap(Iterable<? extends T> f) {

        Iterator<Function<? super T, Function<? super T2, ? extends R>>> fn = delegate().iterator();
        Iterator<? extends T> it = f.iterator();
        return () -> (IterableFunctor) delegate().unitIterator(ReactiveSeq.fromIterator(fn)
                                                                          .zip(ReactiveSeq.fromIterator(it))
                                                                          .map(t -> t.v1.apply(t.v2))
                                                                          .iterator());

    }
}
