package analysis;

/**
 * This interface allows AnalysisWorkers to detect progress through
 * a multithreaded task run in an AbstractProgressAction
 * @author ben
 *
 */
public interface ProgressListener {

	public void progressEventReceived(ProgressEvent event);
}
