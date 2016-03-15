package com.mylovemhz.muse;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.mylovemhz.simplay.MediaControlFragment;
import com.mylovemhz.simplay.MusicService;
import com.mylovemhz.simplay.Track;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MusicService.PermissionCallbacks{

    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.LocalBinder)service).getService();
            musicService.setPermissonCallback(MainActivity.this);
            musicService.setSmallIconResource(android.R.drawable.ic_media_play);

            initControls();

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isBound){
                    Track track = new Track() {
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

                        @Override
                        public Bitmap getArtwork() {
                            return BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_play);
                        }
                    };

                    try {
                        musicService.addTrack(track);
                        Snackbar.make(view, "Added Track", Snackbar.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("Music Test App",e.getMessage());
                    }
                }
            }
        });
    }

    private void initControls(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.playerLayout,MediaControlFragment.newInstance(musicService.getMediaSessionToken()));
        transaction.commit();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            askForPermission(requestCode,permission);
        }
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
}
