package org.xonolithos.xstore;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** A public GitHub catalog. No tokens, accounts, analytics, or Google libraries. */
public final class Catalog {
    public static final String REPOSITORY = "kvxnomxyz/xonolithos";
    private static final String API = "https://api.github.com/repos/" + REPOSITORY;
    public static final long MAX_APK = 512L * 1024 * 1024;

    public static final class Entry {
        public final String id, title, author, description, apk, sha256, packageName;
        Entry(JSONObject o) {
            id=o.optString("id"); title=o.optString("title"); author=o.optString("author");
            description=o.optString("description"); apk=o.optString("apk");
            sha256=o.optString("sha256"); packageName=o.optString("package");
        }
        public boolean installable() {
            return apk.startsWith("https://") && sha256.matches("[a-fA-F0-9]{64}")
                && packageName.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+");
        }
    }

    private static HttpURLConnection connect(String address) throws IOException {
        URL url = new URL(address);
        for (int redirects=0; redirects<6; redirects++) {
            if (!"https".equals(url.getProtocol()) || url.getUserInfo()!=null)
                throw new IOException("Only HTTPS downloads without credentials are accepted.");
            HttpURLConnection c=(HttpURLConnection)url.openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Xstore/0.1 (XonolithOS)");
            c.setRequestProperty("Accept","application/vnd.github+json, */*");
            int code=c.getResponseCode();
            if (code==301 || code==302 || code==303 || code==307 || code==308) {
                String location=c.getHeaderField("Location"); c.disconnect();
                if(location==null) throw new IOException("Download redirect has no location.");
                url=new URL(url,location); continue;
            }
            if (code!=200) {
                c.disconnect();
                if(code==404) throw new IOException("Catalog or file not published. The public appstore branch must exist.");
                if(code==403 || code==429) throw new IOException("GitHub request limit reached. Try again later.");
                throw new IOException("Server returned HTTP "+code);
            }
            return c;
        }
        throw new IOException("Too many download redirects.");
    }

    private static String text(String url, int limit) throws IOException {
        HttpURLConnection c=connect(url);
        try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))!=-1) {
                if(Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                if(out.size()+n>limit) throw new IOException("Catalog file is too large.");
                out.write(buffer,0,n);
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8).trim();
        } finally { c.disconnect(); }
    }

    public static JSONArray fetch() throws Exception {
        JSONObject branch=new JSONObject(text(API+"/branches/appstore",65536));
        String revision=branch.getJSONObject("commit").getString("sha");
        if(!revision.matches("[a-f0-9]{40,64}")) throw new IOException("Invalid catalog revision.");
        JSONObject tree=new JSONObject(text(API+"/git/trees/"+revision+"?recursive=1",4*1024*1024));
        if(tree.optBoolean("truncated")) throw new IOException("Catalog tree exceeds GitHub's listing limit.");
        Map<String,Set<String>> folders=new TreeMap<>();
        JSONArray paths=tree.getJSONArray("tree");
        for(int i=0;i<paths.length();i++) {
            JSONObject item=paths.getJSONObject(i);
            if(!"blob".equals(item.optString("type"))) continue;
            String[] bits=item.getString("path").split("/");
            if(bits.length!=2 || !bits[0].matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) continue;
            if(!folders.containsKey(bits[0])) folders.put(bits[0],new HashSet<String>());
            folders.get(bits[0]).add(bits[1]);
        }
        JSONArray result=new JSONArray();
        for(Map.Entry<String,Set<String>> folder:folders.entrySet()) {
            Set<String> files=folder.getValue();
            if(!files.containsAll(Arrays.asList("title.txt","author.txt","description.txt"))) continue;
            if(result.length()>=100) throw new IOException("This Xstore version supports up to 100 app folders.");
            String base="https://raw.githubusercontent.com/"+REPOSITORY+"/"+revision+"/"+folder.getKey()+"/";
            JSONObject entry=new JSONObject(); entry.put("id",folder.getKey());
            entry.put("title",text(base+"title.txt",512));
            entry.put("author",text(base+"author.txt",512));
            entry.put("description",text(base+"description.txt",32768));
            if(entry.getString("title").isEmpty()) throw new IOException("Empty app title: "+folder.getKey());
            String apk=files.contains("apk.txt")?text(base+"apk.txt",2048):
                files.contains("app.apk")?base+"app.apk":"";
            entry.put("apk",apk);
            entry.put("sha256",files.contains("sha256.txt")?text(base+"sha256.txt",128):"");
            entry.put("package",files.contains("package.txt")?text(base+"package.txt",256):"");
            result.put(entry);
        }
        return result;
    }

    public static List<Entry> entries(JSONArray data) throws Exception {
        List<Entry> list=new ArrayList<>();
        for(int i=0;i<data.length();i++) list.add(new Entry(data.getJSONObject(i)));
        list.sort((a,b)->a.title.compareToIgnoreCase(b.title));
        return list;
    }

    public static void download(Entry entry, File destination) throws Exception {
        if(!entry.installable()) throw new IOException("Download, package name, and SHA-256 are required.");
        HttpURLConnection c=connect(entry.apk);
        boolean complete=false;
        try {
            if(c.getContentLengthLong()>MAX_APK) throw new IOException("APK is larger than 512 MiB.");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            long size=0;
            try(InputStream in=c.getInputStream(); OutputStream out=new FileOutputStream(destination)) {
                byte[] buffer=new byte[65536]; int n;
                while((n=in.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                    size+=n; if(size>MAX_APK) throw new IOException("APK exceeds download limit.");
                    digest.update(buffer,0,n); out.write(buffer,0,n);
                }
            }
            StringBuilder hex=new StringBuilder();
            for(byte b:digest.digest()) hex.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!hex.toString().equalsIgnoreCase(entry.sha256)) throw new IOException("Checksum mismatch. Installation cancelled.");
            complete=true;
        } finally { c.disconnect(); if(!complete) destination.delete(); }
    }
}
