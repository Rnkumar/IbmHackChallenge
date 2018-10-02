package com.codeworks.projects.collectingsocialdata;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;

public class SuggestionsActivity extends AppCompatActivity {

    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Suggestions");
        String tag = getIntent().getExtras().getString("tag");
        HashMap<String,String> suggestionsMap = new HashMap<>();
        suggestionsMap.put("sadness","https://www.saavn.com/s/album/english/Motivational-Songs-2015/MPAXj-5fXqo_");
        suggestionsMap.put("joy","https://www.saavn.com/s/radio/hindi-featured-station/party");
        suggestionsMap.put("anger","https://www.saavn.com/s/artist/relaxing-music-albums/sHxjyM7FIZ4_");
        suggestionsMap.put("tentative","https://gaana.com/song/stress-relief-18");
        suggestionsMap.put("confident","https://gaana.com/album/confident-english-2015-3");
        suggestionsMap.put("fear","https://gaana.com/song/stress-relief-18");
        suggestionsMap.put("analytical","https://gaana.com/song/adventurous-song");
        webView = findViewById(R.id.suggestionswebview);
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Loading!");
        WebViewClient webViewClient = new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                dialog.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dialog.show();
            }
        };
        webView.setWebViewClient(webViewClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.loadUrl(suggestionsMap.get(tag));
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()){
            webView.goBack();
        }else
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
