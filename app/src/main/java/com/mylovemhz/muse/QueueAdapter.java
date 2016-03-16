package com.mylovemhz.muse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mylovemhz.simplay.Track;

import java.util.List;

/**
 * Created by allanpichardo on 3/16/16.
 */
public class QueueAdapter extends BaseAdapter{

    private List<Track> tracks;

    public QueueAdapter(List<Track> tracks){
        this.tracks = tracks;
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public Object getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext())
                                        .inflate(android.R.layout.simple_list_item_1,parent,false);
        }
        Track track = (Track)getItem(position);
        TextView textView = (TextView)convertView.findViewById(android.R.id.text1);
        String text = track.getArtist() + " - " + track.getTitle();
        textView.setText(text);
        return convertView;
    }
}
