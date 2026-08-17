package com.example.f12fanirtester;

import static org.junit.Assert.assertEquals;
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
}
