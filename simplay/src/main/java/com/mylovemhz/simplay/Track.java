package com.mylovemhz.simplay;


import android.graphics.Bitmap;

public interface Track {
    int getId();
    String getArtist();
    String getTitle();
    String getUrl();
    String getArtworkUrl();
    Bitmap getArtwork();
}
