/*
 * LCARS Live Wallpaper
 * A live wallpaper for the Android platform.  Displays the 
 * familiar ST:TNG LCARS interface on your background with 
 * several live system stats.
 * 
 * Author: cedarrapidsboy
 * 
 * 
 */

package com.example.android.maxpapers.lcars;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class LCARSWallpaper extends WallpaperService {

	private final Handler mHandler = new Handler();
	private final float TEXT_LARGE = 24f;
	private final float TEXT_MEDIUM = 16f;
	private final float TEXT_SMALL = 12f;

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
		private final int MAX_MODE = 1;
		private final Paint bitmapPaint = new Paint();
		private final Paint usagePaint = new Paint();
		private final Paint uptimePaint = new Paint();
		private final Paint buttonPaint = new Paint();
		private final Paint processPaint = new Paint();
		private final Paint powerPaint = new Paint();
		private final Paint electronPaint = new Paint();
		private float mPixels;
		private float mTouchX;
		private float mTouchY;
		private Bitmap lcars;
		private Bitmap lcars_land;
		private Bitmap deuterium;
		private MemoryThread memThread;
		private StatsThread statsThread;
		private boolean isPortrait;
		private int mode;
		private String level;
		private String eV;
		private String status;
		private int framerate = 1000;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				drawFrame();
			}
		};


		private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				level = String.valueOf(intent.getIntExtra(
						BatteryManager.EXTRA_LEVEL, 0)) + "%";
				int s = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
				switch (s) {
				case BatteryManager.BATTERY_STATUS_CHARGING:
					status = "Refilling deuterium";
					break;
				case BatteryManager.BATTERY_STATUS_DISCHARGING:
					status = "Deuterium flow normal";
					break;
				case BatteryManager.BATTERY_STATUS_FULL:
					status = "Deuterium tanks full";
					break;
				case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
					status = "Deuterium refill stopped";
					break;
				case BatteryManager.BATTERY_STATUS_UNKNOWN:
					status = "Deuterium status unknown";
					break;

				}
				int i = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
				eV = String.valueOf(DateCalc.roundToDecimals(i / 1000d, 2));
			}
		};
		private boolean mVisible;

		CubeEngine() {
			registerReceiver(batteryReceiver, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
			Resources res = getResources();
			lcars = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars);
			deuterium = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.deuterium);
			lcars_land = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars_land);
			memThread = new MemoryThread(
					(ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE),
					10000);
			statsThread = new StatsThread(1000);
			// Create a Paint to draw the lines for our cube
			final Paint paint = bitmapPaint;
			final Paint text_paint = usagePaint;
			final Paint ul_paint = uptimePaint;
			final Paint b_paint = buttonPaint;
			final Paint p_paint = processPaint;
			final Paint e_paint = electronPaint;
			final Paint w_paint = powerPaint;
			Typeface font = Typeface.createFromAsset(getAssets(),
					"swiss_ec.ttf");
			text_paint.setTypeface(font);
			text_paint.setTextSize(scale * TEXT_LARGE);
			text_paint.setColor(0xffff9f00);
			text_paint.setAntiAlias(true);
			ul_paint.setTypeface(font);
			ul_paint.setTextSize(scale * TEXT_MEDIUM);
			ul_paint.setColor(0xffff9f00);
			ul_paint.setAntiAlias(true);
			ul_paint.setTextAlign(Align.RIGHT);
			b_paint.setTypeface(font);
			b_paint.setTextSize(scale * TEXT_SMALL);
			b_paint.setColor(0xff000000);
			b_paint.setAntiAlias(true);
			b_paint.setTextAlign(Align.RIGHT);

			e_paint.setColor(0xffff9f00);
			e_paint.setAntiAlias(true);

			p_paint.setTypeface(font);
			p_paint.setTextSize(scale * TEXT_SMALL);
			p_paint.setColor(0xff9f9fff);
			p_paint.setAntiAlias(true);
			p_paint.setTextAlign(Align.RIGHT);

			w_paint.setTypeface(font);
			w_paint.setTextSize(scale * TEXT_MEDIUM);
			w_paint.setColor(0xff9f9fff);
			w_paint.setAntiAlias(true);
			w_paint.setTextAlign(Align.RIGHT);

			// text_paint.setStrokeWidth(2);
			text_paint.setStrokeCap(Paint.Cap.ROUND);
			text_paint.setStyle(Paint.Style.STROKE);
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
			mode = 0;

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
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mTouchX = event.getX();
				mTouchY = event.getY();
				if (mTouchX >= mPixels + (487 * scale)
						&& mTouchX <= mPixels + (577 * scale)
						&& mTouchY >= (141 * scale) && mTouchY <= (164 * scale)) {
					mode++;
					if (mode > MAX_MODE) {
						mode = 0;
					}
					framerate = 1000;
					if (mode == 1){
						framerate = 100;
					}
					// Reschedule the next redraw
					mHandler.removeCallbacks(mDrawCube);
					if (mVisible) {
						mHandler.post(mDrawCube);
					}
				}
			} else {
				mTouchX = -1;
				mTouchY = -1;
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
						if (mode == 0) {
							drawProcText(c);
						} else if (mode == 1) {
							drawAtom(c);
							drawPowerStats(c);
						}
					}
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			// Reschedule the next redraw
			mHandler.removeCallbacks(mDrawCube);
			if (mVisible) {
				mHandler.postDelayed(mDrawCube, framerate);
			}
		}

		void drawBitmap(Canvas c) {
			if (!isPortrait) {
				c.drawBitmap(lcars_land, mPixels, 0, bitmapPaint);
			} else {
				c.drawBitmap(lcars, mPixels, 0, bitmapPaint);
			}
		}

		void drawPowerStats(Canvas c) {
			int h = Math.round(scale * 320);
			powerPaint.setTextSize(scale * TEXT_LARGE);
			c.drawText("STATUS:", mPixels + (scale * 625), h, powerPaint);
			h += (scale * 17);
			powerPaint.setTextSize(scale * TEXT_MEDIUM);
			c.drawText(status, mPixels + (scale * 625), h, powerPaint);
			h += (scale * 36);
			powerPaint.setTextSize(scale * TEXT_LARGE);
			c.drawText("LEVEL:", mPixels + (scale * 625), h, powerPaint);
			h += (scale * 17);
			powerPaint.setTextSize(scale * TEXT_MEDIUM);
			c.drawText(level, mPixels + (scale * 625), h, powerPaint);
			h += (scale * 36);
			powerPaint.setTextSize(scale * TEXT_LARGE);
			c.drawText("ENERGY:", mPixels + (scale * 625), h, powerPaint);
			// h += (scale * 17);
			powerPaint.setTextSize(scale * TEXT_MEDIUM);
			// c.drawText(dynCm + " dyn/cm", mPixels + (scale * 625), h,
			// wPaint);
			h += (scale * 17);
			c.drawText(eV + " V", mPixels + (scale * 625), h, powerPaint);
		}

		void drawAtom(Canvas c) {
			if (isPortrait) {
				long time = new Date().getTime() / 100;
				c.drawBitmap(deuterium, mPixels + (scale * 487), (scale * 173),
						bitmapPaint);
				Double x1 = new Double(mPixels + (scale * (834 / 1.5))
						+ (scale * (83 / 1.5))
						* Math.cos((time % 360) * (Math.PI / 180)));
				Double y1 = new Double((scale * (356 / 1.5))
						+ (scale * (83 / 1.5))
						* Math.sin((time % 360) * (Math.PI / 180)));
				electronPaint.setColor(0xffffffff);
				c.drawCircle(x1.floatValue(), y1.floatValue(), 8, electronPaint);
				electronPaint.setColor(0xffff9f00);
				c.drawCircle(x1.floatValue(), y1.floatValue(), 6, electronPaint);
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

			c.drawText(sSpeed, mPixels + (scale * 625), (scale * 161),
					buttonPaint);

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
				c.drawText(toks[0], mPixels + (scale * 548), h, processPaint);
				c.drawText(toks[1], mPixels + (scale * 575), h, processPaint);
				c.drawText(toks[2], mPixels + (scale * 625), h, processPaint);
				h += (scale * 17);
			}
			for (int i = 0; i < 23 && i < old_apps.length; i++) {
				String[] toks = old_apps[i].split(";");
				if (!list.contains(new Integer(toks[1]))) {
					c.drawText(toks[0], mPixels + (scale * 548), h,
							processPaint);
					c.drawText(toks[1], mPixels + (scale * 575), h,
							processPaint);
					c.drawText(toks[2], mPixels + (scale * 625), h,
							processPaint);
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
					usagePaint);
			// c.drawText(dur + " " + sDays + " " + sHours + ":" + sMins + ":" +
			// sSecs, mPixels + 222, 69, ulPaint);
			c.drawText(dur, mPixels + (scale * 87), (scale * 46), uptimePaint);
			c.drawText(sDays, mPixels + (scale * 107), (scale * 46),
					uptimePaint);
			c.drawText(sHours + ":" + sMins + ":" + sSecs, mPixels
					+ (scale * 160), (scale * 46), uptimePaint);
			c.drawText(sd, mPixels + (scale * 87), (scale * 63), uptimePaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 63), uptimePaint);
			c.drawText(String.valueOf(DateCalc.stardate()), mPixels
					+ (scale * 160), (scale * 63), uptimePaint);
			c.drawText("TER", mPixels + (scale * 87), (scale * 81), uptimePaint);
			c.drawText(sThours, mPixels + (scale * 107), (scale * 81),
					uptimePaint);
			c.drawText(sTerran, mPixels + (scale * 160), (scale * 81),
					uptimePaint);
			c.drawText("TTC", mPixels + (scale * 87), (scale * 98), uptimePaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 98), uptimePaint);
			c.drawText(sTTC, mPixels + (scale * 160), (scale * 98), uptimePaint);

		}

	}
}
