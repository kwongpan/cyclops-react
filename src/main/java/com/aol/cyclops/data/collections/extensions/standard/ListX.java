package com.aol.cyclops.data.collections.extensions.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;
import org.reactivestreams.Publisher;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.StreamUtils;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.types.IterableFunctor;
import com.aol.cyclops.types.OnEmptySwitch;
import com.aol.cyclops.types.applicative.zipping.ZippingApplicativable;

public interface ListX<T> extends List<T>, MutableCollectionX<T>, MutableSequenceX<T>, Comparable<T>, IterableFunctor<T>, ZippingApplicativable<T>,
        OnEmptySwitch<T, List<T>> {

    /**
     * Create a ListX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static ListX<Integer> range(int start, int end) {
        return ReactiveSeq.range(start, end)
                          .toListX();
    }

    /**
     * Create a ListX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static ListX<Long> rangeLong(long start, long end) {
        return ReactiveSeq.rangeLong(start, end)
                          .toListX();
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  ListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    static <U, T> ListX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return ReactiveSeq.unfold(seed, unfolder)
                          .toListX();
    }

    /**
     * Generate a ListX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> ListX<T> generate(long limit, Supplier<T> s) {

        return ReactiveSeq.generate(s)
                          .limit(limit)
                          .toListX();
    }

    /**
     * Create a ListX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> ListX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return ReactiveSeq.iterate(seed, f)
                          .limit(limit)
                          .toListX();

    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.sequence.traits.ConvertableSequence#toListX()
     */
    @Override
    default ListX<T> toListX() {
        return this;
    }

    static <T> Collector<T, ?, ListX<T>> listXCollector() {
        return Collectors.toCollection(() -> ListX.of());
    }

    static <T> Collector<T, ?, List<T>> defaultCollector() {
        return Collectors.toCollection(() -> new ArrayList<>());
    }

    static <T> Collector<T, ?, List<T>> immutableCollector() {
        return Collectors.collectingAndThen(defaultCollector(), (List<T> d) -> Collections.unmodifiableList(d));

    }

    public static <T> ListX<T> empty() {
        return fromIterable((List<T>) defaultCollector().supplier()
                                                        .get());
    }

    @SafeVarargs
    public static <T> ListX<T> of(T... values) {
        List<T> res = (List<T>) defaultCollector().supplier()
                                                  .get();
        for (T v : values)
            res.add(v);
        return fromIterable(res);
    }

    public static <T> ListX<T> singleton(T value) {
        return ListX.<T> of(value);
    }

    /**
     * Construct a ListX from an Publisher
     * 
     * @param publisher
     *            to construct ListX from
     * @return ListX
     */
    public static <T> ListX<T> fromPublisher(Publisher<? extends T> publisher) {
        return ReactiveSeq.fromPublisher((Publisher<T>) publisher)
                          .toListX();
    }

    public static <T> ListX<T> fromIterable(Iterable<T> it) {
        return fromIterable(defaultCollector(), it);
    }

    public static <T> ListX<T> fromIterable(Collector<T, ?, List<T>> collector, Iterable<T> it) {
        if (it instanceof ListX)
            return (ListX<T>) it;
        if (it instanceof List)
            return new ListXImpl<T>(
                                    (List<T>) it, collector);
        return new ListXImpl<T>(
                                StreamUtils.stream(it)
                                           .collect(collector),
                                collector);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.FluentCollectionX#unit(java.util.Collection)
     */
    @Override
    default <R> ListX<R> unit(Collection<R> col) {
        return fromIterable(col);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Unit#unit(java.lang.Object)
     */
    @Override
    default <R> ListX<R> unit(R value) {
        return singleton(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.IterableFunctor#unitIterator(java.util.Iterator)
     */
    @Override
    default <R> ListX<R> unitIterator(Iterator<R> it) {
        return fromIterable(() -> it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#patternMatch(java.util.function.Function, java.util.function.Supplier)
     */
    @Override
    default <R> ListX<R> patternMatch(Function<CheckValue1<T, R>, CheckValue1<T, R>> case1, Supplier<? extends R> otherwise) {
        return (ListX<R>) MutableCollectionX.super.patternMatch(case1, otherwise);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#stream()
     */
    @Override
    default ReactiveSeq<T> stream() {

        return ReactiveSeq.fromIterable(this);
    }

    /**
     * @return A Collector to generate a List
     */
    public <T> Collector<T, ?, List<T>> getCollector();

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.CollectionX#from(java.util.Collection)
     */
    default <T1> ListX<T1> from(Collection<T1> c) {
        return ListX.<T1> fromIterable(getCollector(), c);
    }

    default <X> ListX<X> fromStream(Stream<X> stream) {
        return new ListXImpl<>(
                               stream.collect(getCollector()), getCollector());
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#reverse()
     */
    @Override
    default ListX<T> reverse() {

        return (ListX<T>) MutableCollectionX.super.reverse();
    }

    /**
     * Combine two adjacent elements in a ListX using the supplied BinaryOperator
     * This is a stateful grouping and reduction operation. The output of a combination may in turn be combined
     * with it's neighbor
     * <pre>
     * {@code 
     *  ListX.of(1,1,2,3)
                   .combine((a, b)->a.equals(b),Semigroups.intSum)
                   .toListX()
                   
     *  //ListX(3,4) 
     * }</pre>
     * 
     * @param predicate Test to see if two neighbors should be joined
     * @param op Reducer to combine neighbors
     * @return Combined / Partially Reduced ListX
     */
    default ListX<T> combine(BiPredicate<? super T, ? super T> predicate, BinaryOperator<T> op) {
        return (ListX<T>) MutableCollectionX.super.combine(predicate, op);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#filter(java.util.function.Predicate)
     */
    @Override
    default ListX<T> filter(Predicate<? super T> pred) {

        return (ListX<T>) MutableCollectionX.super.filter(pred);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#map(java.util.function.Function)
     */
    @Override
    default <R> ListX<R> map(Function<? super T, ? extends R> mapper) {

        return (ListX<R>) MutableCollectionX.super.<R> map(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#flatMap(java.util.function.Function)
     */
    @Override
    default <R> ListX<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {

        return (ListX<R>) MutableCollectionX.super.<R> flatMap(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#limit(long)
     */
    @Override
    default ListX<T> limit(long num) {

        return (ListX<T>) MutableCollectionX.super.limit(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#skip(long)
     */
    @Override
    default ListX<T> skip(long num) {

        return (ListX<T>) MutableCollectionX.super.skip(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#takeRight(int)
     */
    default ListX<T> takeRight(int num) {
        return (ListX<T>) MutableCollectionX.super.takeRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#dropRight(int)
     */
    default ListX<T> dropRight(int num) {
        return (ListX<T>) MutableCollectionX.super.dropRight(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#takeWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> takeWhile(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.takeWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#dropWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> dropWhile(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.dropWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#takeUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> takeUntil(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.takeUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#dropUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> dropUntil(Predicate<? super T> p) {
        return (ListX<T>) MutableCollectionX.super.dropUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#trampoline(java.util.function.Function)
     */
    @Override
    default <R> ListX<R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (ListX<R>) MutableCollectionX.super.<R> trampoline(mapper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#slice(long, long)
     */
    @Override
    default ListX<T> slice(long from, long to) {
        return (ListX<T>) MutableCollectionX.super.slice(from, to);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#sorted(java.util.function.Function)
     */
    @Override
    default <U extends Comparable<? super U>> ListX<T> sorted(Function<? super T, ? extends U> function) {

        return (ListX<T>) MutableCollectionX.super.sorted(function);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#grouped(int)
     */
    default ListX<ListX<T>> grouped(int groupSize) {
        return (ListX<ListX<T>>) MutableCollectionX.super.grouped(groupSize);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#grouped(java.util.function.Function, java.util.stream.Collector)
     */
    default <K, A, D> ListX<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return (ListX) MutableCollectionX.super.grouped(classifier, downstream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#grouped(java.util.function.Function)
     */
    default <K> ListX<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier) {
        return (ListX) MutableCollectionX.super.grouped(classifier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#zip(java.lang.Iterable)
     */
    default <U> ListX<Tuple2<T, U>> zip(Iterable<? extends U> other) {
        return (ListX) MutableCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#zip(java.lang.Iterable, java.util.function.BiFunction)
     */
    @Override
    default <U, R> ListX<R> zip(Iterable<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (ListX<R>) MutableCollectionX.super.zip(other, zipper);
    }

    @Override
    default <U, R> ListX<R> zip(Seq<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (ListX<R>) MutableCollectionX.super.zip(other, zipper);
    }

    @Override
    default <U, R> ListX<R> zip(Stream<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {

        return (ListX<R>) MutableCollectionX.super.zip(other, zipper);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#sliding(int)
     */
    default ListX<ListX<T>> sliding(int windowSize) {
        return (ListX<ListX<T>>) MutableCollectionX.super.sliding(windowSize);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#sliding(int, int)
     */
    default ListX<ListX<T>> sliding(int windowSize, int increment) {
        return (ListX<ListX<T>>) MutableCollectionX.super.sliding(windowSize, increment);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#scanLeft(com.aol.cyclops.Monoid)
     */
    default ListX<T> scanLeft(Monoid<T> monoid) {
        return (ListX<T>) MutableCollectionX.super.scanLeft(monoid);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#scanLeft(java.lang.Object, java.util.function.BiFunction)
     */
    default <U> ListX<U> scanLeft(U seed, BiFunction<? super U, ? super T, ? extends U> function) {
        return (ListX<U>) MutableCollectionX.super.scanLeft(seed, function);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#scanRight(com.aol.cyclops.Monoid)
     */
    default ListX<T> scanRight(Monoid<T> monoid) {
        return (ListX<T>) MutableCollectionX.super.scanRight(monoid);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#scanRight(java.lang.Object, java.util.function.BiFunction)
     */
    default <U> ListX<U> scanRight(U identity, BiFunction<? super T, ? super U, ? extends U> combiner) {
        return (ListX<U>) MutableCollectionX.super.scanRight(identity, combiner);
    }

    /* Makes a defensive copy of this ListX replacing the value at i with the specified element
     *  (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableSequenceX#with(int, java.lang.Object)
     */
    default ListX<T> with(int i, T element) {
        return from(stream().deleteBetween(i, i + 1)
                            .insertAt(i, element)
                            .collect(getCollector()));
    }

    /* (non-Javadoc)
     * @see java.util.List#subList(int, int)
     */
    public ListX<T> subList(int start, int end);

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#plus(java.lang.Object)
     */
    default ListX<T> plus(T e) {
        add(e);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#plusAll(java.util.Collection)
     */
    default ListX<T> plusAll(Collection<? extends T> list) {
        addAll(list);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableSequenceX#minus(int)
     */
    default ListX<T> minus(int pos) {
        remove(pos);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#minus(java.lang.Object)
     */
    default ListX<T> minus(Object e) {
        remove(e);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#minusAll(java.util.Collection)
     */
    default ListX<T> minusAll(Collection<?> list) {
        removeAll(list);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableSequenceX#plus(int, java.lang.Object)
     */
    @Override
    default ListX<T> plus(int i, T e) {
        add(i, e);
        return this;
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableSequenceX#plusAll(int, java.util.Collection)
     */
    default ListX<T> plusAll(int i, Collection<? extends T> list) {
        addAll(i, list);
        return this;
    }

    @Override
    int size();

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.FluentCollectionX#plusInOrder(java.lang.Object)
     */
    @Override
    default ListX<T> plusInOrder(T e) {

        return (ListX<T>) MutableSequenceX.super.plusInOrder(e);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.CollectionX#peek(java.util.function.Consumer)
     */
    @Override
    default ListX<T> peek(Consumer<? super T> c) {

        return (ListX<T>) MutableCollectionX.super.peek(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#cycle(int)
     */
    @Override
    default ListX<T> cycle(int times) {

        return (ListX<T>) MutableCollectionX.super.cycle(times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#cycle(com.aol.cyclops.sequence.Monoid, int)
     */
    @Override
    default ListX<T> cycle(Monoid<T> m, int times) {

        return (ListX<T>) MutableCollectionX.super.cycle(m, times);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#cycleWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> cycleWhile(Predicate<? super T> predicate) {

        return (ListX<T>) MutableCollectionX.super.cycleWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#cycleUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> cycleUntil(Predicate<? super T> predicate) {

        return (ListX<T>) MutableCollectionX.super.cycleUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#zip(java.util.stream.Stream)
     */
    @Override
    default <U> ListX<Tuple2<T, U>> zip(Stream<? extends U> other) {

        return (ListX) MutableCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#zip(org.jooq.lambda.Seq)
     */
    @Override
    default <U> ListX<Tuple2<T, U>> zip(Seq<? extends U> other) {

        return (ListX) MutableCollectionX.super.zip(other);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#zip3(java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <S, U> ListX<Tuple3<T, S, U>> zip3(Stream<? extends S> second, Stream<? extends U> third) {

        return (ListX) MutableCollectionX.super.zip3(second, third);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#zip4(java.util.stream.Stream, java.util.stream.Stream, java.util.stream.Stream)
     */
    @Override
    default <T2, T3, T4> ListX<Tuple4<T, T2, T3, T4>> zip4(Stream<? extends T2> second, Stream<? extends T3> third, Stream<? extends T4> fourth) {

        return (ListX) MutableCollectionX.super.zip4(second, third, fourth);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#zipWithIndex()
     */
    @Override
    default ListX<Tuple2<T, Long>> zipWithIndex() {

        return (ListX<Tuple2<T, Long>>) MutableCollectionX.super.zipWithIndex();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#sorted()
     */
    @Override
    default ListX<T> sorted() {

        return (ListX<T>) MutableCollectionX.super.sorted();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#sorted(java.util.Comparator)
     */
    @Override
    default ListX<T> sorted(Comparator<? super T> c) {

        return (ListX<T>) MutableCollectionX.super.sorted(c);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#skipWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> skipWhile(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.skipWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#skipUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> skipUntil(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.skipUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#shuffle()
     */
    @Override
    default ListX<T> shuffle() {

        return (ListX<T>) MutableCollectionX.super.shuffle();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#skipLast(int)
     */
    @Override
    default ListX<T> skipLast(int num) {

        return (ListX<T>) MutableCollectionX.super.skipLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#shuffle(java.util.Random)
     */
    @Override
    default ListX<T> shuffle(Random random) {

        return (ListX<T>) MutableCollectionX.super.shuffle(random);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#permutations()
     */
    @Override
    default ListX<ReactiveSeq<T>> permutations() {

        return (ListX<ReactiveSeq<T>>) MutableCollectionX.super.permutations();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#combinations(int)
     */
    @Override
    default ListX<ReactiveSeq<T>> combinations(int size) {

        return (ListX<ReactiveSeq<T>>) MutableCollectionX.super.combinations(size);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Traversable#combinations()
     */
    @Override
    default ListX<ReactiveSeq<T>> combinations() {

        return (ListX<ReactiveSeq<T>>) MutableCollectionX.super.combinations();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.lambda.monads.Functor#cast(java.lang.Class)
     */
    @Override
    default <U> ListX<U> cast(Class<? extends U> type) {

        return (ListX<U>) MutableCollectionX.super.cast(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#distinct()
     */
    @Override
    default ListX<T> distinct() {

        return (ListX<T>) MutableCollectionX.super.distinct();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#limitWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<T> limitWhile(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.limitWhile(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#limitUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<T> limitUntil(Predicate<? super T> p) {

        return (ListX<T>) MutableCollectionX.super.limitUntil(p);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#intersperse(java.lang.Object)
     */
    @Override
    default ListX<T> intersperse(T value) {

        return (ListX<T>) MutableCollectionX.super.intersperse(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#limitLast(int)
     */
    @Override
    default ListX<T> limitLast(int num) {

        return (ListX<T>) MutableCollectionX.super.limitLast(num);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#onEmpty(java.lang.Object)
     */
    @Override
    default ListX<T> onEmpty(T value) {

        return (ListX<T>) MutableCollectionX.super.onEmpty(value);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#onEmptyGet(java.util.function.Supplier)
     */
    @Override
    default ListX<T> onEmptyGet(Supplier<? extends T> supplier) {

        return (ListX<T>) MutableCollectionX.super.onEmptyGet(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#onEmptyThrow(java.util.function.Supplier)
     */
    @Override
    default <X extends Throwable> ListX<T> onEmptyThrow(Supplier<? extends X> supplier) {

        return (ListX<T>) MutableCollectionX.super.onEmptyThrow(supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#ofType(java.lang.Class)
     */
    @Override
    default <U> ListX<U> ofType(Class<? extends U> type) {

        return (ListX<U>) MutableCollectionX.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#filterNot(java.util.function.Predicate)
     */
    @Override
    default ListX<T> filterNot(Predicate<? super T> fn) {

        return (ListX<T>) MutableCollectionX.super.filterNot(fn);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#notNull()
     */
    @Override
    default ListX<T> notNull() {

        return (ListX<T>) MutableCollectionX.super.notNull();
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#removeAll(java.util.stream.Stream)
     */
    @Override
    default ListX<T> removeAll(Stream<? extends T> stream) {

        return (ListX<T>) MutableCollectionX.super.removeAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#removeAll(java.lang.Iterable)
     */
    @Override
    default ListX<T> removeAll(Iterable<? extends T> it) {

        return (ListX<T>) MutableCollectionX.super.removeAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#removeAll(java.lang.Object[])
     */
    @Override
    default ListX<T> removeAll(T... values) {

        return (ListX<T>) MutableCollectionX.super.removeAll(values);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#retainAll(java.lang.Iterable)
     */
    @Override
    default ListX<T> retainAll(Iterable<? extends T> it) {

        return (ListX<T>) MutableCollectionX.super.retainAll(it);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#retainAll(java.util.stream.Stream)
     */
    @Override
    default ListX<T> retainAll(Stream<? extends T> seq) {

        return (ListX<T>) MutableCollectionX.super.retainAll(seq);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.collections.extensions.standard.MutableCollectionX#retainAll(java.lang.Object[])
     */
    @Override
    default ListX<T> retainAll(T... values) {

        return (ListX<T>) MutableCollectionX.super.retainAll(values);
    }

    /* (non-Javadoc)
    * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#grouped(int, java.util.function.Supplier)
    */
    @Override
    default <C extends Collection<? super T>> ListX<C> grouped(int size, Supplier<C> supplier) {

        return (ListX<C>) MutableCollectionX.super.grouped(size, supplier);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#groupedUntil(java.util.function.Predicate)
     */
    @Override
    default ListX<ListX<T>> groupedUntil(Predicate<? super T> predicate) {

        return (ListX<ListX<T>>) MutableCollectionX.super.groupedUntil(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#groupedWhile(java.util.function.Predicate)
     */
    @Override
    default ListX<ListX<T>> groupedWhile(Predicate<? super T> predicate) {

        return (ListX<ListX<T>>) MutableCollectionX.super.groupedWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#groupedWhile(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> ListX<C> groupedWhile(Predicate<? super T> predicate, Supplier<C> factory) {

        return (ListX<C>) MutableCollectionX.super.groupedWhile(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#groupedUntil(java.util.function.Predicate, java.util.function.Supplier)
     */
    @Override
    default <C extends Collection<? super T>> ListX<C> groupedUntil(Predicate<? super T> predicate, Supplier<C> factory) {

        return (ListX<C>) MutableCollectionX.super.groupedUntil(predicate, factory);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#groupedStatefullyWhile(java.util.function.BiPredicate)
     */
    @Override
    default ListX<ListX<T>> groupedStatefullyWhile(BiPredicate<ListX<? super T>, ? super T> predicate) {

        return (ListX<ListX<T>>) MutableCollectionX.super.groupedStatefullyWhile(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#removeAll(org.jooq.lambda.Seq)
     */
    @Override
    default ListX<T> removeAll(Seq<? extends T> stream) {

        return (ListX<T>) MutableCollectionX.super.removeAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.data.collections.extensions.standard.MutableCollectionX#retainAll(org.jooq.lambda.Seq)
     */
    @Override
    default ListX<T> retainAll(Seq<? extends T> stream) {
        return (ListX<T>) MutableCollectionX.super.retainAll(stream);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.OnEmptySwitch#onEmptySwitch(java.util.function.Supplier)
     */
    @Override
    default ListX<T> onEmptySwitch(Supplier<? extends List<T>> supplier) {
        if (this.isEmpty())
            return ListX.fromIterable(supplier.get());
        return this;
    }

}
