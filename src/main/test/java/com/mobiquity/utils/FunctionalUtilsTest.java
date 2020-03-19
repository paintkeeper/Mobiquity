package com.mobiquity.utils;

import com.mobiquity.exception.APIException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
class FunctionalUtilsTest {

    static String stringAsLine = "firstLine\nsecondLine\nlastLine";

    @Test
    void scanLines() throws APIException {
        Scanner scanner = new Scanner(stringAsLine);
        List<String> lines = FunctionalUtils.readLines(FunctionalUtils.scanLines(scanner), s -> s + "test");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).isEqualTo("firstLinetest");
        assertThat(lines.get(2)).isEqualTo("lastLinetest");
    }

    @Test
    void mergeLines() {
        List<String> lines = new ArrayList<>();
        lines.add("firstLine");
        lines.add("secondLine");
        lines.add("lastLine");
        String result = FunctionalUtils.mergeLines(lines);
        assertThat(result).isEqualTo(stringAsLine);
    }
}