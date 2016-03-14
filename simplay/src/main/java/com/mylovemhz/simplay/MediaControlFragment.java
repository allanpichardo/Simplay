package com.mylovemhz.simplay;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;


public class MediaControlFragment extends Fragment{

    private static final String TAG = "MediaControlFragment";

    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private TextView artistText;
    private TextView titleText;
    private ImageView albumImage;
    private SeekBar seekBar;
    private String artUrl;
    private Callbacks callbacks;
    private MediaControllerCompat mediaController;
    private MediaSessionCompat.Token token;

    private int playDrawableResource;
    private int pauseDrawableResource;
    private int nextDrawable;
    private int previousDrawable;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "Received playback state change to state " + state.getState());
            MediaControlFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            Log.d(TAG, "Received metadata state change to mediaId=" +
                    metadata.getDescription().getMediaId() +
                    " song=" + metadata.getDescription().getTitle());
            MediaControlFragment.this.onMetadataChanged(metadata);
        }
    };


    public MediaControlFragment() {
        // Required empty public constructor
    }

    public static MediaControlFragment newInstance(MediaSessionCompat.Token token,
                                                   int playDrawableResource,
                                                   int pauseDrawableResource,
                                                   int nextDrawable,
                                                   int previousDrawable){
        MediaControlFragment instance = new MediaControlFragment();
        instance.playDrawableResource = playDrawableResource;
        instance.pauseDrawableResource = pauseDrawableResource;
        instance.nextDrawable = nextDrawable;
        instance.previousDrawable = previousDrawable;
        instance.token = token;
        return instance;
    }

    public static MediaControlFragment newInstance(MediaSessionCompat.Token token){
        return MediaControlFragment.newInstance(token, R.drawable.ic_play, R.drawable.ic_pause,
                R.drawable.ic_next, R.drawable.ic_previous);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        albumImage = (ImageView)view.findViewById(R.id.albumImage);
        artistText = (TextView)view.findViewById(R.id.artistText);
        nextButton = (ImageButton)view.findViewById(R.id.nextButton);
        playPauseButton = (ImageButton)view.findViewById(R.id.playPauseButton);
        previousButton = (ImageButton)view.findViewById(R.id.previousButton);
        seekBar = (SeekBar)view.findViewById(R.id.seekBar);
        titleText = (TextView)view.findViewById(R.id.titleText);
        try {
            mediaController = new MediaControllerCompat(getContext(),token);
            mediaController.registerCallback(mediaControllerCallback);
        } catch (RemoteException e) {
            Log.e(TAG,e.getMessage());
        }

        initViews(savedInstanceState);
    }

    private void initViews(Bundle savedInstanceState){
        nextButton.setImageResource(R.drawable.ic_next);
        playPauseButton.setImageResource(R.drawable.ic_play);
        previousButton.setImageResource(R.drawable.ic_previous);
        if(savedInstanceState != null){
            //TODO restore view states
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            callbacks = (Callbacks) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement Callbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        Log.d(TAG, "onMetadataChanged ");
        if (getActivity() == null) {
            Log.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

        artistText.setText(metadata.getDescription().getTitle());
        titleText.setText(metadata.getDescription().getSubtitle());
        String artUrl = "";
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!artUrl.equals(this.artUrl)) {
            this.artUrl = artUrl;
            try {
                Picasso.with(getContext().getApplicationContext())
                        .load(this.artUrl)
                        .into(albumImage);
            }catch(IllegalArgumentException e){
                //no artwork
                albumImage.setImageResource(R.drawable.ic_album);
            }
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged ");
        if (getActivity() == null) {
            Log.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                Log.e(TAG, "error playbackstate: " + state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            playPauseButton.setImageResource(playDrawableResource);
        } else {
            playPauseButton.setImageResource(pauseDrawableResource);
        }

        MediaControllerCompat controller = getActivity().getSupportMediaController();
//        String extraInfo = null;
//        if (controller != null && controller.getExtras() != null) {
//            String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
//            if (castName != null) {
//                extraInfo = getResources().getString(R.string.casting_to_device, castName);
//            }
//        }
//        setExtraInfo(extraInfo);
    }

    public interface Callbacks {

    }
}
