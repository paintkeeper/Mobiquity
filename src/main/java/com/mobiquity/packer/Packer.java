package com.mobiquity.packer;

import com.mobiquity.api.Item;
import com.mobiquity.exception.APIException;
import com.mobiquity.utils.FunctionalUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.mobiquity.utils.AlgoUtils.*;
import static com.mobiquity.utils.FunctionalUtils.*;

@Slf4j
public final class Packer {
    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_COST = BigDecimal.valueOf(100);
    private static final int MAX_ITEM_AMOUNT = 15;

    private Packer() {
    }

    public static String pack(@NonNull String filePath) throws APIException {
        AtomicReference<Exception> fileReadException = new AtomicReference<>();
        return safe(filePath)
                .convert(Thread.currentThread().getContextClassLoader()::getResource,
                        fileReadException::set)
                .is(Objects::nonNull)
                .convert(URL::getPath)
                .convert(File::new)
                .is(File::exists)
                .is(File::canRead)
                .convert(f -> new Scanner(f, StandardCharsets.UTF_8), fileReadException::set)
                .convert(FunctionalUtils::scanLines)
                .convert(ls -> readLines(ls,
                        l -> listIndexesWithMaxPrice(l,
                                validateMaxWeight(),
                                validateMaxPrice(),
                                validateMaxAmount())),
                        fileReadException::set)
                .convert(FunctionalUtils::mergeLines)
                .orThrow(() -> safe(fileReadException.get())
                        .convert(Throwable::getMessage)
                        .convert(APIException::new)
                        .orDefault(() -> new APIException("Cannot read file.")));
    }

    public static Predicate<BigDecimal> validateMaxWeight() {
        return i -> MAX_WEIGHT.compareTo(i) > -1;
    }

    public static Predicate<BigDecimal> validateMaxPrice() {
        return i -> MAX_COST.compareTo(i) > -1;
    }

    public static Predicate<List<Item>> validateMaxAmount() {
        return i -> i.size() < MAX_ITEM_AMOUNT;
    }
}
