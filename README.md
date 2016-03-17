# Simplay
Simplay is a simple, reusable API for implementing audio playback applications on Android. It is a layer of abstraction above Android's MediaSession API.

By using the MusicService and the optional MediaControlFragment, Simplay allows you to easily stream audio without worrying about the cumbersome MediaPlayer API.

## Usage
The <b>MusicService</b> handles playback and queing, so at the most basic level, all you need to do is bind to the service and implement its callbacks.

```java
public class ExampleActivity extends AppCompatActivity 
implements MusicService.Callbacks{

  private MusicService musicService;
  private boolean isBound = false;
    
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
  public void onPermissionRequired(final int requestCode, final String permission, String rationale) {
      // For Android Marshmallow and above
  }

  @Override
  public void onLoading() {
      
  }

  @Override
  public void onPlaybackStarted() {
      
  }

  @Override
  public void onPlaybackStopped() {
      
  }

  @Override
  public void onError() {

  }
}
```

### To stream a file
implement the <b>Track</b> interface and return the corresponding resource URLs. 
Then, when you have bound to the <b>MusicService</b>, add the track to enqueue it for playback.
```java
musicService.addTrack(track);
```
The when the first track is queued, the service will begin playback automatically. 
All subsequent tracks will be queued to be played after the previous ends.
