package io.github.memfis19.annca.internal.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.github.memfis19.annca.R;
import io.github.memfis19.annca.internal.Box;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.controller.impl.Camera2Controller;
import io.github.memfis19.annca.internal.ui.camera2.Camera2Activity;
import io.github.memfis19.annca.internal.utils.DateTimeUtils;

import static io.github.memfis19.annca.internal.utils.CameraHelper.TAG;

/**
 * Created by memfis on 7/6/16.
 */
public class CameraControlPanel extends RelativeLayout
        implements RecordButton.RecordButtonListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener {

    public static int x1, x2, y1, y2;
    private Context context;
    private static final int FOCUS_AREA_SIZE = 1000;
    private CameraSwitchView cameraSwitchView;
    private RecordButton recordButton;
    private MediaActionSwitchView mediaActionSwitchView;
    private FlashSwitchView flashSwitchView;
    private TextView recordDurationText;
    private TextView recordSizeText;
    private CameraSettingsView settingsButton;

    private RecordButton.RecordButtonListener recordButtonListener;
    private MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener;
    private CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener;
    private FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener;
    private SettingsClickListener settingsClickListener;

    private TimerTaskBase countDownTimer;
    private long maxVideoFileSize = 0;
    private String mediaFilePath;
    private RelativeLayout rl1;
    private RelativeLayout rl2;
    public static final String TAG = "CameraControlPanel";

    public interface SettingsClickListener {
        void onSettingsClick();
    }

    private boolean hasFlash = false;

    private
    @MediaActionSwitchView.MediaActionState
    int mediaActionState;
    private int mediaAction;
    private FileObserver fileObserver;
    private Box box;


    public CameraControlPanel(Context context) {
        this(context, null);
    }

    public CameraControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }


    private void init() {
//        hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        hasFlash = false;

        LayoutInflater.from(context).inflate(R.layout.camera_control_panel_layout, this);
        setBackgroundColor(Color.TRANSPARENT);

        box = new Box(context);
//        getWindow().addContentView(box, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        settingsButton = (CameraSettingsView) findViewById(R.id.settings_view);
        cameraSwitchView = (CameraSwitchView) findViewById(R.id.front_back_camera_switcher);
        mediaActionSwitchView = (MediaActionSwitchView) findViewById(R.id.photo_video_camera_switcher);
        recordButton = (RecordButton) findViewById(R.id.record_button);
        flashSwitchView = (FlashSwitchView) findViewById(R.id.flash_switch_view);
        recordDurationText = (TextView) findViewById(R.id.record_duration_text);
        recordSizeText = (TextView) findViewById(R.id.record_size_mb_text);
        rl1 = (RelativeLayout) findViewById(R.id.rl1);
        rl2 = (RelativeLayout) findViewById(R.id.rl2);

//        Log.d(TAG,"left: "+rl1.getX());
//        Log.d(TAG,"width: "+rl1.getWidth());
//        Log.d(TAG,"right: "+rl1.getRight());
//        Log.d(TAG,"childCount: "+rl1.getChildCount());

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int[] locations = new int[2];
                rl1.getLocationOnScreen(locations);
                x1 = locations[0];
                y1 = locations[1];
                Log.d(TAG, "xy:::" + x1);
                Log.d(TAG, "xyyy:::" + y1);

                rl2.getLocationOnScreen(locations);
                x2 = locations[0];
                y2 = locations[1];
                Log.d(TAG, "xy2:::" + x2);
                Log.d(TAG, "xyyy2:::" + y2);
            }
        });

        cameraSwitchView.setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        mediaActionSwitchView.setOnMediaActionStateChangeListener(this);

        setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        setOnMediaActionStateChangeListener(onMediaActionStateChangeListener);
        setFlashModeSwitchListener(flashModeSwitchListener);
        setRecordButtonListener(recordButtonListener);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingsClickListener != null) settingsClickListener.onSettingsClick();
            }
        });

        if (hasFlash)
            flashSwitchView.setVisibility(VISIBLE);
        else flashSwitchView.setVisibility(GONE);

        countDownTimer = new TimerTask(recordDurationText);
    }

    public void lockControls() {
        cameraSwitchView.setEnabled(false);
        recordButton.setEnabled(false);
        settingsButton.setEnabled(false);
        flashSwitchView.setEnabled(false);
    }

    public void unLockControls() {
        cameraSwitchView.setEnabled(true);
        recordButton.setEnabled(true);
        settingsButton.setEnabled(true);
        flashSwitchView.setEnabled(true);
    }

    public void setup(int mediaAction) {
        this.mediaAction = mediaAction;
        if (AnncaConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            recordButton.setup(mediaAction, this);
            flashSwitchView.setVisibility(GONE);
        } else {
            recordButton.setup(AnncaConfiguration.MEDIA_ACTION_PHOTO, this);
        }

        if (AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
    }

    public void setMediaFilePath(final File mediaFile) {
        this.mediaFilePath = mediaFile.toString();
    }

    public void setMaxVideoFileSize(long maxVideoFileSize) {
        this.maxVideoFileSize = maxVideoFileSize;
    }

    public void setMaxVideoDuration(int maxVideoDurationInMillis) {
        if (maxVideoDurationInMillis > 0)
            countDownTimer = new CountdownTask(recordDurationText, maxVideoDurationInMillis);
        else countDownTimer = new TimerTask(recordDurationText);
    }

    public void setMediaActionState(@MediaActionSwitchView.MediaActionState int actionState) {
        if (mediaActionState == actionState) return;
        if (MediaActionSwitchView.ACTION_PHOTO == actionState) {
            recordButton.setMediaAction(AnncaConfiguration.MEDIA_ACTION_PHOTO);
            if (hasFlash)
                flashSwitchView.setVisibility(VISIBLE);
        } else {
            recordButton.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
            flashSwitchView.setVisibility(GONE);
        }
        mediaActionState = actionState;
        mediaActionSwitchView.setMediaActionState(actionState);
    }

    public void setRecordButtonListener(RecordButton.RecordButtonListener recordButtonListener) {
        this.recordButtonListener = recordButtonListener;
    }

    public void rotateControls(int rotation) {
        if (Build.VERSION.SDK_INT > 10) {
            cameraSwitchView.setRotation(rotation);
            mediaActionSwitchView.setRotation(rotation);
            flashSwitchView.setRotation(rotation);
            recordDurationText.setRotation(rotation);
            recordSizeText.setRotation(rotation);
        }
    }

    public void setOnMediaActionStateChangeListener(MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
    }

    public void setOnCameraTypeChangeListener(CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
        if (cameraSwitchView != null)
            cameraSwitchView.setOnCameraTypeChangeListener(this.onCameraTypeChangeListener);
    }

    public void setFlashModeSwitchListener(FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener) {
        this.flashModeSwitchListener = flashModeSwitchListener;
        if (flashSwitchView != null)
            flashSwitchView.setFlashSwitchListener(this.flashModeSwitchListener);
    }

    public void setSettingsClickListener(SettingsClickListener settingsClickListener) {
        this.settingsClickListener = settingsClickListener;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        if (recordButtonListener != null)
            recordButtonListener.onTakePhotoButtonPressed();
    }

    public void onStartVideoRecord(final File mediaFile) {
        setMediaFilePath(mediaFile);
        if (maxVideoFileSize > 0) {
            recordSizeText.setText("1Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
            recordSizeText.setVisibility(VISIBLE);
            try {
                fileObserver = new FileObserver(this.mediaFilePath) {
                    private long lastUpdateSize = 0;

                    @Override
                    public void onEvent(int event, String path) {
                        final long fileSize = mediaFile.length() / (1024 * 1024);
                        if ((fileSize - lastUpdateSize) >= 1) {
                            lastUpdateSize = fileSize;
                            recordSizeText.post(new Runnable() {
                                @Override
                                public void run() {
                                    recordSizeText.setText(fileSize + "Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
                                }
                            });
                        }
                    }
                };
                fileObserver.startWatching();
            } catch (Exception e) {
                Log.e("FileObserver", "setMediaFilePath: ", e);
            }
        }
        countDownTimer.start();

    }

    public void allowRecord(boolean isAllowed) {
        recordButton.setEnabled(isAllowed);
    }

    public void allowCameraSwitching(boolean isAllowed) {
        cameraSwitchView.setVisibility(isAllowed ? VISIBLE : GONE);
    }

    public void onStopVideoRecord() {
        if (fileObserver != null)
            fileObserver.stopWatching();
        countDownTimer.stop();

        recordSizeText.setVisibility(GONE);
        cameraSwitchView.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(VISIBLE);

        if (AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);
        recordButton.setRecordState(RecordButton.READY_FOR_RECORD_STATE);
    }

    @Override
    public void onStartRecordingButtonPressed() {

        cameraSwitchView.setVisibility(View.GONE);
        mediaActionSwitchView.setVisibility(GONE);
        settingsButton.setVisibility(GONE);

        if (recordButtonListener != null)
            recordButtonListener.onStartRecordingButtonPressed();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        onStopVideoRecord();
        if (recordButtonListener != null)
            recordButtonListener.onStopRecordingButtonPressed();
    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        setMediaActionState(mediaActionState);
        if (onMediaActionStateChangeListener != null)
            onMediaActionStateChangeListener.onMediaActionChanged(this.mediaActionState);
    }

    abstract class TimerTaskBase {
        protected Handler handler = new Handler(Looper.getMainLooper());
        protected TextView timerView;
        protected boolean alive = false;
        protected long recordingTimeSeconds = 0;
        protected long recordingTimeMinutes = 0;

        public TimerTaskBase(TextView timerView) {
            this.timerView = timerView;
        }

        abstract void stop();

        abstract void start();
    }

    private class CountdownTask extends TimerTaskBase implements Runnable {

        private int maxDurationMilliseconds = 0;

        public CountdownTask(TextView timerView, int maxDurationMilliseconds) {
            super(timerView);
            this.maxDurationMilliseconds = maxDurationMilliseconds;
        }

        @Override
        public void run() {

            recordingTimeSeconds--;

            int millis = (int) recordingTimeSeconds * 1000;

            timerView.setText(
                    String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(millis),
                            TimeUnit.MILLISECONDS.toSeconds(millis) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                    ));

            if (recordingTimeSeconds < 10) {
                timerView.setTextColor(Color.RED);
            }

            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }

        @Override
        void start() {
            alive = true;
            recordingTimeSeconds = maxDurationMilliseconds / 1000;
            timerView.setTextColor(Color.WHITE);
            timerView.setText(
                    String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds),
                            TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds))
                    ));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }
    }

    private class TimerTask extends TimerTaskBase implements Runnable {

        public TimerTask(TextView timerView) {
            super(timerView);
        }

        @Override
        public void run() {
            recordingTimeSeconds++;

            if (recordingTimeSeconds == 60) {
                recordingTimeSeconds = 0;
                recordingTimeMinutes++;
            }
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            if (alive) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void start() {
            alive = true;
            recordingTimeMinutes = 0;
            recordingTimeSeconds = 0;
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }
    }
}
