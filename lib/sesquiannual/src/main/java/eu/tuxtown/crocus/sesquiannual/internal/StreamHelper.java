package eu.tuxtown.crocus.sesquiannual.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NotNullByDefault
public class StreamHelper {

    public static <T> Stream<Map.Entry<Integer, T>> indexed(Stream<T> stream) {
        Iterator<T> iterator = stream.iterator();
        AtomicInteger idx = new AtomicInteger(0);
        Iterator<Map.Entry<Integer, T>> indexedIterator = new Iterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<Integer, T> next() {
                T next = iterator.next();
                return Map.entry(idx.getAndIncrement(), next);
            }
        };
        Spliterator<Map.Entry<Integer, T>> spliterator = Spliterators.spliteratorUnknownSize(indexedIterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    public static <T> Stream<T> mergeOrderedStreams(List<Stream<T>> streams, Comparator<T> order) {
        if (streams.isEmpty()) return Stream.empty();
        List<Iterator<T>> iterators = streams.stream().map(stream -> stream.peek(Objects::requireNonNull).iterator()).toList();
        List<@Nullable T> cur = new ArrayList<>(iterators.size());
        for (Iterator<T> itr : iterators) cur.add(itr.hasNext() ? itr.next() : null);
        Iterator<T> iterator = new Iterator<>() {

            @Override
            public boolean hasNext() {
                for (int i = 0; i < iterators.size(); i++) {
                    if (cur.get(i) != null) return true;
                }
                return false;
            }

            @Override
            public T next() {
                int minIdx = -1;
                T min = null;
                for (int i = 0; i < iterators.size(); i++) {
                    T inst = cur.get(i);
                    if (inst != null && (minIdx < 0 || order.compare(inst, min) < 0)) {
                        minIdx = i;
                        min = inst;
                    }
                }
                if (minIdx < 0) throw new NoSuchElementException();
                cur.set(minIdx, iterators.get(minIdx).hasNext() ? Objects.requireNonNull(iterators.get(minIdx).next()) : null);
                return min;
            }
        };
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }
}
