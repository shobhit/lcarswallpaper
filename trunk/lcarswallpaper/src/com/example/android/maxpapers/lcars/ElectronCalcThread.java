package com.example.android.maxpapers.lcars;

import java.util.Date;

public class ElectronCalcThread extends AbstractThread {
	private float x1;
	private float y1;

	private float xScale;
	private float yScale;

	public ElectronCalcThread(float xScale, float yScale, int poll) {
		super(poll);
		this.xScale = xScale;
		this.yScale = yScale;
	}

	@Override
	protected void doStuff() {
		long time = new Date().getTime() / 33;
		float x1a;
		float y1a;
		x1a = new Double((xScale * (940)) + (xScale * (83))
				* Math.cos((time % 360) * (Math.PI / 180))).floatValue();
		y1a = new Double((yScale * (439)) + (yScale * (83))
				* Math.sin((time % 360) * (Math.PI / 180))).floatValue();
		x1 = x1a;
		y1 = y1a;
		pauseThread();
	}

	public float getY1() {
		return y1;
	}

	public float getX1() {
		return x1;
	}

}
