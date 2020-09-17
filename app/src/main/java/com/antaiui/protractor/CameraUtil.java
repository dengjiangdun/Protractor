package com.antaiui.protractor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class CameraUtil {
    private CameraDevice mCameraDevice;
    private HandlerThread mHandlerThread;
    private Handler mBgHandler;
    private CameraCaptureSession mSession;
    private Context mContext;
    private boolean mIsPreview;
    private final String TAG = "CameraUtil";
    private boolean mPause = false;
    private CaptureRequest.Builder mBuilder;
    private String mCameraId;

    public CameraUtil(Context context) {
        mContext = context;
    }

    public void setPause(boolean pause) {
        mPause = pause;
    }

    public void openCamera(final SurfaceTexture surfaceTexture, String cameraId) {
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mCameraId = cameraId;
        mHandlerThread = new HandlerThread("camera thread");
        mHandlerThread.start();
        mBgHandler = new Handler(mHandlerThread.getLooper());
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    Log.d(TAG, "onOpened");
                    if (mPause) {
                        camera.close();
                        Log.d(TAG, " onOpened pause = " + mPause);
                        return;
                    }
                    startPreview(surfaceTexture);


                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onDisconnected");

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "onError error = " + error);
                }
            }, mBgHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startPreview(final SurfaceTexture surfaceTexture) {
        Log.d(TAG, "startPreview mIsPreview = " + mIsPreview);
        if (mIsPreview) {
            return;
        }
        List<Surface> surfaceList = new ArrayList<>();
        final Surface surface = new Surface(surfaceTexture);
        surfaceList.add(surface);
        try {
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured");
                    mSession = session;
                    try {
                        if (mPause) {
                            Log.d(TAG, "onConfigured mpause = " + mPause);
                            mSession.close();
                            mCameraDevice.close();
                            return;
                        }
                        mBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mBuilder.addTarget(surface);
                        mBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY);
                        mBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);
                        mBuilder.set(CaptureRequest.CONTROL_AF_MODE,  CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        mSession.setRepeatingRequest(mBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);

                                //Log.d(TAG, "onCaptureStarted");
                            }
                        }, mBgHandler);
                        mIsPreview = true;
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigureFailed ");
                }
            }, mBgHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    public void setTouchState(boolean on) {
        mBuilder.set(CaptureRequest.FLASH_MODE, on ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
        try {
            mSession.setRepeatingRequest(mBuilder.build(), null, mBgHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview mIsPreview = " + mIsPreview);
        if (!mIsPreview) {
            return;
        }
        try {
            mSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mSession.close();
        mCameraDevice.close();
        mIsPreview = false;
    }


    private Rect mSensorRect = null;
    public void updateZoom(float zoomRatio) {
        if (mSensorRect == null) {
            CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                mSensorRect = cameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        Rect previewRect = zoomRatio == 0f ? mSensorRect : getPreviewRect(zoomRatio);
        mBuilder.set(CaptureRequest.SCALER_CROP_REGION, previewRect);
        try {
            mSession.setRepeatingRequest(mBuilder.build(), null, mBgHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "updateZoom zoomRatio = "  + zoomRatio);

    }

    private Rect getPreviewRect(float zoomRatio) {
        int centerX = mSensorRect.width() / 2;
        int centerY = mSensorRect.height() / 2;
        int deltaX = (int) (centerX / zoomRatio);
        int deltaY = (int) (centerY / zoomRatio);
        return new Rect(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);
    }

    public static float getAvailableMaxDigitalZoom(Context context, String cameraId) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            Float value = cameraManager.getCameraCharacteristics(cameraId).
                    get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (value != null) {
                return value;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return 0f;
    }

}
