package com.example.android.maxpapers.lcars;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author cebarne2
 * A thread that can be paused and resumed.  Uses a semaphore
 * to control execution.  A single method doWork() must be 
 * overriden.  doWork() will be called once each *poll* period. 
 * doWork() loops until the thread is stopped or paused.
 */
public abstract class AbstractThread extends Thread {
	protected AtomicBoolean run;
	protected AtomicBoolean pause;
	private Semaphore semaphore = new Semaphore(0);  //semaphore with a single permit

	protected int poll;

	/**
	 * Create a thread and set the polling (wait time between executions) 
	 * @param poll TIme to wait in milliseconds between calls to doWork()
	 */
	public AbstractThread(int poll) {
		pause = new AtomicBoolean(false);
		run = new AtomicBoolean();
		this.poll = poll;
	}

	/**
	 * Stops the thread.  Call this in your onDestroy() and 
	 * onSurfaceDestroy() methods.
	 */
	public final void stopThread() {
		this.run.set(false);
		semaphore.release();

	}

	/**
	 * Pauses the thread.  Call this in your onVisibilityChanged() method.
	 */
	public final void pauseThread() {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Resumes the thread.  Call this in your onVisibilityChanged() method.
	 */
	public final void resumeThread() {
		semaphore.release();
	}

	@Override
	public final void run() {
		this.run.set(true);
		while (run.get()) {
			try {
				semaphore.acquire();
				doStuff();
				semaphore.release();
				sleep(poll);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * All the *stuff* you want to do in one iteration of this thread.
	 */
	protected abstract void doStuff();
}
