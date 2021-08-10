package cn.wearbbs.music.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

import api.MusicApi;
import cn.wearbbs.music.R;
import cn.wearbbs.music.ui.MainActivity;
import cn.wearbbs.music.util.SharedPreferencesUtil;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    private final JSONArray data;
    private final Activity activity;
    private View header = null;

    public static final int ITEM_TYPE_HEADER = 0;
    public static final int ITEM_TYPE_CONTENT = 1;
    public MusicAdapter(JSONArray data, Activity activity) {
        this.data = data;
        this.activity = activity;
    }

    public MusicAdapter(JSONArray data, Activity activity, View header) {
        this.data = data;
        this.activity = activity;
        this.header = header;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==ITEM_TYPE_HEADER){
            return new ViewHolder(header);
        }
        else{
            return new ViewHolder(LayoutInflater.from(activity).inflate(R.layout.item_music, parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position==0&&header!=null){
            return ITEM_TYPE_HEADER;
        }
        else{
            return ITEM_TYPE_CONTENT;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        if(getItemViewType(position)==ITEM_TYPE_CONTENT){
            if(header!=null){
                position--;
            }
            JSONObject musicInfo = data.getJSONObject(position);
            JSONArray artists;
            final String[] imgUrl = {""};
            // 兼容音乐云盘
            if (musicInfo.containsKey("simpleSong")) {
                musicInfo = musicInfo.getJSONObject("simpleSong");
            }
            if (musicInfo.containsKey("artists")) {
                artists = musicInfo.getJSONArray("artists");
                MusicApi api;
                api = new MusicApi(SharedPreferencesUtil.getString("cookie", "", activity));
                JSONObject finalMusicInfo = musicInfo;
                new Thread(() -> {
                    try{
                        imgUrl[0] = api.getMusicCover(finalMusicInfo.getJSONObject("album").getString("id"));
                    }
                    catch(Exception ignored){}
                }).start();
            } else {
                artists = musicInfo.getJSONArray("ar");
                imgUrl[0] = musicInfo.getJSONObject("al").getString("picUrl");
            }
            viewHolder.tv_title.setText(musicInfo.getString("name"));
            String artistName = artists.getJSONObject(0).getString("name");
            viewHolder.tv_artists.setText(artistName == null ? activity.getString(R.string.unknown) : artistName);
            viewHolder.iv_cover.setImageResource(R.drawable.ic_baseline_photo_size_select_actual_24);
            RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(10)).placeholder(R.drawable.ic_baseline_photo_size_select_actual_24).error(R.drawable.ic_baseline_photo_size_select_actual_24);
            String finalImgUrl = imgUrl[0];
            viewHolder.iv_cover.setImageResource(R.drawable.ic_baseline_photo_size_select_actual_24);
            try{
                Glide.with(activity).load(finalImgUrl.replace("http://", "https://")).apply(options).into(viewHolder.iv_cover);
            }
            catch (Exception ignored){}
            int finalPosition = position;
            viewHolder.ll_main.setOnClickListener(v -> {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("data", data.toJSONString());
                intent.putExtra("musicIndex", finalPosition);
                activity.startActivity(intent);
                activity.finish();
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_cover;
        TextView tv_title;
        TextView tv_artists;
        LinearLayout ll_main;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_cover = itemView.findViewById(R.id.iv_cover);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_artists = itemView.findViewById(R.id.tv_artists);
            ll_main = itemView.findViewById(R.id.ll_main);
        }
    }
}