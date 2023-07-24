package io.stateoftheart.netcam;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.content.IntentFilter;

import com.pedro.rtplibrary.view.OpenGlView;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import io.stateoftheart.netcam.ml.GraphicOverlay;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private Button btn_start_stop;

    private final BroadcastReceiver receiver;

    private CameraService mCameraService;

    private GraphicOverlay graphic_overlay;
    private OpenGlView surface_view;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG,"[onServiceConnected]");
            mCameraService = ((CameraService.LocalBinder)service).getService();
            mCameraService.startML();
            mCameraService.startPreview(surface_view, graphic_overlay);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"[onServiceDisconnected]");
            mCameraService = null;
        }
    };

    public MainActivity() {
        super();
        receiver = new BroadcastReceiver() {
            public void onReceive(@Nullable Context p0, @Nullable Intent p1) {
                if( p1 != null ) {
                    if( Objects.equals(p1.getAction(), CameraService.ACTION_STOPPED) ) {
                        btn_start_stop.setText(R.string.start_button);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"[onResume]");
        super.onResume();

        if( shouldRequestCameraPermission() ) {
            requestPermissions(PERMISSIONS, REQUEST_CAMERA_PERMISSION);
        }

        registerReceiver(receiver, new IntentFilter(CameraService.ACTION_STOPPED));

        if( Utils.isServiceRunning(this, CameraService.class) ) {
            btn_start_stop.setText(R.string.stop_button);
        }
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "[surfaceCreated]");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "[surfaceChanged] format:" + format + "  width:" + width + "  height:" + height);
            if( mCameraService != null ) {
                mCameraService.startPreview(surface_view, graphic_overlay);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "[surfaceDestroyed]");
            if( mCameraService != null ) {
                mCameraService.stopPreview();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        showWhenLockedAndTurnScreenOn();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set logic for buttons
        btn_start_stop = findViewById(R.id.b_start_stop);
        btn_start_stop.setOnClickListener(v -> {
            if( !Utils.isServiceRunning(this, CameraService.class) ) {
                notifyService(CameraService.ACTION_START);
                if (mCameraService == null) {
                    bindService(new Intent(MainActivity.this, CameraService.class), mConnection, Context.BIND_AUTO_CREATE);
                }
            } else {
                if( mCameraService != null ) {
                    unbindService(mConnection);
                    mCameraService = null;
                }
                stopService(new Intent(this, CameraService.class));
            }
        });
        Button btn_record = findViewById(R.id.b_record);
        btn_record.setOnClickListener(v -> {
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
        });
        Button btn_switch = findViewById(R.id.switch_camera);
        btn_switch.setOnClickListener(v -> {
            /*try {
                rtspCamera2.switchCamera();
            } catch (CameraOpenException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }*/
        });

        surface_view = findViewById(R.id.surface_view);
        surface_view.setKeepAspectRatio(true);
        surface_view.getHolder().addCallback(mSurfaceHolderCallback);

        graphic_overlay = findViewById(R.id.graphic_overlay);
    }

    private void showWhenLockedAndTurnScreenOn() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ) {
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
        unregisterReceiver(receiver);
    }

    private void notifyService(String action) {
        Intent intent = new Intent(this, CameraService.class);
        intent.setAction(action);
        this.startService(intent);
    }

    private boolean shouldRequestCameraPermission() {
        return checkSelfPermission(PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if( requestCode == REQUEST_CAMERA_PERMISSION ) {
            if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(this, "ERROR: Unable to get permission to use camera", Toast.LENGTH_LONG).show();
                finish();
            } else {
            }
        }
    }
}
