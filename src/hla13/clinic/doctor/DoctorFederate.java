package hla13.clinic.doctor;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

import static hla13.clinic.ConstClass.extendedSimulationTime;
import static hla13.clinic.ConstClass.maxSimulationTime;

/**
 * Created by Michal on 2016-04-27.
 */
public class DoctorFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private DoctorAmbassador fedamb;
    private final double timeStep           = 5.0;
    private int doctorHlaHandle;
    private ArrayList<Integer> doctorsCurrentlyWaitingInQue = new ArrayList<>();
    static int doctorMaxAmount = 10;
    private final String doctorNumberString = "doctorNumber";

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

        fedamb = new DoctorAmbassador();
        rtiamb.joinFederationExecution( "DoctorFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as DoctorFederate");

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

        registerDoctorObject();
        //int firstDoctorsAdd = 0;
        while(doctorsCurrentlyWaitingInQue.size() < doctorMaxAmount){
            int doctorNumber = 0;
            for (int i = 0; i < 10; i++){
                if (!doctorsCurrentlyWaitingInQue.contains(i)){
                    doctorNumber = i;
                    break;
                }
            }
            //fedamb.externalEvents.add(new ExternalEvent(0, doctorNumber, ExternalEvent.EventType.GETdoctor, 15.0));
            sendInteraction(15.0);
            //updateHLAObject(fedamb.federateTime + fedamb.federateLookahead +doctorNumber);
            //log("Doctor number " + doctorNumber + " first time added");
            doctorsCurrentlyWaitingInQue.add(doctorNumber);

        }

        while (fedamb.running && fedamb.federateTime < extendedSimulationTime) {
            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ExternalEvent.ExternalEventComparator());
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    //fedamb.federateTime = externalEvent.getTime();
                    if (externalEvent.getEventType() == ExternalEvent.EventType.GETdoctor) {
                        log(" GETdoctor doctor: " + externalEvent.getDoctorNumber());
                        this.doctorsCurrentlyWaitingInQue.remove(Integer.valueOf(externalEvent.getDoctorNumber()));
                    }
                    if(doctorsCurrentlyWaitingInQue.size() < doctorMaxAmount){
                        log(" GETdoctor time:" + (fedamb.federateTime + fedamb.federateLookahead));
                        sendInteraction(fedamb.federateTime + fedamb.federateLookahead + randomTime());
                        updateHLAObject(fedamb.federateTime + fedamb.federateLookahead  + randomTime());
                        int doctorNumber = 0;
                        for (int i = 0; i < 10; i++){
                            if (!doctorsCurrentlyWaitingInQue.contains(i)){
                                doctorNumber = i;
                                break;
                            }
                        }
                        doctorsCurrentlyWaitingInQue.add(doctorNumber);
                    }
                }
                fedamb.externalEvents.clear();
            }
            advanceTime(timeStep + fedamb.federateLookahead);
            log("Current time :" + fedamb.federateTime);
            log("Current amount of doctors: " + doctorsCurrentlyWaitingInQue.size());
            rtiamb.tick();
        }

        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Doctor");
        rtiamb.unpublishObjectClass(classHandle);
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

    private void sendInteraction(double timeToSend) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        //
        int doctorNumber = 0;
        for (int i = 0; i < doctorMaxAmount; i++){
            if (!doctorsCurrentlyWaitingInQue.contains(i)){
                doctorNumber = i;
                break;
            }
        }
        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddDoctorQue");
        int doctorNumberHandle = rtiamb.getParameterHandle(this.doctorNumberString, interactionHandle );

        LogicalTime time = convertTime( timeToSend );
        byte[] doctorNumberByte = EncodingHelpers.encodeInt(doctorNumber);
        parameters.add(doctorNumberHandle, doctorNumberByte);

        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }

    private void updateHLAObject(double time) throws RTIexception{
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        int classHandle = rtiamb.getObjectClass(doctorHlaHandle);
        int doctorNumberHandle = rtiamb.getAttributeHandle(doctorNumberString, classHandle );
        int doctorNumber = 0;
        for (int i = 0; i < 10; i++){
            if (!doctorsCurrentlyWaitingInQue.contains(i)){
                doctorNumber = i;
                break;
            }
        }
        byte[] doctorNumberByte = EncodingHelpers.encodeInt(doctorNumber);

        attributes.add(doctorNumberHandle, doctorNumberByte);
        LogicalTime logicalTime = convertTime( time );
        rtiamb.updateAttributeValues( doctorHlaHandle, attributes, "actualize doctor".getBytes(), logicalTime );
        //rtiamb.getAttribute
    }


    private void registerDoctorObject() throws RTIexception {
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Doctor");
        int doctorHandle    = rtiamb.getAttributeHandle(doctorNumberString, classHandle );
        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( doctorHandle );
        rtiamb.publishObjectClass(classHandle, attributes);
        this.doctorHlaHandle = rtiamb.registerObjectInstance(classHandle);

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
        int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddDoctorQue");
        rtiamb.publishInteractionClass(addProductHandle);

        int getDoctorHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.DoctorTreatPatient" );
        fedamb.getDoctorHandle = getDoctorHandle;
        rtiamb.subscribeInteractionClass( getDoctorHandle );
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

    private double randomTime() {
        Random r = new Random();
        double minimalTimeForDoctorWork = 600.0; //10 minut
        double maximalTimeForDoctorWork = 1500.0; //25 minut
        return r.nextDouble()*(maximalTimeForDoctorWork-minimalTimeForDoctorWork+1)+minimalTimeForDoctorWork;
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
        System.out.println( "DoctorFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new DoctorFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }


}
