package com.mobiquity.utils;

import com.mobiquity.api.Item;
import com.mobiquity.exception.APIException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
class AlgoUtilsTest {
    final String firstLineToTest = "81 : (1,53.38,€45) (2,88.62,€98) (3,78.48,€3) (4,72.30,€76) (5,30.18,€9) (6,46.34,€48)";
    final String noResultLineToTest = "8 : (1,15.3,€34)";
    final String twoResultsLineToTest = "75 : (1,85.31,€29) (2,14.55,€74) (3,3.98,€16) (4,26.24,€55) (5,63.69,€52) (6,76.25,€75) (7,60.02,€74) (8,93.18,€35) (9,89.95,€78)";
    final String otherResultsLineToTest = "56 : (1,90.72,€13) (2,33.80,€40) (3,43.15,€10) (4,37.97,€16) (5,46.81,€36) (6,48.77,€79) (7,81.80,€45) (8,19.36,€79) (9,6.76,€64)";
    final BigDecimal maxItemWeightTotest = BigDecimal.valueOf(57);
    final BigDecimal maxItemPriceTotest = BigDecimal.valueOf(40);
    final int maxItemAmountTotest = 3;
    final BigDecimal maxPackageWeightTotest = BigDecimal.valueOf(50);

    @Test
    void readLine() throws APIException {
        AtomicReference<BigDecimal> maxWeight = new AtomicReference<>();
        List<Item> items = new ArrayList<>();
        AlgoUtils.readLine(firstLineToTest, (w, val) -> {
            maxWeight.set(w);
            items.add(val);
            return null;
        });
        assertThat(maxWeight.get()).isNotNull();
        assertThat(maxWeight.get()).isEqualTo("81");
        assertThat(items).hasSize(6);
        Item first = items.get(0);
        assertThat(first.getIndex()).isEqualTo(1);
        assertThat(first.getPrice()).isEqualTo("45");
        assertThat(first.getWeight()).isEqualTo("53.38");
        Item last = items.get(5);
        assertThat(last.getIndex()).isEqualTo(6);
        assertThat(last.getPrice()).isEqualTo("48");
        assertThat(last.getWeight()).isEqualTo("46.34");
    }

    @Test
    void findMaxPriceLimitedByWeight() throws APIException {
        AtomicReference<BigDecimal> maxWeight = new AtomicReference<>();
        List<Item> items = new ArrayList<>();
        AlgoUtils.readLine(firstLineToTest, (w, val) -> {
            maxWeight.set(w);
            items.add(val);
            return null;
        });
        List<Item> found = AlgoUtils.findMaxPriceLimitedByWeight(items, maxWeight.get());
        assertThat(found).hasSize(1);
        Item itemToCompare = new Item(0, new BigDecimal("72.30"), new BigDecimal("76"));
        Item foundItem = found.get(0);
        assertThat(foundItem).isEqualTo(itemToCompare);

        items.clear();
        AlgoUtils.readLine(noResultLineToTest, (w, val) -> {
            maxWeight.set(w);
            items.add(val);
            return null;
        });
        found = AlgoUtils.findMaxPriceLimitedByWeight(items, maxWeight.get());
        assertThat(found).isEmpty();

        items.clear();
        AlgoUtils.readLine(twoResultsLineToTest, (w, val) -> {
            maxWeight.set(w);
            items.add(val);
            return null;
        });
        found = AlgoUtils.findMaxPriceLimitedByWeight(items, maxWeight.get());
        assertThat(found).hasSize(2);
        itemToCompare = new Item(0, new BigDecimal("14.55"), new BigDecimal("74"));
        Item firstFoundItem = found.get(0);
        Item secondFoundItem = found.get(1);
        assertThat(firstFoundItem).isEqualTo(itemToCompare);
        assertThat(secondFoundItem).isEqualTo(itemToCompare);
    }

    @Test
    void listIndexesWithMaxPriceAndSmallestWeight() throws APIException {
        assertThat(AlgoUtils.listIndexesWithMaxPrice(firstLineToTest, i -> true, i -> true, i -> true)).isEqualTo("4");
        assertThat(AlgoUtils.listIndexesWithMaxPrice(noResultLineToTest, i -> true, i -> true, i -> true)).isEqualTo("-");
        assertThat(AlgoUtils.listIndexesWithMaxPrice(twoResultsLineToTest, i -> true, i -> true, i -> true)).isEqualTo("2,7");
        assertThat(AlgoUtils.listIndexesWithMaxPrice(otherResultsLineToTest, i -> true, i -> true, i -> true)).isEqualTo("8,9");
    }

    @Test
    void testValidationOnMaxLimit() {
        APIException exception = assertThrows(APIException.class,
                () -> AlgoUtils.listIndexesWithMaxPrice(otherResultsLineToTest,
                        i -> true,
                        i -> true,
                        i -> i.size() < 3));
        assertThat(exception.getMessage()).isEqualTo("Max Item amount exceeded.");
    }

    @Test
    void testValidationOnMaxPackageWeight() {
        APIException exception = assertThrows(APIException.class,
                () -> AlgoUtils.listIndexesWithMaxPrice(otherResultsLineToTest,
                        i -> maxPackageWeightTotest.compareTo(i) > -1,
                        i -> true,
                        i -> true));
        assertThat(exception.getMessage()).isEqualTo("Max Package weight value exceeded.");
    }

    @Test
    void testValidationOnMaxItemWeight() throws APIException {
        List<Item> lines = AlgoUtils.readItemsFromLine(otherResultsLineToTest,
                new AtomicReference<>(maxItemWeightTotest),
                i -> maxItemWeightTotest.compareTo(i) > -1,
                i -> true,
                i -> true);
        assertThat(lines).hasSize(7);
    }

    @Test
    void testValidationOnMaxItemPrice() throws APIException {
        List<Item> lines = AlgoUtils.readItemsFromLine(otherResultsLineToTest,
                new AtomicReference<>(maxItemWeightTotest),
                i -> true,
                i -> maxItemPriceTotest.compareTo(i) > -1,
                i -> true);
        assertThat(lines).hasSize(5);
    }

    @Test
    void testValidationOnMaxItemAmount() {
        APIException exception = assertThrows(APIException.class,
                () -> AlgoUtils.listIndexesWithMaxPrice(otherResultsLineToTest,
                        i -> true,
                        i -> true,
                        i -> i.size() < maxItemAmountTotest));
        assertThat(exception.getMessage()).isEqualTo("Max Item amount exceeded.");
    }
}