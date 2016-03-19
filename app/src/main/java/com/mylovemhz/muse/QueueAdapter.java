/**
Copyright 2016 Allan Pichardo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.mylovemhz.muse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mylovemhz.simplay.Track;

import java.util.List;


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
