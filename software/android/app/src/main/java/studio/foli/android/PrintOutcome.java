package studio.foli.android;

/** Legacy state 3 means stopped, including timeout/error; it cannot prove success. */
public enum PrintOutcome {
    IDLE, IN_PROGRESS, STOPPED_UNCONFIRMED, FAILED, RESTART_CONFIRMED;

    public static PrintOutcome fromStatus(boolean awaiting, boolean started,
            boolean failed, int paper, int temperature, int state) {
        if (failed) return FAILED;
        if (!awaiting) return IDLE;
        if (paper == 1 || PrinterProtocol.temperatureBlocksPrinting(temperature)) return FAILED;
        if (started && state == 3) return STOPPED_UNCONFIRMED;
        return IN_PROGRESS;
    }
}
