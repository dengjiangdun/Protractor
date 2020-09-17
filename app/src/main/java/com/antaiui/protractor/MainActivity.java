package com.antaiui.protractor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, ProtractorView.MoveAngleCallBack {
    private CameraUtil mCameraUtil;
    private TextureView mCameraPreview;
    private Size mPreviewSize = new Size(2400, 1080);
    private String mCameraId = "0";
    ProtractorView mProtractorView;
    private TextView mTvAngle;
    private String ANGLE_STRING_FORMAT = "%.2f°";
    private final static int  REQUEST_CAMERA_CODE = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(lp);
        setContentView(R.layout.activity_main);
        mCameraUtil = new CameraUtil(this);
        mCameraPreview = findViewById(R.id.camera_preview);
        mCameraPreview.setSurfaceTextureListener(this);
        mProtractorView = findViewById(R.id.protractor_view);
        mProtractorView.setMoveAngleCallBack(this);
        mTvAngle = findViewById(R.id.tv_angle);
        mTvAngle.setTypeface(Typeface.createFromAsset(getAssets(),  "BatmanForeverAlternate.ttf"));


    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraUtil.setPause(false);
        if (mCameraPreview.getSurfaceTexture() != null) {
            mCameraUtil.openCamera(mCameraPreview.getSurfaceTexture(), mCameraId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraUtil.setPause(true);
        mCameraUtil.stopPreview();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        configureTransform(width, height);
        surface.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        if (grandCameraPermission()) {
            mCameraUtil.openCamera(surface, mCameraId);
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {


    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private boolean grandCameraPermission() {
        return checkCallingOrSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
            mCameraUtil.openCamera(mCameraPreview.getSurfaceTexture(), mCameraId);

        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mCameraPreview.setTransform(matrix);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void angleCallBack(float angle) {
       //mTvAngle.setText(String.format("%.2f", angle) + "°");
        mTvAngle.setText(String.format(ANGLE_STRING_FORMAT, angle));
    }
}
