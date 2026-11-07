package com.sahl.tijaratak;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.codescanner.GmsBarcodeScanning;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    WebView w;
    String pending = "";

    void js(String fn, String arg) {
        w.evaluateJavascript(fn + "(" + JSONObject.quote(arg) + ")", null);
    }

    class Bridge {
        @JavascriptInterface
        public void scan() {
            runOnUiThread(() -> GmsBarcodeScanning.getClient(MainActivity.this).startScan()
                    .addOnSuccessListener(r -> {
                        String v = r.getRawValue();
                        if (v != null) js("onScan", v);
                    })
                    .addOnFailureListener(e -> js("toast", "Scan error")));
        }

        @JavascriptInterface
        public void exportData(String json) {
            pending = json;
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, "sahl-tijaratak.json");
                startActivityForResult(i, 1);
            });
        }

        @JavascriptInterface
        public void importData() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*");
                startActivityForResult(i, 2);
            });
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#0a0e17"));
        setContentView(w);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.addJavascriptInterface(new Bridge(), "Android");
        w.setWebViewClient(new WebViewClient());
        w.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int rq, int rs, Intent d) {
        super.onActivityResult(rq, rs, d);
        if (rs != RESULT_OK || d == null || d.getData() == null) return;
        Uri u = d.getData();
        try {
            if (rq == 1) {
                OutputStream o = getContentResolver().openOutputStream(u);
                o.write(pending.getBytes("UTF-8"));
                o.close();
                js("toast", "✓");
            } else if (rq == 2) {
                InputStream in = getContentResolver().openInputStream(u);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                js("onImport", bo.toString("UTF-8"));
            }
        } catch (Exception e) {
            js("toast", "Error");
        }
    }

    @Override
    public void onBackPressed() {
        w.evaluateJavascript("back()", v -> {
            if (!"1".equals(v)) finish();
        });
    }
}
