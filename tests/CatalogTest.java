package org.xonolithos.xstore;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** End-to-end catalog parsing and downloads over a deterministic HTTPS test transport. */
public class CatalogTest {
    private static final Map<String, byte[]> bodies=new HashMap<>();
    private static final String commit="0123456789abcdef0123456789abcdef01234567";
    private static final String api="https://api.github.com/repos/"+Catalog.REPOSITORY;
    private static final String raw="https://raw.githubusercontent.com/"+Catalog.REPOSITORY+"/"+commit+"/notes/";
    static void put(String url,String body){bodies.put(url,body.getBytes(StandardCharsets.UTF_8));}
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    static String hash(byte[] data)throws Exception {
        StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))out.append(String.format("%02x",b&255));return out.toString();
    }
    public static void main(String[] args)throws Exception {
        URL.setURLStreamHandlerFactory(protocol->"https".equals(protocol)?new URLStreamHandler(){
            protected URLConnection openConnection(URL url){return new HttpURLConnection(url){
                public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
                public int getResponseCode(){return bodies.containsKey(url.toString())?200:404;}
                public InputStream getInputStream(){return new ByteArrayInputStream(bodies.get(url.toString()));}
                public long getContentLengthLong(){return bodies.get(url.toString()).length;}
            };}
        }:null);
        put(api+"/branches/appstore","{\"commit\":{\"sha\":\""+commit+"\"}}");
        JSONArray tree=new JSONArray();
        for(String file:new String[]{"title.txt","author.txt","description.txt"})
            tree.put(new JSONObject().put("type","blob").put("path","notes/"+file));
        put(api+"/git/trees/"+commit+"?recursive=1",new JSONObject().put("tree",tree).toString());
        put(raw+"title.txt","Notes");put(raw+"author.txt","A publisher");put(raw+"description.txt","Plain text notes.");
        List<Catalog.Entry> entries=Catalog.entries(Catalog.fetch());
        check(entries.size()==1,"metadata-only app must be listed");
        check(!entries.get(0).installable(),"metadata-only app must not install");
        for(String file:new String[]{"apk.txt","package.txt","sha256.txt"})
            tree.put(new JSONObject().put("type","blob").put("path","notes/"+file));
        put(api+"/git/trees/"+commit+"?recursive=1",new JSONObject().put("tree",tree).toString());
        byte[] apk="test fixture content".getBytes(StandardCharsets.UTF_8);
        put(raw+"apk.txt","https://downloads.example/app.apk");
        put(raw+"package.txt","org.example.notes");put(raw+"sha256.txt",hash(apk));
        bodies.put("https://downloads.example/app.apk",apk);
        Catalog.Entry entry=Catalog.entries(Catalog.fetch()).get(0);
        check(entry.installable(),"complete entry should be installable");
        File output=File.createTempFile("xstore-test",".apk");
        try {
            Catalog.download(entry,output);
            check(Arrays.equals(apk,Files.readAllBytes(output.toPath())),"verified bytes must be retained");
            put("https://downloads.example/app.apk","tampered");
            boolean rejected=false;try{Catalog.download(entry,output);}catch(IOException expected){rejected=true;}
            check(rejected&&!output.exists(),"tampered download must be rejected and removed");
            put(raw+"apk.txt","http://downloads.example/app.apk");
            check(!Catalog.entries(Catalog.fetch()).get(0).installable(),"plain HTTP must not enable install");
            bodies.remove(api+"/branches/appstore");
            rejected=false;try{Catalog.fetch();}catch(IOException expected){rejected=expected.getMessage().contains("not published");}
            check(rejected,"missing appstore branch must have a useful error");
        } finally{output.delete();}
        System.out.println("PASS: catalog parsing, pinned revision, browse-only entries, verified download, tampering, HTTP and missing-branch handling");
    }
}
