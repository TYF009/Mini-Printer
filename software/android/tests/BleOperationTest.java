import studio.foli.android.BleOperation;

/** Tests the same failure boundary used by asynchronous GATT callbacks. */
public final class BleOperationTest {
    public static void main(String[] args) {
        int[] calls = new int[2];
        BleOperation.run(() -> calls[0]++, () -> calls[1]++);
        if (calls[0] != 1 || calls[1] != 0) throw new AssertionError("Successful operation");
        BleOperation.run(() -> { throw new SecurityException("permission revoked"); }, () -> calls[1]++);
        BleOperation.run(() -> { throw new IllegalStateException("adapter stopped"); }, () -> calls[1]++);
        if (calls[1] != 2) throw new AssertionError("Access changes invoke recovery once");
        try {
            BleOperation.run(() -> { throw new IllegalArgumentException("programmer error"); }, () -> calls[1]++);
            throw new AssertionError("Programming errors must propagate");
        } catch (IllegalArgumentException expected) { }
        if (calls[1] != 2) throw new AssertionError("Do not mislabel programming errors");
        System.out.println("PASS: BLE access-change recovery boundary");
    }
}
