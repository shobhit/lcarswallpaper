package com.example.android.maxpapers.lcars;

public class StatsThread extends AbstractThread {

	private CPULoad cpu = new CPULoad();

	private int usage;
	private int speed;
	private long uptime;

	public StatsThread(int poll) {
		super(poll);
		cpu = new CPULoad();
		setUsage(0);
		setSpeed(0);
		setUptime(0);
	}

	@Override
	protected void doStuff() {
		setUsage(Math.round(cpu.getUsage()));
		setUptime(Math.round(cpu.getUptime()));
		setSpeed(Math.round(cpu.getSpeed()));

	}

	private void setUsage(int usage) {
		this.usage = usage;
	}

	public int getUsage() {
		return usage;
	}

	private void setUptime(long uptime) {
		this.uptime = uptime;
	}

	public long getUptime() {
		return uptime;
	}

	private void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getSpeed() {
		return speed;
	}

}
