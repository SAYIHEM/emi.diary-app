package emi.diary_app.ListManagement;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import emi.diary_app.ActionBar.ActionBarCallback_MainActivity;
import emi.diary_app.Activity.MainActivity;
import emi.diary_app.Note;
import emi.diary_app.PlayButton;
import emi.diary_app.PlayState;
import emi.diary_app.R;


public class EntryAdapter extends BaseAdapter implements Serializable{

    private static final int EDIT_ENTRY = 1;

    private Context context;
    private ArrayList<Note> noteList;
    private TableManager tableManager;

    private Handler seekHandler = new Handler();
    private SeekBar playerSeekBar = null;
    private MediaPlayer mediaPlayer = null;
    private int actualNotePlaying = -1;

    private ActionMode actionMode = null;
    private ActionBarCallback_MainActivity actionBar = null;

    private View selectedTextContent = null;

    public EntryAdapter(Context context, ArrayList<Note> noteList, TableManager tableManager) {
        this.context = context;
        this.noteList = noteList;
        this.tableManager = tableManager;
    }

    @Override
    public int getCount() {
        return noteList.size();
    }

    @Override
    public Object getItem(int i) {
        return noteList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setActionBar(ActionMode mode) {

        actionMode = mode;
    }

    @Override
    public View getView(int pos, View view, ViewGroup viewGroup) {


        final Note note = noteList.get(pos);

        /* Create Main view */
        View entry = View.inflate(context, R.layout.entry, null);

        TextView tvTitle = (TextView) entry.findViewById(R.id.tvTitle);
        TextView tvDate = (TextView) entry.findViewById(R.id.tvDate);

        /* Set Title and Date */
        tvTitle.setText(note.getTitle());
        tvDate.setText(note.getDate_Location());

        LinearLayout entryText = (LinearLayout) entry.findViewById(R.id.entryText);
        final LinearLayout entryVoice = (LinearLayout) entry.findViewById(R.id.entryVoice);
        LinearLayout entryImage = (LinearLayout) entry.findViewById(R.id.entryImage);

        /* Setting up TextEntry */
        final View textEntry = View.inflate(context, R.layout.diary_entry_text, null);
        final TextView textContent = (TextView) textEntry.findViewById(R.id.textContent);
        textContent.setText(note.getTextNote());
        textContent.setMaxHeight(300);

        final LinearLayout entryLinLay = (LinearLayout) entry.findViewById(R.id.entry_linLay);

        /* If Entry is selected set Grey Backround, else set White Backround */
        if (note.isSelected()) {

            entryLinLay.setBackgroundColor(context.getResources().getColor(R.color.entry_selected));

        } else {

            entryLinLay.setBackgroundColor(context.getResources().getColor(R.color.entry_not_selected));
        }

        entryLinLay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {


                if (actionMode != null) {

                    actionMode.finish();
                }

                actionBar = new ActionBarCallback_MainActivity(textContent, EntryAdapter.this, tableManager, context, note, entryLinLay);
                ((MainActivity) context).startActionMode(actionBar);


                return false;
            }
        });




        /* Setting up VoiceEntry */
        View voiceEntry = null;
        if (!note.getVoiceNote().equals("")) {

            voiceEntry = View.inflate(context, R.layout.diary_entry_voice, null);

            /* - - - Set up PlayButton - - - - - - - - - - - - - - - - - - - - - - - */
            final PlayButton playVoiceContent = new PlayButton(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_play));

            } else {
                playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_play));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 6);
            params.setMargins(15, 15, 0, 15);

            playVoiceContent.setLayoutParams(params);
            playVoiceContent.setGravity(Gravity.CENTER_VERTICAL);

            /* Add Button to Linear Layout */
            LinearLayout linearLayout = (LinearLayout) voiceEntry.findViewById(R.id.voiceEntryLinLay);
            linearLayout.addView(playVoiceContent, 0);
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


            /* - - - Set up SeekBar - - - - - - - - - - - - - - - - - - - - - - - -  */
            playerSeekBar = (SeekBar) voiceEntry.findViewById(R.id.playerProgressBar);

            /* Creating Listener for moving SeekBar */
            View.OnTouchListener touchSeek = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {

                    switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);

                            if (playVoiceContent.state == PlayState.PLAYING || playVoiceContent.state == PlayState.PAUSED) {

                                mediaPlayer.pause();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_play));

                                } else {
                                    playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_play));
                                }

                                actualNotePlaying = -1;

                            } else if (playVoiceContent.state == PlayState.STOPPED) {

                                /* Do noting... */
                            }

                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_OUTSIDE:
                        case MotionEvent.ACTION_CANCEL:
                            view.setPressed(false);

                            if (playVoiceContent.state == PlayState.PLAYING) {

                                mediaPlayer.seekTo(playerSeekBar.getProgress());

                                /* Set up "Pause" Icon */
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_pause));

                                } else {
                                    playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_pause));
                                }

                                mediaPlayer.start();

                                /* SeekBar updation */
                                myHandler.postDelayed(UpdateSongTime,100);

                                actualNotePlaying = note.getID();

                            } else if (playVoiceContent.state == PlayState.STOPPED) {

                                note.setLastPlayedDuration(playerSeekBar.getProgress());

                            } else if (playVoiceContent.state == PlayState.PAUSED) {

                                /* Seek to Duration*/
                                mediaPlayer.seekTo(playerSeekBar.getProgress());
                            }

                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            break;
                        case MotionEvent.ACTION_MOVE:
                            break;
                    }

                    return false;
                }
            };

            playerSeekBar.setOnTouchListener(touchSeek);

            note.setSeekBar(playerSeekBar);

            /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */



            /* - - - Set up PlayButton and MediaPlayer - - - - -  - - - - - - - - -  */

            /* Check if Audio is playing
             * and Set the right Symbol and State
             * of the Button */
            if (mediaPlayer != null && note.getID() == actualNotePlaying) {
                if (mediaPlayer.isPlaying()) {

                    playVoiceContent.setPlaying();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_pause));

                    } else {
                        playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_pause));
                    }

                    // TODO: Update SeekBar when Audio was paused
                    // TODO: And List scrolled down
                    if (note.getLastPlayedDuration() != 0) {

                        playerSeekBar = note.getSeekBar();

                    }

                } else if (mediaPlayer.getCurrentPosition() != 0) {


                }
            }
            playVoiceContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    /* If Audio is playing */
                    if (playVoiceContent.state == PlayState.PLAYING) {

                        /* Set up "Play" Icon */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_play));

                        } else {
                            playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_play));
                        }

                        /* Pause Audio */
                        mediaPlayer.pause();
                        actualNotePlaying = -1;

                        playVoiceContent.setPaused();

                    /* If Audio is stopped */
                    } else if (playVoiceContent.state == PlayState.STOPPED && actualNotePlaying == -1){

                        /* Set up "Play" Icon */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_pause));

                        } else {
                            playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_pause));
                        }

                        // TODO: Stop Player if there are Playing other Audios

                        /* - - - Preparing MediaPlayer - - - - - - - - - - - - - - - - - - - - - */
                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(note.getVoiceNote());
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        /* Set OnCompletitionListener to MediaPlayer */
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {

                                /* Re-Preparing MediaPlayer */
                                mediaPlayer.stop();
                                mediaPlayer.reset();
                                mediaPlayer.release();

                                /* Set State of PlayButton to "Stopped" */
                                playVoiceContent.setStopped();

                                /* Stop SeekBar-Updation */
                                playerSeekBar.setProgress(0);

                                /* Set up "Pause" Icon */
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_play));

                                } else {
                                    playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_play));
                                }

                                 /* Reset LastPlayedDuration in Note */
                                note.setLastPlayedDuration(0);

                                actualNotePlaying = -1;
                            }
                        });

                        /* Start Audio */
                        mediaPlayer.start();
                        actualNotePlaying = note.getID();

                        /* If LastPlayedDuration not 0 -> Seek to Duration*/
                        if (note.getLastPlayedDuration() > 0) {

                            mediaPlayer.seekTo(note.getLastPlayedDuration());
                        }


                        /* SeekBar updation */
                        playerSeekBar = note.getSeekBar();
                        playerSeekBar.setMax(mediaPlayer.getDuration());
                        myHandler.postDelayed(UpdateSongTime,100);

                        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


                        playVoiceContent.setPlaying();

                    /* If Audio is paused */
                    } else if (playVoiceContent.state == PlayState.PAUSED) {

                        /* Set up "Play" Icon */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            playVoiceContent.setBackground(context.getResources().getDrawable(R.drawable.ic_media_pause));

                        } else {
                            playVoiceContent.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_media_pause));
                        }

                        mediaPlayer.start();
                        actualNotePlaying = note.getID();

                        /* SeekBar updation */
                        playerSeekBar = note.getSeekBar();
                        playerSeekBar.setMax(mediaPlayer.getDuration());
                        myHandler.postDelayed(UpdateSongTime,100);

                        playVoiceContent.setPlaying();
                    }
                }
            });
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


        }


        /* Setting up ImageContent */
        View imageEntry = View.inflate(context, R.layout.diary_entry_picture, null);
        ImageView imageContent = (ImageView) imageEntry.findViewById(R.id.imageContent);
        if (!note.getImageNote().equals("")) {

            imageContent.setImageBitmap(note.getBitmap());
            imageContent.setMaxHeight(350);
        }

        /* Adding LinLayouts to the Entry */
        entryText.addView(textEntry);
        if (voiceEntry != null) {

            entryVoice.addView(voiceEntry);
        }
        entryImage.addView(imageEntry);

        return entry;
    }

    private double startTime = 0;

    private Handler myHandler = new Handler();;

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {

            if (actualNotePlaying != -1) {

                startTime = mediaPlayer.getCurrentPosition();

                TextView tx1 = new TextView(context);

                tx1.setText(String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                        toMinutes((long) startTime)))
                );

                playerSeekBar.setProgress((int)startTime);
                myHandler.postDelayed(this, 5);
            }
        }
    };


}