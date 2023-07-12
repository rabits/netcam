package io.stateoftheart.netcam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtsp.rtsp.VideoCodec;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera2;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class MainActivity extends Activity implements View.OnClickListener, ConnectCheckerRtsp {
    private static final String TAG = "camera2";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private RtspServerCamera2 rtspCamera2;
    private OpenGlView mSurfaceView;

    private Button button;
    private Button bRecord;

    @Override
    protected void onResume() {
        Log.d(TAG,"[onResume]");
        super.onResume();
        prepareCamera();
    }

    protected void onCreate(Bundle savedInstanceState) {
        showWhenLockedAndTurnScreenOn();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        bRecord = findViewById(R.id.b_record);
        bRecord.setOnClickListener(this);
        Button switch_camera = findViewById(R.id.switch_camera);
        switch_camera.setOnClickListener(this);

        mSurfaceView = findViewById(R.id.surfaceView);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
        Context context = getApplicationContext();
        assert context != null;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            );
        }
    }

    protected void onPause(){
        super.onPause();
        closeCamera();
    }

    @Override
    public void onClick(View v) {
        switch( v.getId() ) {
            case R.id.b_start_stop:
                if (!rtspCamera2.isStreaming()) {
                    if (rtspCamera2.isRecording() || rtspCamera2.prepareAudio() && rtspCamera2.prepareVideo()) {
                        button.setText(R.string.stop_button);
                        rtspCamera2.startStream();
                        ((TextView)findViewById(R.id.tv_url)).setText(rtspCamera2.getEndPointConnection());
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
                            .show();
                    }
                } else {
                    button.setText(R.string.start_button);
                    rtspCamera2.stopStream();
                    ((TextView)findViewById(R.id.tv_url)).setText("");
                }
                break;
            case R.id.switch_camera:
                try {
                    rtspCamera2.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            /*case R.id.b_record:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (!rtspCamera2.isRecording) {
                      try {
                        if (!folder.exists()) {
                          folder.mkdir()
                        }
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        currentDateAndTime = sdf.format(Date())
                        if (!rtspCamera2.isStreaming) {
                          if (rtspCamera2.prepareAudio() && rtspCamera2.prepareVideo()) {
                            rtspCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                            bRecord.setText(R.string.stop_record)
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                          } else {
                            Toast.makeText(
                              this, "Error preparing stream, This device cant do it",
                              Toast.LENGTH_SHORT
                            ).show()
                          }
                    } else {
                        rtspCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                        bRecord.setText(R.string.stop_record)
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                      }
                    } catch (e: IOException) {
                      rtspCamera2.stopRecord()
                      bRecord.setText(R.string.start_record)
                      Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                  } else {
                    rtspCamera2.stopRecord()
                    bRecord.setText(R.string.start_record)
                    Toast.makeText(
                      this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                      Toast.LENGTH_SHORT
                    ).show()
                    }
                } else {
                  Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
                }
                break;*/
        }
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "[surfaceCreated]");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "[surfaceChanged] format:" + format + "  width:" + width + "  height:" + height);
            rtspCamera2.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            closeCamera();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void prepareCamera(){
        if (shouldRequestCameraPermission()) {
            requestPermissions(PERMISSIONS, REQUEST_CAMERA_PERMISSION);
        }
        Log.d(TAG,"[prepareCamera]");

        rtspCamera2 = new RtspServerCamera2(mSurfaceView, this, 8080);

        List<android.util.Size> videoSizes = rtspCamera2.getResolutionsBack();
        List<android.util.Range<Integer>> supportedFps = rtspCamera2.getSupportedFps();
        int b = rtspCamera2.getBitrate();
        int w = rtspCamera2.getStreamWidth();
        int h = rtspCamera2.getStreamHeight();
        Log.i(TAG, "Init RTSP Camera2: video sizes: " + videoSizes.toString() + ", fps: " + supportedFps.toString() + ", bitrate: " + b + ", width: " + w + ", height: " + h);
        rtspCamera2.setVideoCodec(VideoCodec.H264);
        rtspCamera2.prepareVideo(1280, 720, 30, 5000, 10, 0);
        rtspCamera2.setReTries(10);
    }

    @Override
    public void onAuthErrorRtsp() {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),"RTSP auth error", Toast.LENGTH_SHORT).show();
            rtspCamera2.stopStream();
            button.setText(R.string.start_button);
            ((TextView)findViewById(R.id.tv_url)).setText("");
        });
    }

    @Override
    public void onAuthSuccessRtsp() {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"RTSP auth success", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onConnectionFailedRtsp(@NonNull String reason) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),"RTSP connection failed: " + reason, Toast.LENGTH_SHORT).show();
            rtspCamera2.stopStream();
            button.setText(R.string.start_button);
        });
    }

    @Override
    public void onConnectionStartedRtsp(@NonNull String s) {
        // Empty
    }

    @Override
    public void onConnectionSuccessRtsp() {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"RTSP connection success", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDisconnectRtsp() {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"RTSP disconnected", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onNewBitrateRtsp(long l) {
        Log.i(TAG, "NewBitrateRtsp: " + l);
    }

    private void closeCamera() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (rtspCamera2.isRecording()) {
                rtspCamera2.stopRecord();
                bRecord.setText(R.string.start_record);
                Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath, Toast.LENGTH_SHORT).show();
                currentDateAndTime = "";
            }
        }*/
        if (rtspCamera2.isStreaming()) {
            rtspCamera2.stopStream();
            button.setText(R.string.start_button);
            ((TextView)findViewById(R.id.tv_url)).setText("");
        }
        rtspCamera2.stopPreview();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean shouldRequestCameraPermission() {
        return checkSelfPermission(PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
    }
}
