package com.mylovemhz.simplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;

public class MediaButtonEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            MusicService service = getMusicService(context);
            if (service != null) {
                switch(event.getKeyCode()){
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        service.play();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        service.pause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        service.stop();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        service.next();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        service.previous();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        service.fastForward();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        service.rewind();
                        break;
                }
            }
        }

    }

    private MusicService getMusicService(Context context) {
        IBinder binder = peekService(context, new Intent(context, MusicService.class));
        if (binder == null)
            return null;
        MusicService service = ((MusicService.LocalBinder) binder).getService();
        return service;
    }
}