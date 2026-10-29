import studio.foli.android.PrinterProtocol;

public final class TemperatureTest {
    static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    public static void main(String[] args) {
        check(PrinterProtocol.temperatureUnavailable(255), "255 is the sensor sentinel");
        check(PrinterProtocol.temperatureBlocksPrinting(255), "Unavailable sensor blocks heating");
        check(PrinterProtocol.temperatureBlocksPrinting(65), "Thermal threshold blocks printing");
        check(!PrinterProtocol.temperatureBlocksPrinting(64), "Ordinary value below thermal threshold");
        check("Sensor unavailable".equals(PrinterProtocol.temperatureText(255)), "No fictitious 255 degrees");
        check("0 °C".equals(PrinterProtocol.temperatureText(0)), "Legacy zero remains a reported value");
        check("—".equals(PrinterProtocol.temperatureText(-1)), "No received value remains unknown");
        System.out.println("PASS: 7 sensor-temperature cases");
    }
}
