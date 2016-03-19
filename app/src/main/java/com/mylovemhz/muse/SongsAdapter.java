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

import java.util.ArrayList;
import java.util.List;

public class SongsAdapter extends BaseAdapter{

    private List<Track> trackList;
    private Callback callback;

    public SongsAdapter(){
        trackList = new ArrayList<>();
        populateTracks();
    }

    private void populateTracks(){
        trackList.add(new Track() {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public String getArtist() {
                return "Stark & Nimo";
            }

            @Override
            public String getTitle() {
                return "Fading Sonically";
            }

            @Override
            public String getUrl() {
                return "https://archive.org/download/NotDarkAndEmo/03FadingSonically.mp3";
            }

            @Override
            public String getArtworkUrl() {
                return "https://archive.org/download/NotDarkAndEmo/NotDarkAndEmo.jpg";
            }

        });
        trackList.add(new Track() {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public String getArtist() {
                return "Stark & Nimo";
            }

            @Override
            public String getTitle() {
                return "Stumbling";
            }

            @Override
            public String getUrl() {
                return "https://archive.org/download/NotDarkAndEmo/05Stumbling.mp3";
            }

            @Override
            public String getArtworkUrl() {
                return "https://archive.org/download/NotDarkAndEmo/NotDarkAndEmo.jpg";
            }

        });
        trackList.add(new Track() {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public String getArtist() {
                return "Stark & Nimo";
            }

            @Override
            public String getTitle() {
                return "Morgan Stop";
            }

            @Override
            public String getUrl() {
                return "https://archive.org/download/NotDarkAndEmo/02MorganStop.mp3";
            }

            @Override
            public String getArtworkUrl() {
                return "https://archive.org/download/NotDarkAndEmo/NotDarkAndEmo.jpg";
            }

        });
    }

    @Override
    public int getCount() {
        return trackList.size();
    }

    @Override
    public Object getItem(int position) {
        return trackList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext())
                                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        final Track track = trackList.get(position);
        TextView textView = (TextView)convertView.findViewById(android.R.id.text1);
        String text = track.getArtist() + " - " + track.getTitle();
        textView.setText(text);
        if(callback != null){
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onTrackSelected(track);
                }
            });
        }
        return convertView;
    }

    public void setCallback(Callback callback){
        this.callback = callback;
    }

    public interface Callback{
        void onTrackSelected(Track track);
    }
}
