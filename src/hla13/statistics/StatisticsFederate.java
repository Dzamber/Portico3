package hla13.statistics;

import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import static hla13.clinic.ConstClass.extendedSimulationTime;
import static hla13.clinic.ConstClass.maxSimulationTime;

public class StatisticsFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

	private RTIambassador rtiamb;
	private StatisticsAmbassador fedamb;
    protected ArrayList<PatientStatistics> patientsStatistics = new ArrayList<>();
    protected ArrayList<DoctorStatistics> doctorStatistics = new ArrayList<>();
	public void runFederate() throws Exception{
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

		try
		{
			File fom = new File( "producer-consumer.fed" );
			rtiamb.createFederationExecution( "ExampleFederation",
			                                  fom.toURI().toURL() );
			log( "Created Federation" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			log( "Didn't create federation, it already existed" );
		}
		catch( MalformedURLException urle )
		{
			log( "Exception processing fom: " + urle.getMessage() );
			urle.printStackTrace();
			return;
		}
		
		fedamb = new StatisticsAmbassador();
		rtiamb.joinFederationExecution( "StatisticsFederate", "ExampleFederation", fedamb );
		log( "Joined Federation as " + "StatisticsFederate" );

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }


        enableTimePolicy();

		publishAndSubscribe();
		log( "Published and Subscribed" );

		for (int i = 0; i < 10; i++){
		    fedamb.externalEvents.add(new ExternalEvent(0, i, ExternalEvent.EventType.GETdoctor, 15.0));
		}

		
		while(fedamb.running && fedamb.federateTime < extendedSimulationTime) {
            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ExternalEvent.ExternalEventComparator());
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    //fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case ADDpatientToQue:
                            log("ADDpatientToQue " + externalEvent.getPersonNumber() + " time: " + externalEvent.getTime());
                            for(int i = 0; i<patientsStatistics.size(); i++){
                                if(patientsStatistics.get(i).patientNumber == externalEvent.getPersonNumber()){
                                    patientsStatistics.get(i).getToQueTime = externalEvent.getTime();
                                    break;
                                }
                            }
                            break;
                        //case GET:
                        //    ;
//
                        //    break;
                        case ADDdoctor:
                            log("ADDdoctor " + externalEvent.getPersonNumber() + " time: " + externalEvent.getTime());
                            boolean found = false;
                            for(int i = 0;i<doctorStatistics.size(); i++){
                                if(doctorStatistics.get(i).doctorNumber == externalEvent.getPersonNumber()){
                                    doctorStatistics.get(i).workTime += externalEvent.getTime();
                                    found = true;
                                    break;
                                }
                            }
                            if(found == false){
                                doctorStatistics.add(new DoctorStatistics(externalEvent.getPersonNumber()));
                            }
                            break;

                        case GETdoctor:
                            log("GETdoctor doctor: " + externalEvent.getDoctorNumber() + ", patient:" + externalEvent.getPersonNumber() + " time: " + externalEvent.getTime());
                            for(int i = 0; i<patientsStatistics.size(); i++){
                                if(patientsStatistics.get(i).patientNumber == externalEvent.getPersonNumber()){
                                    patientsStatistics.get(i).getToDoctorTime = externalEvent.getTime();
                                    for(int j = 0; j < doctorStatistics.size(); j++){
                                        if(doctorStatistics.get(j).doctorNumber == externalEvent.getDoctorNumber()){
                                            doctorStatistics.get(j).amountOfPatientsTreated++;
                                            doctorStatistics.get(j).workTime -= externalEvent.getTime();
                                            patientsStatistics.get(i).doctorNumber = externalEvent.getDoctorNumber();
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        case ADDpatientToReception:
                            log("ADDpatientToReception " + externalEvent.getPersonNumber() + " time: " + externalEvent.getTime());
                            PatientStatistics patientToAdd = new PatientStatistics(externalEvent.getPersonNumber(), externalEvent.getTime());
                            patientsStatistics.add(patientToAdd);
                            break;
                    }
                }
            }
            fedamb.externalEvents.clear();

			rtiamb.tick();
            advanceTime(1.0);
		}

		rtiamb.resignFederationExecution( ResignAction.NO_ACTION );
		log( "Resigned from Federation" );
		
		try
		{
			rtiamb.destroyFederationExecution( "ExampleFederation" );
			log( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			log( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			log( "Didn't destroy federation, federates still joined" );
		}
        //ObjectMapper mapper = new ObjectMapper();
	}

    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        // request the advance
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }
	
	private void publishAndSubscribe() throws RTIexception {
        int getDoctorHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.DoctorTreatPatient" );
        fedamb.doctorTreatPatientHandle = getDoctorHandle;
        log("getDoctorHandle " + getDoctorHandle);
        rtiamb.subscribeInteractionClass( getDoctorHandle );
        log("Subscribed to DoctorTreatPatient");


        int addPatientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientQue" );
        fedamb.addPatientToQueHandle = addPatientHandle;
        log("addPatientHandle " + addPatientHandle);
        rtiamb.subscribeInteractionClass( addPatientHandle );
        log("Subscribed to AddPatientQue");

        int addDoctorHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddDoctorQue" );
        fedamb.addDoctorHandle = addDoctorHandle;
        log("addDoctorHandle " + addDoctorHandle);
        rtiamb.subscribeInteractionClass( addDoctorHandle );
        log("Subscribed to AddDoctorQue");

        int addPatientToReceptionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientToReception" );
        fedamb.addPatientToReceptionHandle = addPatientToReceptionHandle;
        log("addPatientToReceptionHandle " + addPatientToReceptionHandle);
        rtiamb.subscribeInteractionClass( addPatientToReceptionHandle );
        log("Subscribed to AddPatientToReception");

	}

    private void enableTimePolicy() throws RTIexception
    {

        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        ////////////////////////////
        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        // tick until we get the callback
        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private LogicalTime convertTime( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    /**
     * Same as for {@link #convertTime(double)}
     */
    private LogicalTimeInterval convertInterval( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTimeInterval( time );
    }


    private void log( String message )
	{
		System.out.println( "StatisticsFederate  : " + message );
	}

	public static void main(String[] args) {
		StatisticsFederate sf = new StatisticsFederate();
		
		try {
			sf.runFederate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
