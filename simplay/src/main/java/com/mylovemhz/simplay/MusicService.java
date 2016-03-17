package com.mylovemhz.simplay;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    public static final String TAG_MUSIC_SERVICE = "Music Service";
    public static final int ID_NOTIFICATION = 1;
    public static final int TIME_FFWD_RWD = 10 * 1000;

    public static final String RATIONALE_WAKE_LOCK =
            "The music player needs to keep your phone from going into sleep mode " +
                    "to prevent interrupting the music.";

    public static final int REQUEST_PERMISSION_INTERNET = 0;
    public static final int REQUEST_PERMISSION_WAKE_LOCK = 1;
    public static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 2;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    public enum State {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED,
        STOPPED, PAUSED, COMPLETED, END, ERROR
    }

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private State currentState;
    private WifiManager.WifiLock wifiLock;
    private Callbacks callbacks;

    private ArrayList<Track> trackQueue;
    private int currentTrackIndex = 0;
    private boolean isInitialized = false;
    private int smallIcon;
    private PendingIntent contentIntent;

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    private void initialize() {
        trackQueue = new ArrayList<>();


        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "music lock");


        ComponentName receiver = new ComponentName(getPackageName(), MediaButtonEventReceiver.class.getName());
        mediaSession = new MediaSessionCompat(this, TAG_MUSIC_SERVICE, receiver, null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new SimpleSessionCallback(this));

        currentState = State.IDLE;
        mediaSession.setActive(true);

        isInitialized = true;
        Log.d(TAG_MUSIC_SERVICE, "Initialized...");
    }

    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setContentIntent(PendingIntent contentIntent) {
        this.contentIntent = contentIntent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isInitialized) {
            initialize();
        }
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {

        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            pause();
        } else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
            fastForward();
        } else if (action.equalsIgnoreCase(ACTION_REWIND)) {
            rewind();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            previous();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            next();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            stop();
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG_MUSIC_SERVICE, "prepared");
        currentState = State.PREPARED;
        updateSessionState(PlaybackStateCompat.STATE_BUFFERING);
        showNotification();
        play();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (!mp.isLooping()) {
            currentState = State.COMPLETED;
            if(hasNext()) {
                next();
            }else{
                stop();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        currentState = State.ERROR;
        mp.reset();
        currentState = State.IDLE;
        if(callbacks != null) callbacks.onError();
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG_MUSIC_SERVICE, "onDestroy()");
        stop();
        mediaPlayer.release();
        mediaPlayer = null;
        currentState = State.END;
        mediaSession.release();
    }

    public List<Track> getTracks() {
        return trackQueue;
    }

    public void addTrack(Track track) throws Exception {
        trackQueue.add(track);
        try {
            cueTrack();
        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    public void addTrack(List<Track> tracks) {
        trackQueue.addAll(tracks);
    }

    public void clearTrackQueue() {
        trackQueue.clear();
    }

    private void cueTrack() throws IOException, IndexOutOfBoundsException {
        Log.d(TAG_MUSIC_SERVICE, "Cue Track...");

        AudioManager audioManager = (AudioManager)
                getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_GAIN) {
            return; //Failed to gain audio focus
        }

        if (getCurrentState() == State.STOPPED) {
            mediaPlayer.reset();
            currentState = State.IDLE;

        }
        if (getCurrentState() == State.IDLE) {
            if (trackQueue.size() == 0) {
                return; //nothing to play
            }
            if (hasPermission(Manifest.permission.WAKE_LOCK)) {
                if (!wifiLock.isHeld()) {
                    wifiLock.acquire();
                }
                Track track = trackQueue.get(currentTrackIndex);
                mediaPlayer.setDataSource(track.getUrl());
                currentState = State.INITIALIZED;
                mediaPlayer.prepareAsync();
                currentState = State.PREPARING;
                if(callbacks != null) callbacks.onLoading();
            } else {
                Log.e(TAG_MUSIC_SERVICE, "need permission " + Manifest.permission.WAKE_LOCK);
                if (callbacks != null) {
                    callbacks.onPermissionRequired(
                            REQUEST_PERMISSION_WAKE_LOCK,
                            Manifest.permission.WAKE_LOCK,
                            RATIONALE_WAKE_LOCK);
                }
            }
        }
    }

    private void updateSessionState(int state) {
        int position = 0;
        if (currentState == State.STARTED ||
                currentState == State.PAUSED) {
            position = mediaPlayer.getCurrentPosition();
        }
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        builder.setState(state, position, 1f);
        mediaSession.setPlaybackState(builder.build());
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                stopSelf();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                lowerVolume();
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            case AudioManager.AUDIOFOCUS_GAIN:
                restoreVolume();
                break;
        }
    }

    private void lowerVolume() {
        mediaPlayer.setVolume(0.5f, 0.5f);
    }

    private void restoreVolume() {
        mediaPlayer.setVolume(1f, 1f);
    }

    public void play() {
        Log.d(TAG_MUSIC_SERVICE, "play()");
        if (currentState == State.PREPARED || currentState == State.PAUSED) {
            mediaPlayer.start();
            currentState = State.STARTED;
            updateSessionState(PlaybackStateCompat.STATE_PLAYING);
            showNotification();
            if(callbacks != null) callbacks.onPlaybackStarted();
        }
        if (currentState == State.STOPPED || currentState == State.COMPLETED) {
            try {
                cueTrack();
            } catch (IOException e) {
                stop();
            }
        }
    }

    public void pause() {
        if (currentState == State.STARTED) {
            mediaPlayer.pause();
            currentState = State.PAUSED;
            updateSessionState(PlaybackStateCompat.STATE_PAUSED);
            showNotification();
        }
    }

    public void stop() {
        Log.d(TAG_MUSIC_SERVICE, "stop()");
        if (currentState == State.STARTED || currentState == State.PAUSED ||
                currentState == State.PREPARED || currentState == State.COMPLETED) {
            mediaPlayer.stop();
            currentState = State.STOPPED;
            updateSessionState(PlaybackStateCompat.STATE_STOPPED);
            currentTrackIndex = 0;
            trackQueue = new ArrayList<>();
            removeNotification();
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
            if(callbacks != null)callbacks.onPlaybackStopped();
        }
    }

    public void next() {
        if (hasNext()) {
            if (currentState == State.STARTED || currentState == State.PAUSED) {
                mediaPlayer.stop();
                currentState = State.STOPPED;
                updateSessionState(PlaybackStateCompat.STATE_STOPPED);
            }
            if (currentState == State.STOPPED || currentState == State.COMPLETED) {
                mediaPlayer.reset();
                currentState = State.IDLE;
                updateSessionState(PlaybackStateCompat.STATE_STOPPED);
                showNotification();
            }
            if (currentState == State.IDLE) {
                try {
                    updateSessionState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
                    currentTrackIndex++;
                    cueTrack();
                } catch (Exception e) {
                    stop();
                }
            }
        }
    }

    public void previous() {
        if (hasPrevious()) {
            if (currentState == State.STARTED || currentState == State.PAUSED) {
                mediaPlayer.stop();
                currentState = State.STOPPED;
                updateSessionState(PlaybackStateCompat.STATE_STOPPED);
            }
            if (currentState == State.STOPPED) {
                mediaPlayer.reset();
                currentState = State.IDLE;
                updateSessionState(PlaybackStateCompat.STATE_STOPPED);
                showNotification();
            }
            if (currentState == State.IDLE) {
                try {
                    currentTrackIndex--;
                    updateSessionState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
                    cueTrack();
                } catch (Exception e) {
                    stop();
                }
            }
        }
    }

    public void fastForward() {
        if (currentState == State.STARTED || currentState == State.PAUSED ||
                currentState == State.PREPARED || currentState == State.COMPLETED) {
            if (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration() + TIME_FFWD_RWD) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + TIME_FFWD_RWD);
                updateSessionState(PlaybackStateCompat.STATE_FAST_FORWARDING);
            }
        }
    }

    public void seekTo(int position){
        if (currentState == State.STARTED || currentState == State.PAUSED ||
                currentState == State.PREPARED || currentState == State.COMPLETED) {
                mediaPlayer.seekTo(position);
                updateSessionState(PlaybackStateCompat.STATE_FAST_FORWARDING);
        }
    }

    public void rewind() {
        if (currentState == State.STARTED || currentState == State.PAUSED ||
                currentState == State.PREPARED || currentState == State.COMPLETED) {
            if (mediaPlayer.getCurrentPosition() > TIME_FFWD_RWD) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - TIME_FFWD_RWD);
                updateSessionState(PlaybackStateCompat.STATE_REWINDING);
            }
        }
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        return mediaSession.getSessionToken();
    }

    public Track getCurrentTrack() throws IllegalStateException {
        try {
            return trackQueue.get(currentTrackIndex);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("No tracks have been added");
        }
    }

    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), MusicService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    private void showNotification() {
        updateMetadata();

        Track track;
        try {
            track = getCurrentTrack();
        } catch (IllegalStateException e) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), MusicService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent cancelIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle(builder);
        style.setMediaSession(getMediaSessionToken());
        style.setShowCancelButton(true);
        style.setCancelButtonIntent(cancelIntent);
        style.setShowActionsInCompactView(1);

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS)); //0
        if (!isPlaying()) {
            builder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY)); // 1
        } else {
            builder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)); //1
        }
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT)); //2

        builder
                .setSmallIcon(smallIcon)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setDeleteIntent(cancelIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(style);
        if (!track.getArtworkUrl().isEmpty()) {
            try {
                Target artTarget = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        builder.setLargeIcon(bitmap);
                        publishNotification(builder);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        publishNotification(builder);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };
                Picasso.with(this)
                        .load(track.getArtworkUrl())
                        .into(artTarget);
            } catch (IllegalArgumentException e) {
                //no artwork. Ignore.
                publishNotification(builder);
            }
        }else{
            publishNotification(builder);
        }
    }

    private void publishNotification(NotificationCompat.Builder builder) {
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        } else {
            Log.e(TAG_MUSIC_SERVICE, "Did you forget to setContentIntent()? Nothing will happen " +
                    "when you touch the notification.");
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        try {
            notificationManager.notify(ID_NOTIFICATION, builder.build());
        } catch (IllegalStateException e) {
            Log.e(TAG_MUSIC_SERVICE, "Did you forget to setSmallIconResource() in your onBind()?");
        }
    }

    public boolean hasNext() {
        return (trackQueue.size() > 0) && (currentTrackIndex < trackQueue.size() - 1);
    }

    public boolean hasPrevious() {
        return (trackQueue.size() > 0) && (currentTrackIndex > 0);
    }

    public PlaybackStateCompat getPlaybackState() {
        return mediaSession.getController().getPlaybackState();
    }

    public boolean isPlaying() {
        int state = getPlaybackState().getState();
        return state == PlaybackStateCompat.STATE_BUFFERING ||
                state == PlaybackStateCompat.STATE_FAST_FORWARDING ||
                state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_REWINDING;
    }

    public void setSmallIconResource(int resId) {
        this.smallIcon = resId;
    }

    private void updateMetadata() {
        if(getCurrentState() == State.PREPARED) {
            Track track;
            try {
                track = getCurrentTrack();
                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.getArtworkUrl());
                builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, track.getArtist());
                builder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle());
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
                mediaSession.setMetadata(builder.build());
            } catch (IllegalStateException e) {
                //nothing to update
            }
        }
    }

    private void removeNotification() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.cancel(ID_NOTIFICATION);
    }

    public void setCallback(Callbacks callback) {
        this.callbacks = callback;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_WAKE_LOCK) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    cueTrack();
                } catch (IOException e) {
                    Log.e(TAG_MUSIC_SERVICE, e.getMessage());
                    stop();
                }
            } else {
                //TODO cannot get wifi lock
                stop();
            }
            return;
        }
    }

    public interface Callbacks {
        void onPermissionRequired(int requestCode, String permission, String rationale);
        void onLoading();
        void onPlaybackStarted();
        void onPlaybackStopped();
        void onError();
    }
}
