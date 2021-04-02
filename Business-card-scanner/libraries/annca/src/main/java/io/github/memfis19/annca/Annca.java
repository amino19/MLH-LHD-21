package io.github.memfis19.annca;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.ui.camera.Camera1Activity;
import io.github.memfis19.annca.internal.ui.camera2.Camera2Activity;
import io.github.memfis19.annca.internal.utils.CameraHelper;

/**
 * Created by memfis on 7/6/16.
 */
public class Annca {

    private AnncaConfiguration anncaConfiguration;

    /***
     * Creates Annca instance with default configuration set to photo with medium quality.
     *
     * @param activity    - fromList which request was invoked
     * @param requestCode - request code which will return in onActivityForResult
     */
    public Annca(Activity activity, @IntRange(from = 0) int requestCode) {
        AnncaConfiguration.Builder builder = new AnncaConfiguration.Builder(activity, requestCode);
        anncaConfiguration = builder.build();
    }

    /***
     * Creates Annca instance with custom camera configuration.
     *
     * @param cameraConfiguration
     */
    public Annca(AnncaConfiguration cameraConfiguration) {
        this.anncaConfiguration = cameraConfiguration;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void launchCamera() {
        if (anncaConfiguration == null || anncaConfiguration.getActivity() == null) return;

        Intent cameraIntent;

        if (CameraHelper.hasCamera2(anncaConfiguration.getActivity())) {
            cameraIntent = new Intent(this.anncaConfiguration.getActivity(), Camera2Activity.class);
        } else {
            cameraIntent = new Intent(this.anncaConfiguration.getActivity(), Camera1Activity.class);
        }

        cameraIntent.putExtra(AnncaConfiguration.Arguments.REQUEST_CODE, anncaConfiguration.getRequestCode());
        Log.d("Annca: ","RequestCode:"+anncaConfiguration.getRequestCode());

        if (anncaConfiguration.getMediaAction() > 0)
            cameraIntent.putExtra(AnncaConfiguration.Arguments.MEDIA_ACTION, anncaConfiguration.getMediaAction());

        if (anncaConfiguration.getMediaQuality() > 0)
            cameraIntent.putExtra(AnncaConfiguration.Arguments.MEDIA_QUALITY, anncaConfiguration.getMediaQuality());

        if (anncaConfiguration.getVideoDuration() > 0)
            cameraIntent.putExtra(AnncaConfiguration.Arguments.VIDEO_DURATION, anncaConfiguration.getVideoDuration());

        if (anncaConfiguration.getVideoFileSize() > 0)
            cameraIntent.putExtra(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE, anncaConfiguration.getVideoFileSize());

        if (anncaConfiguration.getMinimumVideoDuration() > 0)
            cameraIntent.putExtra(AnncaConfiguration.Arguments.MINIMUM_VIDEO_DURATION, anncaConfiguration.getMinimumVideoDuration());

        anncaConfiguration.getActivity().startActivityForResult(cameraIntent, anncaConfiguration.getRequestCode());

    }
}
