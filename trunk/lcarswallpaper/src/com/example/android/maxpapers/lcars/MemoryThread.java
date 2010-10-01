package com.example.android.maxpapers.lcars;

import android.app.ActivityManager;

public class MemoryThread extends AbstractThread {

	private ProcInfo proc;
	private String[] services = new String[0];
	private String[] apps = new String[0];
	
	public MemoryThread(ActivityManager man, int poll){
		super(poll);
		proc = new ProcInfo(man);
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



	@Override
	protected void doStuff() {
		setServices(proc.getServiceInfo());
		setApps(proc.getAppInfo());		
	}

}
