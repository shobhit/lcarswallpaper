package com.example.android.maxpapers.lcars;

public class Process {
	public int getPid() {
		return pid;
	}
	public String getName() {
		return name;
	}
	public int getMemory() {
		return memory;
	}
	public Process(int pid, String name, int memory) {
		super();
		this.pid = pid;
		this.name = name;
		this.memory = memory;
	}
	private int pid;
	private String name;
	private int memory;
}
