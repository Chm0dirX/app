package com.zalexdev.stryker.utils;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.zalexdev.stryker.BuildConfig;
import com.zalexdev.stryker.MainActivity;
import com.zalexdev.stryker.custom.Exploit;
import com.zalexdev.stryker.custom.Module;
import com.zalexdev.stryker.custom.Router;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Core {
    public final static String EXECUTE = "/data/data/com.zalexdev.stryker/files/chroot_exec ";
    public final static String BUSYBOX = "/data/data/com.zalexdev.stryker/files/busybox ";
    public final String versionName = BuildConfig.VERSION_NAME;
    public final int versionInt = BuildConfig.VERSION_CODE;
    private final SharedPreferences preferences;
    public Context context;

    public Core(Context context1) {

        context = context1;
        preferences = PreferenceManager.getDefaultSharedPreferences(context1);
    }

    public Context getContext2() {
        return context;
    }

    public int connectWiFi2(String ssid, String psk){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"",psk);
        WifiManager wifiManager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        return netId;
    }
    public void delwifi(int netid){
        WifiManager wifiManager = (WifiManager)context.getSystemService(WIFI_SERVICE);
        wifiManager.removeNetwork(netid);
    }
    public void savenetwork(String bssid,String psk,String pin){
        ArrayList<String> nw = new ArrayList<>();
        nw.add(psk);
        nw.add(pin);
        putListString(bssid,nw);
    }
    public ArrayList<String> getnetwork(String bssid){
        return getListString(bssid);
    }
    public String chroot() {
        return getString("chroot_path");
    }

    public int getInt(String key) {
        return preferences.getInt(key, 0);
    }

    public float getFloat(String key) {
        return preferences.getFloat(key, 0);
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    public ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }
    public ArrayList<Integer> getListInt(String key) {
        String[] myList = TextUtils.split(preferences.getString(key, ""), "‚‗‚");
        ArrayList<String> arrayToList = new ArrayList<String>(Arrays.asList(myList));
        ArrayList<Integer> newList = new ArrayList<Integer>();

        for (String item : arrayToList)
            newList.add(Integer.parseInt(item));

        return newList;
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    public void putInt(String key, int value) {
        isNull(key);
        preferences.edit().putInt(key, value).apply();
    }

    public void putString(String key, String value) {
        isNull(key);
        checkNull(value);
        preferences.edit().putString(key, value).apply();
    }

    public void putListString(String key, ArrayList<String> stringList) {
        isNull(key);
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
    public void putListInt(String key, ArrayList<Integer> intList) {
        Integer[] myIntList = intList.toArray(new Integer[intList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myIntList)).apply();
    }
    public void putBoolean(String key, boolean value) {
        isNull(key);
        preferences.edit().putBoolean(key, value).apply();
    }

    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    private void isNull(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
    }

    private void checkNull(String value) {
        if (value == null) {
            throw new NullPointerException();
        }
    }

    public void toaster(String msg) {
        Toast toast = Toast.makeText(context,
                msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void writetolog(ArrayList<String> datas, boolean iserror) {
        if (getBoolean("debug")) {
            String path = "/storage/emulated/0/Stryker/";
            File folder = new File(path);
            folder.mkdirs();
            File file = new File(folder, "log.txt");
            try {
                boolean newf = file.createNewFile();

                FileOutputStream fOut = new FileOutputStream(file, true);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                if (newf) {
                    myOutWriter.append("Model:" + android.os.Build.MANUFACTURER + android.os.Build.MODEL + "\n");
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                        myOutWriter.append("Android:" + " " + Build.VERSION.SDK_INT + " (arm64)\n");
                    } else {
                        myOutWriter.append("Android:" + " " + Build.VERSION.SDK_INT + " (arm32)\n");
                    }

                }
                for (int i = 0; i < datas.size(); i++) {
                    SimpleDateFormat s = new SimpleDateFormat("[hh:mm:ss] ");
                    String format = s.format(new Date());
                    if (iserror) {
                        myOutWriter.append(format + "[ERROR] " + datas.get(i) + "\n");
                    } else {
                        myOutWriter.append(format + datas.get(i) + "\n");
                    }
                }
                myOutWriter.close();
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }

    public void writelinetolog(String data) {

        String path = "/storage/emulated/0/Stryker/";
        File folder = new File(path);
        folder.mkdirs();
        File file = new File(folder, "ips.txt");
        try {
            boolean newf = file.createNewFile();

            FileOutputStream fOut = new FileOutputStream(file, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            if (newf) {
                myOutWriter.append("Model:" + android.os.Build.MANUFACTURER + android.os.Build.MODEL + "\n");
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    myOutWriter.append("Android:" + " " + Build.VERSION.SDK_INT + " (arm64)\n");
                } else {
                    myOutWriter.append("Android:" + " " + Build.VERSION.SDK_INT + " (arm32)\n");
                }

            }

            myOutWriter.append(data + "\n");

            myOutWriter.close();
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    public void saveresult(ArrayList<Router> rs) {
        String path = "/storage/emulated/0/Stryker/";
        File folder = new File(path);
        folder.mkdirs();
        File file = new File(folder, "routerscan.csv");
        boolean newf = false;
        try {
            newf = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
        if (newf) {
            try {
                myOutWriter.append("\"IP Address\";\"Port\";\"Time (ms)\";\"Status\";\"Authorization\";\"Server name / Realm name / Device type\";\"Radio Off\";\"Hidden\";\"BSSID\";\"ESSID\";\"Security\";\"Key\";\"WPS PIN\";\"LAN IP Address\";\"LAN Subnet Mask\";\"WAN IP Address\";\"WAN Subnet Mask\";\"WAN Gateway\";\"Domain Name Servers\";\"Latitude\";\"Longitude\";\"Comments\"" + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Router r : rs) {
            if (r.getSuccess()) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append('"' + r.getIp() + '"' + ';');
                    sb.append('"' + "80" + '"' + ';');
                    sb.append('"' + "100" + '"' + ';');
                    sb.append('"' + "Done" + '"' + ';');
                    sb.append('"' + r.getAuth() + '"' + ';');
                    sb.append('"' + r.getTitle() + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + r.getBssid() + '"' + ';');
                    sb.append('"' + r.getSsid() + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + r.getPsk() + '"' + ';');
                    sb.append('"' + r.getWps() + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    sb.append('"' + " " + '"' + ';');
                    myOutWriter.append(sb + "\r\n");

                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e);
                }
            }
        }
        try {
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void vibrate(int mil) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(mil, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).vibrate(mil);
        }
    }

    public ArrayList<String> getListFiles(File parentDir) {
        ArrayList<String> inFiles = new ArrayList<String>();
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    inFiles.addAll(getListFiles(file));
                } else {

                    inFiles.add(file.getAbsolutePath());

                }
            }
        }
        return inFiles;
    }
    public void saveExploit(Exploit exploit){
        ArrayList<String> exploits = getListString("exploits");
        exploits.add(parseExploit(exploit));
        putListString("exploits",exploits);
    }
    public String parseExploit(Exploit exploit){
        JSONObject exp = new JSONObject();
        try {
            exp.put("title",exploit.getTitle());
            exp.put("path",exploit.getPath());
            exp.put("pattern",exploit.getSuccesspatern());
            exp.put("lang",exploit.getLang());
            exp.put("args",exploit.getArgs());
            exp.put("issys",exploit.getIssystem());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return exp.toString();
    }
    public Exploit getExploitbyPos(int pos){
        Exploit exploit = new Exploit();
        ArrayList<String> exploits = getListString("exploits");
        return unparseExploit(exploits.get(pos));
    }
    public Exploit getExploitbyTitle(String title){
        Exploit t = new Exploit();
        ArrayList<Exploit> exploits = getExploits();
        for (Exploit e: exploits){
            if (e.getTitle().equals(title)){
                t = e;
            }
        }
        return t;
    }
    public void updateexploits(){
        new CustomCommand("rm -rf /data/local/stryker/release/exploits",this).execute();
        new CustomCommand("cp -R /storage/emulated/0/Stryker/exploits /data/local/stryker/release/exploits",this).execute();
        new CustomCommand("chmod 777 -R /data/local/stryker/release/exploits",this).execute();
    }
    public void changeExploitbyPos(int pos,Exploit exploit){
        ArrayList<String> exploits = getListString("exploits");
        exploits.set(pos,parseExploit(exploit));
        putListString("exploits",exploits);
    }
    public void deleteExploit(int id){
        ArrayList<String> exploits = getListString("exploits");
        exploits.remove(id);
        putListString("exploits",exploits);
    }
    public Exploit unparseExploit(String exploitstring){
        Exploit exploit = new Exploit();
        try {
            JSONObject exp = new JSONObject(exploitstring);
            exploit.setTitle(exp.getString("title"));
            exploit.setPath(exp.getString("path"));
            exploit.setSuccesspatern(exp.getString("pattern"));
            exploit.setLang(exp.getString("lang"));
            exploit.setArgs(exp.getString("args"));
            exploit.setIssystem(exp.getBoolean("issys"));
        } catch (JSONException e) {e.printStackTrace();}
        return exploit;
    }
    public ArrayList<Exploit> getExploits(){
        ArrayList<Exploit> list= new ArrayList<>();
        ArrayList<String> exploits = getListString("exploits");
        for (String e : exploits){
            list.add(unparseExploit(e));
        }
        if (list.size() < 3){
            Exploit eternal = new Exploit();
            eternal.setTitle("Eternalblue");
            eternal.setPath("eternalscan.py");
            eternal.setArgs(" {IP}");
            eternal.setIssystem(true);
            eternal.setSuccesspatern("VUNLFOUNDED");
            eternal.setLang("Python");
            saveExploit(eternal);
            Exploit ghost = new Exploit();
            ghost.setTitle("SMBGhost");
            ghost.setPath("ghostscanner.py");
            ghost.setArgs(" {IP}");
            ghost.setIssystem(true);
            ghost.setSuccesspatern("VUNLFOUNDED");
            ghost.setLang("Python");
            saveExploit(ghost);
            Exploit blue = new Exploit();
            blue.setTitle("Bluekeep");
            blue.setPath("bluekeepscan.py");
            blue.setArgs(" {IP}");
            blue.setIssystem(true);
            blue.setSuccesspatern("VULNERABLE");
            blue.setLang("Python");
            saveExploit(blue);
            exploits = getListString("exploits");
            for (String e : exploits){
                list.add(unparseExploit(e));
            }
        }
        return list;
    }
    public String str(String aString) {
        return context.getString(context.getResources().getIdentifier(aString, "string", context.getPackageName()));
    }
    public boolean is64Bit() {
        return (Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0);
    }
    public String getDeviceNameByPid(String pid){
        String JSON = "";
        String result = "";
        BufferedReader reader = null;
        JSONObject usblist = null;
            try {
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open("devices.txt")));
                String mLine;
                while ((mLine = reader.readLine()) != null) {
                    JSON = mLine;
                }
                usblist = new JSONObject(JSON);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            try {
                JSONArray list = usblist.getJSONArray("list");
                for (int i = 0;i<list.length();i++){
                    JSONObject temp = list.getJSONObject(i);
                    if (temp.has(pid)){
                        result = temp.getString(pid);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return  result;

    }
    public boolean checkroot() {
        try {
            return new CheckRoot().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return  false;
        }
    }
    public String getStorage() {
        return getExternalStorageDirectory().getAbsolutePath() + "/";
    }
    public boolean checkmodel(String model){
        BufferedReader reader = null;
        boolean result = false;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("routes.txt")));
        String mLine;
        while ((mLine = reader.readLine()) != null) {
            if(model.toLowerCase(Locale.ROOT).contains(mLine.toLowerCase(Locale.ROOT))){
                result = true;
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    public  void installApplication(Context context, String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uriFromFile(context, new File(filePath)), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("TAG", "Error in opening the file!");
        }
    }

    private static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public JSONObject getjsonbyurl(String url){
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {

            if (doc != null) {
                return new JSONObject(doc.text());
            }else {
                return new JSONObject();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    public ArrayList<Module> getModules(){
        ArrayList<Module> modules = new ArrayList<>();
        JSONObject getmodules = getjsonbyurl("https://raw.githubusercontent.com/stryker-project/stryker-modules/main/modules.list");
        try {
            if (getmodules.has("status") && getmodules.getBoolean("status")){
                JSONArray mlist = getmodules.getJSONArray("list");
                for (int i = 0;i<mlist.length();i++){
                    JSONObject modulejson = mlist.getJSONObject(i);
                    Module m = new Module();
                    m.setAuthor(modulejson.getString("author"));
                    m.setName(modulejson.getString("name"));
                    m.setDesc(modulejson.getString("desc"));
                    m.setPksg(modulejson.getString("pkgs"));
                    m.setOnly64bit(modulejson.getBoolean("bit"));
                    m.setProper(modulejson.getBoolean("proper"));
                    m.setSrcinstall(modulejson.getString("src"));
                    m.setVersion(modulejson.getDouble("version"));
                    modules.add(m);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  modules;
    }
    public void installmod(String name){
        ArrayList<String> mods = getListString("installed_modules");
        mods.add(name);
        putListString("installed_modules",mods);
    }
    public boolean checkmod(String name){
        ArrayList<String> mods = getListString("installed_modules");
        return mods.contains(name);
    }
    public void deletemod(String name){
        ArrayList<String> mods = getListString("installed_modules");
        mods.remove(name);
        putListString("installed_modules",mods);
    }
    public void unzip(File zipFile, File targetDirectory)  {

        if(!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        try {ZipInputStream zis;

            zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipFile)));

        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    } catch (IOException e) {
        e.printStackTrace();

        }
    }
    public Boolean remountcore(){
        try {unmountcore();
            mountcore();

            boolean o3 = new FixInet(this).execute().get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }

    }
    public Boolean mountcore(){
        try {
           boolean o2 = new CustomCommand("/data/data/com.zalexdev.stryker/files/bootroot",this).execute().get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }

    }
    public Boolean unmountcore(){
        try {
            boolean o2 = new CustomCommand("/data/data/com.zalexdev.stryker/files/killroot",this).execute().get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }

    }
    public boolean ping(String ip, int port,int timeout) {
        try {
            URI uri;
            Log.e("Ping","Pinging... "+ip+":"+port);
            if(port !=443){uri = URI.create("http://" + ip + ":" + port + "/");}else{uri = URI.create("https://" + ip + "/"); }
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            Log.e("Ping","Finished ping "+ip+":"+port);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(connection::disconnect, timeout + 4000);
            return connection.getResponseCode() >= 200 && !(connection.getResponseCode() == 404) && !(connection.getResponseCode() == 403);
        } catch (Exception e) {
            return false;
        }
    }
    public void MoveNext(ViewPager mPager) {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }

    public void MovePrevious(ViewPager mPager) {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }
    public boolean isInstalledOnSdCard() {
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
                ApplicationInfo ai = pi.applicationInfo;
                return (ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE;
            } catch (PackageManager.NameNotFoundException ignored) {
            }

        return false;
    }

}
