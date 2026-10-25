package org.xonolithos.systemcenter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Local, explicitly confirmed xOS maintenance controls. No remote update feed. */
public final class MainActivity extends Activity {
    private static final int PICK_UPDATE = 40;
    private static final long MAX_UPDATE_BYTES = 5L * 1024 * 1024 * 1024;
    private static final int BG = 0xff08111f, CARD = 0xff142237;
    private static final int TEXT = 0xffecf8fb, MUTED = 0xffa2bac8, CYAN = 0xff59ddec;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout body;
    private TextView status;
    private boolean busy;

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextColor(color); view.setTextSize(size);
        return view;
    }
    private GradientDrawable panel(int color, int radius) {
        GradientDrawable draw = new GradientDrawable();
        draw.setColor(color); draw.setCornerRadius(dp(radius)); return draw;
    }
    private Button button(String title, View.OnClickListener action) {
        Button result = new Button(this);
        result.setText(title); result.setAllCaps(false); result.setOnClickListener(action);
        return result;
    }
    private void onUi(Runnable run) {
        runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) run.run(); });
    }
    private void message(String value) { status.setText(value); }
    private LinearLayout card(String title, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20), dp(18), dp(20), dp(18));
        box.setBackground(panel(CARD, 20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(14)); body.addView(box, lp);
        TextView heading = text(title, 21, TEXT); heading.setTypeface(null, Typeface.BOLD);
        box.addView(heading);
        TextView copy = text(description, 14, MUTED);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(6), 0, dp(10)); box.addView(copy, cp);
        return box;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(14), dp(20), 0);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20) + insets.getSystemWindowInsetLeft(),
                dp(14) + insets.getSystemWindowInsetTop(),
                dp(20) + insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this); mark.setImageResource(R.drawable.ic_system_center);
        header.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout headings = new LinearLayout(this); headings.setOrientation(LinearLayout.VERTICAL);
        headings.setPadding(dp(12), 0, 0, 0);
        TextView name = text("XonolithSystemCenter", 23, TEXT);
        name.setTypeface(null, Typeface.BOLD); headings.addView(name);
        headings.addView(text("xOS tools · " + Build.DEVICE, 13, CYAN));
        header.addView(headings); root.addView(header);
        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"Updates", "Developer", "Wallpapers"};
        Runnable[] actions = {this::updates, this::developer, this::wallpapers};
        for (int i = 0; i < names.length; i++) {
            final int position = i;
            Button tab = button(names[i], v -> actions[position].run());
            tabs.addView(tab, new LinearLayout.LayoutParams(0, dp(50), 1));
        }
        root.addView(tabs);
        status = text("Choose a tool.", 13, MUTED);
        status.setPadding(0, dp(10), 0, dp(12)); root.addView(status);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(8), 0, dp(20)); scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root); updates();
    }

    private void updates() {
        body.removeAllViews();
        LinearLayout intro = card("Install an update pack",
            "Choose a local XonolithOS OTA ZIP. The package is checked against this device and verified with the recovery certificate before rebooting to install.");
        intro.addView(button("Choose update ZIP", v -> chooseUpdate()));
        LinearLayout current = card("Current device",
            "Device: " + Build.DEVICE + "\nBuild: " + Build.DISPLAY
            + "\nAndroid: " + Build.VERSION.RELEASE);
        current.addView(text("Installation always requires your confirmation. Recovery reports the final result.", 13, CYAN));
    }

    private void chooseUpdate() {
        if (busy) return;
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.setType("*/*");
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(pick, PICK_UPDATE); }
        catch (Exception e) { message("No file picker is available: " + e.getMessage()); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_UPDATE && result == RESULT_OK && data != null && data.getData() != null) {
            stage(data.getData());
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte value : bytes) out.append(String.format(Locale.ROOT, "%02x", value & 255));
        return out.toString();
    }

    private Properties inspect(File zip) throws IOException {
        try (ZipFile archive = new ZipFile(zip)) {
            ZipEntry meta = archive.getEntry("META-INF/com/android/metadata");
            if (meta == null || meta.getSize() < 1 || meta.getSize() > 65536
                    || archive.getEntry("META-INF/com/google/android/update-binary") == null)
                throw new IOException("Not a supported recovery OTA ZIP.");
            Properties props = new Properties();
            try (InputStream input = archive.getInputStream(meta)) { props.load(input); }
            if (!Build.DEVICE.equals(props.getProperty("pre-device")))
                throw new IOException("Package is for " + props.getProperty("pre-device", "an unknown device")
                    + ", not " + Build.DEVICE + ".");
            return props;
        }
    }

    private void stage(Uri uri) {
        busy = true; message("Copying update pack…");
        worker.execute(() -> {
            File directory = new File(getFilesDir(), "updates");
            if (!directory.exists() && !directory.mkdirs()) {
                onUi(() -> { busy = false; message("Could not create update staging folder."); }); return;
            }
            File target = new File(directory, "pending.zip");
            boolean complete = false;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long count = 0;
                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream output = new FileOutputStream(target)) {
                    if (input == null) throw new IOException("Could not open selected ZIP.");
                    byte[] bytes = new byte[1024 * 1024];
                    int n;
                    while ((n = input.read(bytes)) != -1) {
                        count += n;
                        if (count > MAX_UPDATE_BYTES) throw new IOException("Update exceeds 5 GiB.");
                        if (count > target.getUsableSpace() + target.length())
                            throw new IOException("Not enough free storage to stage the update.");
                        output.write(bytes, 0, n); digest.update(bytes, 0, n);
                    }
                    output.getFD().sync();
                }
                Properties metadata = inspect(target);
                onUi(() -> message("Checking recovery signature…"));
                RecoverySystem.verifyPackage(target, progress -> {
                    if (progress % 20 == 0) onUi(() -> message("Checking recovery signature… " + progress + "%"));
                }, null);
                String checksum = hex(digest.digest());
                complete = true;
                onUi(() -> {
                    busy = false;
                    message("Verified OTA for " + Build.DEVICE + ".");
                    confirmInstall(target, metadata, checksum);
                });
            } catch (Exception error) {
                String reason = error.getMessage();
                onUi(() -> { busy = false; message("Update rejected: " + reason); });
            } finally {
                if (!complete) target.delete();
            }
        });
    }

    private void confirmInstall(File file, Properties metadata, String checksum) {
        String kind = metadata.getProperty("xos-content",
            metadata.getProperty("ota-type", "OTA"));
        new AlertDialog.Builder(this)
            .setTitle("Install update and reboot?")
            .setMessage("Device: " + Build.DEVICE + "\nType: " + kind
                + "\nSHA-256: " + checksum + "\n\nRecovery will install this verified package. Save your work first.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Install and reboot", (dialog, which) -> {
                busy = true; message("Preparing recovery install…");
                worker.execute(() -> {
                    try { RecoverySystem.installPackage(this, file); }
                    catch (Exception error) {
                        onUi(() -> { busy = false; message("Recovery install failed: " + error.getMessage()); });
                    }
                });
            }).show();
    }

    private void developer() {
        body.removeAllViews();
        boolean available = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
            == PackageManager.PERMISSION_GRANTED;
        int enabled = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        LinearLayout adb = card("Android Debug Bridge",
            "USB debugging is currently " + (enabled == 1 ? "on." : "off.")
            + " Changing it affects this device immediately.");
        Button toggle = button(enabled == 1 ? "Turn off USB debugging" : "Turn on USB debugging",
            v -> new AlertDialog.Builder(this).setTitle("Change USB debugging?")
                .setMessage("Only enable debugging for computers you trust.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (d, w) -> {
                    try {
                        Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED,
                            enabled == 1 ? 0 : 1);
                        message("USB debugging setting changed."); developer();
                    } catch (SecurityException e) { message("This build has not granted developer control."); }
                }).show());
        toggle.setEnabled(available); adb.addView(toggle);
        if (!available) adb.addView(text("Available in a platform-signed xOS build.", 13, MUTED));
        LinearLayout boot = card("Boot options",
            "Recovery accepts signed update packages. The bootloader mode depends on the device; Samsung uses Download Mode, while Xiaomi uses fastboot.");
        boot.addView(button("Reboot to recovery", v -> confirmReboot("recovery")));
        boot.addView(button("Reboot to bootloader", v -> confirmReboot("bootloader")));
    }

    private void confirmReboot(String target) {
        new AlertDialog.Builder(this).setTitle("Reboot to " + target + "?")
            .setMessage("Save your work before restarting the device.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reboot", (d, w) -> {
                try { ((PowerManager)getSystemService(POWER_SERVICE)).reboot(target); }
                catch (SecurityException e) { message("This build has not granted reboot permission."); }
            }).show();
    }

    private void wallpapers() {
        body.removeAllViews();
        wallpaper("Obsidian · tablet", "Dark cyan crystalline layers",
            R.drawable.wallpaper_obsidian_tablet);
        wallpaper("Glacier · tablet", "Bright glass monolith at dawn",
            R.drawable.wallpaper_glacier_tablet);
        wallpaper("Obsidian · phone", "Tall dark crystalline fissure",
            R.drawable.wallpaper_obsidian_phone);
    }

    private void wallpaper(String title, String description, int resource) {
        LinearLayout item = card(title, description);
        ImageView preview = new ImageView(this);
        preview.setImageResource(resource);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        item.addView(preview, new LinearLayout.LayoutParams(-1, dp(165)));
        item.addView(button("Use as wallpaper", v -> {
            if (busy) return;
            busy = true; message("Applying wallpaper…");
            worker.execute(() -> {
                try {
                    WallpaperManager.getInstance(this).setResource(resource,
                        WallpaperManager.FLAG_SYSTEM);
                    onUi(() -> { busy = false; message(title + " applied."); });
                } catch (Exception error) {
                    onUi(() -> { busy = false; message("Wallpaper failed: " + error.getMessage()); });
                }
            });
        }));
    }

    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
