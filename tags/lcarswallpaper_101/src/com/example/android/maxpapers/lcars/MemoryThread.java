package com.example.android.maxpapers.lcars;

import android.app.ActivityManager;

public class MemoryThread extends Thread {

	boolean run;
	boolean wait;

	private ProcInfo proc;
	private String[] services = new String[0];
	private String[] apps = new String[0];
	
	int poll;

	public MemoryThread(ActivityManager man, int poll) {
		proc = new ProcInfo(man);
		wait = false;
		this.poll = poll;
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
			setServices(proc.getServiceInfo());
			setApps(proc.getAppInfo());
			try {
				sleep(poll);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void setServices(String[] services) {
		this.services = services;
	}

	public String[] getServices() {
		return services;
	}

	private void setApps(String[] apps) {
		this.apps = apps;
	}

	public String[] getApps() {
		return apps;
	}

}
