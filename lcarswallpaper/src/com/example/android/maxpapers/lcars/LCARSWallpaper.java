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

import com.example.android.maxpapers.R;

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

	private int[] ships = { R.drawable.lcars, R.drawable.lcars_constitution,
			R.drawable.lcars_defiant, R.drawable.lcars_galaxy_refit,
			R.drawable.lcars_krenim };
	private final Handler mHandler = new Handler();
	private final float TEXT_LARGE = 24f;
	private final float TEXT_MEDIUM = 16f;
	private final float TEXT_SMALL = 12f;
	private final int CAUTION_FR = 100;
	private final int NORMAL_FR = 1000;
	private final int DEFAULT_BACKGROUND = 0;

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
		private Bitmap caution;
		private MemoryThread memThread;
		private StatsThread statsThread;
		private ElectronCalcThread electronThread;
		private boolean isPortrait;
		private int mode;
		private boolean bCaution = false;
		private String level;
		private String eV;
		private String status;
		private int framerate = 1000;
		private int background = DEFAULT_BACKGROUND;
		private Resources res;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int iLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				level = String.valueOf(iLevel) + "%";
				bCaution = (iLevel <= 20);
				framerate = NORMAL_FR;
				if (bCaution) {
					framerate = CAUTION_FR;
				}
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
			res = getResources();
			lcars = BitmapFactory.decodeResource(res, ships[DEFAULT_BACKGROUND]);
			deuterium = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.deuterium);
			caution = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.caution);
			lcars_land = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars_land);
			memThread = new MemoryThread(
					(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE),
					10000);
			statsThread = new StatsThread(1000);
			electronThread = new ElectronCalcThread(0, scale, 33);
			// electronThread.pauseThread();
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
			memThread.stopThread();
			statsThread.stopThread();
			electronThread.stopThread();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				memThread.resumeThread();
				statsThread.resumeThread();
				if (mode == 1) {
					electronThread.resumeThread();
				}
				drawFrame();
			} else {
				mHandler.removeCallbacks(mDrawCube);
				memThread.pauseThread();
				statsThread.pauseThread();
				if (mode == 1) {
					electronThread.pauseThread();
				}
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
			electronThread.start();
			electronThread.pauseThread();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawCube);
			memThread.stopThread();
			statsThread.stopThread();
			electronThread.stopThread();
			boolean retry = true;
			while (retry) {
				try {
					memThread.join();
					statsThread.join();
					electronThread.join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}

		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			mPixels = xPixels;
			electronThread.setmPixels(mPixels);
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
				// WARP EF button
				if (mTouchX >= mPixels + (487 * scale)
						&& mTouchX <= mPixels + (577 * scale)
						&& mTouchY >= (141 * scale) && mTouchY <= (164 * scale)) {
					mode++;
					if (mode > MAX_MODE) {
						mode = 0;
					}
					if (mode == 1) {
						electronThread.resumeThread();
						framerate = 100;
					} else {
						framerate = 1000;
						electronThread.pauseThread();
					}
				}
				// SECURITY button
				if (mTouchX >= mPixels + (4 / 1.5 * scale)
						&& mTouchX <= mPixels + (88 / 1.5 * scale)
						&& mTouchY >= (291 / 1.5 * scale)
						&& mTouchY <= (366 / 1.5 * scale)) {
					background++;
					if (background >= ships.length) {
						background = DEFAULT_BACKGROUND;
					}
					lcars = BitmapFactory.decodeResource(res, ships[background]);

				}
				// Reschedule the next redraw
				mHandler.removeCallbacks(mDrawCube);
				if (mVisible) {
					mHandler.post(mDrawCube);
				}

			} else {
				mTouchX = -1;
				mTouchY = -1;
			}
			super.onTouchEvent(event);
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					// draw something
					drawBitmap(c);
					if (isPortrait) {
						if (bCaution)
							drawCaution(c);
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
				c.drawBitmap(deuterium, mPixels + (scale * 487), (scale * 173),
						bitmapPaint);
				electronPaint.setColor(0xffffffff);
				c.drawCircle(electronThread.getX1().floatValue(),
						electronThread.getY1().floatValue(), 8, electronPaint);
				electronPaint.setColor(0xffff9f00);
				c.drawCircle(electronThread.getX1().floatValue(),
						electronThread.getY1().floatValue(), 6, electronPaint);
			}
		}

		private int loop = 0;

		void drawCaution(Canvas c) {
			if (isPortrait) {
				loop++;
				if (loop > 20)
					loop = 0;
				c.drawBitmap(caution, mPixels + (scale * (100 / 1.5f)),
						(scale * (285 / 1.5f)), bitmapPaint);
				int factor = Math.abs(10 - loop);
				factor = factor * 5;
				factor = 50 + factor;
				int hex = Integer.parseInt(Integer.toHexString(factor)
						+ "000000", 16);
				electronPaint.setColor(hex);
				Align align = usagePaint.getTextAlign();
				usagePaint.setTextAlign(Align.CENTER);
				c.drawText("DEUTERIUM LEVELS AT " + level, mPixels + scale
						* ((285 + 80) / 1.5f), (scale * ((285 + 305) / 1.5f)),
						usagePaint);
				usagePaint.setTextAlign(align);

				c.drawRect(mPixels + (scale * (100 / 1.5f)),
						(scale * (285 / 1.5f)), mPixels
								+ (scale * ((100 + 517) / 1.5f)),
						(scale * ((285 + 458) / 1.5f)), electronPaint);
			}
		}

		void drawButtonText(Canvas c) {
			c.drawText(statsThread.getsSpeed(), mPixels + (scale * 625),
					(scale * 161), buttonPaint);
		}

		void drawProcText(Canvas c) {

			int h = Math.round(scale * 177);
			Process[] processes = memThread.getProcesses();
			for (int i = 0; i < processes.length; i++) {
				c.drawText(processes[i].getName(), mPixels + (scale * 548), h,
						processPaint);
				c.drawText(String.valueOf(processes[i].getPid()), mPixels
						+ (scale * 575), h, processPaint);
				c.drawText(String.valueOf(processes[i].getMemory()), mPixels
						+ (scale * 625), h, processPaint);
				h += (scale * 17);
			}
		}

		void drawText(Canvas c) {

			c.drawText(statsThread.getUsage() + "%", mPixels + (scale * 367),
					(scale * 200), usagePaint);
			// c.drawText(dur + " " + sDays + " " + sHours + ":" + sMins + ":" +
			// sSecs, mPixels + 222, 69, ulPaint);
			c.drawText("DUR", mPixels + (scale * 87), (scale * 46), uptimePaint);
			c.drawText(statsThread.getUpDays(), mPixels + (scale * 107),
					(scale * 46), uptimePaint);
			c.drawText(statsThread.getUpHours() + ":" + statsThread.getUpMins()
					+ ":" + statsThread.getUpSecs(), mPixels + (scale * 160),
					(scale * 46), uptimePaint);
			c.drawText("SD", mPixels + (scale * 87), (scale * 63), uptimePaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 63), uptimePaint);
			c.drawText(String.valueOf(DateCalc.stardate()), mPixels
					+ (scale * 160), (scale * 63), uptimePaint);
			c.drawText("TER", mPixels + (scale * 87), (scale * 81), uptimePaint);
			c.drawText(statsThread.gettHours(), mPixels + (scale * 107),
					(scale * 81), uptimePaint);
			c.drawText(statsThread.gettDate(), mPixels + (scale * 160),
					(scale * 81), uptimePaint);
			c.drawText("TTC", mPixels + (scale * 87), (scale * 98), uptimePaint);
			c.drawText("--", mPixels + (scale * 107), (scale * 98), uptimePaint);
			c.drawText(statsThread.getsTTC(), mPixels + (scale * 160),
					(scale * 98), uptimePaint);

		}

	}
}
