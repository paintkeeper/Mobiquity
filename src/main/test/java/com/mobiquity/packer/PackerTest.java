package com.mobiquity.packer;

import com.mobiquity.exception.APIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
class PackerTest {
    static String inputResourcePath = "example_input";
    static String outputResourcePath = "example_output";
    static String outputString;

    @BeforeAll
    static void init() throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(outputResourcePath);
        assert url != null;
        outputString = Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8);
        assert outputString != null;
    }

    @Test
    void testIfFilenotAccessible() {
        APIException exception = assertThrows(APIException.class, () -> Packer.pack("not a file at all"));
        assertThat(exception.getMessage()).isEqualTo("Cannot read file.");
    }

    @Test
    void testIfFileContentReadable() throws APIException {
        String output = Packer.pack(inputResourcePath);
        assertThat(output).isNotNull();
    }

    @Test
    void pack() throws APIException {
        String output = Packer.pack(inputResourcePath);
        assertThat(output).isNotNull();
        assertThat(output).isEqualTo(outputString);
    }

}