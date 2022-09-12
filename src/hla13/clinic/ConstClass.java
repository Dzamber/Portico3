package hla13.clinic;

public class ConstClass {
    public static final String READY_TO_RUN = "ReadyToRun";

    public static final double maxSimulationTime = 3000.0;//28800.0; //8*60*60
    public static double simulationTimeForReception = maxSimulationTime - 1200.0;
    public static double extendedSimulationTime = maxSimulationTime + 1500.0;
}
