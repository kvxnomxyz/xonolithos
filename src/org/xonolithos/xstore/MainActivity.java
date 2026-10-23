package org.xonolithos.xstore;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private final int BG=0xff10120f, CARD=0xff1d211b, FG=0xffeef2e9, MUTED=0xffa7afa0, ACCENT=0xffb8f36b;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private List<Catalog.Entry> entries=new ArrayList<>();
    private LinearLayout cards;
    private TextView status;
    private EditText search;
    private Button refresh;
    private boolean busy=false;
    private String pendingApk;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable box(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private void post(Runnable action){runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())action.run();});}

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(saved!=null) pendingApk=saved.getString("pendingApk");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setPadding(dp(24),dp(16),dp(24),0);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(dp(24)+insets.getSystemWindowInsetLeft(),dp(16)+insets.getSystemWindowInsetTop(),
                dp(24)+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });
        TextView brand=label("xOS  /  XONOLITHOS",12,ACCENT);brand.setLetterSpacing(.15f);root.addView(brand);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("Xstore",38,FG);title.setTypeface(null,Typeface.BOLD);
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(70),1));
        refresh=new Button(this);refresh.setText("Refresh");refresh.setAllCaps(false);refresh.setOnClickListener(v->refresh());heading.addView(refresh);root.addView(heading);
        TextView intro=label("A place for your apps.",17,MUTED);root.addView(intro);
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(FG);search.setHintTextColor(MUTED);
        search.setHint("Search apps or authors");search.setTextSize(16);search.setPadding(dp(16),0,dp(16),0);search.setBackground(box(CARD,18));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));sp.setMargins(0,dp(24),0,dp(12));root.addView(search,sp);
        status=label("Loading catalog…",13,MUTED);status.setPadding(0,dp(4),0,dp(16));root.addView(status);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        cards=new LinearLayout(this);cards.setOrientation(LinearLayout.VERTICAL);scroll.addView(cards);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){render();} public void afterTextChanged(Editable e){}});
        // Expire only downloads older than a day; preserve files while the installer reads them.
        File[] cached=getCacheDir().listFiles();
        if(cached!=null)for(File f:cached)if(f.getName().endsWith(".apk") && System.currentTimeMillis()-f.lastModified()>86400000L)f.delete();
        SharedPreferences prefs=getPreferences(MODE_PRIVATE);
        try {
            entries=Catalog.entries(new JSONArray(prefs.getString("catalog","[]")));render();
            if(!entries.isEmpty()) status.setText("Saved catalog · Refresh to check for changes");
        } catch(Exception ignored){prefs.edit().remove("catalog").apply();}
        if(entries.isEmpty() || System.currentTimeMillis()-prefs.getLong("fetched",0)>3600000L)refresh();
    }

    private void refresh() {
        if(busy)return;
        busy=true;refresh.setEnabled(false);status.setText("Fetching the xOS app catalog…");
        worker.execute(()->{
            try {
                JSONArray data=Catalog.fetch();List<Catalog.Entry> next=Catalog.entries(data);
                getPreferences(MODE_PRIVATE).edit().putString("catalog",data.toString()).putLong("fetched",System.currentTimeMillis()).apply();
                post(()->{entries=next;status.setText(entries.size()+" apps · Xonolith catalog");busy=false;refresh.setEnabled(true);render();});
            } catch(Exception e){post(()->{busy=false;refresh.setEnabled(true);status.setText(e.getMessage());render();});}
        });
    }

    private void render() {
        cards.removeAllViews();
        String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int count=0;
        for(Catalog.Entry app:entries) {
            if(!(app.title+" "+app.author+" "+app.description).toLowerCase(Locale.ROOT).contains(query))continue;
            count++;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(20),dp(20),dp(16));card.setBackground(box(CARD,24));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(14));cards.addView(card,cp);
            TextView title=label(app.title,23,FG);title.setTypeface(null,Typeface.BOLD);card.addView(title);
            TextView author=label(app.author,13,ACCENT);author.setPadding(0,dp(5),0,dp(12));card.addView(author);
            TextView description=label(app.description,15,MUTED);description.setMaxLines(4);description.setEllipsize(TextUtils.TruncateAt.END);card.addView(description);
            Button details=new Button(this);details.setAllCaps(false);details.setText("View app");details.setOnClickListener(v->details(app));card.addView(details);
        }
        if(count==0) {
            TextView empty=label(entries.isEmpty()?"Your catalog starts here.\n\nApps will appear when the xOS appstore branch is published.":"No apps match your search.",19,MUTED);
            empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(64),dp(16),dp(32));cards.addView(empty);
        }
    }

    private void details(Catalog.Entry app) {
        String note=app.installable()?"\n\nAndroid will ask you to confirm installation.":"\n\nThe publisher has not added complete installation metadata yet.";
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(app.title).setMessage("By "+app.author+"\n\n"+app.description+note).setNegativeButton("Close",null);
        if(app.installable())dialog.setPositiveButton("Install",(d,w)->download(app));
        dialog.show();
    }

    private void download(Catalog.Entry app) {
        if(busy){Toast.makeText(this,"Please wait for the current request.",Toast.LENGTH_SHORT).show();return;}
        busy=true;refresh.setEnabled(false);status.setText("Downloading "+app.title+"…");
        worker.execute(()->{
            File file=new File(getCacheDir(),UUID.randomUUID()+".apk");
            try {
                Catalog.download(app,file);
                PackageInfo info=getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                if(info==null || !app.packageName.equals(info.packageName))throw new IOException("The APK package does not match its catalog entry.");
                post(()->{busy=false;refresh.setEnabled(true);pendingApk=file.getName();status.setText("Download verified. Ready to install.");install();});
            } catch(Exception e){file.delete();post(()->{busy=false;refresh.setEnabled(true);status.setText(e.getMessage());});}
        });
    }

    private void install() {
        if(pendingApk==null)return;
        if(!getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("Allow Xstore installations")
                .setMessage("Enable “Allow from this source” for Xstore, then return here to continue.")
                .setNegativeButton("Later",null).setPositiveButton("Open settings",(d,w)->{
                    try {startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}
                    catch(ActivityNotFoundException e){status.setText("Installation settings are unavailable on this device.");}
                }).show();
            return;
        }
        Uri uri=Uri.parse("content://"+ApkProvider.AUTHORITY+"/"+pendingApk);
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("APK",uri));
        try{startActivity(intent);pendingApk=null;}catch(ActivityNotFoundException e){status.setText("No package installer is available.");}
    }
    @Override public void onResume(){super.onResume();if(pendingApk!=null && getPackageManager().canRequestPackageInstalls())install();}
    @Override public void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("pendingApk",pendingApk);}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
