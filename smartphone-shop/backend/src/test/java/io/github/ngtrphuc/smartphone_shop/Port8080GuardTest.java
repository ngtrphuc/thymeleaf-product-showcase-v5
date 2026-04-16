package io.github.ngtrphuc.smartphone_shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class Port8080GuardTest {

    @Test
    void parseListeningPids_shouldReturnOnlyListeningEntriesForRequestedPort() {
        String netstatOutput = """
                TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       63512
                TCP    127.0.0.1:8080         127.0.0.1:61142        ESTABLISHED     63512
                TCP    [::]:8080              [::]:0                 LISTENING       64222
                TCP    0.0.0.0:3000           0.0.0.0:0              LISTENING       11111
                """;

        Set<Long> pids = Port8080Guard.parseListeningPids(netstatOutput, 8080);

        assertEquals(Set.of(63512L, 64222L), pids);
    }

    @Test
    void isProdProfileValue_shouldDetectProdInCsvProfileList() {
        assertTrue(Port8080Guard.isProdProfileValue("dev,prod"));
        assertTrue(Port8080Guard.isProdProfileValue("prod"));
    }
}

