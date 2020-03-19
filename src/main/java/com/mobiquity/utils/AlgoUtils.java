package com.mobiquity.utils;

import com.mobiquity.api.Item;
import com.mobiquity.exception.APIException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
@Slf4j
public final class AlgoUtils {

    /**
     * Read result to string value.
     *
     * @param line              line of text to parse
     * @param validateMaxWeight validator for maximum package and item size
     * @param validateMaxCost   validator for maximum item price
     * @param validateItemLimit restriction for item limit* @return result as string value
     */
    public static String listIndexesWithMaxPrice(@NonNull String line,
                                                 @NonNull Predicate<BigDecimal> validateMaxWeight,
                                                 @NonNull Predicate<BigDecimal> validateMaxCost,
                                                 @NonNull Predicate<List<Item>> validateItemLimit) throws APIException {
        AtomicReference<BigDecimal> maxWeight = new AtomicReference<>();
        List<Item> items = readItemsFromLine(line, maxWeight, validateMaxWeight, validateMaxCost, validateItemLimit);
        StringJoiner sj = new StringJoiner(",");
        findMaxPriceLimitedByWeight(items, maxWeight.get())
                .stream()
                .map(Item::getIndex)
                .map(String::valueOf)
                .forEach(sj::add);
        return sj.length() > 0 ? sj.toString() : "-";
    }

    /**
     * Parse line to Item list
     *
     * @param line              Line to read
     * @param maxWeight         temporary storage for package size
     * @param validateMaxWeight validator for maximum package and item size
     * @param validateMaxCost   validator for maximum item price
     * @param validateItemLimit restriction for item limit
     * @return
     * @throws APIException
     */
    public static List<Item> readItemsFromLine(@NonNull String line,
                                               @NonNull AtomicReference<BigDecimal> maxWeight,
                                               @NonNull Predicate<BigDecimal> validateMaxWeight,
                                               @NonNull Predicate<BigDecimal> validateMaxCost,
                                               @NonNull Predicate<List<Item>> validateItemLimit) throws APIException {
        List<Item> items = new ArrayList<>();
        AlgoUtils.readLine(line, (w, val) -> {
            if (maxWeight.compareAndSet(null, w)) {
                if (!validateMaxWeight.test(w)) {
                    return new APIException("Max Package weight value exceeded.");
                }
            }
            if (!validateItemLimit.test(items)) {
                return new APIException("Max Item amount exceeded.");
            }
            if (validateMaxWeight.test(val.getWeight()) && validateMaxCost.test(val.getPrice())) {
                items.add(val);
            }
            return null;
        });
        return items;
    }

    /**
     * Filter initial list by specific weight.
     *
     * @param items  initial array of items
     * @param weight weight to limit
     * @return filtered result
     */
    public static List<Item> findMaxPriceLimitedByWeight(@NonNull List<Item> items, @NonNull BigDecimal weight) {
        List<Item> findings = new ArrayList<>();
        int lastIdx = find(items, 0, 0, BigDecimal.valueOf(0), BigDecimal.valueOf(0), weight, findings);
        List<Item> result;
        int expectedSize = lastIdx + 1;
        if (lastIdx > 0 && expectedSize < findings.size()) {
            result = findings.subList(0, expectedSize);
            log.debug("Shrinking array to expected size of {}", expectedSize);
        } else {
            result = findings;
        }
        return result;
    }

    /**
     * Line parser method.
     *
     * @param line              next readed line to parse
     * @param maxWeightConsumer consumer function for picking parsed items
     */
    public static void readLine(String line, FunctionalUtils.ApiConsumer maxWeightConsumer) throws APIException {
        StringBuilder buff = new StringBuilder();
        AtomicReference<BigDecimal> expectedWeight = new AtomicReference<>();
        AtomicInteger scope = new AtomicInteger(1);
        AtomicReference<APIException> ex = new AtomicReference<>();
        line.codePoints()
                .filter(i -> ex.get() == null)
                .forEach(i -> {
                    if (i == ':') {
                        expectedWeight.set(new BigDecimal(buff.toString().trim()));
                        buff.setLength(0);
                        scope.decrementAndGet();
                    } else if (i == '(') {
                        scope.incrementAndGet();
                    } else if (i == ')') {
                        ex.set(maxWeightConsumer.consume(expectedWeight.get(), pourString(buff)));
                        scope.decrementAndGet();
                    } else if (scope.get() > 0) {
                        buff.append((char) i);
                    }
                });
        if (ex.get() != null) {
            throw ex.get();
        }
    }

    /**
     * Retrieve all data from buffer string.
     *
     * @param sb
     * @return
     */
    private static Item pourString(StringBuilder sb) {
        int indexIdx = sb.indexOf(",", 0);
        int weightIdx = sb.indexOf(",", indexIdx + 1);
        int priceIdx = sb.indexOf("€", weightIdx);
        Integer index = new BigDecimal(sb.substring(0, indexIdx)).intValue();
        BigDecimal weight = new BigDecimal(sb.substring(indexIdx + 1, weightIdx));
        BigDecimal price = new BigDecimal(sb.substring(priceIdx + 1));
        sb.setLength(0);
        return Item.builder().index(index).weight(weight).price(price).build();
    }

    /**
     * @param items     List of Items to check
     * @param next      Next array position
     * @param depth     Result index
     * @param weightSum initial weight amount
     * @param priceSum  initial price amount
     * @param maxWeight maximum allowed weight
     * @param findings  result list to pick values.
     * @return
     */
    private static int find(List<Item> items, int next, int depth, BigDecimal weightSum,
                            BigDecimal priceSum, BigDecimal maxWeight, List<Item> findings) {
        BigDecimal maxWsum = weightSum;
        BigDecimal maxPsum = priceSum;
        int lastIdx = -1;
        for (int i = next; i < items.size(); i++) {
            Item item = items.get(i);
            for (int j = next + 1; j < items.size(); j++) {
                BigDecimal wsum = weightSum.add(item.getWeight());
                BigDecimal psum = priceSum.add(item.getPrice());
                if (wsum.compareTo(maxWeight) < 1) {
                    if (maxPsum.compareTo(psum) < 0 || (maxPsum.compareTo(psum) == 0 && wsum.compareTo(maxWsum) < 0)) {
                        maxWsum = wsum;
                        maxPsum = psum;
                        if (findings.size() == depth) {
                            findings.add(item);
                            log.debug("Possible Item added to result {}. " +
                                    "For max weight {}. Final price {}", item, maxWeight, maxPsum);
                        } else {
                            Item old = findings.set(depth, item);
                            log.debug("Old {} replaced by {}, at index {}. " +
                                    "For max weight {}. " +
                                    "Final price for on this index {}", old, item, depth, maxWeight, maxPsum);
                        }
                        lastIdx = Math.max(find(items, i + 1, depth + 1, wsum, psum, maxWeight, findings), depth);
                    }
                }
            }
        }
        return lastIdx;
    }
}
