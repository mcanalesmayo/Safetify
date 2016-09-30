package org.jojoma.safe;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.FP_MODES;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.PREVIEW_ROTATION_ANGLE;

@SuppressLint("NewApi")
public class CameraPreviewActivity extends Activity implements Camera.PreviewCallback {

    Camera cameraObj;
    FrameLayout preview;
    FacialProcessing faceProc;
    FaceData[] faceArray = null;
    View myView;
    Canvas canvas = new Canvas();
    private CameraSurfacePreview mPreview;
    private DrawView drawView;
    private final int FRONT_CAMERA_INDEX = 1;

    boolean fpFeatureSupported = false;
    boolean info = true;
    boolean landScapeMode = false;

    int cameraIndex;// Integer to keep track of which camera is open.
    int smileValue = 0;
    int leftEyeBlink = 0;
    int rightEyeBlink = 0;
    int faceRollValue = 0;
    int pitch = 0;
    int yaw = 0;
    int horizontalGaze = 0;
    int verticalGaze = 0;
    PointF gazePointValue = null;
    private final String TAG = "CameraPreviewActivity";

    // TextView Variables
    TextView numFaceText, smileValueText, leftBlinkText, rightBlinkText, gazePointText, faceRollText, faceYawText,
    facePitchText, horizontalGazeText, verticalGazeText, gazePoint;

    ImageView alertIcon;

    // Progress bar
    RoundCornerProgressBar progressBar;

    int surfaceWidth = 0;
    int surfaceHeight = 0;

    OrientationEventListener orientationEventListener;
    int deviceOrientation;
    int presentOrientation;
    Display display;
    int displayAngle;

    private static final int TIMES_TO_CALIBRATE = 25;
    boolean calibrating = true;
    int calibratedTimes = 0;
    Handler handler;
    Runnable periodicTask;
    static final long TASK_PERIOD = 200;

    final static int COLOR_20 = Color.parseColor("#99e527");
    final static int COLOR_40 = Color.parseColor("#dfe527");
    final static int COLOR_60 = Color.parseColor("#e5a927");
    final static int COLOR_80 = Color.parseColor("#e56627");
    final static int COLOR_100 = Color.parseColor("#e52727");

    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String PREFS_NAME = "Preferencias";

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //TODO: PARA CAMBIAR ENTRE PRIMER PLANO O SEGUNDO PLANO
        //Primer Plano
        setContentView(R.layout.activity_camera_preview);
        //Segundo Plano
        //setContentView(R.layout.activity_camera_preview_background);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);


        if (settings.getBoolean("firstTime", true)) {
            // The app is being launched for first time, do something
            // first time task
            ProgressBar mProgress;
            mProgress = (ProgressBar) findViewById(R.id.calibrating_circle);
            TextView loading = (TextView) findViewById(R.id.calibrating_text);
            loading.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.VISIBLE);

            // record the fact that the app has been started at least once
            settings.edit().putBoolean("firstTime", false).commit();
        }

        myView = new View(CameraPreviewActivity.this);
        // Create our Preview view and set it as the content of our activity.
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        numFaceText = (TextView) findViewById(R.id.numFaces);
        smileValueText = (TextView) findViewById(R.id.smileValue);
        rightBlinkText = (TextView) findViewById(R.id.rightEyeBlink);
        leftBlinkText = (TextView) findViewById(R.id.leftEyeBlink);
        faceRollText = (TextView) findViewById(R.id.faceRoll);
        gazePointText = (TextView) findViewById(R.id.gazePoint);
        faceYawText = (TextView) findViewById(R.id.faceYawValue);
        facePitchText = (TextView) findViewById(R.id.facePitchValue);
        horizontalGazeText = (TextView) findViewById(R.id.horizontalGazeAngle);
        verticalGazeText = (TextView) findViewById(R.id.verticalGazeAngle);
        progressBar = (RoundCornerProgressBar) findViewById(R.id.progressBar);
        gazePoint = (TextView) findViewById(R.id.gazePoint);

        alertIcon = (ImageView) findViewById(R.id.alertIcon);

        mp = MediaPlayer.create(this,R.raw.alarm);

        handler = new Handler();
        periodicTask = new Runnable(){

           @Override
           public void run(){
               // Save params
               FaceRecogHelper.addLeftEyeMeasure(leftEyeBlink);
               FaceRecogHelper.addRightEyeMeasure(rightEyeBlink);
               FaceRecogHelper.addRollMeasure(faceRollValue);
               FaceRecogHelper.addPitchMeasure(pitch);
               FaceRecogHelper.addYawMeasure(yaw);

               Log.d(TAG, "calibrating: " + calibrating);
               // Check if frame is used to calibrate
               if (calibrating){
                   calibratedTimes+=1;
                   Log.d(TAG, "calibratedTimes: " + calibratedTimes);
                   if (calibratedTimes == TIMES_TO_CALIBRATE){
                       // Got all the necessary frames
                       FaceRecogHelper.setAllMeasureReferences();
                       calibrating = false;
                   }
               }
               else{
                   // Check whether an alert should be popped up
                   List<Object> alertInfo = FaceRecogHelper.getAlertLevel();
                   changeProgress(progressBar, ((Double) alertInfo.get(1)).intValue());
                   Log.d(TAG, "alert: " + (boolean) alertInfo.get(0));
                   if ((boolean) alertInfo.get(0)){
                       // Pop up an alert
                       if (!mp.isPlaying()) {
                           Log.d(TAG, "starting sound");
                           mp.start();
                       }
                       alertIcon.setVisibility(View.VISIBLE);
                   }
                   else{
                       // Stop alert
                       if (mp.isPlaying()) {
                           Log.d(TAG, "stopping sound");
                           mp.stop();
                           try {
                               mp.prepare();
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                       }
                       alertIcon.setVisibility(View.INVISIBLE);
                   }
               }
               // Delay N millis
               handler.postDelayed(periodicTask, TASK_PERIOD);
           }
        };

        // Check to see if the FacialProc feature is supported in the device or no.
        fpFeatureSupported = FacialProcessing.isFeatureSupported(FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_PROCESSING);

        if (fpFeatureSupported && faceProc == null) {
            Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();  // Calling the Facial Processing Constructor.
            faceProc.setProcessingMode(FP_MODES.FP_MODE_VIDEO);
        } else {
            Log.e("TAG", "Feature is NOT supported");
            return;
        }

        cameraIndex = FRONT_CAMERA_INDEX;// Start with front Camera

        try {
            cameraObj = Camera.open(cameraIndex); // attempt to get a Camera instance
            //CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
           // String[] cams = manager.getCameraIdList();
        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        // Change the sizes according to phone's compatibility.
        mPreview = new CameraSurfacePreview(CameraPreviewActivity.this, cameraObj, faceProc);
        preview.removeView(mPreview);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(CameraPreviewActivity.this);

        // Action listener for the Switch Camera Button.
        ButtonsListeners();

        orientationListener();

        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        // Start the periodic task
        handler.post(periodicTask);
    }

    private void orientationListener() {
        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                deviceOrientation = orientation;
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        presentOrientation = 90 * (deviceOrientation / 360) % 360;
    }

    private void ButtonsListeners() {
        ImageView openSettingsButton = (ImageView) findViewById(R.id.openSettingsButton);
        openSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                changeProgress(progressBar, 10);
            }
        });
        ImageView recalibrateButton = (ImageView) findViewById(R.id.openRecalibrateButton);
        recalibrateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                changeProgress(progressBar, 0);
                calibratedTimes = 0;
                calibrating = true;
            }
        });
        ImageView viewDataButton = (ImageView) findViewById(R.id.openViewDataButton);
        viewDataButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                info = !info;
                if(info){
                    setUIData(View.VISIBLE);
                }
                else{
                    setUIData(View.INVISIBLE);
                }
            }
        });
    }

    private void changeProgress (RoundCornerProgressBar pg, int percentage){
        pg.setProgress( (float) percentage);
        if (percentage <= 20){
            pg.setProgressColor(COLOR_20);
        }
        else if (percentage <= 40){
            pg.setProgressColor(COLOR_40);
        }
        else if (percentage <= 60){
            pg.setProgressColor(COLOR_60);
        }
        else if (percentage <= 80){
            pg.setProgressColor(COLOR_80);
        }
        else{
            pg.setProgressColor(COLOR_100);
        }
    }

    private void setUIData(int type){
        numFaceText.setVisibility(type);
        smileValueText.setVisibility(type);
        leftBlinkText.setVisibility(type);
        rightBlinkText.setVisibility(type);
        faceRollText.setVisibility(type);
        faceYawText.setVisibility(type);
        facePitchText.setVisibility(type);
        horizontalGazeText.setVisibility(type);
        verticalGazeText.setVisibility(type);
        gazePoint.setVisibility(type);
    }

    public void setUI(int numFaces, int smileValue, int leftEyeBlink, int rightEyeBlink, int faceRollValue,
            int faceYawValue, int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle) {
        numFaceText.setText("Number of Faces: " + numFaces);
        smileValueText.setText("Smile Value: " + smileValue);
        leftBlinkText.setText("Left Eye Blink Value: " + leftEyeBlink);
        rightBlinkText.setText("Right Eye Blink Value " + rightEyeBlink);
        faceRollText.setText("Face Roll Value: " + faceRollValue);
        faceYawText.setText("Face Yaw Value: " + faceYawValue);
        facePitchText.setText("Face Pitch Value: " + facePitchValue);
        horizontalGazeText.setText("Horizontal Gaze: " + horizontalGazeAngle);
        verticalGazeText.setText("VerticalGaze: " + verticalGazeAngle);

        if (gazePointValue != null) {
            double x = Math.round(gazePointValue.x * 100.0) / 100.0;// Rounding the gaze point value.
            double y = Math.round(gazePointValue.y * 100.0) / 100.0;
            gazePointText.setText("Gaze Point: (" + x + "," + y + ")");
        } else {
            gazePointText.setText("Gaze Point: ( , )");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraObj != null) {
            stopCamera();
        }

        startCamera(FRONT_CAMERA_INDEX);
    }

    public void stopCamera() {
        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview.removeView(mPreview);
            cameraObj.release();
            faceProc.release();
            faceProc = null;
        }
        cameraObj = null;
    }

    public void startCamera(int cameraIndex) {
        if (fpFeatureSupported && faceProc == null) {
            Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();
        }

        try {
            cameraObj = Camera.open(cameraIndex);
        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");
        }

        mPreview = new CameraSurfacePreview(CameraPreviewActivity.this, cameraObj, faceProc);
        preview.removeView(mPreview);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(CameraPreviewActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camera_preview, menu);
        return true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera arg1) {

        presentOrientation = (90 * Math.round(deviceOrientation / 90)) % 360;
        int dRotation = display.getRotation();
        PREVIEW_ROTATION_ANGLE angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;

        switch (dRotation) {
        case 0:
            displayAngle = 270;
            angleEnum = PREVIEW_ROTATION_ANGLE.ROT_270;
            break;
        case 1:
            displayAngle = 180;
            angleEnum = PREVIEW_ROTATION_ANGLE.ROT_180;
            break;
        case 2:
            // This case is never reached.
            break;
        case 3:
            displayAngle = 0;
            angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;
            break;
        }

        if (faceProc == null) {
            faceProc = FacialProcessing.getInstance();
        }

        Parameters params = cameraObj.getParameters();
        Size previewSize = params.getPreviewSize();
        surfaceWidth = mPreview.getWidth();
        surfaceHeight = mPreview.getHeight();

        // Landscape mode - front camera
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = true;
        }
        // Portrait mode - front camera
        else  {
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = false;
        }

        int numFaces = faceProc.getNumFaces();

        if (numFaces == 0) {
            Log.d("TAG", "No Face Detected");
            if (drawView != null) {
                preview.removeView(drawView);
                drawView = new DrawView(this, null, false, 0, 0, null, landScapeMode);
                preview.addView(drawView);
            }
            canvas.drawColor(0, Mode.CLEAR);
            setUI(0, 0, 0, 0, 0, 0, 0, null, 0, 0);
            calibratedTimes = 0;
        } else {
            Log.d("TAG", "Face Detected");
            faceArray = faceProc.getFaceData(EnumSet.of(FacialProcessing.FP_DATA.FACE_RECT,
                    FacialProcessing.FP_DATA.FACE_COORDINATES, FacialProcessing.FP_DATA.FACE_CONTOUR,
                    FacialProcessing.FP_DATA.FACE_SMILE, FacialProcessing.FP_DATA.FACE_ORIENTATION,
                    FacialProcessing.FP_DATA.FACE_BLINK, FacialProcessing.FP_DATA.FACE_GAZE));
            if (faceArray == null) {
                Log.e("TAG", "Face array is null");
            } else {
                if (faceArray[0].leftEyeObj == null) {
                    Log.e(TAG, "Eye Object NULL");
                } else {
                    Log.e(TAG, "Eye Object not NULL");
                }

                faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);
                preview.removeView(drawView);
                drawView = new DrawView(this, faceArray, true, surfaceWidth, surfaceHeight, cameraObj, landScapeMode);
                preview.addView(drawView);

                for (int j = 0; j < numFaces; j++) {
                    smileValue = faceArray[j].getSmileValue();
                    leftEyeBlink = faceArray[j].getLeftEyeBlink();
                    rightEyeBlink = faceArray[j].getRightEyeBlink();
                    faceRollValue = faceArray[j].getRoll();
                    gazePointValue = faceArray[j].getEyeGazePoint();
                    pitch = faceArray[j].getPitch();
                    yaw = faceArray[j].getYaw();
                    horizontalGaze = faceArray[j].getEyeHorizontalGazeAngle();
                    verticalGaze = faceArray[j].getEyeVerticalGazeAngle();
                }

                if(info){
                    setUI(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue, yaw, pitch, gazePointValue, horizontalGaze, verticalGaze);
                }

                // Send notification to the driver if there is a symptom
                // if ( ... )
                /*NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                mBuilder.setSmallIcon(R.drawable.ic_launcher);
                mBuilder.setContentTitle("Custom title");
                mBuilder.setContentText("Custom text");
                // Customize notification
                int mNotificationId = 1;
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());*/
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        // exit activity
        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            handler.removeCallbacks(periodicTask);
            stopCamera();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
