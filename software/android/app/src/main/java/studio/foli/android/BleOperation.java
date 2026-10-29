package studio.foli.android;

/** Platform calls can fail after a permission or adapter-state change. */
public final class BleOperation {
    private BleOperation() {}
    public static void run(Runnable operation, Runnable unavailable) {
        try { operation.run(); }
        catch (SecurityException | IllegalStateException changedAccess) { unavailable.run(); }
    }
}
