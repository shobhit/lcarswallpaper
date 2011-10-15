package com.example.android.maxpapers.lcars;

import java.util.Date;

public class ElectronCalcThread extends AbstractThread {
	private double x1;
	private double y1;
	private long time;


	public ElectronCalcThread(int poll) {
		super(poll);
	}

	@Override
	protected void doStuff() {
		synchronized (this) {
			time = new Date().getTime() / 33;
			x1 = 940 + (83 * Math.cos((time % 360.0) * (Math.PI / 180.0)));
			y1 = 439 + (83 * Math.sin((time % 360.0) * (Math.PI / 180.0)));
		}
		pauseThread();
	}

	public float getY1() {
		return (float) y1;
	}

	public float getX1() {
		return (float) x1;
	}

}
