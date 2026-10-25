package org.xonolithos.xstore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/** F-Droid-inspired browsing for the public, revision-pinned xOS app catalog. */
public final class MainActivity extends Activity {
    private static final int BG=0xffedf4f7, CARD=0xffffffff, INK=0xff14313d;
    private static final int MUTED=0xff657e89, BLUE=0xff087d9b, PALE=0xffd9f1f5;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private List<Catalog.Entry> entries=new ArrayList<>();
    private LinearLayout cards, filters;
    private TextView status, count;
    private EditText search;
    private Button refresh;
    private boolean busy=false;
    private String pendingApk;
    private int filter=0;

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable box(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private TextView label(String value,int size,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;
    }
    private Button button(String name,View.OnClickListener action){
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setOnClickListener(action);return b;
    }
    private void post(Runnable action){
        runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())action.run();});
    }

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(saved!=null)pendingApk=saved.getString("pendingApk");
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(22),dp(22),dp(22),dp(20));
        top.setBackgroundColor(BLUE);
        TextView eyebrow=label("XONOLITHOS  /  COMMUNITY CATALOG",11,0xffc6f4fb);
        eyebrow.setLetterSpacing(.12f);top.addView(eyebrow);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("Xstore",34,0xffffffff);title.setTypeface(null,Typeface.BOLD);
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(62),1));
        refresh=button("Refresh",v->refresh());heading.addView(refresh);top.addView(heading);
        top.addView(label("Find independent apps for your device.",16,0xffe0f7fb));
        root.addView(top);
        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(17),dp(18),0);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);
        search.setHintTextColor(MUTED);search.setHint("Search apps and authors");
        search.setTextSize(16);search.setPadding(dp(16),0,dp(16),0);
        search.setBackground(box(CARD,16));
        content.addView(search,new LinearLayout.LayoutParams(-1,dp(54)));
        filters=new LinearLayout(this);filters.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(58));
        content.addView(filters,fp);
        count=label("Discover",19,INK);count.setTypeface(null,Typeface.BOLD);
        content.addView(count);
        status=label("Loading catalog…",13,MUTED);
        status.setPadding(0,dp(3),0,dp(12));content.addView(status);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        cards=new LinearLayout(this);cards.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(cards);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        renderFilters();
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){render();}
            public void afterTextChanged(Editable e){}
        });
        File[] cached=getCacheDir().listFiles();
        if(cached!=null)for(File f:cached)
            if(f.getName().endsWith(".apk") && System.currentTimeMillis()-f.lastModified()>86400000L)
                f.delete();
        SharedPreferences prefs=getPreferences(MODE_PRIVATE);
        try{
            entries=Catalog.entries(new JSONArray(prefs.getString("catalog","[]")));render();
            if(!entries.isEmpty())status.setText("Saved catalog · Refresh for changes");
        }catch(Exception ignored){prefs.edit().remove("catalog").apply();}
        if(entries.isEmpty()||System.currentTimeMillis()-prefs.getLong("fetched",0)>3600000L)refresh();
    }

    private void renderFilters(){
        filters.removeAllViews();
        String[] titles={"All","Installable","Browse only"};
        for(int i=0;i<titles.length;i++){
            final int selected=i;
            TextView chip=label(titles[i],13,i==filter?0xffffffff:BLUE);
            chip.setGravity(Gravity.CENTER);chip.setTypeface(null,Typeface.BOLD);
            chip.setBackground(box(i==filter?BLUE:PALE,16));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(34),1);
            cp.setMargins(0,0,i==2?0:dp(7),0);filters.addView(chip,cp);
            chip.setOnClickListener(v->{filter=selected;renderFilters();render();});
        }
    }

    private void refresh(){
        if(busy)return;
        busy=true;refresh.setEnabled(false);status.setText("Checking the appstore branch…");
        worker.execute(()->{
            try{
                JSONArray data=Catalog.fetch();
                List<Catalog.Entry> next=Catalog.entries(data);
                getPreferences(MODE_PRIVATE).edit().putString("catalog",data.toString())
                    .putLong("fetched",System.currentTimeMillis()).apply();
                post(()->{entries=next;status.setText("Catalog updated");busy=false;
                    refresh.setEnabled(true);render();});
            }catch(Exception e){
                post(()->{busy=false;refresh.setEnabled(true);
                    status.setText(e.getMessage()+(entries.isEmpty()?"":" · Showing saved catalog"));render();});
            }
        });
    }

    private void render(){
        if(cards==null||search==null)return;
        cards.removeAllViews();
        String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int visible=0;
        for(Catalog.Entry app:entries){
            if(filter==1&&!app.installable()||filter==2&&app.installable())continue;
            if(!(app.title+" "+app.author+" "+app.description).toLowerCase(Locale.ROOT).contains(query))
                continue;
            visible++;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.TOP);
            row.setPadding(dp(16),dp(16),dp(16),dp(16));row.setBackground(box(CARD,18));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
            rp.setMargins(0,0,0,dp(10));cards.addView(row,rp);
            TextView icon=label(app.title.isEmpty()?"?":app.title.substring(0,1).toUpperCase(Locale.ROOT),
                27,0xffffffff);
            icon.setTypeface(null,Typeface.BOLD);icon.setGravity(Gravity.CENTER);
            icon.setBackground(box(BLUE,14));
            row.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(14),0,0,0);
            row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView name=label(app.title,19,INK);name.setTypeface(null,Typeface.BOLD);
            name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);info.addView(name);
            TextView author=label(app.author,13,BLUE);info.addView(author);
            TextView description=label(app.description,14,MUTED);
            description.setMaxLines(2);description.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams descParams=new LinearLayout.LayoutParams(-1,-2);
            descParams.setMargins(0,dp(7),0,dp(8));info.addView(description,descParams);
            TextView badge=label(app.installable()?"Verified APK available":"Details only",12,
                app.installable()?BLUE:MUTED);
            info.addView(badge);
            row.setOnClickListener(v->details(app));
        }
        count.setText(visible+" "+(visible==1?"app":"apps"));
        if(visible==0){
            String text=entries.isEmpty()
                ?"The catalog has no published apps yet. Tap Refresh after the appstore branch is populated."
                :"No apps match this search or filter.";
            TextView empty=label(text,16,MUTED);empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24),dp(64),dp(24),dp(36));cards.addView(empty);
        }
    }

    private void details(Catalog.Entry app){
        String note=app.installable()
            ?"\n\nThe APK checksum and package name will be checked before Android asks to install."
            :"\n\nThis listing has no verified APK yet.";
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(app.title)
            .setMessage("By "+app.author+"\n\n"+app.description+note)
            .setNegativeButton("Close",null);
        if(app.installable())dialog.setPositiveButton("Download & install",(d,w)->download(app));
        dialog.show();
    }

    private void download(Catalog.Entry app){
        if(busy){Toast.makeText(this,"Please wait for the current request.",Toast.LENGTH_SHORT).show();return;}
        busy=true;refresh.setEnabled(false);status.setText("Downloading "+app.title+"…");
        worker.execute(()->{
            File file=new File(getCacheDir(),UUID.randomUUID()+".apk");
            try{
                Catalog.download(app,file);
                PackageInfo info=getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                if(info==null||!app.packageName.equals(info.packageName))
                    throw new IOException("The APK package does not match its catalog entry.");
                post(()->{busy=false;refresh.setEnabled(true);pendingApk=file.getName();
                    status.setText("Download verified. Ready to install.");install();});
            }catch(Exception e){
                file.delete();post(()->{busy=false;refresh.setEnabled(true);status.setText(e.getMessage());});
            }
        });
    }

    private void install(){
        if(pendingApk==null)return;
        if(!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setTitle("Allow Xstore installations")
                .setMessage("Enable “Allow from this source” for Xstore, then return here to continue.")
                .setNegativeButton("Later",null).setPositiveButton("Open settings",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName())));}
                    catch(ActivityNotFoundException e){status.setText("Installation settings are unavailable.");}
                }).show();
            return;
        }
        Uri uri=Uri.parse("content://"+ApkProvider.AUTHORITY+"/"+pendingApk);
        Intent intent=new Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri,"application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("APK",uri));
        try{startActivity(intent);pendingApk=null;}
        catch(ActivityNotFoundException e){status.setText("No package installer is available.");}
    }
    @Override public void onResume(){super.onResume();
        if(pendingApk!=null&&getPackageManager().canRequestPackageInstalls())install();}
    @Override public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putString("pendingApk",pendingApk);}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
