package com.mobiquity.utils;

import com.mobiquity.api.Item;
import com.mobiquity.exception.APIException;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
public final class FunctionalUtils {
    private FunctionalUtils() {
    }

    public static <T> Safe<T> safe(T object) {
        return () -> ofNullable(object);
    }

    /**
     * Read each line and convert it to resulted line.
     *
     * @param lines
     * @param conversionFunction
     * @return
     * @throws APIException
     */
    public static List<String> readLines(Stream<String> lines, ApiFunction<String, String> conversionFunction) throws APIException {
        AtomicReference<APIException> ex = new AtomicReference<>();
        List<String> result = lines.map(s -> {
            String value = null;
            try {
                value = conversionFunction.apply(s);
            } catch (APIException e) {
                ex.set(e);
            }
            return value;
        }).filter(s -> ex.get() == null).collect(Collectors.toList());
        if (ex.get() != null) {
            throw ex.get();
        }
        return result;
    }

    /**
     * Scan initial string and separate it by lines.
     *
     * @param scanner
     * @return
     */
    public static Stream<String> scanLines(@NonNull Scanner scanner) {
        Spliterator<String> lineReader = Spliterators.spliterator(scanner.useDelimiter("\n"), Long.MAX_VALUE,
                Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(lineReader, false)
                .onClose(scanner::close);
    }

    /**
     * Merge result lines into final string.
     *
     * @param lines
     * @return
     */
    public static String mergeLines(@NonNull List<String> lines) {
        StringJoiner sj = new StringJoiner("\n");
        lines.forEach(sj::add);
        return sj.toString();
    }

    @FunctionalInterface
    public interface SilentFunction<I, O> {
        O apply(I in) throws Exception;
    }

    @FunctionalInterface
    public interface ApiConsumer {
        APIException consume(BigDecimal weight, Item item);
    }

    @FunctionalInterface
    public interface ApiFunction<I, O> {
        O apply(I in) throws APIException;
    }

    @FunctionalInterface
    public interface Safe<T> {
        Optional<T> result();

        default <O> Safe<O> convert(@NonNull SilentFunction<T, O> convert) {
            return convert(convert, null);
        }

        default <O> Safe<O> convert(@NonNull SilentFunction<T, O> convert, Consumer<Exception> handle) {
            return () -> result().map(v -> {
                try {
                    return convert.apply(v);
                } catch (Exception ex) {
                    ofNullable(handle).ifPresent(h -> h.accept(ex));
                }
                return null;
            });
        }

        default <X extends Throwable> T orThrow(@NonNull Supplier<X> exception) throws X {
            return result().orElseThrow(exception);
        }

        default Safe<T> is(@NonNull Predicate<T> predicate) {
            return () -> result().filter(predicate);
        }

        default T orDefault(@NonNull Supplier<T> value) {
            return result().orElseGet(value);
        }
    }
}