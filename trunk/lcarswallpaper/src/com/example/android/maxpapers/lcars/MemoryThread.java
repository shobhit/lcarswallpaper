package com.example.android.maxpapers.lcars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.ActivityManager;

public class MemoryThread extends AbstractThread {

	private final int MAX_ENTRIES = 23;
	private ProcInfo proc;
	private Process[] services = new Process[0];
	private Process[] apps = new Process[0];
	private Process[] processes = new Process[0];

	public Process[] getProcesses() {
		return processes;
	}

	private Comparator<Process> sorter = new Comparator<Process>() {

		public int compare(Process object1, Process object2) {
			if (object1.getMemory() > object2.getMemory())
				return -1;
			else if (object1.getMemory() < object2.getMemory())
				return 1;
			return 0;
		}
	};

	public MemoryThread(ActivityManager man, int poll) {
		super(poll);
		proc = new ProcInfo(man);
	}

	@Override
	protected void doStuff() {
		ArrayList<Process> procs = new ArrayList<Process>();
		services = (proc.getServiceInfo());
		apps = (proc.getAppInfo());
		Arrays.sort(services, sorter);
		Arrays.sort(apps, sorter);
		procs.addAll(Arrays.asList(apps));
		procs.addAll(Arrays.asList(services));
		int size = (procs.size() - 1 > MAX_ENTRIES) ? MAX_ENTRIES : procs
				.size() - 1;
		processes = procs.subList(0, size - 1).toArray(new Process[0]);

	}

}
