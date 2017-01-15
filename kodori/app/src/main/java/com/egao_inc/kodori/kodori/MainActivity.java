package com.egao_inc.kodori.kodori;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.egao_inc.kodori.kodori.ui.camera.CameraSourcePreview;
import com.egao_inc.kodori.kodori.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Landmark;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 * 顔追跡アプリのための活動。このアプリは、カメラを後ろ向きで顔を検出し、
 * それぞれの顔の位置、大きさ、およびIDを示すために、オーバーレイグラフィックスを描画
 */
public final class MainActivity extends AppCompatActivity {

    private static final String TAG         = "FaceTracker";

    private CameraSource mCameraSource      = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS  = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    final Handler handler = new Handler();
    private static final int kTAG_MOUTH  = 200;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     * UIを初期化し、顔検出器の作成を開始
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        // イメージを非表示
        ImageView imageView = (ImageView)findViewById(R.id.mouth_01);
        imageView.setVisibility(View.GONE);
    }

    public void onClickShutter(View view) {
        Log.w(TAG, "Face　shutter!!");
        takeShot();
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     * カメラの権限を要求して処理
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * カメラの起動
     * 長い距離で小さなバーコードを検出するために、バーコード検出を有効にするには、他の検出例と比較して、
     * より高い解像度を使用します
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .build();

//        detector.setProcessor(
//                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
//                        .build());

        GraphicFaceTrackerFactory faceFactory = new GraphicFaceTrackerFactory();
        faceFactory.owner = this;
        detector.setProcessor(
                new MultiProcessor.Builder<>(faceFactory)
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     * カメラの再起動
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     * カメラの停止
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     * カメラ関連の解放
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     * 開始時、カメラソースを再起動
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void takeShot() {
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {
                mPreview.stop();
                File shot = null;
                try {
                    shot = FileUtils.saveImage(bytes, "jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shot));
                startActivity(Intent.createChooser(shareIntent, ""));
            }
        });
    }

    public void  onUpdateLandmarks(List<Landmark> langMarks)
    {
        PointF mouthRightNosePoint = new PointF(0,0);
        PointF mouthLeftNosePoint = new PointF(0,0);
        for (Landmark land : langMarks)
        {
            String str;
            switch (land.getType())
            {
                case Landmark.LEFT_EYE:
                    str = "LEFT_EYE";
                    break;
                case Landmark.LEFT_MOUTH:
                    mouthLeftNosePoint = land.getPosition();
                    str = "LEFT_MOUTH";
                    break;
                case Landmark.RIGHT_EYE:
                    str = "RIGHT_EYE";
                    break;
                case Landmark.RIGHT_MOUTH:
                {
                    str = "RIGHT_MOUTH";
                    mouthRightNosePoint = land.getPosition();
                    // mouth_01



//                    Log.e(TAG, "imageViewMouth = x:" + imageView.getTranslationX() + " y:" + imageView.getTranslationY());

//                    Log.e(TAG, "type =" + str + " x:" + mouthNosePoint.x + " y:" + mouthNosePoint.y);
                    // log type =RIGHT_MOUTH x:92.31844 y:284.27118
                }

                    break;
                case Landmark.NOSE_BASE:
                    str = "NOSE_BASE";
                    break;
                default:
                    str = "default";
                    break;
            }

//            PointF p = land.getPosition();
//            Log.e(TAG, "type =" + str + " x:" + p.x + " y:" + p.y);
        }

//        if (mouthRightNosePoint.x != 0 && mouthLeftNosePoint.x != 0)
//        {
//
//        }

//        //画像の表示
//            Log.e(TAG, "right x =" + mouthRightNosePoint.x + " left x:" + mouthLeftNosePoint.x );
//            float mouthWidth = mouthLeftNosePoint.x - mouthRightNosePoint.x;
//            float mouthX = mouthRightNosePoint.x + mouthWidth / 2;
//            float mouthY = mouthRightNosePoint.y + (mouthLeftNosePoint.y - mouthRightNosePoint.y) / 2;
//            ImageView imageView = (ImageView)findViewById(R.id.mouth_01);
//
//            float imageScale = mouthWidth / imageView.getWidth();
//            Log.e(TAG, "mouth width=" + mouthWidth + " resources width=:" + imageView.getWidth() + " scale:" + imageScale );
//            imageView.setScaleX(imageScale);
//            imageView.setScaleY(imageScale);
//            imageView.setTranslationX(mouthX -250);
//            imageView.setTranslationY(mouthY - 140);

    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     * 顔トラッカーを作成するためのファクトリは、新しい顔に関連付けられます。
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        public  MainActivity owner;
        @Override
        public Tracker<Face> create(Face face)
        {
//            return new GraphicFaceTracker(mGraphicOverlay);
            GraphicFaceTracker graphicFaceTracker = new GraphicFaceTracker(mGraphicOverlay);
            graphicFaceTracker.owner = owner;
            return graphicFaceTracker;
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     * 検出された各個人の顔トラッカー
     * これは顔グラフィックを保持
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        public  MainActivity owner;
        private static final double SMILING_THRESHOLD = 0.4;
        private static final double WINK_THRESHOLD = 0.5;
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         * 顔オーバーレイ内で検出された顔のインスタンスを追跡を開始
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         * オーバーレイ内の顔の位置を更新
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            boolean isSmiling = face.getIsSmilingProbability() > SMILING_THRESHOLD;
            if (isSmiling) {
                float leftEye = face.getIsLeftEyeOpenProbability();
                float rightEye = face.getIsRightEyeOpenProbability();
                Log.e(TAG, "log face leftEye" + leftEye + " rightEye" + rightEye);
//                if (Math.abs(leftEye - rightEye) >= WINK_THRESHOLD) {
//                    takeShot();
//                }
            }

            mFaceGraphic.setIsReady(isSmiling);
            mFaceGraphic.updateFace(face);

            if (owner != null){
                owner.onUpdateLandmarks(mFaceGraphic.getLandmarks());
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         * 対応する顔が検出されなかったときにグラフィックを非表示にします。
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         * 顔が消えるときに呼ばれる
         * オーバーレイからグラフィック注釈を削除します。
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

    }
}

