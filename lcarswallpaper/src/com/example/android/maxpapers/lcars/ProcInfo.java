package com.example.android.maxpapers.lcars;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.os.Debug.MemoryInfo;

public class ProcInfo {

	private ActivityManager activityManager;

	public ProcInfo(ActivityManager man) {
		activityManager = man;
	}

	/**
	 * Convenience method, calls getServiceInfo(100).
	 * 
	 * @return String[] processName;pid;memory
	 */
	public String[] getServiceInfo() {
		return getServiceInfo(100);
	}

	/**
	 * Returns information about running services, including the process name,
	 * process ID, and consumed memory (approx.)
	 * 
	 * @param count
	 *            Number of services to return. Actual returned number may be
	 *            less, or *unlikely* an empty array.
	 * @return String[] processName;pid;memory
	 */
	public String[] getServiceInfo(int count) {

		List<RunningServiceInfo> services = activityManager
				.getRunningServices(count);
		List<Integer> pids = new ArrayList<Integer>();
		Iterator<RunningServiceInfo> serviceIterator = services.iterator();
		// build pid and RunningServiceInfo lists
		while (serviceIterator.hasNext()) {
			RunningServiceInfo info = serviceIterator.next();

			if (info.pid > 0 && !pids.contains(new Integer(info.pid))) {
				pids.add(new Integer(info.pid));
			}
		}
		int[] aPids = new int[pids.size()];
		Integer[] aIpids = pids.toArray(new Integer[pids.size()]);
		//convert List<Integer> to int[] for use in getProcessMemoryInfo
		for (int i = 0; i < pids.size(); i++) {
			aPids[i] = aIpids[i].intValue();
		}
		RunningServiceInfo[] aSvcs = services.toArray(new RunningServiceInfo[services
				.size()]);
		MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(aPids);
		String[] allInfo = new String[memInfos.length];
		//Build running services array of parsable strings
		for (int i = 0; i < memInfos.length; i++) {
			String procName = aSvcs[i].process;
			if (procName.length() > 16) {
				procName = "..." + procName.substring(procName.length() - 13);
			}
			allInfo[i] = procName
					+ ";"
					+ aSvcs[i].pid
					+ ";"
					+ String.valueOf((memInfos[i].dalvikPss
							+ memInfos[i].nativePss + memInfos[i].otherPss));
		}
		return allInfo;

	}

	/**
	 * Returns information about running applications, including the process
	 * name, process ID, and consumed memory (approx.)
	 * 
	 * @return String[] processName;pid;memory
	 **/
	public String[] getAppInfo() {

		List<RunningAppProcessInfo> processes = activityManager
				.getRunningAppProcesses();
		List<Integer> pids = new ArrayList<Integer>();
		Iterator<RunningAppProcessInfo> serviceIterator = processes.iterator();
		// build pid and RunningAppProcessInfo lists
		while (serviceIterator.hasNext()) {
			RunningAppProcessInfo info = serviceIterator.next();

			if (info.pid > 0 && !pids.contains(new Integer(info.pid))) {
				pids.add(new Integer(info.pid));
			}
		}
		int[] aPids = new int[pids.size()];
		Integer[] aIpids = pids.toArray(new Integer[pids.size()]);
		RunningAppProcessInfo[] aSvcs = processes
				.toArray(new RunningAppProcessInfo[processes.size()]);
		//convert List<Integer> to int[] for use in getProcessMemoryInfo
		for (int i = 0; i < pids.size(); i++) {
			aPids[i] = aIpids[i].intValue();
		}
		MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(aPids);
		String[] allInfo = new String[memInfos.length];
		//Build running services array of parsable strings
		for (int i = 0; i < memInfos.length; i++) {
			String procName = aSvcs[i].processName;
			if (procName.length() > 16) {
				procName = "..." + procName.substring(procName.length() - 13);
			}
			allInfo[i] = procName
					+ ";"
					+ aSvcs[i].pid
					+ ";"
					+ String.valueOf((memInfos[i].dalvikPss
							+ memInfos[i].nativePss + memInfos[i].otherPss));
		}
		return allInfo;

	}
}
