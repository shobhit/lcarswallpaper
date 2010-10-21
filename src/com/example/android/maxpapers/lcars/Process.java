package com.example.android.maxpapers.lcars;

public class Process {
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return pid;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * returns semicolon delimited description of process
	 */
	@Override
	public String toString() {
		return pid + ";" + name + ";" + memory;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof Process){
			return (((Process) o).getPid() == pid);
		}
		return super.equals(o);
	}
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
