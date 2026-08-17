package com.example.f12fanirtester;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

final class PowerCodeCatalog {
    static final class Candidate {
        final String label;
        final int carrier;
        final int[] pattern;

        Candidate(String label, int carrier, int[] pattern) {
            this.label = label;
            this.carrier = carrier;
            this.pattern = pattern;
        }
    }

    // Deduplicated power signals from the MIT-licensed flipperdevices/IRDB
    // database/categories/Fan/Airmate profiles, snapshot inspected 2026-08-17.
    private static final String COMPRESSED_RAW_CATALOG =
            "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";

    private PowerCodeCatalog() { }

    static List<Candidate> load() {
        try {
            ArrayList<Candidate> result = new ArrayList<>();
            byte[] compressed = decodeBase64(COMPRESSED_RAW_CATALOG);
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream plain = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = gzip.read(buffer)) != -1) plain.write(buffer, 0, count);
            gzip.close();

            String catalog = new String(plain.toByteArray(), StandardCharsets.UTF_8);
            for (String line : catalog.split("\\n")) {
                String[] fields = line.split("\\|", 3);
                String[] durations = fields[2].trim().split("\\s+");
                int[] pattern = new int[durations.length];
                for (int i = 0; i < durations.length; i++) {
                    pattern[i] = Integer.parseInt(durations[i]);
                }
                result.add(new Candidate("Airmate " + fields[0],
                        Integer.parseInt(fields[1]), pattern));
            }

            result.add(new Candidate("Airmate 35_8188 (NEC 00/00)", 38000,
                    buildNec(0x00, 0x00)));
            result.add(new Candidate("Generic fan NEC 00/45", 38000,
                    buildNec(0x00, 0x45)));
            return Collections.unmodifiableList(result);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load power-code catalog", e);
        }
    }

    static int[] buildNec(int address, int command) {
        if (address < 0 || address > 255) {
            throw new IllegalArgumentException("NEC address must be 0..255");
        }
        if (command < 0 || command > 255) {
            throw new IllegalArgumentException("NEC command must be 0..255");
        }
        ArrayList<Integer> pattern = new ArrayList<>();
        pattern.add(9000);
        pattern.add(4500);
        appendNecByte(pattern, address);
        appendNecByte(pattern, (~address) & 0xFF);
        appendNecByte(pattern, command);
        appendNecByte(pattern, (~command) & 0xFF);
        pattern.add(560);
        pattern.add(40000);
        int[] out = new int[pattern.size()];
        for (int i = 0; i < pattern.size(); i++) out[i] = pattern.get(i);
        return out;
    }

    private static void appendNecByte(ArrayList<Integer> pattern, int value) {
        for (int bit = 0; bit < 8; bit++) {
            pattern.add(560);
            pattern.add(((value >> bit) & 1) == 1 ? 1690 : 560);
        }
    }

    private static byte[] decodeBase64(String value) {
        final String alphabet =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length() * 3 / 4);
        int accumulator = 0;
        int bits = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=') break;
            int digit = alphabet.indexOf(c);
            if (digit < 0) continue;
            accumulator = (accumulator << 6) | digit;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((accumulator >> bits) & 0xFF);
            }
        }
        return out.toByteArray();
    }
}
