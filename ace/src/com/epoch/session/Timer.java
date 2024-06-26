package com.epoch.session;

/** Halts a thread that is taking too long. */
class Timer extends Thread {

	/** How long to let the process run before stopping it. */
	transient int timeout;
	/** The thread that may be stopped. */
	transient Thread victim;

	/** Constructor.
	 * @param	aTimeout	how long to let the process run before stopping it
	 * @param	aVictim	the thread that may be stopped
	 */
	Timer(int aTimeout, Thread aVictim) {
		timeout = aTimeout;
		victim = aVictim;
	} // Timer()

	/** Runs the timer. */
	public void run() {
		try {
			sleep(timeout);
			victim.interrupt();
		} catch (InterruptedException e) { 
			// sleep() was interrupted; no need for any action
		} // try
	} // run()

} // Timer

