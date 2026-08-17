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

    private TextView statusView;
    private TextView codeView;
    private TextView marksView;
    private EditText noteEdit;
    private CheckBox autoAdvance;
    private Spinner deviceSpinner;
    private Spinner subdeviceSpinner;

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

        TextView title = text("F12 Fan IR Tester", 24, true);
        root.addView(title);

        TextView intro = text(
                "Defaults: F12, 37.9 kHz, Device 3, Subdevice 1.\n" +
                "Aim the phone IR blaster at the fan, press SEND, then mark what happened.",
                14, false);
        intro.setPadding(0, dp(4), 0, dp(8));
        root.addView(intro);

        statusView = text("", 13, false);
        root.addView(statusView);

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
        deviceSpinner.setSelection(3);
        settings.addView(deviceSpinner, new LinearLayout.LayoutParams(0, dp(52), 1));

        TextView sLabel = text("S", 14, true);
        sLabel.setPadding(dp(12), 0, 0, 0);
        settings.addView(sLabel);

        subdeviceSpinner = new Spinner(this);
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"0","1"});
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subdeviceSpinner.setAdapter(sAdapter);
        subdeviceSpinner.setSelection(1);
        settings.addView(subdeviceSpinner, new LinearLayout.LayoutParams(0, dp(52), 1));

        root.addView(settings);

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

        LinearLayout known = horizontal();
        Button knownA = button("Airmate known codes");
        Button export = button("COPY CSV");
        known.addView(knownA, weight());
        known.addView(export, weight());
        root.addView(known);

        knownA.setOnClickListener(v ->
                toastLong("IRDB Airmate F12 D=3 S=1: " +
                        "Osc 9 (0x09), Timer 17 (0x11), Mode 33 (0x21), " +
                        "Speed 65 (0x41), Light 99 (0x63), Shutdown 129 (0x81), Presets 195 (0xC3)."));
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
            prefs.edit()
                    .remove(key(function) + "_label")
                    .remove(key(function) + "_note")
                    .apply();
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
            int[] pattern = buildF12(device, subdevice, function);
            ir.transmit(carrier, pattern);
            statusView.setText("Sent F12  D=" + device +
                    " S=" + subdevice +
                    " F=" + function + " (0x" +
                    String.format(Locale.US, "%02X", function) + ")" +
                    "\nPattern entries: " + pattern.length +
                    "   Carrier: " + carrier + " Hz");
        } catch (Exception e) {
            statusView.setText("Transmit failed: " + e);
        }
    }

    private int[] buildF12(int d, int s, int f) {
        final int T = 422;
        ArrayList<Integer> p = new ArrayList<>();

        for (int frame = 0; frame < 2; frame++) {
            appendBits(p, d, 3, T);
            appendBits(p, s, 1, T);
            appendBits(p, f, 8, T);
            int last = p.size() - 1;
            p.set(last, p.get(last) + 80 * T);
        }

        int[] out = new int[p.size()];
        for (int i = 0; i < p.size(); i++) out[i] = p.get(i);
        return out;
    }

    private void appendBits(ArrayList<Integer> p, int value, int bits, int T) {
        for (int i = 0; i < bits; i++) {
            boolean one = ((value >> i) & 1) != 0;
            p.add(one ? 3 * T : T);
            p.add(one ? T : 3 * T);
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

        if (autoAdvance.isChecked()) changeFunction(1);
    }

    private String key(int f) {
        int d = deviceSpinner == null ? device : deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner == null ? subdevice : subdeviceSpinner.getSelectedItemPosition();
        return "D" + d + "_S" + s + "_F" + f;
    }

    private void updateMarks() {
        if (marksView == null) return;
        int d = deviceSpinner == null ? 3 : deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner == null ? 1 : subdeviceSpinner.getSelectedItemPosition();

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (int f = 0; f < 256; f++) {
            String base = "D" + d + "_S" + s + "_F" + f;
            String label = prefs.getString(base + "_label", null);
            if (label != null && !label.equals("NO RESPONSE")) {
                String note = prefs.getString(base + "_note", "");
                sb.append(String.format(Locale.US, "%3d  0x%02X   %-10s", f, f, label));
                if (!note.isEmpty()) sb.append("   ").append(note);
                sb.append("\n");
                count++;
            }
        }

        if (count == 0) {
            sb.append("No responding codes marked yet.");
        } else {
            sb.insert(0, "D=" + d + " S=" + s + "    " + count + " responding code(s)\n\n");
        }
        marksView.setText(sb.toString());
    }

    private void copyCsv() {
        int d = deviceSpinner.getSelectedItemPosition();
        int s = subdeviceSpinner.getSelectedItemPosition();

        StringBuilder csv = new StringBuilder();
        csv.append("device,subdevice,function_dec,function_hex,result,note\n");

        for (int f = 0; f < 256; f++) {
            String base = "D" + d + "_S" + s + "_F" + f;
            String label = prefs.getString(base + "_label", null);
            if (label != null) {
                String note = prefs.getString(base + "_note", "");
                csv.append(d).append(",")
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
            String base = key(function);
            noteEdit.setText(prefs.getString(base + "_note", ""));
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
