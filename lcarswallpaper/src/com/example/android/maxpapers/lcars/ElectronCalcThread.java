package com.example.android.maxpapers.lcars;

import java.util.Date;

public class ElectronCalcThread extends AbstractThread {
	private float x1;
	private float y1;
	private float mPixels;

	public synchronized void setmPixels(float mPixels) {
		this.mPixels = mPixels;
	}

	public synchronized void setScale(float scale) {
		this.scale = scale;
	}

	private float scale;

	public ElectronCalcThread(float mPixels, float scale, int poll) {
		super(poll);
		this.mPixels = mPixels;
		this.scale = scale;
	}

	@Override
	protected void doStuff() {
		long time = new Date().getTime() / 33;
		float x1a;
		float y1a;
		x1a = new Double(mPixels + (scale * (834 / 1.5)) + (scale * (83 / 1.5))
				* Math.cos((time % 360) * (Math.PI / 180))).floatValue();
		y1a = new Double((scale * (356 / 1.5)) + (scale * (83 / 1.5))
				* Math.sin((time % 360) * (Math.PI / 180))).floatValue();
		x1 = x1a;
		y1 = y1a;
	}

	public float getY1() {
		return y1;
	}

	public float getX1() {
		return x1;
	}

}
