package hla13.statistics;

public class PatientStatistics {
    int patientNumber;
    int doctorNumber;
    double getToReceptionTime;
    double getToQueTime;
    double getToDoctorTime;

    PatientStatistics(int patientNumber, double getToReceptionTime){
        this.patientNumber = patientNumber;
        this.getToReceptionTime = getToReceptionTime;
    }

}
