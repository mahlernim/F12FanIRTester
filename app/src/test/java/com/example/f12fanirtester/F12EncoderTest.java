package com.example.f12fanirtester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class F12EncoderTest {
    @Test
    public void f12_1UsesFourFramesWithCorrectGaps() {
        int[] pattern = F12Encoder.build(0, 3, 1, 9);
        assertEquals(96, pattern.length);
        assertEquals(37 * F12Encoder.T, pattern[23]);
        assertEquals(91 * F12Encoder.T, pattern[47]);
        assertEquals(37 * F12Encoder.T, pattern[71]);
        assertEquals(3 * F12Encoder.T, pattern[95]);
    }

    @Test
    public void f12_0UsesTwoFramesWith34TGap() {
        int[] pattern = F12Encoder.build(1, 3, 0, 9);
        assertEquals(48, pattern.length);
        assertEquals(37 * F12Encoder.T, pattern[23]);
        assertEquals(3 * F12Encoder.T, pattern[47]);
    }

    @Test
    public void legacyUsesTwo80TLeadouts() {
        int[] pattern = F12Encoder.build(2, 3, 1, 9);
        assertEquals(48, pattern.length);
        assertEquals(83 * F12Encoder.T, pattern[23]);
        assertEquals(83 * F12Encoder.T, pattern[47]);
    }

    @Test
    public void encodesDThenHThenFunctionLsbFirst() {
        int[] pattern = F12Encoder.build(1, 3, 1, 9);
        int[] expectedMarks = {3, 3, 1, 3, 3, 1, 1, 3, 1, 1, 1, 1};
        for (int bit = 0; bit < expectedMarks.length; bit++) {
            assertEquals(expectedMarks[bit] * F12Encoder.T, pattern[bit * 2]);
        }
    }

    @Test
    public void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> F12Encoder.build(0, 8, 1, 9));
        assertThrows(IllegalArgumentException.class, () -> F12Encoder.build(0, 3, 2, 9));
        assertThrows(IllegalArgumentException.class, () -> F12Encoder.build(0, 3, 1, 256));
    }
}
