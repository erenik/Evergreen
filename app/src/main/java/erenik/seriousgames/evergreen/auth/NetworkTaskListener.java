package erenik.seriousgames.evergreen.auth;

/**
 * Created by Emil on 2016-11-24.
 */
public interface NetworkTaskListener {
    /// True on success.
    void OnTaskCompleted(boolean success);

    void OnTaskCanceled();
//    abstract void
}
