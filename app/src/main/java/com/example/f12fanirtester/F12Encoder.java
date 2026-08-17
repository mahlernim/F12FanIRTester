package com.example.f12fanirtester;

import java.util.ArrayList;

final class F12Encoder {
    static final int T = 422;

    private F12Encoder() { }

    static int[] build(int mode, int device, int hOrSubdevice, int function) {
        if (device < 0 || device > 7) throw new IllegalArgumentException("Device must be 0..7");
        if (hOrSubdevice < 0 || hOrSubdevice > 1) {
            throw new IllegalArgumentException("H/S must be 0 or 1");
        }
        if (function < 0 || function > 255) {
            throw new IllegalArgumentException("Function must be 0..255");
        }

        ArrayList<Integer> pattern = new ArrayList<>();
        switch (mode) {
            case 0: // F12-1: K, -34T, K, -88T, K, -34T, K
                appendFrame(pattern, device, hOrSubdevice, function, 34);
                appendFrame(pattern, device, hOrSubdevice, function, 88);
                appendFrame(pattern, device, hOrSubdevice, function, 34);
                appendFrame(pattern, device, hOrSubdevice, function, 0);
                break;
            case 1: // F12-0: K, -34T, K
                appendFrame(pattern, device, hOrSubdevice, function, 34);
                appendFrame(pattern, device, hOrSubdevice, function, 0);
                break;
            case 2: // Old F12: (K, -80T) twice
                appendFrame(pattern, device, hOrSubdevice, function, 80);
                appendFrame(pattern, device, hOrSubdevice, function, 80);
                break;
            default:
                throw new IllegalArgumentException("Unknown waveform mode: " + mode);
        }

        int[] out = new int[pattern.size()];
        for (int i = 0; i < pattern.size(); i++) out[i] = pattern.get(i);
        return out;
    }

    private static void appendFrame(ArrayList<Integer> pattern, int d, int h, int f,
                                    int leadoutUnits) {
        appendBits(pattern, d, 3);
        appendBits(pattern, h, 1);
        appendBits(pattern, f, 8);
        if (leadoutUnits > 0) {
            int lastSpace = pattern.size() - 1;
            pattern.set(lastSpace, pattern.get(lastSpace) + leadoutUnits * T);
        }
    }

    private static void appendBits(ArrayList<Integer> pattern, int value, int bits) {
        for (int i = 0; i < bits; i++) {
            boolean one = ((value >> i) & 1) != 0;
            pattern.add(one ? 3 * T : T);
            pattern.add(one ? T : 3 * T);
        }
    }
}
