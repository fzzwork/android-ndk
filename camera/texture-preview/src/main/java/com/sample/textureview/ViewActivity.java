/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.textureview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import junit.framework.Assert;

public class ViewActivity extends Activity
		implements TextureView.SurfaceTextureListener,
		ActivityCompat.OnRequestPermissionsResultCallback {
	private  TextureView textureView_;
	Surface  surface_ = null;
	private  int cameraWidth_;
	private  int cameraHeight_;
    private  int cameraOrientation_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		RequestCamera();
	}

	private void CreateTextureView() {
		textureView_ = (TextureView)findViewById(R.id.texturePreview); //TextureView(this);
		textureView_.setSurfaceTextureListener(this);
	}

	public void onSurfaceTextureAvailable(SurfaceTexture surface,
										  int width, int height) {
		CreatePreviewEngine();

		resizeTextureView(width, height);
		surface.setDefaultBufferSize(cameraWidth_, cameraHeight_);
        surface_ = new Surface(surface);
		notifySurfaceTextureCreated(surface_);
	}

	private void resizeTextureView(int textureWidth, int textureHeight)
	{
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int newWidth = textureWidth;
		int newHeight = textureWidth * cameraWidth_ / cameraHeight_;

		if( Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			newWidth = textureHeight;
			newHeight = (textureHeight * cameraWidth_ / cameraHeight_);
		}
		Matrix matrix = new Matrix();
		RectF viewRect = new RectF(0, 0, newWidth, newHeight);
		float centerX = viewRect.centerX();
		float centerY = viewRect.centerY();
		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			matrix.postRotate(90 * (rotation - 2), centerX, centerY);
		}
		else if (Surface.ROTATION_180 == rotation) {
			matrix.postRotate(180, centerX, centerY);
		}
		textureView_.setTransform(matrix);
		textureView_.setLayoutParams(
				new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
	}

	public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
											int width, int height) {}

	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		notifySurfaceTextureDestroyed(surface_);
		surface_ = null;
		return true;
	}

	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	private static final int PERMISSION_REQUEST_CODE_CAMERA = 1;
	public void RequestCamera() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
					this,
					new String[] { Manifest.permission.CAMERA },
					PERMISSION_REQUEST_CODE_CAMERA);
			return;
		}
		CreateTextureView();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
		if (PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
			super.onRequestPermissionsResult(requestCode,
					permissions,
					grantResults);
			return;
		}

		Assert.assertEquals(grantResults.length, 1);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			CreateTextureView();
		}
	}

	private void CreatePreviewEngine() {
		int rotation = 90 * ((WindowManager)(getSystemService(WINDOW_SERVICE)))
					.getDefaultDisplay()
					.getRotation();
		Display display = getWindowManager().getDefaultDisplay();
		int height = display.getMode().getPhysicalHeight();
		int width = display.getMode().getPhysicalWidth();
        CreateCamera(width, height, rotation);
		cameraWidth_ = GetCameraCompatibleWidth();
		cameraHeight_ = GetCameraCompatibleHeight();
		cameraOrientation_ = GetCameraSensorOrientation();
	}

	private int GetTextureRotationAngle() {

		int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;

		switch(rotation){
			case Surface.ROTATION_0:
				degrees = 0;
				break;

			case Surface.ROTATION_90:
				degrees = 90;
				break;

			case Surface.ROTATION_180:
				degrees = 180;
				break;

			case Surface.ROTATION_270:
				degrees = 270;
				break;

		}

		int result;
		result = (cameraOrientation_ + degrees) % 360;
		result = (360 - result) % 360;

		return result;
	}

	private native void notifySurfaceTextureCreated(Surface surface);
	private native void notifySurfaceTextureDestroyed(Surface surface);
	/*
	 * Create a camera mgr, select back facing camera, and find the best resolution
	 * for display mode. The returned type is Native side object
	 */
	private native long CreateCamera(int width, int height, int rotation);
	private native int  GetCameraCompatibleWidth();
	private native int  GetCameraCompatibleHeight();
    private native int  GetCameraSensorOrientation();

	static {
		System.loadLibrary("camera_textureview");
	}

}