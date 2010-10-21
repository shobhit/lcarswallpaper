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
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.example.android.maxpapers.R;

/*
 * This animated wallpaper draws a rotating wireframe cube.
 */
public class LCARSWallpaper extends WallpaperService {

	private int[] ships = { 0, R.drawable.galaxy_dorsal, R.drawable.excelsior,
			R.drawable.birdofprey };
	private int[] ships_spots = { 0, R.array.galaxy_dorsal, R.array.excelsior,
			R.array.birdofprey };
	private int[] ships_names = { 0, R.string.galaxy_dorsal,
			R.string.excelsior, R.string.birdofprey };
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
		final int wallWidth = getResources().getDisplayMetrics().widthPixels * 2;
		final int wallHeight = getResources().getDisplayMetrics().heightPixels;
		private final int MAX_MODE = 1;
		private final Paint bitmapPaint = new Paint();
		private final Paint usagePaint = new Paint();
		private final Paint uptimePaint = new Paint();
		private final Paint buttonPaint = new Paint();
		private final Paint processPaint = new Paint();
		private final Paint powerPaint = new Paint();
		private final Paint electronPaint = new Paint();
		private final Paint hotspotPaint = new Paint();
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
		private Rect shipRect = new Rect();
		private Rect shipFrame = new Rect();
		private Resources res;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				drawFrame(true);
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
			ship = BitmapFactory.decodeResource(res, ships[background]);
			lcars = BitmapFactory.decodeResource(res, R.drawable.lcars);
			// lcars = Bitmap.createScaledBitmap(lcars, wallWidth, wallHeight,
			// true);
			deuterium = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.deuterium);
			caution = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.caution);
			lcars_land = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars_land);

			// electronThread.pauseThread();
			// Create a Paint to draw the lines for our cube
			final Paint paint = bitmapPaint;
			final Paint text_paint = usagePaint;
			final Paint ul_paint = uptimePaint;
			final Paint b_paint = buttonPaint;
			final Paint p_paint = processPaint;
			final Paint e_paint = electronPaint;
			final Paint w_paint = powerPaint;
			final Paint h_paint = hotspotPaint;
			Typeface font = Typeface.createFromAsset(getAssets(),
					"swiss_ec.ttf");
			text_paint.setTypeface(font);
			text_paint.setTextSize(scale * TEXT_LARGE);
			text_paint.setColor(0xffff9f00);
			text_paint.setAntiAlias(true);
			h_paint.setTypeface(font);
			h_paint.setTextSize(scale * TEXT_SMALL);
			h_paint.setColor(0xffcf6060); // reddish
			h_paint.setAntiAlias(true);
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

			memThread = new MemoryThread(
					(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE),
					10000);
			statsThread = new StatsThread(1000);
			electronThread = new ElectronCalcThread(0, scale, 0);

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
				drawFrame(true);
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
			drawFrame(true);
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
			float diff = xPixels - mPixels;
			mPixels = xPixels;

			electronThread.setmPixels(mPixels);
			int frameX1 = new Double((100 / 1.5 * scale) + mPixels).intValue();
			int frameX2 = new Double((625 / 1.5 * scale) + mPixels).intValue();
			int frameY1 = new Double((256 / 1.5 * scale)).intValue();
			int frameY2 = new Double((760 / 1.5 * scale)).intValue();
			if (hotSpot != null) {
				hotSpot.hotspot.left = new Float(hotSpot.hotspot.left + diff)
						.intValue();
				hotSpot.hotspot.right = new Float(hotSpot.hotspot.right + diff)
						.intValue();
			}
			shipFrame.set(frameX1, frameY1, frameX2, frameY2);
			drawFrame(true);
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
						framerate = 30;
					} else {
						framerate = 1000;
					}
				}
				// SECURITY button
				if (mTouchX >= mPixels + (4 / 1.5 * scale)
						&& mTouchX <= mPixels + (88 / 1.5 * scale)
						&& mTouchY >= (291 / 1.5 * scale)
						&& mTouchY <= (366 / 1.5 * scale)) {
					background++;
					hotSpot = null;
					if (background >= ships.length) {
						background = DEFAULT_BACKGROUND;
						if (ship != null)
							ship.recycle();
					} else {
						if (ship != null)
							ship.recycle();

						ship = BitmapFactory.decodeResource(res,
								ships[background]);
					}

				}
				// ship hotspot
				if (shipFrame.contains(new Float(mTouchX).intValue(),
						new Float(mTouchY).intValue())) {
					shipFrameTouch();
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

		private void shipFrameTouch() {
			hotSpot = null;
			if (background > 0) {
				String[] hotspots = res.getStringArray(ships_spots[background]);
				for (int i = 0; i < hotspots.length; i++) {
					String[] toks = hotspots[i].split(";");
					Rect spot = new Rect();
					String name = toks[0];
					int x = Integer.parseInt(toks[1]);
					int y = Integer.parseInt(toks[2]);
					if (toks.length < 5) {
						spot.set(
								new Double(((x - 6) / 1.5) * scale + mPixels
										+ (shipFrame.left - mPixels))
										.intValue(),
								new Double((y / 1.5) * scale - (6 * scale))
										.intValue() + shipFrame.top,
								new Double((x / 1.5) * scale + (6 * scale)
										+ mPixels + (shipFrame.left - mPixels))
										.intValue(), new Double((y / 1.5)
										* scale + (6 * scale)).intValue()
										+ shipFrame.top);
					} else {
						int x2 = Integer.parseInt(toks[3]);
						int y2 = Integer.parseInt(toks[4]);
						spot.set(new Double(((x) / 1.5) * scale + mPixels
								+ (shipFrame.left - mPixels)).intValue(),
								new Double((y / 1.5) * scale).intValue()
										+ shipFrame.top, new Double((x2 / 1.5)
										* scale + mPixels
										+ (shipFrame.left - mPixels))
										.intValue(), new Double((y2 / 1.5)
										* scale).intValue()
										+ shipFrame.top);

					}
					if (spot.contains(new Float(mTouchX).intValue(), new Float(
							mTouchY).intValue())) {
						hotSpot = new ShipHotSpot(new Rect(spot.left, spot.top,
								spot.right, spot.bottom), name);
					}
				}
			}
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here.
		 */
		void drawFrame(boolean force) {
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
						if (background > DEFAULT_BACKGROUND)
							drawShip(c);
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

		void drawShip(Canvas c) {
			int shipHeight = ship.getHeight();
			int shipWidth = ship.getWidth();
			float ratio = (float) shipWidth / (float) shipHeight;
			float frameWidth = (540 / 1.5f) * scale;
			float frameHeight = frameWidth / ratio;
			shipRect.right = shipRect.left + shipWidth;
			shipRect.bottom = shipRect.top + shipHeight;
			shipFrame.right = shipFrame.left + new Float(frameWidth).intValue();
			shipFrame.bottom = shipFrame.top
					+ new Float(frameHeight).intValue();
			hotspotPaint.setColor(0xffFF9F00); // yellowish
			hotspotPaint.setTextSize(scale * TEXT_MEDIUM);
			c.drawText(res.getString(ships_names[background]), shipFrame.left,
					shipFrame.top, hotspotPaint);
			c.drawBitmap(ship, shipRect, shipFrame, bitmapPaint);
			if (hotSpot != null) {
				hotspotPaint.setColor(0xff9f9fff); // blueish
				hotspotPaint.setTextSize(scale * TEXT_SMALL);
				c.drawText(hotSpot.name, new Float(shipFrame.left
						+ (12 * scale)).intValue(), shipFrame.top
						+ (TEXT_MEDIUM * scale), hotspotPaint);
				drawLabel(
						new Point(
								new Float(shipFrame.left + (3 * scale))
										.intValue(),
								new Float(shipFrame.top
										+ ((TEXT_MEDIUM / 1.5) * scale))
										.intValue()),
						new Point(hotSpot.hotspot.centerX(), hotSpot.hotspot
								.centerY()), c);
				hotspotPaint.setColor(0xffcf6060); // reddish
				// c.drawRect(hotSpot.hotspot.left, hotSpot.hotspot.top,
				// hotSpot.hotspot.right, hotSpot.hotspot.bottom,
				// hotspotPaint);
			}

		}

		void drawLabel(Point start, Point end, Canvas c) {
			Path linePath = new Path();
			Paint linePaint = new Paint(hotspotPaint);
			// background shadow
			linePaint.setStyle(Paint.Style.STROKE);
			linePath.moveTo(start.x, start.y);
			linePath.lineTo(start.x, end.y - 2 * scale);
			linePath.cubicTo(start.x, end.y - 1 * scale, start.x + 1 * scale,
					end.y, start.x + 2 * scale, end.y);
			linePath.lineTo(end.x, end.y);
			linePaint.setColor(0x4B000000); // fade
			linePaint.setStrokeWidth(6 * scale);
			c.drawPath(linePath, linePaint);
			linePaint.setStrokeWidth(0);
			linePaint.setStyle(Paint.Style.FILL);
			c.drawCircle(start.x, start.y, 6 * scale, linePaint);
			c.drawCircle(end.x, end.y, 6 * scale, linePaint);

			// foreground line
			linePaint.setStyle(Paint.Style.STROKE);
			linePath.moveTo(start.x, start.y);
			linePath.lineTo(start.x, end.y - 2 * scale);
			linePath.cubicTo(start.x, end.y - 1 * scale, start.x + 1 * scale,
					end.y, start.x + 2 * scale, end.y);
			linePath.lineTo(end.x, end.y);
			linePaint.setColor(0xff9f9fff); // blueish
			linePaint.setStrokeWidth(2 * scale);
			c.drawPath(linePath, linePaint);
			linePaint.setStrokeWidth(0);
			linePaint.setStyle(Paint.Style.FILL);
			c.drawCircle(start.x, start.y, 2 * scale, linePaint);
			c.drawCircle(end.x, end.y, 2 * scale, linePaint);
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
				c.drawCircle(electronThread.getX1(), electronThread.getY1(), 8,
						electronPaint);
				electronPaint.setColor(0xffff9f00);
				c.drawCircle(electronThread.getX1(), electronThread.getY1(), 6,
						electronPaint);
				// c.drawPath(LCARSPath.getTopRightCorner(0f, 0f, 50f,
				// electronThread.getX1(), electronThread.getY1(), 100f),
				// electronPaint);
				electronThread.resumeThread(); // calculate next position
			}
		}

		private int loop = 0;
		private Bitmap ship;
		private ShipHotSpot hotSpot;

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

			try {
				c.drawText(statsThread.getUsage() + "%", mPixels
						+ (scale * 367), (scale * 200), usagePaint);
				// c.drawText(dur + " " + sDays + " " + sHours + ":" + sMins +
				// ":" +
				// sSecs, mPixels + 222, 69, ulPaint);
				c.drawText("DUR", mPixels + (scale * 87), (scale * 46),
						uptimePaint);
				c.drawText(statsThread.getUpDays(), mPixels + (scale * 107),
						(scale * 46), uptimePaint);
				c.drawText(
						statsThread.getUpHours() + ":"
								+ statsThread.getUpMins() + ":"
								+ statsThread.getUpSecs(), mPixels
								+ (scale * 160), (scale * 46), uptimePaint);
				c.drawText("SD", mPixels + (scale * 87), (scale * 63),
						uptimePaint);
				c.drawText("--", mPixels + (scale * 107), (scale * 63),
						uptimePaint);
				c.drawText(String.valueOf(DateCalc.stardate()), mPixels
						+ (scale * 160), (scale * 63), uptimePaint);
				c.drawText("TER", mPixels + (scale * 87), (scale * 81),
						uptimePaint);
				c.drawText(statsThread.gettHours(), mPixels + (scale * 107),
						(scale * 81), uptimePaint);
				c.drawText(statsThread.gettDate(), mPixels + (scale * 160),
						(scale * 81), uptimePaint);
				c.drawText("TTC", mPixels + (scale * 87), (scale * 98),
						uptimePaint);
				c.drawText("--", mPixels + (scale * 107), (scale * 98),
						uptimePaint);
				c.drawText(statsThread.getsTTC(), mPixels + (scale * 160),
						(scale * 98), uptimePaint);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
