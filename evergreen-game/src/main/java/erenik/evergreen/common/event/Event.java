package erenik.evergreen.common.event;

public class Event {
    Event(EventType t, float difficulty)
    {
        this.type = t;
        this.difficulty = difficulty;
        ClampDifficulty();
    };
    void ClampDifficulty()
    {
        if (difficulty < min_diff)
            difficulty = min_diff;
        if (difficulty > max_diff)
            difficulty = max_diff;
    }
    // Plays the event. This will start activities, etc.
    void Play()
    {
        if (started)
            return;
        started = true;
        // Launch the necessary activity?
        // Pass along arguments as needed.

    }
    boolean IsPlaying()
    {
        return started && !ended;
    }
    boolean Ended()
    {
        return ended;
    }

    EventType type;
    /// Difficulty rating from 1.0f to 100.0f ?
    float difficulty;
    static final float min_diff = 1, max_diff = 100;
    //
    boolean optional;


    private boolean started = false;
    private boolean ended = false;
}
