package com.example.android.maxpapers.lcars;


public class StatsThread extends Thread {

	boolean run;
	boolean wait;

	private CPULoad cpu = new CPULoad();
	
	private int usage;
	private int speed;
	private long uptime;
	
	private int polling;
	
	public StatsThread(int poll) {
		cpu = new CPULoad();
		setUsage(0);
		setSpeed(0);
		setUptime(0);
		wait = false;
		polling = poll;
	}
	
    public void stopCollection() {
        this.run = false;
        synchronized(this) {
            this.notify();
        }
    }

    public void pauseCollection(){
    	wait = true;
        synchronized(this) {
            this.notify();
        }
    }
    
    public void resumeCollection(){
    	wait = false;
        synchronized(this) {
            this.notify();
        }
    }
    
	@Override
	public void run() {
		this.run = true;
		while (run) {
			synchronized (this){
				if (wait){
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			setUsage(Math.round(cpu.getUsage()));
			setUptime(Math.round(cpu.getUptime()));
			setSpeed(Math.round(cpu.getSpeed()));
			try {
				sleep(polling);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
