package com.mylovemhz.simplay;

import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;


public class SimpleSessionCallback extends MediaSessionCompat.Callback {

    public static final String TAG_DEBUG = SimpleSessionCallback.class.getName();

    private MusicService musicService;

    public SimpleSessionCallback(MusicService musicService){
        this.musicService = musicService;
    }

    @Override
    public void onFastForward() {
        Log.d(TAG_DEBUG,"fast forward");
        musicService.fastForward();
    }

    @Override
    public void onPause() {
        Log.d(TAG_DEBUG, "pause");
        musicService.pause();
    }

    @Override
    public void onPlay() {
        Log.d(TAG_DEBUG, "play");
        musicService.play();
    }

    @Override
    public void onRewind() {
        Log.d(TAG_DEBUG, "rewind");
        musicService.rewind();
    }

    @Override
    public void onSeekTo(long pos) {
        Log.d(TAG_DEBUG, "seek to: " + pos);

    }

    @Override
    public void onSkipToNext() {
        Log.d(TAG_DEBUG, "skip to next");
        musicService.next();
    }

    @Override
    public void onSkipToPrevious() {
        Log.d(TAG_DEBUG, "skip to previous");
        musicService.previous();
    }

    @Override
    public void onStop() {
        Log.d(TAG_DEBUG, "stop");
        musicService.stop();
    }
}
