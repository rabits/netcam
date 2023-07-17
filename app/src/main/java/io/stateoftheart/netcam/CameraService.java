package io.stateoftheart.netcam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Build.VERSION;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.util.Size;

import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtsp.rtsp.VideoCodec;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class CameraService extends Service implements ConnectCheckerRtsp {
    @NotNull
    private static final String TAG = "CameraService";

    @NotNull
    private static final String CHANNEL_ID = "io.stateoftheart.netcam.cam_channel_id";
    @NotNull
    private static final String CHANNEL_NAME = "io.stateoftheart.netcam.cam_channel_name";
    private static final int ONGOING_NOTIFICATION_ID = 6660;
    @NotNull
    protected static final String ACTION_START = "io.stateoftheart.netcam.action.START";
    @NotNull
    protected static final String ACTION_STOPPED = "io.stateoftheart.netcam.action.STOPPED";

    private NotificationManager mNotificationManager;
    private RtspServerCamera2 rtspCamera2;

    // Object that receives interactions from clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }

    @Nullable
    public IBinder onBind(@Nullable Intent p0) {
        return mBinder;
    }

    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if( Objects.equals(action, ACTION_START) ) {
            start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Allows to start preview from any activity
     * @param previewSurface - where to show the capture preview
     */
    public void startPreview(OpenGlView previewSurface) {
        rtspCamera2.replaceView(previewSurface);
        rtspCamera2.startPreview();
    }

    /**
     * Stops preview drawing
     */
    public void stopPreview() {
        rtspCamera2.stopPreview();
        rtspCamera2.replaceView(this);
    }

    public void onCreate() {
        super.onCreate();
        startForeground();
    }

    public void onDestroy() {
        super.onDestroy();
        stopCamera();

        sendBroadcast(new Intent(ACTION_STOPPED));
    }

    private void start() {
        //this.shouldShowPreview = false;

        initCam(1280, 720);
        rtspCamera2.startStream();
    }

    private void initCam(int width, int height) {
        rtspCamera2 = new RtspServerCamera2(this, true, this, 8080);

        List<Size> videoSizes = rtspCamera2.getResolutionsBack();
        List<android.util.Range<Integer>> supportedFps = rtspCamera2.getSupportedFps();
        int b = rtspCamera2.getBitrate();
        int w = rtspCamera2.getStreamWidth();
        int h = rtspCamera2.getStreamHeight();
        Log.i(TAG, "Init RTSP Camera2: video sizes: " + videoSizes.toString() + ", fps: " + supportedFps.toString() + ", bitrate: " + b + ", width: " + w + ", height: " + h);

        rtspCamera2.setVideoCodec(VideoCodec.H264);
        int rotation = CameraHelper.getCameraOrientation(this);
        rtspCamera2.prepareVideo(width, height, 30, 5000, 10, rotation);
        rtspCamera2.setReTries(10);
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
        }

        Notification notification = (new NotificationCompat.Builder(this, CHANNEL_ID))
                .setOngoing(true)
                .setContentTitle(this.getText(R.string.app_name))
                .setContentText("RTSP Server started")
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setContentIntent(pendingIntent)
                .setTicker(this.getText(R.string.app_name))
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void stopCamera() {
        if (rtspCamera2.isStreaming()) {
            rtspCamera2.stopStream();
            //button.setText(R.string.start_button);
            //((TextView)findViewById(R.id.tv_url)).setText("");
        }
        rtspCamera2.stopPreview();
    }

    private void showNotification(String text) {
        Log.i(TAG, "[showNotification]");
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("NetCam")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        mNotificationManager.notify(12345, notification.build());
    }

    @Override
    public void onAuthErrorRtsp() {
        showNotification("RTSP auth error");
        //rtspCamera2.stopStream();
        //button.setText(R.string.start_button);
        //((TextView)findViewById(R.id.tv_url)).setText("");
    }

    @Override
    public void onAuthSuccessRtsp() {
        showNotification("RTSP auth success");
    }

    @Override
    public void onConnectionFailedRtsp(@NonNull String reason) {
        showNotification("RTSP connection failed: " + reason);
        //rtspCamera2.stopStream();
        //button.setText(R.string.start_button);
    }

    @Override
    public void onConnectionStartedRtsp(@NonNull String s) {
        // Empty
    }

    @Override
    public void onConnectionSuccessRtsp() {
        showNotification("RTSP connection success");
    }

    @Override
    public void onDisconnectRtsp() {
        showNotification("RTSP disconnected");
    }

    @Override
    public void onNewBitrateRtsp(long l) {
        Log.i(TAG, "NewBitrateRtsp: " + l);
    }
}
