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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
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
	private final int ELECTRON_FR = 10;
	private final int CAUTION_FR = 100;
	private final int NORMAL_FR = 1000;
	private final int MEMORY_FR = 15000;
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
		private static final int SHIP_FRAME_TOP = 300;
		private static final int SHIP_FRAME_LEFT = 110;
		private static final int ELECTRON_MODE = 1;
		private static final int PROCESSES_MODE = 0;
		private static final int MAX_MODE = 1;
		float scale = getResources().getDisplayMetrics().density;
		private LCARSPaint lcarsPaint;
		float xScale;
		float yScale;
		private int wallWidth = 0;
		private int wallHeight = 0;
		private int mPixels;
		private float mTouchX;
		private float mTouchY;
		private Bitmap bitmapLcarsPortrait;
		private Bitmap bitmapMutablePortrait;
		private Bitmap bitmapLcarsLandscape;
		private Bitmap bitmapDeuterium;
		private Bitmap bitmapCaution;
		private MemoryThread memThread;
		private StatsThread statsThread;
		private ElectronCalcThread electronThread;
		private boolean isPortrait;
		private int systemPanelMode;
		private boolean bCaution = false;
		private int framerate = NORMAL_FR;
		private int background = DEFAULT_BACKGROUND;
		private Rect shipRect = new Rect();
		private Rect shipFrame = new Rect();
		private Rect shipDrawFrame = new Rect();
		private Rect lcarsPortraitRect;
		private Rect lcarsLandscapeRect;
		private Rect wallpaperRect = new Rect();
		private Resources res = getResources();
		private ShipHotSpot[] screenShipSpots;
		private ShipHotSpot[] bitmapShipSpots;

		private final Runnable drawingThread = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		private BatteryReceiver batteryReceiver = new BatteryReceiver();
		private boolean mVisible;

		CubeEngine() {
			// android.os.Debug.waitForDebugger();
			registerReceiver(batteryReceiver, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
			bitmapShip = BitmapFactory.decodeResource(res, ships[background]);
			bitmapLcarsPortrait = BitmapFactory.decodeResource(res,
					R.drawable.lcars);
			bitmapMutablePortrait = Bitmap.createBitmap(
					bitmapLcarsPortrait.getWidth(),
					bitmapLcarsPortrait.getHeight(),
					bitmapLcarsPortrait.getConfig());
			bitmapDeuterium = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.deuterium);
			bitmapCaution = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.caution);
			bitmapLcarsLandscape = BitmapFactory.decodeResource(res,
					com.example.android.maxpapers.R.drawable.lcars_land);
			lcarsLandscapeRect = new Rect(0, 0, bitmapLcarsLandscape.getWidth(),
					bitmapLcarsLandscape.getHeight());
			lcarsPortraitRect = new Rect(0, 0, bitmapLcarsPortrait.getWidth(),
					bitmapLcarsPortrait.getHeight());
			lcarsPaint = new LCARSPaint(Typeface.createFromAsset(getAssets(),
					"swiss_ec.ttf"), scale);
			systemPanelMode = PROCESSES_MODE;
			memThread = new MemoryThread(
					(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE),
					MEMORY_FR);
			statsThread = new StatsThread(NORMAL_FR);
			electronThread = new ElectronCalcThread(0);

		}

		private void setDimensions() {
			// android.os.Debug.waitForDebugger();
			wallpaperRect.set(mPixels, 0, mPixels + wallWidth, wallHeight);
			xScale = (float) wallWidth / (isPortrait?bitmapLcarsPortrait.getWidth():bitmapLcarsLandscape.getWidth());
			yScale = (float) wallHeight / (isPortrait?bitmapLcarsPortrait.getHeight():bitmapLcarsLandscape.getHeight());
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
			mHandler.removeCallbacks(drawingThread);
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
				if (systemPanelMode == ELECTRON_MODE) {
					electronThread.resumeThread();
				}
				drawFrame();
			} else {
				mHandler.removeCallbacks(drawingThread);
				memThread.pauseThread();
				statsThread.pauseThread();
				if (systemPanelMode == ELECTRON_MODE) {
					electronThread.pauseThread();
				}
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			if (width > height) {
				isPortrait = false;
			} else {
				isPortrait = true;
			}
			wallWidth = width * 2;
			wallHeight = height;
			setDimensions();
			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			setDimensions();
			memThread.start();
			statsThread.start();
			electronThread.start();
			electronThread.pauseThread();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(drawingThread);
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
			// Using xOffset because some launchers don't provide xPixels (looking at you Samsung)
			mPixels = (int) -((wallWidth / 2) * xOffset);
			
			shipFrame.set((int) (SHIP_FRAME_LEFT * xScale) + xPixels,
					(int) (SHIP_FRAME_TOP * yScale),
					(int) ((SHIP_FRAME_LEFT + shipDrawWidth) * xScale)
							+ xPixels,
					(int) ((SHIP_FRAME_TOP + shipDrawHeight) * yScale));
			if (background > 0) {
				screenShipSpots = getScreenShipSpots();
				bitmapShipSpots = getBitmapShipSpots();
			}
			setDimensions();
			drawFrame();
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				// pause some threads
				memThread.pauseThread();

				mTouchX = event.getX();
				mTouchY = event.getY();
				// WARP EF button
				if (mTouchX >= mPixels + (821 * xScale)
						&& mTouchX <= mPixels + (974 * xScale)
						&& mTouchY >= (238 * yScale)
						&& mTouchY <= (278 * yScale)) {
					systemPanelMode++;
					if (systemPanelMode > MAX_MODE) {
						systemPanelMode = PROCESSES_MODE;
					}
				}
				// SECURITY button
				if (mTouchX >= mPixels + (4 * xScale)
						&& mTouchX <= mPixels + (98 * xScale)
						&& mTouchY >= (328 * yScale)
						&& mTouchY <= (408 * yScale)) {
					hotSpot = null;
					background++;
					if (background >= ships.length) {
						background = DEFAULT_BACKGROUND;
						if (bitmapShip != null)
							bitmapShip.recycle();
					} else {
						if (bitmapShip != null)
							bitmapShip.recycle();
						bitmapShip = BitmapFactory.decodeResource(res,
								ships[background]);

					}

				}
				// ship hotspot
				if (background != DEFAULT_BACKGROUND) {
					shipFrame.set((int) (SHIP_FRAME_LEFT * xScale) + mPixels,
							(int) (SHIP_FRAME_TOP * yScale),
							(int) ((SHIP_FRAME_LEFT + shipDrawWidth) * xScale)
									+ mPixels,
							(int) ((SHIP_FRAME_TOP + shipDrawHeight) * yScale));

					screenShipSpots = getScreenShipSpots();
					bitmapShipSpots = getBitmapShipSpots();
					if (shipFrame.contains(new Float(mTouchX).intValue(),
							new Float(mTouchY).intValue())) {
						shipFrameTouch();
					}
				}
				// Reschedule the next redraw
				mHandler.removeCallbacks(drawingThread);
				if (mVisible) {
					mHandler.post(drawingThread);
				}

			} else {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					memThread.resumeThread();
				}
				mTouchX = -1;
				mTouchY = -1;
			}
			super.onTouchEvent(event);
		}

		private void shipFrameTouch() {
			hotSpot = null;
			if (background > 0) {
				for (int i = 0; i < screenShipSpots.length; i++) {
					Rect spot = screenShipSpots[i].rect;
					if (spot.contains(new Float(mTouchX).intValue(), new Float(
							mTouchY).intValue())) {
						hotSpot = new ShipHotSpot(bitmapShipSpots[i].rect,
								bitmapShipSpots[i].name);
					}
				}
			}
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here.
		 */
		void drawFrame() {

			Canvas c;
			// draw something
			if (isPortrait) {

				c = new Canvas(bitmapMutablePortrait);
				c.drawBitmap(bitmapLcarsPortrait, 0, 0,
						lcarsPaint.getBitmapPaint());
				if (bCaution)
					drawCaution(c);
				if (background > DEFAULT_BACKGROUND) {
					shipHeight = bitmapShip.getHeight();
					shipWidth = bitmapShip.getWidth();
					shipRatio = (float) shipWidth / (float) shipHeight;
					shipDrawWidth = 600;
					shipDrawHeight = shipDrawWidth / shipRatio;

					drawShip(c);
				}
				drawText(c);
				drawButtonText(c);
				if (systemPanelMode == PROCESSES_MODE) {
					drawProcText(c);
				} else if (systemPanelMode == ELECTRON_MODE) {
					drawAtom(c);
					drawPowerStats(c);
				}
			}
			SurfaceHolder holder = getSurfaceHolder();
			c = holder.lockCanvas();
			if (c != null)
				drawBitmap(c);
			holder.unlockCanvasAndPost(c);

			// Reschedule the next redraw
			mHandler.removeCallbacks(drawingThread);
			if (mVisible) {
				if (batteryReceiver.isBatteryLow(20)) {
					framerate = CAUTION_FR;
				} else if (systemPanelMode == ELECTRON_MODE) {
					framerate = ELECTRON_FR;
				} else {
					framerate = NORMAL_FR;
				}
				mHandler.postDelayed(drawingThread, framerate);
			}
		}

		void drawBitmap(Canvas c) {
			if (!isPortrait) {
				c.drawBitmap(bitmapLcarsLandscape, lcarsLandscapeRect,
						wallpaperRect, lcarsPaint.getBitmapPaint());
			} else {
				c.drawBitmap(bitmapMutablePortrait, lcarsPortraitRect,
						wallpaperRect, lcarsPaint.getBitmapPaint());
				lcarsPaint.getHotspotPaint().setStyle(Style.STROKE);
				// c.drawRect(shipFrame, lcarsPaint.getHotspotPaint());
				// if (screenShipSpots != null) {
				// for (int i = 0; i < screenShipSpots.length; i++) {
				// if (screenShipSpots[i] != null)
				// c.drawRect(screenShipSpots[i].rect,
				// lcarsPaint.getHotspotPaint());
				// }
				// }
			}
		}

		void drawShip(Canvas c) {
			shipDrawFrame.set(110, 300, 690, 860);
			shipRect.right = shipRect.left + shipWidth;
			shipRect.bottom = shipRect.top + shipHeight;
			shipDrawFrame.right = shipDrawFrame.left
					+ new Float(shipDrawWidth).intValue();
			shipDrawFrame.bottom = shipDrawFrame.top
					+ new Float(shipDrawHeight).intValue();
			lcarsPaint.getHotspotPaint().setColor(0xffFF9F00); // yellowish
			lcarsPaint.getHotspotPaint().setTextSize(LCARSPaint.TEXT_LARGE);
			c.drawText(res.getString(ships_names[background]),
					shipDrawFrame.left, shipDrawFrame.top,
					lcarsPaint.getHotspotPaint());
			c.drawBitmap(bitmapShip, shipRect, shipDrawFrame,
					lcarsPaint.getBitmapPaint());
			if (hotSpot != null) {
				lcarsPaint.getHotspotPaint().setColor(0xff9f9fff); // blueish
				lcarsPaint.getHotspotPaint()
						.setTextSize(LCARSPaint.TEXT_MEDIUM);
				c.drawText(hotSpot.name,
						new Float(shipDrawFrame.left + 12).intValue(),
						shipDrawFrame.top + LCARSPaint.TEXT_MEDIUM,
						lcarsPaint.getHotspotPaint());
				drawLabel(
						new Point(new Float(shipDrawFrame.left + 3).intValue(),
								new Float(shipDrawFrame.top
										+ LCARSPaint.TEXT_MEDIUM).intValue()),
						new Point(hotSpot.rect.centerX(), hotSpot.rect
								.centerY()), c);
				lcarsPaint.getHotspotPaint().setColor(0xffcf6060); // reddish
				// c.drawRect(hotSpot.hotspot.left, hotSpot.hotspot.top,
				// hotSpot.hotspot.right, hotSpot.hotspot.bottom,
				// hotspotPaint);
			}

		}

		void drawLabel(Point start, Point end, Canvas c) {
			Path linePath = new Path();
			Paint linePaint = new Paint(lcarsPaint.getHotspotPaint());
			// background shadow
			linePaint.setStyle(Paint.Style.STROKE);
			linePath.moveTo(start.x, start.y);
			linePath.lineTo(start.x, end.y - 2);
			linePath.cubicTo(start.x, end.y - 1, start.x + 1, end.y,
					start.x + 2, end.y);
			linePath.lineTo(end.x, end.y);
			linePaint.setColor(0x4B000000); // fade
			linePaint.setStrokeWidth(6);
			c.drawPath(linePath, linePaint);
			linePaint.setStrokeWidth(0);
			linePaint.setStyle(Paint.Style.FILL);
			c.drawCircle(start.x, start.y, 6, linePaint);
			c.drawCircle(end.x, end.y, 6, linePaint);

			// foreground line
			linePaint.setStyle(Paint.Style.STROKE);
			linePath.moveTo(start.x, start.y);
			linePath.lineTo(start.x, end.y - 2);
			linePath.cubicTo(start.x, end.y - 1, start.x + 1, end.y,
					start.x + 2, end.y);
			linePath.lineTo(end.x, end.y);
			linePaint.setColor(0xff9f9fff); // blueish
			linePaint.setStrokeWidth(2);
			c.drawPath(linePath, linePaint);
			linePaint.setStrokeWidth(0);
			linePaint.setStyle(Paint.Style.FILL);
			c.drawCircle(start.x, start.y, 2, linePaint);
			c.drawCircle(end.x, end.y, 2, linePaint);
		}

		void drawPowerStats(Canvas c) {
			int h = 600;
			int x = 1040;
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_LARGE);
			c.drawText("STATUS:", x, h, lcarsPaint.getPowerPaint());
			h += 17;
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_MEDIUM);
			c.drawText(batteryReceiver.getStatus(), x, h,
					lcarsPaint.getPowerPaint());
			h += 36;
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_LARGE);
			c.drawText("LEVEL:", x, h, lcarsPaint.getPowerPaint());
			h += 17;
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_MEDIUM);
			c.drawText(batteryReceiver.getBatteryLevel() + "%", x, h,
					lcarsPaint.getPowerPaint());
			h += 36;
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_LARGE);
			c.drawText("ENERGY:", x, h, lcarsPaint.getPowerPaint());
			lcarsPaint.getPowerPaint().setTextSize(LCARSPaint.TEXT_MEDIUM);
			h += 17;
			c.drawText(batteryReceiver.geteV() + " V", x, h,
					lcarsPaint.getPowerPaint());
		}

		void drawAtom(Canvas c) {
			if (isPortrait) {
				c.drawBitmap(bitmapDeuterium,
						940 - (bitmapDeuterium.getWidth() / 2),
						439 - (bitmapDeuterium.getHeight() / 2),
						lcarsPaint.getBitmapPaint());
				lcarsPaint.getElectronPaint().setColor(0xffffffff);
				c.drawCircle(electronThread.getX1(), electronThread.getY1(), 8,
						lcarsPaint.getElectronPaint());
				lcarsPaint.getElectronPaint().setColor(0xffff9f00);
				c.drawCircle(electronThread.getX1(), electronThread.getY1(), 6,
						lcarsPaint.getElectronPaint());
				// c.drawPath(LCARSPath.getTopRightCorner(0f, 0f, 50f,
				// electronThread.getX1(), electronThread.getY1(), 100f),
				// electronPaint);
				electronThread.resumeThread(); // calculate next position
			}
		}

		private int loop = 0;
		private Bitmap bitmapShip;
		private ShipHotSpot hotSpot;
		private float shipDrawWidth;
		private float shipDrawHeight;
		private int shipHeight;
		private int shipWidth;
		private float shipRatio;

		void drawCaution(Canvas c) {
			if (isPortrait) {
				loop++;
				if (loop > 20)
					loop = 0;
				c.drawBitmap(bitmapCaution, mPixels + (scale * (100 / 1.5f)),
						(scale * (285 / 1.5f)), lcarsPaint.getBitmapPaint());
				int factor = Math.abs(10 - loop);
				factor = factor * 5;
				factor = 50 + factor;
				int hex = Integer.parseInt(Integer.toHexString(factor)
						+ "000000", 16);
				lcarsPaint.getElectronPaint().setColor(hex);
				Align align = lcarsPaint.getUsagePaint().getTextAlign();
				lcarsPaint.getUsagePaint().setTextAlign(Align.CENTER);
				c.drawText(
						"DEUTERIUM LEVELS AT "
								+ batteryReceiver.getBatteryLevel() + "%",
						mPixels + scale * ((285 + 80) / 1.5f),
						(scale * ((285 + 305) / 1.5f)),
						lcarsPaint.getUsagePaint());
				lcarsPaint.getUsagePaint().setTextAlign(align);

				c.drawRect(mPixels + (scale * (100 / 1.5f)),
						(scale * (285 / 1.5f)), mPixels
								+ (scale * ((100 + 517) / 1.5f)),
						(scale * ((285 + 458) / 1.5f)),
						lcarsPaint.getElectronPaint());
			}
		}

		void drawButtonText(Canvas c) {

			c.drawText(statsThread.getsSpeed(), 1016, 274,
					lcarsPaint.getButtonPaint());
		}

		void drawProcText(Canvas c) {

			int h = 300;
			Process[] processes = memThread.getProcesses();
			for (int i = 0; i < processes.length; i++) {
				c.drawText(processes[i].getName(), 930, h,
						lcarsPaint.getProcessPaint());
				c.drawText(String.valueOf(processes[i].getPid()), 975, h,
						lcarsPaint.getProcessPaint());
				c.drawText(String.valueOf(processes[i].getMemory()), 1050, h,
						lcarsPaint.getProcessPaint());
				h += (scale * 17);
			}
		}

		void drawText(Canvas c) {
			int[] columns = { 140, 170, 250 };
			int[] rows = { 70, 100, 130, 160, 190, 220 };
			try {
				c.drawText(statsThread.getUsage() + "%", 520, 300,
						lcarsPaint.getUsagePaint());
				// c.drawText(dur + " " + sDays + " " + sHours + ":" + sMins +
				// ":" +
				// sSecs, mPixels + 222, 69, ulPaint);
				c.drawText("DUR", columns[0], rows[0],
						lcarsPaint.getUptimePaint());
				c.drawText(statsThread.getUpDays(), columns[1], rows[0],
						lcarsPaint.getUptimePaint());
				c.drawText(
						statsThread.getUpHours() + ":"
								+ statsThread.getUpMins() + ":"
								+ statsThread.getUpSecs(), columns[2], rows[0],
						lcarsPaint.getUptimePaint());
				c.drawText("SD", columns[0], rows[1],
						lcarsPaint.getUptimePaint());
				c.drawText("--", columns[1], rows[1],
						lcarsPaint.getUptimePaint());
				c.drawText(String.valueOf(DateCalc.stardate()), columns[2],
						rows[1], lcarsPaint.getUptimePaint());
				c.drawText("TER", columns[0], rows[2],
						lcarsPaint.getUptimePaint());
				c.drawText(statsThread.gettHours(), columns[1], rows[2],
						lcarsPaint.getUptimePaint());
				c.drawText(statsThread.gettDate(), columns[2], rows[2],
						lcarsPaint.getUptimePaint());
				c.drawText("TTC", columns[0], rows[3],
						lcarsPaint.getUptimePaint());
				c.drawText("--", columns[1], rows[3],
						lcarsPaint.getUptimePaint());
				c.drawText(statsThread.getsTTC(), columns[2], rows[3],
						lcarsPaint.getUptimePaint());
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		private ShipHotSpot[] getScreenShipSpots() {
			return getSpots(((shipDrawWidth / shipWidth) * xScale),
					((shipDrawHeight / shipHeight) * yScale), shipFrame);
		}

		private ShipHotSpot[] getBitmapShipSpots() {
			return getSpots((shipDrawWidth / shipWidth),
					(shipDrawHeight / shipHeight), shipDrawFrame);
		}

		private ShipHotSpot[] getSpots(float widthScale, float heightScale,
				Rect shipFrame) {
			String[] hotspots = res.getStringArray(ships_spots[background]);
			ShipHotSpot[] spots = new ShipHotSpot[hotspots.length];
			for (int i = 0; i < hotspots.length; i++) {
				String[] toks = hotspots[i].split(";");
				Rect spot = new Rect();
				String name = toks[0];
				int x = (int) (Integer.parseInt(toks[1]) * widthScale);
				int y = (int) (Integer.parseInt(toks[2]) * heightScale);
				if (toks.length < 5) {
					spot.set(new Double((x - 6) + (shipFrame.left)).intValue(),
							new Double(y - (6)).intValue() + shipFrame.top,
							new Double(x + (6) + (shipFrame.left)).intValue(),
							new Double(y + 6).intValue() + shipFrame.top);
				} else {
					int x2 = (int) (Integer.parseInt(toks[3]) * widthScale);
					int y2 = (int) (Integer.parseInt(toks[4]) * heightScale);
					spot.set(new Double(x + (shipFrame.left)).intValue(),
							new Double(y).intValue() + shipFrame.top,
							new Double(x2 + (shipFrame.left)).intValue(),
							new Double(y2).intValue() + shipFrame.top);

				}
				spots[i] = new ShipHotSpot(spot, name);
			}
			return spots;
		}

	}
}
