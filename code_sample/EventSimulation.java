/*
*	Alazar Shenkute
*	October 31st 2016
*	Event Simulation
*	15 channels, 3 cells per cluster, 3 clusters
*	onyl care about co-channel interference 
*/
import java.lang.*;
import java.util.*;
import java.io.*;

public class EventSimulation{
	
	private List<Process> event_queue;
	MobileSwitchingCenter msc;
	private static char CALL_ATTEMPT = 'A';
	private static char CALL_DISCONNECT = 'D';
	public EventSimulation( List<Process> queue )
	{
		event_queue = queue;
		msc = new MobileSwitchingCenter( queue );
	}
	// take an event of the queue
	// process it
	public void processEvents()
	{
		Process event;
		while( event_queue.size() !=0 ){
			event = event_queue.get( 0 );
			event_queue.remove( event );
			if( event.getEventType() == CALL_ATTEMPT ){
				msc.call_attempt(  event );
			}else if( event.getEventType() == CALL_DISCONNECT ){
				msc.call_disconnect( event );
			}
		}
		msc.print_stat();
	}
}
