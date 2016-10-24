package gui;

public interface CancellableRunnable extends Runnable {
	void cancel();
}
