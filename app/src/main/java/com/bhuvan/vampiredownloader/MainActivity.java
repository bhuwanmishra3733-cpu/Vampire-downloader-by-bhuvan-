package com.bhuvan.vampiredownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout root;
    private WebView webView;
    private EditText addressBox;
    private TextView status;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private Button makeButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private TextView heading(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(23);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Color.rgb(100, 20, 20));
        t.setPadding(dp(16), dp(16), dp(16), dp(8));
        return t;
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 248, 248));
        setContentView(root);
    }

    private void addNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);

        nav.addView(
                makeButton("Home", this::showHome),
                new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        nav.addView(
                makeButton("Browse", this::showBrowser),
                new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        nav.addView(
                makeButton("My Files", this::showLibrary),
                new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        root.addView(nav);
    }

    private void showHome() {
        setupRoot();

        root.addView(heading("Vampire Downloader"));

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "by Bhuvan\n\n" +
                "Browse websites, download supported direct media links, " +
                "and keep saved files in private storage."
        );
        subtitle.setTextSize(16);
        subtitle.setPadding(
                dp(16), dp(4), dp(16), dp(16)
        );

        root.addView(subtitle);

        root.addView(makeButton(
                "Browse Websites",
                this::showBrowser
        ));

        root.addView(makeButton(
                "Open YouTube",
                () -> loadUrl("https://www.youtube.com/")
        ));

        root.addView(makeButton(
                "My Private Downloads",
                this::showLibrary
        ));

        TextView note = new TextView(this);
        note.setText(
                "\nNote: Not every website supports direct downloads. " +
                "Protected streams and restricted content may not work."
        );
        note.setPadding(
                dp(16), dp(12), dp(16), dp(12)
        );
        note.setTextSize(13);

        root.addView(note);
        addNav();
    }

    private void showBrowser() {
        setupRoot();

        root.addView(heading("Browse & Download"));

        LinearLayout addressRow = new LinearLayout(this);
        addressRow.setOrientation(LinearLayout.HORIZONTAL);

        addressBox = new EditText(this);
        addressBox.setSingleLine(true);
        addressBox.setHint("Enter website URL");
        addressBox.setText("https://www.youtube.com/");

        addressRow.addView(
                addressBox,
                new LinearLayout.LayoutParams(0, dp(52), 1)
        );

        addressRow.addView(
                makeButton("Go", this::loadAddress),
                new LinearLayout.LayoutParams(dp(72), dp(52))
        );

        root.addView(addressRow);

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);

        shortcuts.addView(
                makeButton(
                        "YouTube",
                        () -> loadUrl("https://www.youtube.com/")
                ),
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        shortcuts.addView(
                makeButton("Search Web", () -> {
                    String q = addressBox.getText().toString().trim();

                    if (q.isEmpty()) {
                        q = "video";
                    }

                    loadUrl(
                            "https://www.google.com/search?q=" +
                            Uri.encode(q)
                    );
                }),
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        root.addView(shortcuts);

        webView = new WebView(this);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings()
                .setMediaPlaybackRequiresUserGesture(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {
                if (addressBox != null) {
                    addressBox.setText(url);
                }
            }
        });

        webView.setDownloadListener(
                (url, userAgent, contentDisposition, mimeType,
                 contentLength) -> {

                    String name = URLUtil.guessFileName(
                            url, contentDisposition, mimeType
                    );

                    downloadToPrivateStorage(
                            url, userAgent, name
                    );
                }
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        status = new TextView(this);
        status.setText(
                "Direct downloadable media links may work."
        );
        status.setTextSize(12);
        status.setPadding(
                dp(10), dp(4), dp(10), dp(4)
        );

        root.addView(status);
        addNav();

        webView.loadUrl("https://www.youtube.com/");
    }

    private void loadAddress() {
        if (addressBox == null) {
            return;
        }

        String input = addressBox.getText().toString().trim();

        if (input.isEmpty()) {
            return;
        }

        if (!input.matches("(?i)^https?://.*")) {
            if (input.contains(".") && !input.contains(" ")) {
                input = "https://" + input;
            } else {
                input = "https://www.google.com/search?q=" +
                        Uri.encode(input);
            }
        }

        loadUrl(input);
    }

    private void loadUrl(String url) {
        if (webView == null) {
            showBrowser();
        }

        if (addressBox != null) {
            addressBox.setText(url);
        }

        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void downloadToPrivateStorage(
            String mediaUrl,
            String userAgent,
            String suggestedName) {

        if (status != null) {
            status.setText("Downloading: " + suggestedName);
        }

        Toast.makeText(
                this,
                "Download started",
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {
            HttpURLConnection connection = null;
            FileOutputStream output = null;
            InputStream input = null;

            try {
                URL url = new URL(mediaUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);

                if (userAgent != null && !userAgent.isEmpty()) {
                    connection.setRequestProperty(
                            "User-Agent", userAgent
                    );
                }

                connection.connect();

                int code = connection.getResponseCode();

                if (code < 200 || code >= 300) {
                    throw new Exception(
                            "Server response " + code
                    );
                }

                String safeName = sanitizeName(suggestedName);

                if (safeName.isEmpty()) {
                    safeName = "download_" +
                            System.currentTimeMillis();
                }

                File dir = new File(
                        getFilesDir(), "downloads"
                );

                if (!dir.exists() && !dir.mkdirs()) {
                    throw new Exception(
                            "Could not create private folder"
                    );
                }

                File destination = new File(dir, safeName);

                if (destination.exists()) {
                    int dot = safeName.lastIndexOf('.');

                    String stem = dot > 0
                            ? safeName.substring(0, dot)
                            : safeName;

                    String ext = dot > 0
                            ? safeName.substring(dot)
                            : "";

                    destination = new File(
                            dir,
                            stem + "_" +
                            System.currentTimeMillis() + ext
                    );
                }

                input = new BufferedInputStream(
                        connection.getInputStream()
                );

                output = new FileOutputStream(destination);

                byte[] buffer = new byte[8192];
                int count;

                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                output.flush();

                File done = destination;

                runOnUiThread(() -> {
                    if (status != null) {
                        status.setText(
                                "Saved privately: " +
                                done.getName()
                        );
                    }

                    Toast.makeText(
                            this,
                            "Saved in My Files",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {
                String msg = e.getMessage() == null
                        ? "Download failed"
                        : e.getMessage();

                runOnUiThread(() -> {
                    if (status != null) {
                        status.setText(
                                "Download failed: " + msg
                        );
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Download failed")
                            .setMessage(
                                    msg +
                                    "\nThe link may require login, " +
                                    "cookies, or a site-specific method."
                            )
                            .setPositiveButton("OK", null)
                            .show();
                });

            } finally {
                try {
                    if (input != null) input.close();
                } catch (Exception ignored) {
                }

                try {
                    if (output != null) output.close();
                } catch (Exception ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "";
        }

        String decoded = name;

        try {
            decoded = URLDecoder.decode(name, "UTF-8");
        } catch (Exception ignored) {
        }

        decoded = decoded.replaceAll(
                "[\\\\/:*?\"<>|\\p{Cntrl}]",
                "_"
        ).trim();

        if (decoded.length() > 120) {
            decoded = decoded.substring(0, 120);
        }

        return decoded;
    }

    private void showLibrary() {
        setupRoot();

        root.addView(heading("My Private Files"));

        TextView info = new TextView(this);
        info.setText(
                "Files are stored inside this app's private storage, " +
                "not in the public Gallery or Downloads folder."
        );
        info.setPadding(
                dp(16), 0, dp(16), dp(8)
        );

        root.addView(info);

        ScrollView scroll = new ScrollView(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        File dir = new File(
                getFilesDir(), "downloads"
        );

        File[] items = dir.exists()
                ? dir.listFiles()
                : null;

        if (items == null || items.length == 0) {
            TextView empty = new TextView(this);

            empty.setText(
                    "No downloads yet.\n\n" +
                    "Browse a website and tap a direct " +
                    "downloadable media link."
            );

            empty.setPadding(
                    dp(16), dp(20), dp(16), dp(20)
            );

            list.addView(empty);

        } else {
            for (File f : items) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(
                        dp(12), dp(8), dp(12), dp(8)
                );

                TextView name = new TextView(this);
                name.setText(
                        f.getName() + "\n" +
                        readableSize(f.length())
                );
                name.setTextSize(15);

                row.addView(name);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);

                actions.addView(
                        makeButton("Play", () -> playFile(f)),
                        new LinearLayout.LayoutParams(0, dp(44), 1)
                );

                actions.addView(
                        makeButton("Delete", () -> confirmDelete(f)),
                        new LinearLayout.LayoutParams(0, dp(44), 1)
                );

                row.addView(actions);
                list.addView(row);

                View divider = new View(this);
                divider.setBackgroundColor(Color.LTGRAY);

                list.addView(
                        divider,
                        new LinearLayout.LayoutParams(-1, dp(1))
                );
            }
        }

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        addNav();
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    Locale.US, "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                Locale.US, "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    private void playFile(File f) {
        String n = f.getName().toLowerCase(Locale.US);

        if (!(n.endsWith(".mp4")
                || n.endsWith(".m4v")
                || n.endsWith(".3gp")
                || n.endsWith(".mp3")
                || n.endsWith(".m4a")
                || n.endsWith(".aac")
                || n.endsWith(".wav")
                || n.endsWith(".ogg")
                || n.endsWith(".webm"))) {

            new AlertDialog.Builder(this)
                    .setMessage(
                            "This file type may not be playable."
                    )
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this).create();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(
                dp(12), dp(12), dp(12), dp(12)
        );

        TextView title = new TextView(this);
        title.setText(f.getName());
        title.setTextSize(16);

        box.addView(title);

        VideoView video = new VideoView(this);

        box.addView(
                video,
                new LinearLayout.LayoutParams(-1, dp(240))
        );

        video.setVideoURI(Uri.fromFile(f));
        video.setOnPreparedListener(MediaPlayer::start);

        video.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(
                    this,
                    "Android may not support this format.",
                    Toast.LENGTH_LONG
            ).show();

            return true;
        });

        dialog.setView(box);

        dialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                "Close",
                (d, w) -> {
                    try {
                        video.stopPlayback();
                    } catch (Exception ignored) {
                    }
                }
        );

        dialog.show();

        dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener(v -> {
            try {
                video.stopPlayback();
            } catch (Exception ignored) {
            }

            dialog.dismiss();
        });

        video.start();
    }

    private void confirmDelete(File f) {
        new AlertDialog.Builder(this)
                .setTitle("Delete file?")
                .setMessage(
                        "Delete " + f.getName() +
                        " from private storage?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    if (f.delete()) {
                        Toast.makeText(
                                this,
                                "Deleted",
                                Toast.LENGTH_SHORT
                        ).show();
}
  
