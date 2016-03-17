package com.mylovemhz.muse;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.mylovemhz.simplay.MediaControlFragment;
import com.mylovemhz.simplay.MusicService;
import com.mylovemhz.simplay.Track;

public class MainActivity extends AppCompatActivity
        implements MusicService.Callbacks, SongsAdapter.Callback{

    private static final String TAG_MEDIA_CONTROLS = "media_controls";

    private MusicService musicService;
    private boolean isBound = false;

    private ListView songList;
    private ListView queueList;
    private QueueAdapter queueAdapter;
    private MediaControlFragment mediaControlFragment;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.LocalBinder)service).getService();
            musicService.setCallback(MainActivity.this);
            musicService.setSmallIconResource(android.R.drawable.ic_media_play);
            initMediaControls();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        songList = (ListView)findViewById(R.id.songsList);
        queueList = (ListView)findViewById(R.id.queuedList);

        SongsAdapter songsAdapter = new SongsAdapter();
        songsAdapter.setCallback(this);
        songList.setAdapter(songsAdapter);
    }

    private void initMediaControls(){
        if(isBound) {
            mediaControlFragment = MediaControlFragment.newInstance(musicService.getMediaSessionToken());
        }
    }

    private void showMediaControls(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.playerLayout, mediaControlFragment, TAG_MEDIA_CONTROLS);
        transaction.commit();
    }

    private void hideMediaControls(){
        if(!isFinishing()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(mediaControlFragment);
            transaction.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this,MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(serviceConnection);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(!isChangingConfigurations()) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
        }
        super.onDestroy();
    }

    @Override
    public void onPermissionRequired(final int requestCode, final String permission, String rationale) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                permission)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(rationale);
            builder.setNeutralButton("Got it", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askForPermission(requestCode,permission);
                }
            });
            builder.create().show();
        } else {
            // No explanation needed, we can request the permission.
            askForPermission(requestCode, permission);
        }
    }

    @Override
    public void onLoading() {
        Snackbar sb = Snackbar.make(findViewById(R.id.coordinatorLayout),"Loading track...",Snackbar.LENGTH_SHORT);
        sb.show();
    }

    @Override
    public void onPlaybackStarted() {
        showMediaControls();
    }

    @Override
    public void onPlaybackStopped() {
        hideMediaControls();
        refreshQueue();
    }

    @Override
    public void onError() {

    }

    private void askForPermission(int requestCode, String permission){
        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(isBound){
            musicService.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    private void refreshQueue(){
        queueList.setAdapter(new QueueAdapter(musicService.getTracks()));
    }

    @Override
    public void onTrackSelected(Track track) {
        if(isBound){
            if(mediaControlFragment == null) initMediaControls();
            try {
                musicService.addTrack(track);
                refreshQueue();
            } catch (Exception e) {
                Log.e("Track queue example",e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.clearItem){
            if(isBound){
                musicService.stop();
                musicService.clearTrackQueue();
                refreshQueue();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
