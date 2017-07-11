/*
  Alazar Shenkute
  October 31st 2016
  Simulation of a Mobile switching center using fixed channel allocation scheme
  15 channels, 3 cells per cluster, 3 clusters
  only care about co-channel interference 
*/
// TO DO
// handoff
// dynamic allocation

import java.lang.*;
import java.util.*;
import java.io.*;
public class MobileSwitchingCenter{
    // constants
    private static int CLUSTERS = 3, CELLS = 3,  CHANNELS = 5;
    private static char CHANNLE_IN_USE = 'Y';
    private static char CHANNEL_NOT_IN_USE = 'N';
    private static int SIGNAL_TO_INTERFERENCE_RATIO = 22;// we require SIR of 22db to complete a call
    private static int SUCCESS = 1;
    private static double SIR_NUMERATOR;
    // chose 3d array so I can use a table of 3 2d arrays of size 5 * 3, 
    // 5 is the # of avaliable voice channels in each cell, 
    // 3 is where a channel can be reused in a cell,
    // organized by interference, easy to check co-channel ineterference 
    // each table represents a cluster of cells where you can resuse the channels
    // for each table of 2d array --> row is channles, column is cell
    // channel usage mextrix keeps track of which channels are in use 
    private char [][][] channel_usage_matrix;// a voice channel can only be used in 0,1 or 2
    private int[][] distance_table; // used to store interfer for use by signal_to_interference()
    private int[][] cells_to_index; // 
    // need to map from 3d to channel number
    private HashMap<Integer, Integer> channels_to_3dindex; 
    // map cell to column index for each table
    // next two list are parallel arrays interferer --> cell 
    private List<Integer> interferers_distance; // needed so I can display interferers if call rejected
    private List<Integer> interferers_cell;// needed to display which cell the interferers are
    private List<Process> event_queue;
    private int num_of_accepted_calls;
    private int num_of_rejected_calls;
    private int num_of_call_attempts;
    private Process event;// which event is being processed 
    private int average_SIR;
    
    public MobileSwitchingCenter( List<Process> queue )
    {
	event_queue = queue;
	channel_usage_matrix = new char[CLUSTERS][CHANNELS][CELLS];
	distance_table = new int[9][9];
	cells_to_index = new int[CELLS][CELLS];
	channels_to_3dindex = new HashMap<Integer, Integer>();
	num_of_call_attempts = num_of_accepted_calls = num_of_rejected_calls = 0;
	interferers_distance = new ArrayList<>();
	interferers_cell = new ArrayList<>();
	init();// intialize tabbles, mapp index to channel in 3d array
	SIR_NUMERATOR = Math.pow( 1000.0, -4.0 );
    }
    // fill distance table with distance information
    // intialize channel usage metrix
    private void init()
    {
	init_distance_table();
	init_channel_usage_matrix();
	map_channels_to_index();
	init_cells_to_index();
    }
    // used to calculate co-channel interference 
    // first fill everything with zeros then fill the top half with the distance information
    private void init_distance_table()
    {
	for( int i = 0; i < 9; i++ ){
	    for( int j = 0; j < 9; j++ ){
		distance_table[i][j] = 0;
	    }
	}
	distance_table[0][0] = distance_table[1][1] = distance_table[2][2] = distance_table[3][3] = 0;
	distance_table[4][4] = distance_table[5][5] = distance_table[6][6] = distance_table[7][7] = 0;
	distance_table[8][8] = 0;
	distance_table[0][1] = distance_table[0][2] = distance_table[1][2] = distance_table[1][3] = distance_table[1][5] = distance_table[2][5] = distance_table[2][6] = 2000;
	distance_table[3][4] = distance_table[3][5] = distance_table[4][5] = distance_table[4][7] = distance_table[5][6] = distance_table[5][7] = distance_table[6][7] = 2000;
	distance_table[6][8] =  distance_table[7][8] = 2000;
	distance_table[0][3] = distance_table[0][5] = distance_table[0][6] = distance_table[1][4] = distance_table[1][6] = distance_table[1][7] = distance_table[2][3] = 4000;
	distance_table[2][4] = distance_table[2][7] = distance_table[2][8] = distance_table[3][6] = distance_table[3][7] = distance_table[4][6] = distance_table[4][8] = 4000;
	distance_table[5][8] = 4000;
	distance_table[0][4] = distance_table[0][7] = distance_table[0][8]= distance_table[1][8] = distance_table[3][8] = 6000;

    }
    // initially all channels are free for use
    // only time I traverse my 3d array
    private void init_channel_usage_matrix()
    {
	for( int i = 0; i < CLUSTERS; i++ ){
	    for( int j = 0; j < CHANNELS; j++ ){
		for( int k = 0; k < CELLS; k++ ){
		    channel_usage_matrix[i][j][k] = CHANNEL_NOT_IN_USE;
		}
	    }
	}
    }
    // Tabling the complexity
    // cluster in my 3d array maps to a row here
    // channel 1 - 5 can be used in cells: 1 , 6, 9 which are in row 0, and so on
    private void init_cells_to_index()
    {
	cells_to_index[0][0] = 1;
	cells_to_index[0][1] = 6;
	cells_to_index[0][2] = 9;
	cells_to_index[1][0] = 2;
	cells_to_index[1][1] = 5;
	cells_to_index[1][2] = 7;
	cells_to_index[2][0] = 3;
	cells_to_index[2][1] = 4;
	cells_to_index[2][2] = 8;
    }
    // since I am storing 5 channels in each array of 2d's
    // it wraps every 5th
    // map channels to index, 0 - 4
    // tells me which row a channel is in my 3d array
    private void map_channels_to_index()
    {
	// channel 1,2,3,4,5 to indecies 0,1,2,3,4, repectively 
	int value = 0;
	for( int i = 0; i < 15; i++ ){
	    channels_to_3dindex.put( (i+1), value );
	    value++;
	    if( value == 5 ){// reset because we have 3 2d arrays, rows 0 - 4
		value = 0;
	    }
	}
    }
    private List<Integer> map_cells_to_index( int cell )
    {
	int cluster_location, cell_col;
	List<Integer> cell_info = new ArrayList<>();
	// where is the channel usage in my 3d array for this cell?
	for( int i =0; i < CELLS; i++ ){
	    for( int j=0; j < CELLS; j++ ){
		if( cell == cells_to_index[i][j] ){// can map back from cell number to 3d array,
		    cell_info.add( i );// gives me the cluster index
		    cell_info.add( j ); // gives me the index for a cell's avaliable channels 
		}
	    }
	}
	return cell_info;
    }
    // helper method for calculate_SIR
    // finds interfers and adds them to a list
    private void find_interfers( int cluster_location, int row, int col, int cell )
    {
	interferers_cell.clear();// clear previous interferers
	interferers_distance.clear();
	for( int i = 0; i < CELLS; i++ ){
	    if( channel_usage_matrix[cluster_location][col][i] == CHANNLE_IN_USE ){
		// get which cell we have the co-channel interference 
		int cell_number = cells_to_index[cluster_location][i];
		int co_channel_distance = distance_table[cell-1][cell_number-1];
		if( co_channel_distance!= 0 ){
		    interferers_distance.add( co_channel_distance );
		    interferers_cell.add( cell_number );
		}
	    }
	}
    }
    // needed when figuring out the co-channel inteference
    private Integer calculate_SIR( int cluster_location, int row, int col, int cell )
    {
	int no_intereference = 35;
	Double sum = 0.0;
	find_interfers( cluster_location, row, col, cell );
	for( int i=0; i<interferers_distance.size(); i++ ){
	    double x = Math.pow( interferers_distance.get(i), -4.0 );
	    sum += x;
	}
	if( sum == 0 ){
	    return no_intereference;
	}
	sum = SIR_NUMERATOR/ sum;
	sum = Math.log10(sum) * 10;
	return sum.intValue();
    }
    // we have a call attempt
    public void call_attempt( Process event )
    {
	this.event = event;
	num_of_call_attempts++;
	int cell = event.getCell();
	update_state( hunting_sequence( cell ), cell );
    }
    // we have a free channel when there is a call disconnect
    public void call_disconnect( Process event )
    {
	this.event = event;
	List<Integer> cell_info = map_cells_to_index( event.getCell() );
	int row = channels_to_3dindex.get( event.getChannel() );
	channel_usage_matrix[cell_info.get(0)][row][cell_info.get(1)] = CHANNEL_NOT_IN_USE;
	System.out.println( "Disconnect: Number= " + event.getProcessId() + " Start Time= " 
			    + ( event.getArrivalTime() - event.getDuration() ) 
			    + " End Time= "+ event.getArrivalTime() + " Cell=  " 
			    + event.getCell() + " Duration= " + event.getDuration() + " Channel= " + event.getChannel() );
    }
    // helper for update_state()
    private void change_state()
    {
	int t = event.getArrivalTime();
	num_of_accepted_calls++;
	event.setEventType( 'D' );
	event.setArrivalTime( event.getDuration() + t );// change name of function
	event_queue.add( event );
	event_queue.sort( ( o1, o2 ) -> o1.getArrivalTime().compareTo( o2.getArrivalTime()) );
    }
    private void update_state( int success, int cell )
    {
	if( success == SUCCESS ){// if channel sucessfully allocated, create new state
	    change_state();
	}else{
	    num_of_rejected_calls++;
	}
	print_state( success, cell );
    }
    // map from cluster to channel
    // cluster 0 contains channels 1-5 respectively, cluster 2: 6-10, cluster 3: 11-15
    private int cluster_to_channel( int cluster, int row )
    {
	if( cluster == 0 ){
	    return row + 1;
	}else if( cluster == 1 ){
	    return row + 6;
	}else{
	    return row + 11;
	}
    }
    // allocate the next avaliable channel
    private int hunting_sequence( int cell )
    {
	List<Integer> cell_info = map_cells_to_index( cell );
	int cluster_location = cell_info.get( 0 );
	int cell_col = cell_info.get( 1 );
	boolean channel_allocated = false;
	int ret_value = 0;// 1 is SUCCESS, anything else means call got rejected
	int SIR = SIGNAL_TO_INTERFERENCE_RATIO;
	int row = 0;
	while( row < CHANNELS && channel_allocated == false ){
	    if( channel_usage_matrix[cluster_location][row][cell_col] == CHANNEL_NOT_IN_USE){// dynamic allocation would not work?
		SIR =  calculate_SIR( cluster_location, cell_col, row, cell );
		if( SIR >= SIGNAL_TO_INTERFERENCE_RATIO ){
		    channel_usage_matrix[cluster_location][row][cell_col] = CHANNLE_IN_USE;
		    channel_allocated = true;// we have successfully allocated a channel, we are done
		    ret_value = SUCCESS;
		}
	    }
	    row++;
	}
	event.setChannel( cluster_to_channel( cluster_location, row-1 ) ); 
	event.setSIR( SIR );
	average_SIR += SIR;
	return ret_value;
    }
    public void print_stat()
    {
	double num_attempts = (double) num_of_call_attempts;
	double num_blocked = (double) num_of_rejected_calls;
	System.out.println( "Totals: " + num_of_accepted_calls + " calls accepted, " 
			    + num_of_rejected_calls + " calls rejected, "
			    + num_blocked/num_attempts + "% GOS, "
			    + "Average SIR = "  + (average_SIR/num_of_accepted_calls) );
    }
    private void print_state( int success, int cell )
    {
	List<Integer> cell_info = map_cells_to_index( cell );
	int cluster_location = cell_info.get( 0 );
	int cell_loc = cell_info.get( 1 );
	System.out.print( "New Call: " + event.getProcessId() + "\t");
	if( success == SUCCESS ) {
	    System.out.print( (event.getArrivalTime() -  event.getDuration()) + "\t" + event.getCell() + "\t" );
	}else{
	    System.out.print( event.getArrivalTime() + "\t" +  event.getCell() + "\t" );
	} 
	System.out.print( event.getDuration() );
	if( success == SUCCESS ){
	    System.out.println( " Accepted, Channel = " + event.getChannel() + " SIR=" + event.getSIR()  );
	    System.out.print("interferers: ");
	    if( interferers_distance.size() == 0 ){
		System.out.println("None");
	    }else{
		for( int i = 0; i < interferers_distance.size(); i++ ){
		    System.out.print( interferers_cell.get(i) + "/" + interferers_distance.get(i) + " " );
		}
		System.out.println();
	    }
	}else{
	    System.out.print( " Rejected\nReasons: " );
	    for( int i = 0; i < CHANNELS; i++ ){
		if( channel_usage_matrix[cluster_location][i][cell_loc] == CHANNLE_IN_USE ){
		    System.out.print( (i+1) + "/In Use "  );
		}else{
		    System.out.print( (i+1) + "/Low SIR=" + calculate_SIR( cluster_location, cell_loc, i, cell ) + " ");
		}
	    }
	    System.out.println();
	}
    }
}
