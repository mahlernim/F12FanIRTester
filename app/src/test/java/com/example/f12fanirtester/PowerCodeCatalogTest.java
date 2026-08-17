package com.example.f12fanirtester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class PowerCodeCatalogTest {
    @Test
    public void loadsTwentyOneValidUniquePowerCandidates() {
        List<PowerCodeCatalog.Candidate> candidates = PowerCodeCatalog.load();
        assertEquals(21, candidates.size());

        Set<String> waveforms = new HashSet<>();
        for (PowerCodeCatalog.Candidate candidate : candidates) {
            assertTrue(candidate.label, candidate.carrier >= 37000);
            assertTrue(candidate.label, candidate.carrier <= 40000);
            assertTrue(candidate.label, candidate.pattern.length >= 2);
            assertEquals(candidate.label, 0, candidate.pattern.length % 2);

            long totalMicros = 0;
            for (int duration : candidate.pattern) {
                assertTrue(candidate.label, duration > 0);
                totalMicros += duration;
            }
            assertTrue(candidate.label, totalMicros < 2_000_000L);
            assertTrue(candidate.label, waveforms.add(candidate.carrier + ":" +
                    Arrays.toString(candidate.pattern)));
        }
    }

    @Test
    public void necEncoderProducesConfirmedPowerPayload() {
        int[] pattern = PowerCodeCatalog.buildNec(0x00, 0x45);
        assertEquals(68, pattern.length);
        assertEquals(9000, pattern[0]);
        assertEquals(4500, pattern[1]);
        assertEquals(0x00, decodeNecByte(pattern, 2));
        assertEquals(0xFF, decodeNecByte(pattern, 18));
        assertEquals(0x45, decodeNecByte(pattern, 34));
        assertEquals(0xBA, decodeNecByte(pattern, 50));
        assertEquals(560, pattern[66]);
        assertEquals(40000, pattern[67]);
    }

    @Test
    public void necEncoderRejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> PowerCodeCatalog.buildNec(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> PowerCodeCatalog.buildNec(0, 256));
    }

    private int decodeNecByte(int[] pattern, int offset) {
        int value = 0;
        for (int bit = 0; bit < 8; bit++) {
            assertEquals(560, pattern[offset + bit * 2]);
            if (pattern[offset + bit * 2 + 1] > 1000) value |= 1 << bit;
        }
        return value;
    }
}
