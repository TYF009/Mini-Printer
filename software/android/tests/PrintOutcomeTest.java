import studio.foli.android.PrintOutcome;

public final class PrintOutcomeTest {
    static void expect(PrintOutcome actual, PrintOutcome expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual);
    }
    public static void main(String[] args) {
        expect(PrintOutcome.fromStatus(true,true,false,0,24,3),PrintOutcome.STOPPED_UNCONFIRMED,"Nominal terminal is still unconfirmed");
        expect(PrintOutcome.fromStatus(true,true,false,1,24,3),PrintOutcome.FAILED,"Paper-out cannot become success");
        expect(PrintOutcome.fromStatus(true,true,false,0,65,3),PrintOutcome.FAILED,"Overheat cannot become success");
        expect(PrintOutcome.fromStatus(true,true,true,0,24,3),PrintOutcome.FAILED,"Prior failure cannot become success");
        expect(PrintOutcome.fromStatus(true,false,false,0,24,3),PrintOutcome.IN_PROGRESS,"Stale terminal without start must wait");
        expect(PrintOutcome.fromStatus(false,false,false,0,24,3),PrintOutcome.IDLE,"Idle terminal does not identify a job");
        expect(PrintOutcome.fromStatus(true,true,false,0,24,2),PrintOutcome.IN_PROGRESS,"Working remains pending");
        System.out.println("PASS: 7 legacy print-outcome cases");
    }
}
