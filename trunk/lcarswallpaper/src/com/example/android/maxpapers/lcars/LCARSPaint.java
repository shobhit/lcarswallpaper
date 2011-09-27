package com.example.android.maxpapers.lcars;

import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;

public class LCARSPaint {
	private Typeface font;
	private float scale;
	public static final float TEXT_XLARGE = 32f;
	public static final float TEXT_LARGE = 24f;
	public static final float TEXT_MEDIUM = 16f;
	public static final float TEXT_SMALL = 12f;
	private Paint bitmapPaint = new Paint();
	private Paint usagePaint = new Paint();
	private Paint uptimePaint = new Paint();
	private Paint buttonPaint = new Paint();
	private Paint processPaint = new Paint();
	private Paint powerPaint = new Paint();
	private Paint electronPaint = new Paint();
	private Paint hotspotPaint = new Paint();

	public LCARSPaint(Typeface font, float scale) {
		super();
		this.font = font;
		this.scale = scale;
		initialize();
	}

	private void initialize() {
		bitmapPaint.setColor(0xffffffff);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setStrokeWidth(2);
		bitmapPaint.setStrokeCap(Paint.Cap.ROUND);
		bitmapPaint.setStyle(Paint.Style.STROKE);

		usagePaint.setTypeface(font);
		usagePaint.setTextSize(scale * TEXT_XLARGE);
		usagePaint.setColor(0xffff9f00);
		usagePaint.setAntiAlias(true);
		usagePaint.setStrokeCap(Paint.Cap.ROUND);
		usagePaint.setStyle(Paint.Style.STROKE);

		hotspotPaint.setTypeface(font);
		hotspotPaint.setTextSize(scale * TEXT_SMALL);
		hotspotPaint.setColor(0xffcf6060); // reddish
		hotspotPaint.setAntiAlias(true);

		uptimePaint.setTypeface(font);
		uptimePaint.setTextSize(scale * TEXT_MEDIUM);
		uptimePaint.setColor(0xffff9f00);
		uptimePaint.setAntiAlias(true);
		uptimePaint.setTextAlign(Align.RIGHT);

		buttonPaint.setTypeface(font);
		buttonPaint.setTextSize(scale * TEXT_SMALL);
		buttonPaint.setColor(0xff000000);
		buttonPaint.setAntiAlias(true);
		buttonPaint.setTextAlign(Align.RIGHT);

		electronPaint.setColor(0xffff9f00);
		electronPaint.setAntiAlias(true);

		processPaint.setTypeface(font);
		processPaint.setTextSize(scale * TEXT_SMALL);
		processPaint.setColor(0xff9f9fff);
		processPaint.setAntiAlias(true);
		processPaint.setTextAlign(Align.RIGHT);

		powerPaint.setTypeface(font);
		powerPaint.setTextSize(scale * TEXT_MEDIUM);
		powerPaint.setColor(0xff9f9fff);
		powerPaint.setAntiAlias(true);
		powerPaint.setTextAlign(Align.RIGHT);

	}

	public Paint getBitmapPaint() {
		return bitmapPaint;
	}

	public Paint getUsagePaint() {
		return usagePaint;
	}

	public Paint getUptimePaint() {
		return uptimePaint;
	}

	public Paint getButtonPaint() {
		return buttonPaint;
	}

	public Paint getProcessPaint() {
		return processPaint;
	}

	public Paint getPowerPaint() {
		return powerPaint;
	}

	public Paint getElectronPaint() {
		return electronPaint;
	}

	public Paint getHotspotPaint() {
		return hotspotPaint;
	}

}
