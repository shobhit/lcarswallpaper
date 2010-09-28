/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.example.android.maxpapers.lcars;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class LCARSWallpaper extends WallpaperService {

	private final Handler mHandler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		return new CubeEngine();
	}

	class CubeEngine extends Engine {
		final float scale = getResources().getDisplayMetrics().density;

		private final Paint mPaint = new Paint();
		private final Paint tPaint = new Paint();
		private final Paint ulPaint = new Paint();
		private final Paint bPaint = new Paint();
		private final Paint pPaint = new Paint();
		private float mPixels;
		private Bitmap lcars;
		private Bitmap lcars_land;
		private MemoryThread memThread;
		private StatsThread statsThread;
		ArrayList<String[]> dirs = new ArrayList<String[]>();
		ArrayList<String[]> temp = new ArrayList<String[]>();
		boolean isPortrait;
		WindowManager win;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		private boolean mVisible;

		CubeEngine() {
			win = (WindowManager) getSystemService(WINDOW_SERVICE);
			Resources res = getResources();
			lcars = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars);
			lcars_land = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars_land);
			memThread = new MemoryThread(
					(ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE),
					10000);
			statsThread = new StatsThread(1000);
			// Create a Paint to draw the lines for our cube
			final Paint paint = mPaint;
			final Paint text_paint = tPaint;
			final Paint ul_paint = ulPaint;
			final Paint b_paint = bPaint;
			final Paint p_paint = pPaint;
			Typeface font = Typeface.createFromAsset(getAssets(),
					"swiss_ec.ttf");
			text_paint.setTypeface(font);
			text_paint.setTextSize(scale * 24f);
			text_paint.setColor(0xffff9f00);
			text_paint.setAntiAlias(true);
			ul_paint.setTypeface(font);
			ul_paint.setTextSize(scale * 16f);
			ul_paint.setColor(0xffff9f00);
			ul_paint.setAntiAlias(true);
			ul_paint.setTextAlign(Align.RIGHT);
			b_paint.setTypeface(font);
			b_paint.setTextSize(scale * 12f);
			b_paint.setColor(0xff000000);
			b_paint.setAntiAlias(true);
			b_paint.setTextAlign(Align.RIGHT);
			p_paint.setTypeface(font);
			p_paint.setTextSize(scale * 12f);
			p_paint.setColor(0xff9f9fff);
			p_paint.setAntiAlias(true);
			p_paint.setTextAlign(Align.RIGHT);

			// text_paint.setStrokeWidth(2);
			text_paint.setStrokeCap(Paint.Cap.ROUND);
			text_paint.setStyle(Paint.Style.STROKE);
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);

		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDrawCube);
			memThread.stopCollection();
			statsThread.stopCollection();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				memThread.resumeCollection();
				statsThread.resumeCollection();
				drawFrame();
			} else {
				mHandler.removeCallbacks(mDrawCube);
				memThread.pauseCollection();
				statsThread.pauseCollection();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			isPortrait = true;
			if (width > height) {
				isPortrait = false;
			}
			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			memThread.start();
			statsThread.start();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawCube);
			memThread.stopCollection();
			statsThread.stopCollection();
			boolean retry = true;
			while (retry) {
				try {
					memThread.join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}
			while (retry) {
				try {
					statsThread.join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			mPixels = xPixels;
			drawFrame();
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				event.getX();
				event.getY();
			} else {
			}
			super.onTouchEvent(event);
		}

		void drawFrame() {
			drawFrame(true);
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		void drawFrame(boolean positionChanged) {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					// draw something
					if (positionChanged) {
						drawBitmap(c);
					}
					// drawCube(c);
					// drawTouchPoint(c);
					if (isPortrait) {
						drawText(c);
						drawButtonText(c);
						drawProcText(c);
					}
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			// Reschedule the next redraw
			mHandler.removeCallbacks(mDrawCube);
			if (mVisible) {
				mHandler.postDelayed(mDrawCube, 1000);
			}
		}

		void drawBitmap(Canvas c) {
			if (!isPortrait) {
				c.drawBitmap(lcars_land, mPixels, 0, mPaint);
			} else {
				c.drawBitmap(lcars, mPixels, 0, mPaint);
			}
		}

		void drawButtonText(Canvas c) {
			int speed = Math.round(statsThread.getSpeed());
			String sSpeed = "0000";
			if (speed < 10) {
				sSpeed = "000" + String.valueOf(speed);
			} else if (speed < 100) {
				sSpeed = "00" + String.valueOf(speed);
			} else if (speed < 1000) {
				sSpeed = "0" + String.valueOf(speed);
			} else {
				sSpeed = String.valueOf(Math.round(speed));
			}

			c.drawText(sSpeed, mPixels + (625 * scale), (scale * 161), bPaint);

		}

		void drawProcText(Canvas c) {

			int h = Math.round(scale * 177);
			String[] old_services = memThread.getServices();
			String[] old_apps = memThread.getApps();
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < (23 - old_apps.length)
					&& i < old_services.length; i++) {
				String[] toks = old_services[i].split(";");
				list.add(new Integer(toks[1]));
				c.drawText(toks[0], mPixels + (scale * 548), h, pPaint);
				c.drawText(toks[1], mPixels + (scale * 575), h, pPaint);
				c.drawText(toks[2], mPixels + (scale * 625), h, pPaint);
				h += (scale * 17);
			}
			for (int i = 0; i < 23 && i < old_apps.length; i++) {
				String[] toks = old_apps[i].split(";");
				if (!list.contains(new Integer(toks[1]))) {
					c.drawText(toks[0], mPixels + 822, h, pPaint);
					c.drawText(toks[1], mPixels + 862, h, pPaint);
					c.drawText(toks[2], mPixels + 937, h, pPaint);
					h += (scale * 17);
				}
			}
		}

		void drawText(Canvas c) {
			long up = statsThread.getUptime();
			int reading = statsThread.getUsage();
			int days = (int) (up / 86400);
			int hours = (int) ((up % 86400) / 3600);
			int minutes = (int) (((up % 86400) % 3600) / 60);
			int seconds = (int) (((up % 86400) % 3600) % 60);
			String sReading = "000";
			if (reading < 10) {
				sReading = "00" + String.valueOf(reading);
			} else if (reading < 100) {
				sReading = "0" + String.valueOf(reading);
			} else {
				sReading = "100";
			}
			String dur = "DUR";
			String sd = "SD";
			String sDays = (days < 10) ? "0" + String.valueOf(days) : String
					.valueOf(days);
			String sHours = (hours < 10) ? "0" + String.valueOf(hours) : String
					.valueOf(hours);
			String sMins = (minutes < 10) ? "0" + String.valueOf(minutes)
					: String.valueOf(minutes);
			String sSecs = (seconds < 10) ? "0" + String.valueOf(seconds)
					: String.valueOf(seconds);
			String sTmonth = (DateCalc.getMonth()) < 10 ? "0"
					+ String.valueOf(DateCalc.getMonth()) : String
					.valueOf(DateCalc.getMonth());
			String sThours = (DateCalc.getHours() < 10) ? "0"
					+ String.valueOf(DateCalc.getHours()) : String
					.valueOf(DateCalc.getHours());
			String sTerran = String.valueOf(DateCalc.getYear()) + sTmonth + "."
					+ String.valueOf(DateCalc.getDecDay());
			String sTTC = String.valueOf(DateCalc.getDaysToFirstContact());
			c.drawText(sReading + "%", mPixels + (scale * 367), (scale * 200),
					tPaint);
			// c.drawText(dur + " " + sDays + " " + sHours + ":" + sMins + ":" +
			// sSecs, mPixels + 222, 69, ulPaint);
			c.drawText(dur, mPixels + (scale * 87), (scale * 46), ulPaint);
			c.drawText(sDays, mPixels + (scale * 107), (scale * 46), ulPaint);
			c.drawText(sHours + ":" + sMins + ":" + sSecs, mPixels
					+ (scale * 160), (scale * 46), ulPaint);
			c.drawText(sd, mPixels + (scale * 87), (scale * 63), ulPaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 63), ulPaint);
			c.drawText(String.valueOf(DateCalc.stardate()), mPixels
					+ (scale * 160), (scale * 63), ulPaint);
			c.drawText("TER", mPixels + (scale * 87), (scale * 81), ulPaint);
			c.drawText(sThours, mPixels + (scale * 107), (scale * 81), ulPaint);
			c.drawText(sTerran, mPixels + (scale * 160), (scale * 81), ulPaint);
			c.drawText("TTC", mPixels + (scale * 87), (scale * 98), ulPaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 98), ulPaint);
			c.drawText(sTTC, mPixels + (scale * 160), (scale * 98), ulPaint);

		}

	}
}
