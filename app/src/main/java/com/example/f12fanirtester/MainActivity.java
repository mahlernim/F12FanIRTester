package com.example.f12fanirtester;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    private ConsumerIrManager ir;
    private SharedPreferences prefs;
    private int function = 0;
    private int device = 3;
    private int subdevice = 1;
    private int carrier = 37900;
    private int powerIndex = 0;
    private List<PowerCodeCatalog.Candidate> powerCandidates;

    private static final String MODE_F12_1 = "F12-1 (4 frames: 34T / 88T / 34T)";
    private static final String MODE_F12_0 = "F12-0 (2 frames: 34T)";
    private static final String MODE_LEGACY = "Legacy F12 (2 frames: 80T / 80T)";
    private static final String[] MODES = {MODE_F12_1, MODE_F12_0, MODE_LEGACY};
    private static final int[] AIRMATE_FUNCTIONS = {9, 17, 33, 65, 99, 129, 195};

    private TextView statusView;
    private TextView codeView;
    private TextView marksView;
    private EditText noteEdit;
    private CheckBox autoAdvance;
    private CheckBox autoSendNext;
    private Spinner deviceSpinner;
    private Spinner subdeviceSpinner;
    private Spinner modeSpinner;
    private TextView powerCandidateView;
    private CheckBox autoSendPowerNext;

    private final String[] labels = {
            "NO RESPONSE", "POWER", "SPEED +", "SPEED -",
            "OSCILLATE", "TIMER", "LIGHT", "OTHER"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ir = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        prefs = getSharedPreferences("results", MODE_PRIVATE);
        function = prefs.getInt("current_function", 0);
        powerCandidates = PowerCodeCatalog.load();
        powerIndex = Math.min(prefs.getInt("current_power_candidate", 0),
                powerCandidates.size() - 1);

        setContentView(buildUi());
        updateIrStatus();
        updateCode();
        updateMarks();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        TextView title = text("Fan IR Tester", 24, true);
        root.addView(title);

        TextView intro = text(
                "Default: corrected F12-1, 37.9 kHz, Device 3, H/S 1.\n" +
                "Aim the phone IR blaster at the fan, press SEND, then mark what happened.",
                14, false);
        intro.setPadding(0, dp(4), 0, dp(8));
        root.addView(intro);

        statusView = text("", 13, false);
        root.addView(statusView);

        TextView modeLabel = text("WAVEFORM MODE", 14, true);
        modeLabel.setPadding(0, dp(10), 0, 0);
        root.addView(modeLabel);

        modeSpinner = new Spinner(this);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MODES);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(prefs.getInt("current_mode", 0));
        root.addView(modeSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout settings = horizontal();
        settings.setGravity(Gravity.CENTER_VERTICAL);

        TextView dLabel = text("D", 14, true);
        settings.addView(dLabel);

        deviceSpinner = new Spinner(this);
        ArrayAdapter<String> dAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"0","1","2","3","4","5","6","7"});
        dAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(dAdapter);
        deviceSpinner.setSelection(prefs.getInt("current_device", 3));
        settings.addView(deviceSpinner, new LinearLayout.LayoutParams(0, dp(52), 1));

        TextView sLabel = text("H/S", 14, true);
        sLabel.setPadding(dp(12), 0, 0, 0);
        settings.addView(sLabel);

        subdeviceSpinner = new Spinner(this);
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"0","1"});
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subdeviceSpinner.setAdapter(sAdapter);
        subdeviceSpinner.setSelection(prefs.getInt("current_subdevice", 1));
        settings.addView(subdeviceSpinner, new LinearLayout.LayoutParams(0, dp(52), 1));

        root.addView(settings);

        AdapterView.OnItemSelectedListener settingsListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit()
                        .putInt("current_mode", modeSpinner.getSelectedItemPosition())
                        .putInt("current_device", deviceSpinner.getSelectedItemPosition())
                        .putInt("current_subdevice", subdeviceSpinner.getSelectedItemPosition())
                        .apply();
                updateCode();
                updateMarks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };
        modeSpinner.setOnItemSelectedListener(settingsListener);
        deviceSpinner.setOnItemSelectedListener(settingsListener);
        subdeviceSpinner.setOnItemSelectedListener(settingsListener);

        TextView quickTitle = text("QUICK TEST AIRMATE VALUES (tap to send)", 15, true);
        quickTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(quickTitle);

        LinearLayout quickA = horizontal();
        LinearLayout quickB = horizontal();
        for (int i = 0; i < AIRMATE_FUNCTIONS.length; i++) {
            int quickFunction = AIRMATE_FUNCTIONS[i];
            Button quick = button(quickFunction + "\n0x" +
                    String.format(Locale.US, "%02X", quickFunction));
            quick.setTextSize(11);
            quick.setOnClickListener(v -> {
                function = quickFunction;
                persistCurrent();
                updateCode();
                sendCurrent();
            });
            if (i < 4) quickA.addView(quick, weight());
            else quickB.addView(quick, weight());
        }
        root.addView(quickA);
        root.addView(quickB);

        TextView powerScanTitle = text("AIRMATE POWER-CODE SCAN", 17, true);
        powerScanTitle.setPadding(0, dp(18), 0, dp(4));
        root.addView(powerScanTitle);

        TextView powerHelp = text(
                "21 known power candidates: 20 unique Airmate signals plus one generic fan fallback. " +
                "Press SEND POWER once, then mark the result.", 13, false);
        root.addView(powerHelp);

        powerCandidateView = text("", 16, true);
        powerCandidateView.setGravity(Gravity.CENTER);
        powerCandidateView.setPadding(0, dp(10), 0, dp(8));
        root.addView(powerCandidateView);

        LinearLayout powerNav = horizontal();
        Button previousPower = button("PREV");
        Button sendPower = button("SEND POWER");
        Button nextPower = button("NEXT");
        powerNav.addView(previousPower, weight());
        powerNav.addView(sendPower, weight());
        powerNav.addView(nextPower, weight());
        root.addView(powerNav);

        previousPower.setOnClickListener(v -> changePowerCandidate(-1));
        sendPower.setOnClickListener(v -> sendPowerCandidate());
        nextPower.setOnClickListener(v -> changePowerCandidate(1));

        LinearLayout powerMark = horizontal();
        Button noPowerEffect = button("NO EFFECT");
        Button powerWorked = button("POWER CHANGED");
        powerMark.addView(noPowerEffect, weight());
        powerMark.addView(powerWorked, weight());
        root.addView(powerMark);

        noPowerEffect.setOnClickListener(v -> markPowerCandidate(false));
        powerWorked.setOnClickListener(v -> markPowerCandidate(true));

        autoSendPowerNext = new CheckBox(this);
        autoSendPowerNext.setText("After NO EFFECT, advance and send the next power candidate");
        autoSendPowerNext.setChecked(true);
        root.addView(autoSendPowerNext);

        updatePowerCandidate();

        codeView = text("", 34, true);
        codeView.setGravity(Gravity.CENTER);
        codeView.setPadding(0, dp(14), 0, dp(14));
        root.addView(codeView);

        LinearLayout nav1 = horizontal();
        Button minus16 = button("-16");
        Button prev = button("PREV");
        Button send = button("SEND");
        Button next = button("NEXT");
        Button plus16 = button("+16");

        nav1.addView(minus16, weight());
        nav1.addView(prev, weight());
        nav1.addView(send, weight());
        nav1.addView(next, weight());
        nav1.addView(plus16, weight());
        root.addView(nav1);

        minus16.setOnClickListener(v -> changeFunction(-16));
        prev.setOnClickListener(v -> changeFunction(-1));
        send.setOnClickListener(v -> sendCurrent());
        next.setOnClickListener(v -> changeFunction(1));
        plus16.setOnClickListener(v -> changeFunction(16));

        TextView jumpLabel = text("Jump directly to function (decimal or 0xHEX)", 13, true);
        jumpLabel.setPadding(0, dp(14), 0, 0);
        root.addView(jumpLabel);

        LinearLayout jump = horizontal();
        EditText jumpEdit = new EditText(this);
        jumpEdit.setSingleLine(true);
        jumpEdit.setHint("e.g. 65 or 0x41");
        Button jumpButton = button("GO");
        jump.addView(jumpEdit, new LinearLayout.LayoutParams(0, dp(52), 1));
        jump.addView(jumpButton, new LinearLayout.LayoutParams(dp(90), dp(52)));
        root.addView(jump);

        jumpButton.setOnClickListener(v -> {
            try {
                String s = jumpEdit.getText().toString().trim().toLowerCase(Locale.US);
                int value = s.startsWith("0x")
                        ? Integer.parseInt(s.substring(2), 16)
                        : Integer.parseInt(s);
                if (value < 0 || value > 255) throw new Exception();
                function = value;
                persistCurrent();
                updateCode();
            } catch (Exception e) {
                toast("Enter 0–255, or 0x00–0xFF");
            }
        });

        TextView markTitle = text("MARK RESPONSE", 15, true);
        markTitle.setPadding(0, dp(18), 0, dp(4));
        root.addView(markTitle);

        LinearLayout rowA = horizontal();
        LinearLayout rowB = horizontal();

        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            Button b = button(label);
            b.setTextSize(11);
            b.setOnClickListener(v -> mark(label));
            if (i < 4) rowA.addView(b, weight());
            else rowB.addView(b, weight());
        }
        root.addView(rowA);
        root.addView(rowB);

        noteEdit = new EditText(this);
        noteEdit.setHint("Optional note, e.g. speed changed 12→11");
        noteEdit.setSingleLine(true);
        root.addView(noteEdit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        autoAdvance = new CheckBox(this);
        autoAdvance.setText("Automatically advance after marking");
        autoAdvance.setChecked(true);
        root.addView(autoAdvance);

        autoSendNext = new CheckBox(this);
        autoSendNext.setText("Send next code immediately after marking");
        autoSendNext.setChecked(false);
        root.addView(autoSendNext);

        Button export = button("COPY CSV");
        root.addView(export);
        export.setOnClickListener(v -> copyCsv());

        TextView resultsTitle = text("RESPONDING / MARKED CODES", 15, true);
        resultsTitle.setPadding(0, dp(18), 0, dp(4));
        root.addView(resultsTitle);

        marksView = text("", 14, false);
        marksView.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(marksView);

        Button clearCurrent = button("CLEAR CURRENT MARK");
        root.addView(clearCurrent);
        clearCurrent.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit()
                    .remove(key(function) + "_label")
                    .remove(key(function) + "_note");
            if (selectedModeIndex() == 2) {
                editor.remove(legacyKey(function) + "_label")
                        .remove(legacyKey(function) + "_note");
            }
            editor.apply();
            updateMarks();
            toast("Current mark cleared");
        });

        return scroll;
    }

    private void updateIrStatus() {
        if (ir == null || !ir.hasIrEmitter()) {
            statusView.setText("IR status: NO consumer IR emitter reported by Android.");
            return;
        }

        ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
        StringBuilder sb = new StringBuilder("IR status: emitter detected");
        if (ranges != null && ranges.length > 0) {
            sb.append("\nSupported carrier ranges: ");
            for (int i = 0; i < ranges.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(ranges[i].getMinFrequency()).append("–")
                        .append(ranges[i].getMaxFrequency()).append(" Hz");
            }
        }
        sb.append("\nRequested carrier: ").append(carrier).append(" Hz");
        statusView.setText(sb.toString());
    }

    private void sendCurrent() {
        if (ir == null || !ir.hasIrEmitter()) {
            toast("Android does not report an IR emitter.");
            return;
        }

        device = deviceSpinner.getSelectedItemPosition();
        subdevice = subdeviceSpinner.getSelectedItemPosition();

        try {
            int mode = selectedModeIndex();
            int[] pattern = F12Encoder.build(mode, device, subdevice, function);
            ir.transmit(carrier, pattern);
            statusView.setText("Sent " + shortModeName(mode) + "  D=" + device +
                    " H/S=" + subdevice +
                    " F=" + function + " (0x" +
                    String.format(Locale.US, "%02X", function) + ")" +
                    "\nPattern entries: " + pattern.length +
                    "   Carrier: " + carrier + " Hz");
        } catch (Exception e) {
            statusView.setText("Transmit failed: " + e);
        }
    }

    private void mark(String label) {
        String base = key(function);
        String note = noteEdit.getText().toString().trim();
        prefs.edit()
                .putString(base + "_label", label)
                .putString(base + "_note", note)
                .apply();

        noteEdit.setText("");
        updateMarks();

        if (autoAdvance.isChecked()) {
            changeFunction(1);
            if (autoSendNext.isChecked()) sendCurrent();
        }
    }

    private String key(int f) {
        int d = deviceSpinner == null ? device : deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner == null ? subdevice : subdeviceSpinner.getSelectedItemPosition();
        return "M" + selectedModeIndex() + "_D" + d + "_S" + s + "_F" + f;
    }

    private void sendPowerCandidate() {
        if (ir == null || !ir.hasIrEmitter()) {
            toast("Android does not report an IR emitter.");
            return;
        }
        try {
            PowerCodeCatalog.Candidate candidate = powerCandidates.get(powerIndex);
            ir.transmit(candidate.carrier, candidate.pattern);
            statusView.setText("Sent power candidate " + (powerIndex + 1) + "/" +
                    powerCandidates.size() + "\n" + candidate.label +
                    "   " + candidate.carrier + " Hz   " +
                    candidate.pattern.length + " entries");
        } catch (Exception e) {
            statusView.setText("Power candidate transmit failed: " + e);
        }
    }

    private void changePowerCandidate(int delta) {
        powerIndex = (powerIndex + delta + powerCandidates.size()) % powerCandidates.size();
        prefs.edit().putInt("current_power_candidate", powerIndex).apply();
        updatePowerCandidate();
    }

    private void markPowerCandidate(boolean worked) {
        prefs.edit().putBoolean("power_candidate_" + powerIndex + "_tested", true)
                .putBoolean("power_candidate_" + powerIndex + "_worked", worked)
                .apply();
        updatePowerCandidate();
        if (worked) {
            toastLong("Power response saved. Scan stopped on this candidate.");
        } else {
            if (powerIndex == powerCandidates.size() - 1) {
                toastLong("Power scan complete: no candidate matched.");
                return;
            }
            changePowerCandidate(1);
            if (autoSendPowerNext.isChecked()) sendPowerCandidate();
        }
    }

    private void updatePowerCandidate() {
        if (powerCandidateView == null || powerCandidates == null) return;
        PowerCodeCatalog.Candidate candidate = powerCandidates.get(powerIndex);
        boolean tested = prefs.getBoolean("power_candidate_" + powerIndex + "_tested", false);
        boolean worked = prefs.getBoolean("power_candidate_" + powerIndex + "_worked", false);
        String result = tested ? (worked ? "  ✓ POWER CHANGED" : "  · tested") : "";
        powerCandidateView.setText((powerIndex + 1) + " / " + powerCandidates.size() +
                "   " + candidate.label + "\n" + candidate.carrier + " Hz" + result);
    }

    private String legacyKey(int f) {
        int d = deviceSpinner == null ? device : deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner == null ? subdevice : subdeviceSpinner.getSelectedItemPosition();
        return "D" + d + "_S" + s + "_F" + f;
    }

    private int selectedModeIndex() {
        return modeSpinner == null ? 0 : modeSpinner.getSelectedItemPosition();
    }

    private String shortModeName(int mode) {
        if (mode == 1) return "F12-0";
        if (mode == 2) return "Legacy F12";
        return "F12-1";
    }

    private String storedString(int f, String suffix, String defaultValue) {
        String value = prefs.getString(key(f) + suffix, null);
        if (value == null && selectedModeIndex() == 2) {
            value = prefs.getString(legacyKey(f) + suffix, null);
        }
        return value == null ? defaultValue : value;
    }

    private void updateMarks() {
        if (marksView == null) return;
        int d = deviceSpinner == null ? 3 : deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner == null ? 1 : subdeviceSpinner.getSelectedItemPosition();

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (int f = 0; f < 256; f++) {
            String label = storedString(f, "_label", null);
            if (label != null && !label.equals("NO RESPONSE")) {
                String note = storedString(f, "_note", "");
                sb.append(String.format(Locale.US, "%3d  0x%02X   %-10s", f, f, label));
                if (!note.isEmpty()) sb.append("   ").append(note);
                sb.append("\n");
                count++;
            }
        }

        if (count == 0) {
            sb.append("No responding codes marked yet.");
        } else {
            sb.insert(0, shortModeName(selectedModeIndex()) + "  D=" + d + " H/S=" + s +
                    "    " + count + " responding code(s)\n\n");
        }
        marksView.setText(sb.toString());
    }

    private void copyCsv() {
        int d = deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner.getSelectedItemPosition();

        StringBuilder csv = new StringBuilder();
        csv.append("mode,device,h_or_subdevice,function_dec,function_hex,result,note\n");

        for (int f = 0; f < 256; f++) {
            String label = storedString(f, "_label", null);
            if (label != null) {
                String note = storedString(f, "_note", "");
                csv.append(csvEscape(shortModeName(selectedModeIndex()))).append(",")
                        .append(d).append(",")
                        .append(s).append(",")
                        .append(f).append(",")
                        .append(String.format(Locale.US, "0x%02X", f)).append(",")
                        .append(csvEscape(label)).append(",")
                        .append(csvEscape(note)).append("\n");
            }
        }

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("F12 scan CSV", csv.toString()));
        toast("CSV copied to clipboard");
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void changeFunction(int delta) {
        function = (function + delta) & 0xFF;
        persistCurrent();
        updateCode();
    }

    private void persistCurrent() {
        prefs.edit().putInt("current_function", function).apply();
    }

    private void updateCode() {
        if (codeView != null) {
            codeView.setText(String.format(Locale.US,
                    "F = %d   •   0x%02X", function, function));
        }
        if (noteEdit != null) {
            noteEdit.setText(storedString(function, "_note", ""));
        }
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, dp(52), 1);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void toastLong(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
