package com.discordclone.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.discordclone.utils.GifService;

import java.util.List;

public class GifGridAdapter extends BaseAdapter {
    private List<GifService.GifResult> gifs;

    public void setGifs(List<GifService.GifResult> gifs) {
        this.gifs = gifs;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return gifs != null ? gifs.size() : 0;
    }

    @Override
    public GifService.GifResult getItem(int position) {
        return gifs != null ? gifs.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(200, 150));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(2, 2, 2, 2);
            convertView = iv;
        }

        ImageView imageView = (ImageView) convertView;
        GifService.GifResult gif = getItem(position);
        if (gif != null) {
            Glide.with(parent.getContext())
                .asGif()
                .load(gif.previewUrl)
                .into(imageView);
        }

        return convertView;
    }
}
