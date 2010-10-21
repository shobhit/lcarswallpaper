package com.example.android.maxpapers.lcars;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import android.app.ActivityManager;

public class MemoryThread extends AbstractThread {

	private final int MAX_ENTRIES = 23;
	private ProcInfo proc;
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
		HashSet<Process> hash = new HashSet<Process>(Arrays.asList(proc.getServiceInfo()));
		hash.addAll(Arrays.asList(proc.getAppInfo()));
		Process[] aProcs = hash.toArray(new Process[0]);
		Arrays.sort(aProcs, sorter);		
		int size = (aProcs.length - 1 > MAX_ENTRIES) ? MAX_ENTRIES : aProcs.length - 1;
		processes = Arrays.asList(aProcs).subList(0, size - 1).toArray(new Process[0]);

	}

}
