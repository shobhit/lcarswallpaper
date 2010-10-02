package com.example.android.maxpapers.lcars;

import java.util.Date;

public class ElectronCalcThread extends AbstractThread {
	private Double x1;
	private Double y1;
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
		x1 = new Double(mPixels + (scale * (834 / 1.5)) + (scale * (83 / 1.5))
				* Math.cos((time % 360) * (Math.PI / 180)));
		y1 = new Double((scale * (356 / 1.5)) + (scale * (83 / 1.5))
				* Math.sin((time % 360) * (Math.PI / 180)));
	}

	public Double getY1() {
		return y1;
	}

	public Double getX1() {
		return x1;
	}

}
