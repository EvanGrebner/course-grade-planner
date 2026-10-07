// Purpose: The answer to "what do I need on the remaining work?".
// It holds a status (like ACHIEVABLE) and the percent needed.
// It can also turn itself into a sentence for the screen.

package gradeplanner.logic;

public record NeededScore(Status status, double percent) {

    public enum Status {
        // the target is safe even with zeros on everything left
        ALREADY_SECURED,
        // the target can be reached with a normal score (0 to 100%)
        ACHIEVABLE,
        // the target needs over 100% on what is left, so only extra credit works
        NEEDS_EXTRA_CREDIT,
        // the target cannot be reached with the work that is left
        UNREACHABLE
    }

    /** Sentence for the screen that explains the result */
    public String describe(double target) {
        return switch (status) {
            case ALREADY_SECURED ->
                    String.format("You've already secured %.1f%% - no more points needed.", target);
            case ACHIEVABLE ->
                    String.format("You need an average of %.2f%% on the remaining work to finish with %.1f%%.",
                            percent, target);
            case NEEDS_EXTRA_CREDIT ->
                    String.format("You'd need %.2f%% on the remaining work to finish with %.1f%% - "
                            + "that's above 100%%, so only extra credit could get you there.", percent, target);
            case UNREACHABLE ->
                    String.format("%.1f%% is out of reach with the work that's left.", target);
        };
    }
}
