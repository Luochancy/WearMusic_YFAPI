package cn.wearbbs.music.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.wearbbs.music.R;
import cn.wearbbs.music.api.FMApi;
import cn.wearbbs.music.api.MusicApi;
import cn.wearbbs.music.api.UpdateApi;
import cn.wearbbs.music.api.UserApi;
import cn.wearbbs.music.util.HeadSetUtil;
import cn.wearbbs.music.util.PermissionUtil;
import cn.wearbbs.music.util.UserInfoUtil;
import me.wcy.lrcview.LrcView;

public class MainActivity extends SlideBackActivity {
    public static MediaPlayer mediaPlayer;
    public static boolean playing = false;
    public static int musicIndex = 0;
    List search_list;
    String url;
    String type;
    static double Version = 2.1;
    File lrcFile;
    LrcView lrcView;
    Map lrc_map;
    String id;
    int zt = 0;
    String cookie;
    Boolean will_next = false;
    List mvids;
    String coverUrl;
    public static boolean prepareDone = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        search_list = new ArrayList();
        mvids = new ArrayList();
        prepareDone = false;
        String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        HeadSetUtil.getInstance().setOnHeadSetListener(headSetListener);
        HeadSetUtil.getInstance().open(MainActivity.this);
        if (PermissionUtil.checkPermission(this,PERMISSION_STORAGE)) {
            try {
                File dl = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/deleted.lock");
                File old_cookie = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/cookie.txt");
                if(!dl.exists()){
                    File dir = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/");
                    dir.delete();
                    dir.mkdir();
                    dl.createNewFile();
                }
                if(old_cookie.exists()){
                    old_cookie.delete();
                }
                Thread updateThread = new Thread(()->{
                    Map update_map = null;
                    try {
                        update_map = new UpdateApi().checkUpdate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(Double.parseDouble(update_map.get("version").toString()) > Version){
                        Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                        intent.putExtra("data",update_map.toString());
                        MainActivity.this.runOnUiThread(()->startActivity(intent));
                        finish();
                    }
                });
                updateThread.start();
                File ol = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/outline.ini");
                if(ol.exists()){
                    ol.delete();
                }
            } catch (Exception e) {
                File ol = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/outline.ini");
                if(ol.exists()){
                    type = "1";
                    Intent get_music = getIntent();
                    mvids = JSON.parseArray(get_music.getStringExtra("mvids"));
                    String start = get_music.getStringExtra("start");
                    ScrollView sv_main = findViewById(R.id.sv_main);
                    LinearLayout ly = findViewById(R.id.ly_search);
                    sv_main.setVisibility(View.VISIBLE);
                    ly.setVisibility(View.GONE);
                    if (start != null) {
                        search_list = JSONObject.parseArray(get_music.getStringExtra("list"));
                        musicIndex = Integer.parseInt(start);
                        type = get_music.getStringExtra("type");
                        if(!MainActivity.this.isFinishing()){
                            
                            will_next = true;
                            TextView msg = findViewById(R.id.msg);
                            msg.setText("加载中");
                        }
                    }
                }
                else {
                    try {
                        ol.createNewFile();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this).setTitle("无网络")
                            .setMessage("似乎没有网络哦~是否进入离线模式？").setPositiveButton("开启", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                Intent intent = new Intent(MainActivity.this, LocalMusicActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                startActivity(intent);
                                finish();

                            }).setNegativeButton("取消", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                Intent get_music = getIntent();
                                mvids = JSON.parseArray(get_music.getStringExtra("mvids"));
                                String start = get_music.getStringExtra("start");
                                ScrollView sv_main = findViewById(R.id.sv_main);
                                LinearLayout ly = findViewById(R.id.ly_search);
                                sv_main.setVisibility(View.VISIBLE);
                                ly.setVisibility(View.GONE);
                                File user = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/user.txt");
                                cookie = UserInfoUtil.getUserInfo(this,"cookie");
                                if (!user.exists() || cookie == null) {
                                    Log.d("Main","登陆过期");
                                    Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                                    Intent intent_ = new Intent(MainActivity.this, LoginActivity.class);
                                    intent_.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                    intent_.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                    startActivity(intent_);
                                    finish();
                                } else {
                                    Map maps = null;
                                    try {
                                        maps = new UserApi().checkLogin(cookie);
                                    } catch (InterruptedException ea) {
                                        e.printStackTrace();
                                    }
                                    if (maps.get("code").toString().equals("200")) {
                                        try {
                                            File us = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/user.txt");
                                            us.createNewFile();
                                            FileOutputStream outputStream;
                                            outputStream = new FileOutputStream(us);
                                            Map profile = (Map) JSON.parse(maps.get("profile").toString());
                                            outputStream.write(profile.toString().getBytes());
                                            outputStream.close();
                                        } catch (IOException ea) {
                                            ea.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            relogin();
                                        } catch (Exception ea) {
                                            if (!MainActivity.this.isFinishing()) {
                                                Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                                            }
                                            Intent intentLogin = new Intent(MainActivity.this,LoginActivity.class);
                                            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                            startActivity(intentLogin
                                            );
                                            finish();
                                        }
                                    }
                                }
                                if (start == null) {
                                    //无音乐
                                    try {
                                        //无音乐
                                        cookie = UserInfoUtil.getUserInfo(this,"cookie");
                                        if(cookie == null){
                                            Log.d("Main","登陆过期");
                                            Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                            startActivity(intent);
                                            finish();
                                        }
                                        Map maps = null;
                                        try {
                                            maps = new FMApi().FM(cookie);
                                        } catch (InterruptedException interruptedException) {
                                            interruptedException.printStackTrace();
                                        }
                                        search_list = JSON.parseArray(maps.get("data").toString());
                                        if(search_list.size() == 0){
                                            maps = new FMApi().FM(cookie);
                                            search_list = JSON.parseArray(maps.get("data").toString());
                                            if(search_list.size() == 0){
                                                Log.d("Main","登陆过期");
                                                Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                                startActivity(intent);
                                                finish();
                                            }
                                        }
                                        mvids = new ArrayList();
                                        for (int j = 0;j<search_list.size();j++){
                                            Map temp = (Map)JSON.parse(search_list.get(j).toString());
                                            mvids.add(temp.get("mvid").toString());
                                        }
                                        musicIndex = 0;
                                        type = "3";
                                        will_next = true;
                                        TextView msg = findViewById(R.id.msg);
                                        msg.setText("加载中");
                                        ImageView imageView = findViewById(R.id.btn_open);
                                        imageView.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
                                    } catch (Exception ea) {
                                        if (!MainActivity.this.isFinishing()) {
                                            Toast.makeText(MainActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                                        }
                                        ea.printStackTrace();
                                    }
                                } else {
                                    search_list = JSONObject.parseArray(get_music.getStringExtra("list"));
                                    musicIndex = Integer.parseInt(start);
                                    type = get_music.getStringExtra("type");
                                    will_next = true;
                                    TextView msg = findViewById(R.id.msg);
                                    msg.setText("加载中");
                                }
                            });
                    builder.create().show();
                }
            }
            File ol = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/outline.ini");
            if(!ol.exists()){
                Intent get_music = getIntent();
                mvids = JSON.parseArray(get_music.getStringExtra("mvids"));
                String start = get_music.getStringExtra("start");
                ScrollView sv_main = findViewById(R.id.sv_main);
                LinearLayout ly = findViewById(R.id.ly_search);
                sv_main.setVisibility(View.VISIBLE);
                ly.setVisibility(View.GONE);
                File user = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/user.txt");
                cookie = UserInfoUtil.getUserInfo(this,"cookie");
                if (!user.exists() || cookie == null){
                    Log.d("Main","登录过期");
                    Intent intent_ = new Intent(MainActivity.this, LoginActivity.class);
                    intent_.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                    intent_.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                    startActivity(intent_);
                    finish();
                }
                else{
                    Map maps = null;
                    try {
                        maps = new UserApi().checkLogin(cookie);
                        maps = (Map) maps.get("data");
                    } catch (InterruptedException ea) {
                        ea.printStackTrace();
                    }
                    if(maps.get("code").toString().equals("200")){
                        try {
                            File us = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/user.txt");
                            us.createNewFile();
                            FileOutputStream outputStream;
                            outputStream = new FileOutputStream(us);
                            Map profile = (Map)JSON.parse(maps.get("profile").toString());
                            outputStream.write(profile.toString().getBytes());
                            outputStream.close();
                        } catch (IOException ea) {
                            ea.printStackTrace();
                        }
                    }
                    else{
                        try {
                            relogin();
                        } catch (Exception ea) {
                            if (!MainActivity.this.isFinishing()) {
                                Toast.makeText(MainActivity.this, "登录过期，请重新登录", Toast.LENGTH_SHORT).show();
                            }
                            Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                            startActivity(intent);
                            finish();
                        }
                    }
                }
                if (start == null){
                    //无音乐
                    try {
                        //无音乐
                        cookie = UserInfoUtil.getUserInfo(this,"cookie");
                        if(cookie == null){
                            Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                            startActivity(intent);
                            finish();
                        }
                        Map maps = new FMApi().FM(cookie);
                        search_list = JSON.parseArray(maps.get("data").toString());
                        if(search_list.size() == 0){
                            maps = new FMApi().FM(cookie);
                            search_list = JSON.parseArray(maps.get("data").toString());
                            if(search_list.size() == 0){
                                Log.d("Main","登陆过期");
                                Toast.makeText(MainActivity.this, "登录过期，请重新登陆", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
                                startActivity(intent);
                                finish();
                            }
                        }
                        mvids = new ArrayList();
                        for (int j = 0;j<search_list.size();j++){
                            Map temp = (Map)JSON.parse(search_list.get(j).toString());
                            mvids.add(temp.get("mvid").toString());
                        }
                        musicIndex = 0;
                        type = "3";
                        
                        will_next = true;
                        TextView msg = findViewById(R.id.msg);
                        msg.setText("加载中");
                        ImageView imageView = findViewById(R.id.btn_open);
                        imageView.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
                    } catch (Exception ea) {
                        if(!MainActivity.this.isFinishing()){
                            Toast.makeText(MainActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                        }
                        ea.printStackTrace();
                    }
                }
                else {
                    search_list = JSONObject.parseArray(get_music.getStringExtra("list"));
                    musicIndex = Integer.parseInt(start);
                    type = get_music.getStringExtra("type");
                    if(!MainActivity.this.isFinishing()){
                        
                        will_next = true;
                        TextView msg = findViewById(R.id.msg);
                        msg.setText("加载中");
                    }
                }
            }
        }else {
            Intent intent = new Intent(MainActivity.this, PermissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            startActivity(intent);
            finish();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        HeadSetUtil.getInstance().close(MainActivity.this);
    }
    HeadSetUtil.OnHeadSetListener headSetListener = new HeadSetUtil.OnHeadSetListener() {
        @Override
        public void onDoubleClick() {
            if(mediaPlayer!=null){
                right(null);
            }
        }
        @Override
        public void onClick() {
            if(mediaPlayer!=null){
                c(null);
            }
        }
        @Override
        public void onThreeClick() {
            if(mediaPlayer!=null){
                left(null);
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(will_next){
            nextMusic();
            will_next=false;
        }
    }
    public void relogin() throws Exception {
        String check = UserInfoUtil.getUserInfo(this,"account");
        Map map = new UserApi().Login(this,check, UserInfoUtil.getUserInfo(this,"password"));
        if(map.containsKey("error")){
            //请求失败
            Toast.makeText(MainActivity.this,"登录过期，请重新登录",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            startActivity(intent);
            finish();
        }
        Log.d("relogin",check);
    }

    public void menu(View view){
        File user = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/user.txt");
        File ol = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/outline.ini");
        if(ol.exists()){
            Intent intent = new Intent(MainActivity.this, LocalMusicActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            startActivity(intent);
        }
        else if (!user.exists() || cookie == null){
            Intent intent = new Intent(MainActivity.this,LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            startActivity(intent);
            finish();
        }
        else{
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            startActivity(intent);
        }
    }
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the u
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
    public void init_player(){
        if(mediaPlayer!=null){
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        // 设置设备进入锁状态模式-可在后台播放或者缓冲音乐-CPU一直工作
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        // 如果你使用wifi播放流媒体，你还需要持有wifi锁
        WifiManager.WifiLock wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "wifilock");
        wifiLock.acquire();

        AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // 获取audio focus
                    if (mediaPlayer == null)
                        init_player();
                    else if (!mediaPlayer.isPlaying() && playing)
                        mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // 失去audio focus很长一段时间，必须停止所有的audio播放，清理资源
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // 暂时失去audio focus，但是很快就会重新获得，在此状态应该暂停所有音频播放，但是不能清除资源
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.pause();
                        ImageView imageView = findViewById(R.id.btn_open);
                        imageView.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // 暂时失去 audio focus，但是允许持续播放音频(以很小的声音)，不需要完全停止播放。
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.setVolume(0.1f, 0.1f);
                    break;
            }
        };
        // 处理音频焦点-处理多个程序会来竞争音频输出设备
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 征对于Android 8.0+
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setOnAudioFocusChangeListener(focusChangeListener).build();
            audioFocusRequest.acceptsDelayedFocusGain();
            audioManager.requestAudioFocus(audioFocusRequest);
        }
        
    }
    public void playList(View view){
        Intent intent = new Intent(MainActivity.this, PlayListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
        intent.putExtra("mvids",JSON.toJSONString(mvids));
        intent.putExtra("list", JSON.toJSONString(search_list));
        intent.putExtra("type",type);
        intent.putExtra("musicIndex",String.valueOf(musicIndex));
        startActivity(intent);
    }
    public static MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    String tmp_name;
    public void nextMusic(){
        prepareDone = false;
        zt = 1;
        MainActivity.this.runOnUiThread(()->{
            ScrollView sv_main = findViewById(R.id.sv_main);
            LinearLayout ly = findViewById(R.id.ly_search);
            sv_main.setVisibility(View.VISIBLE);
            ly.setVisibility(View.GONE);
        });
        //有音乐
        try {
            if (mediaPlayer == null) init_player();
            mediaPlayer.reset();
            //开始播放
            String text;
            if(type.equals("0") || type.equals("3")){

                try{
                    String temp = ((search_list.get(musicIndex)).toString());
                    temp_ni = (Map) JSON.parse(temp);
                    id = temp_ni.get("id").toString();
                } catch (Exception e) {
                    musicIndex = 0;
                    String temp = ((search_list.get(musicIndex)).toString());
                    temp_ni = (Map) JSON.parse(temp);
                    id = temp_ni.get("id").toString();
                }
                Map maps_yz = new MusicApi().checkMusic(cookie,id);
                if(maps_yz == null){
                    maps_yz = new MusicApi().checkMusic(cookie,id);
                }
                if(maps_yz == null){
                    MainActivity.this.runOnUiThread(()->{
                        TextView textView = findViewById(R.id.msg);
                        textView.setText("播放出错（请求失败）");
                        ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_error_24);
                    });
                }
                else if(maps_yz.get("success").toString().equals("true")){
                    Map maps = new MusicApi().getMusicUrl(cookie,id);
                    if (maps.get("code").toString().equals("200")){
                        System.out.println(maps);
                        Map data = (Map)JSON.parse(maps.get("data").toString().replace("[","").replace("]",""));
                        if(data.get("url").toString().equals("null")){
                            MainActivity.this.runOnUiThread(()->{
                                TextView textView = findViewById(R.id.msg);
                                textView.setText("播放出错（链接无效）");
                                ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_error_24);});
                        }
                        else{
                            url = data.get("url").toString();
                            Thread thread = new Thread(()->{
                                Log.d("MediaPlayer","开始准备音乐");
                                try {
                                    mediaPlayer.setDataSource(url);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try{
                                    mediaPlayer.prepare();
                                    prepareDone = true;
                                    Log.d("MediaPlayer","音乐准备成功");
                                    if(!type.equals("3")){
                                        mediaPlayer.start();
                                        playing = true;
                                    }
                                    else{
                                        playing = false;
                                    }
                                    MainActivity.this.runOnUiThread(()->Toast.makeText(MainActivity.this,"点击音乐名查看歌词",Toast.LENGTH_SHORT).show());
                                }
                                catch(Exception e){
                                    Log.d("MediaPlayer","音乐准备失败");
                                    MainActivity.this.runOnUiThread(()->Toast.makeText(MainActivity.this,"音乐准备失败",Toast.LENGTH_SHORT).show());
                                    playing = false;
                                }
                            });
                            thread.start();

                            if(type.equals("0")){
                                tmp_name = "<font color='#2A2B2C'>" + temp_ni.get("name").toString() +  "</font> - " + "<font color='#999999'>" + temp_ni.get("artists").toString() + "</font>";
                                RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(20)).placeholder(R.drawable.ic_baseline_photo_size_select_actual_24).error(R.drawable.ic_baseline_photo_size_select_actual_24);
                                if(temp_ni.containsKey("picUrl")){
                                    coverUrl = temp_ni.get("picUrl").toString();
                                }
                                else{
                                    System.out.println(temp_ni);
                                    coverUrl = new MusicApi().getMusicCover(String.valueOf(temp_ni.get("albumId")));
                                }
                                MainActivity.this.runOnUiThread(()->Glide.with(getApplicationContext()).load(coverUrl).apply(options).into((ImageView) findViewById(R.id.imageView11)));
                            }
                            else{
                                MainActivity.this.runOnUiThread(()->{
                                    List temp_1 = JSON.parseArray(temp_ni.get("artists").toString());
                                    Map temp_2 = (Map)JSON.parse(temp_1.get(0).toString());
                                    Map temp_3 = (Map)JSON.parse(temp_ni.get("album").toString());
                                    tmp_name = "<font color='#2A2B2C'>" + temp_ni.get("name").toString() +  "</font> - " + "<font color='#999999'>" + temp_2.get("name") + "</font>";
                                    RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(20)).placeholder(R.drawable.ic_baseline_photo_size_select_actual_24).error(R.drawable.ic_baseline_photo_size_select_actual_24);
                                    try {
                                        coverUrl = new MusicApi().getMusicCover(String.valueOf(temp_3.get("id")));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    Glide.with(getApplicationContext()).load(coverUrl)
                                            .apply(options)
                                            .into((ImageView) findViewById(R.id.imageView11));});
                            }
                            MainActivity.this.runOnUiThread(()->{
                                TextView textView = findViewById(R.id.msg);
                                textView.setText(Html.fromHtml(tmp_name));
                            });
                        }
                    }
                    else{
                        //播放出错
                        MainActivity.this.runOnUiThread(()->{
                            TextView textView = findViewById(R.id.msg);
                            textView.setText("播放出错（请求失败）");});
                        ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_error_24);
                    }
                }
                else{
                    MainActivity.this.runOnUiThread(()->Toast.makeText(MainActivity.this,"该音乐暂无版权",Toast.LENGTH_SHORT).show());
                    musicIndex += 1;
                    nextMusic();
                }

            }
            else{
                try{
                    Thread thread = new Thread(()->{
                        Log.d("MediaPlayer","开始准备音乐");
                        try {
                            mediaPlayer.setDataSource(search_list.get(musicIndex).toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try{
                            mediaPlayer.prepare();
                            prepareDone = true;
                            Log.d("MediaPlayer","音乐准备成功");
                            mediaPlayer.start();
                            playing = true;
                            MainActivity.this.runOnUiThread(()->Toast.makeText(MainActivity.this,"点击音乐名查看歌词",Toast.LENGTH_SHORT).show());
                        }
                        catch(Exception e){
                            MainActivity.this.runOnUiThread(() ->Toast.makeText(MainActivity.this,"音乐加载失败",Toast.LENGTH_SHORT).show());
                            playing = false;
                        }
                        Log.d("MediaPlayer","音乐准备完成");
                    });
                    thread.start();
                    MainActivity.this.runOnUiThread(()->{
                        TextView textView = findViewById(R.id.msg);
                                String temp = (search_list.get(musicIndex).toString().replace("/storage/emulated/0/Android/data/cn.wearbbs.music/download/music/","")).replace(".mp3","");
                        textView.setText(Html.fromHtml(temp));
                        File file = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/download/cover/" + temp + ".jpg");
                        RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(20)).placeholder(R.drawable.ic_baseline_photo_size_select_actual_24).error(R.drawable.ic_baseline_photo_size_select_actual_24);
                        Glide.with(getApplicationContext()).load(file)
                                .apply(options)
                                .into((ImageView) findViewById(R.id.imageView11));
                    });
                } catch (Exception e) {
                    musicIndex = 0;
                    Thread thread = new Thread(()->{
                        Log.d("MediaPlayer","开始准备音乐");
                        try {
                            mediaPlayer.setDataSource(search_list.get(musicIndex).toString());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        try{
                            mediaPlayer.prepare();
                            prepareDone = true;
                            Log.d("MediaPlayer","音乐准备完成");
                            mediaPlayer.start();
                            playing = true;
                            MainActivity.this.runOnUiThread(()->Toast.makeText(MainActivity.this,"点击音乐名查看歌词",Toast.LENGTH_SHORT).show());
                        }
                        catch (Exception es){
                            MainActivity.this.runOnUiThread(() ->Toast.makeText(MainActivity.this,"音乐加载失败",Toast.LENGTH_SHORT).show());
                            playing = false;
                        }

                    });
                    thread.start();
                    TextView textView = findViewById(R.id.msg);
                    String temp = (search_list.get(musicIndex).toString().replace("/storage/emulated/0/Android/data/cn.wearbbs.music/download/music/","")).replace(".mp3","");
                    textView.setText(Html.fromHtml(temp));
                    File file = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/download/cover/" + temp + ".jpg");
                    RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(20)).placeholder(R.drawable.ic_baseline_photo_size_select_actual_24).error(R.drawable.ic_baseline_photo_size_select_actual_24);
                    Glide.with(getApplicationContext()).load(file)
                            .apply(options)
                            .into((ImageView) findViewById(R.id.imageView11));
                }
            }
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    //间隔时间
                    handler.sendEmptyMessageDelayed(1, 1000);
                }
            }.start();
        } catch (Exception e) {
            //播放出错
            MainActivity.this.runOnUiThread(()->{
                TextView textView = findViewById(R.id.msg);
                textView.setText("播放出错（" + e + "）");
                ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_error_24);
            });
            e.printStackTrace();
        }
    }
    @SuppressLint("HandlerLeak")
    Handler handler=new Handler(){
        public void handleMessage(android.os.Message msg) {
            ProgressBar pb_main = findViewById(R.id.pb_main);
            ProgressBar pb_lyrics = findViewById(R.id.pb_lyrics);
            if(prepareDone){
                if(pb_main.isIndeterminate()) pb_main.setIndeterminate(false);
                if(pb_lyrics.isIndeterminate()) pb_lyrics.setIndeterminate(false);
                if(playing){
                    pb_main.setMax(mediaPlayer.getDuration());
                    pb_main.setProgress(mediaPlayer.getCurrentPosition());
                    pb_lyrics.setMax(mediaPlayer.getDuration());
                    pb_lyrics.setProgress(mediaPlayer.getCurrentPosition());
                    if(mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()){
                        pb_main.setProgress(0);
                        pb_lyrics.setProgress(0);
                        right(null);
                    }
                }
                // 防止控件与 MediaPlayer 不同步
                ImageView imageViewBtn = findViewById(R.id.btn_open);
                if(mediaPlayer.isPlaying()) imageViewBtn.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24);
                else imageViewBtn.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
            }
            else{
                if(!pb_main.isIndeterminate()) pb_main.setIndeterminate(true);
                if(!pb_lyrics.isIndeterminate()) pb_lyrics.setIndeterminate(true);
            }
            //调取子线程
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };
    public void c(View view){
        if(prepareDone){
            if(playing){
                pause();
            }
            else{
                start();
            }
        }
        else{
            if(view!=null) Toast.makeText(MainActivity.this,"音乐准备中",Toast.LENGTH_SHORT).show();
        }
    }
    public void pause(){
        mediaPlayer.pause();
        playing = false;
        MainActivity.this.runOnUiThread(()->((ImageView)findViewById(R.id.btn_open)).setImageResource(R.drawable.ic_baseline_play_circle_filled_24));
    }
    public void start(){
        mediaPlayer.start();
        playing = true;
        MainActivity.this.runOnUiThread(()->((ImageView)findViewById(R.id.btn_open)).setImageResource(R.drawable.ic_baseline_pause_circle_filled_24));
    }
    public void right(View view){
        mediaPlayer.reset();
        if(mediaPlayer.isPlaying()) pause();
        musicIndex += 1;
        ((TextView)findViewById(R.id.msg)).setText("加载中");
        ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_photo_size_select_actual_24);
        Thread thread = new Thread(()-> {
            nextMusic();
            start();
            MainActivity.this.runOnUiThread(()->((ImageView)findViewById(R.id.btn_open)).setImageResource(R.drawable.ic_baseline_pause_circle_filled_24));
        });
        thread.start();
    }
    public void left(View view){
        mediaPlayer.reset();
        
        if(mediaPlayer.isPlaying()) pause();
        musicIndex -= 1;
        ((TextView)findViewById(R.id.msg)).setText("加载中");
        ((ImageView)findViewById(R.id.imageView11)).setImageResource(R.drawable.ic_baseline_photo_size_select_actual_24);
        Thread thread = new Thread(()-> {
            nextMusic();
            start();
            MainActivity.this.runOnUiThread(()->((ImageView)findViewById(R.id.btn_open)).setImageResource(R.drawable.ic_baseline_pause_circle_filled_24));
        });
        thread.start();
    }
    /**
     * 取两个文本之间的文本值
     * @param text 源文本 比如：欲取全文本为 12345
     * @param left 文本前面
     * @param right  后面文本
     * @return 返回 String
     */
    public static String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }
    public void console(View view){
        Intent intent = new Intent(MainActivity.this, ConsoleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
        intent.putExtra("type",type);
        if (type.equals("0")){
            String temp = ((search_list.get(musicIndex)).toString());
            Map temp_ni = (Map) JSON.parse(temp);
            intent.putExtra("id",temp_ni.get("id").toString());
            intent.putExtra("name",temp_ni.get("name").toString());
            intent.putExtra("artists",temp_ni.get("artists").toString());
            intent.putExtra("song",temp_ni.get("name").toString() + " - " + temp_ni.get("artists").toString());
            intent.putExtra("url",url);
            if(temp_ni.get("comment")!=null) intent.putExtra("comment",temp_ni.get("comment").toString());
            intent.putExtra("coverUrl",coverUrl);
            try {
                intent.putExtra("mvid", mvids.get(musicIndex).toString());
            }
            catch (Exception e){
                intent.putExtra("mvid","");
            }
        }
        else if(type.equals("3")){
            String temp = ((search_list.get(musicIndex)).toString());
            Map temp_ni = (Map) JSON.parse(temp);
            List temp_1 = JSON.parseArray(temp_ni.get("artists").toString());
            Map temp_2 = (Map)JSON.parse(temp_1.get(0).toString());
            intent.putExtra("id",temp_ni.get("id").toString());
            intent.putExtra("mvid",temp_ni.get("mvid").toString());
            intent.putExtra("name",temp_ni.get("name").toString());
            intent.putExtra("artists",temp_2.get("name").toString());
            intent.putExtra("song",temp_ni.get("name").toString() + " - " + temp_2.get("name").toString());
            intent.putExtra("url",url);
            intent.putExtra("coverUrl",coverUrl);
        }
        startActivity(intent);
    }
    public void init_lyrics() throws Exception {
        zt = 0;
        ScrollView sv_main = findViewById(R.id.sv_main);
        LinearLayout ly_main = findViewById(R.id.ly_search);
        sv_main.setVisibility(View.GONE);
        ly_main.setVisibility(View.VISIBLE);
        if(!type.equals("1")){
            Map maps = new MusicApi().getMusicLrc(cookie,id);
            System.out.println(maps);
            try{
                lrc_map = (Map) JSON.parse(maps.get("lrc").toString());
                File dir = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/temp/");
                dir.mkdirs();
                lrcFile = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/temp/temp.lrc");
                lrcFile.createNewFile();
                FileOutputStream outputStream;
                outputStream = new FileOutputStream(lrcFile);
                outputStream.write(lrc_map.get("lyric").toString().getBytes());
                outputStream.close();
            } catch (Exception e) {
                File dir = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/temp/");
                dir.mkdirs();
                lrcFile = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/temp/temp.lrc");
                lrcFile.createNewFile();
                FileOutputStream outputStream;
                outputStream = new FileOutputStream(lrcFile);
                outputStream.write("[00:00.00]无歌词".getBytes());
                outputStream.close();
                e.printStackTrace();
            }
        }
        else{
            String temp = ((search_list.get(musicIndex)).toString());
            lrcFile = new File((temp.replace("/music/","/lrc/")).replace(".mp3",".lrc"));
        }
        lrcView = findViewById(R.id.lv_main);
        lrcView.loadLrc(lrcFile);
        new Thread(()->{
            while (true){
                lrcView.updateTime(mediaPlayer.getCurrentPosition());
            }
        }).start();
        lrcView.setDraggable(true, (view, time) -> {
            try{
                mediaPlayer.seekTo((int) time);
                ProgressBar pb_main = findViewById(R.id.pb_main);
                ProgressBar pb_lyrics = findViewById(R.id.pb_lyrics);
                pb_main.setMax(mediaPlayer.getDuration());
                pb_main.setProgress(mediaPlayer.getCurrentPosition());
                pb_lyrics.setMax(mediaPlayer.getDuration());
                pb_lyrics.setProgress(mediaPlayer.getCurrentPosition());
            }
            catch (Exception ignored){
            }
            return true;
        } );
    }
    public void lyr(View view) throws Exception {
        init_lyrics();
    }
    public void t(View view){
        ScrollView sv_main = findViewById(R.id.sv_main);
        LinearLayout ly1 = findViewById(R.id.ly_search);
        sv_main.setVisibility(View.VISIBLE);
        ly1.setVisibility(View.GONE);
    }
    Map temp_ni;
    public void share_ly(View view) {
        String lrcResult = "";
        try {
            File lrc = new File("/storage/emulated/0/Android/data/cn.wearbbs.music/temp/temp.lrc");
            BufferedReader in = new BufferedReader(new FileReader(lrc));
            lrcResult = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!lrcResult.equals("[00:00.00]无歌词")){
            Intent intent = new Intent(MainActivity.this, ChooseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
            intent.putExtra("type", type);
            if (type.equals("3")){
                String temp = ((search_list.get(musicIndex)).toString());
                temp_ni = (Map) JSON.parse(temp);
                List temp_1 = JSON.parseArray(temp_ni.get("artists").toString());
                Map temp_2 = (Map)JSON.parse(temp_1.get(0).toString());
                intent.putExtra("song",temp_ni.get("name").toString() + " - " + temp_2.get("name").toString());
                intent.putExtra("pic",coverUrl);
            }
            if(type.equals("1")){
                String tmp = search_list.get(musicIndex).toString().replace("/storage/emulated/0/Android/data/cn.wearbbs.music/download/music/","");
                intent.putExtra("song",tmp);
                intent.putExtra("pic",coverUrl);
            }
            if(type.equals("0")){
                String temp = ((search_list.get(musicIndex)).toString());
                Map temp_ni = (Map) JSON.parse(temp);
                intent.putExtra("song",temp_ni.get("name").toString() + " - " + temp_ni.get("artists").toString());
                intent.putExtra("pic",coverUrl);
            }
            startActivity(intent);
        }
        else{
            Toast.makeText(MainActivity.this,"该音乐没有歌词",Toast.LENGTH_SHORT).show();
        }
    }
    public void onImgClick(View view){
        Intent intent = new Intent(MainActivity.this, PicActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//防止重复
        intent.putExtra("url",coverUrl);
        startActivity(intent);
    }
}