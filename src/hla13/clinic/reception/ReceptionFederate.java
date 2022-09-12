package hla13.clinic.reception;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import hla13.clinic.ExternalEvent;
import hla13.clinic.doctor.DoctorAmbassador;
import hla13.clinic.patient.PatientFederate;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;
import org.portico.lrc.services.object.msg.SendInteraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

import static hla13.clinic.ConstClass.maxSimulationTime;
import static hla13.clinic.ConstClass.simulationTimeForReception;


public class ReceptionFederate {
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private ReceptionAmbassador fedamb;
    private final double timeStep           = 5.0;
    private int receptionHlaHandle;
    private ArrayList<Integer> arrayListPatientsToReception = new ArrayList<>();
    private ArrayList<Integer> arrayListPatientsToQue = new ArrayList<>();


    public void runFederate() throws RTIexception {
        rtiamb = RtiFactoryFactory.getRtiFactory().
                createRtiAmbassador();

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

        fedamb = new ReceptionAmbassador();
        rtiamb.joinFederationExecution( "ReceptionFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as ReceptionFederate");

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


        while (fedamb.running && fedamb.federateTime < (simulationTimeForReception)) {//reception is closed hour earlier
            arrayListPatientsToQue = arrayListPatientsToReception;
            arrayListPatientsToReception = new ArrayList<>();
            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ExternalEvent.ExternalEventComparator());
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    if (externalEvent.getEventType() == ExternalEvent.EventType.ADDpatientToReception) {
                        arrayListPatientsToReception.add(externalEvent.getPersonNumber());
                        log("Patient " + externalEvent.getPersonNumber() + " got to reception");
                    }
                }
                fedamb.externalEvents.clear();
            }
            advanceTime(timeStep + fedamb.federateLookahead);
            sendInteraction(randomTime()+ fedamb.federateTime);
            log("Current time :" + fedamb.federateTime);
            //log("Current amount of doctors: " + doctorsCurrentlyWaitingInQue.size());
            rtiamb.tick();
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
    }

    private double randomTime() {
        Random r = new Random();
        double minimalTimeForDoctorWork = 600.0; //10 minut
        double maximalTimeForDoctorWork = 1200.0; //20 minut
        return r.nextDouble()*(maximalTimeForDoctorWork-minimalTimeForDoctorWork+1)+minimalTimeForDoctorWork;
    }

    private void sendInteraction(double timeToSend) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        for(int i = 0; i<arrayListPatientsToQue.size(); i++){
            byte[] quantity = EncodingHelpers.encodeInt(arrayListPatientsToQue.get(i));
            int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddPatientQue");
            int quantityHandle = rtiamb.getParameterHandle( "patientNumber", interactionHandle );
            LogicalTime time = convertTime( timeToSend + 5*i);
            parameters.add(quantityHandle, quantity);
            log("Patient " + arrayListPatientsToQue.get(i) + " got to que");
            rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
        }
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

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        int addPatientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientToReception" );
        fedamb.addPatientHandle = addPatientHandle;
        rtiamb.subscribeInteractionClass( addPatientHandle );


        int addToReceptionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientQue" );
        rtiamb.publishInteractionClass(addToReceptionHandle);
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

    private void advanceTime( double timestep ) throws RTIexception
    {
        log("requesting time advance for: " + timestep + ", in total: " + fedamb.federateTime + timestep);
        // request the advance
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private void log( String message )
    {
        System.out.println( "ReceptionFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new ReceptionFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }
}
