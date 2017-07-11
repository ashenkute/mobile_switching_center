/*
 * Alazar Shenkute
 * this class represents an instance of a single process
 *
 */
public class Process implements Comparable<Process>{

    private String processId;   
    private Integer arrivalTime;   
    private Integer remainingTime;
    private Integer duration;  
    private Integer cell;
    private Integer channel;
    private char eventType;     //'A' = call attempt and 'D' = call disconnect 
    private Integer SIR;
    public Process(){}
    public Process( String processId, Integer arrivalTime , Integer cell, Integer duration, char type ){
	   setAll( processId, arrivalTime, cell, duration, type );
    }
    public Integer getSIR(){
        return SIR;
    }
    public String getProcessId(){
        return processId;
    }
    public Integer getArrivalTime(){
        return arrivalTime;
    }
    public Integer getRemainingTime(){
        return remainingTime;
    }    
    public Integer getCell(){
        return cell;
    }
    public Integer getDuration(){
        return duration;
    }
    public Integer getChannel(){
        return channel;
    }
    public char getEventType(){
        return eventType;
    }
    public void setSIR( Integer SIR ){
        this.SIR = SIR;
    }
    public void setEventType( char eventType ){
        this.eventType = eventType;
    }
    public void setChannel( Integer channel ){
        this.channel = channel;
    }
    public void setProcessId( String processId ){
	   this.processId = processId;
    }
    public void setArrivalTime( Integer arrivalTime ){
	   this.arrivalTime = arrivalTime;
    }
    public void setRemainingTime( Integer remainingTime ){
        this.remainingTime = remainingTime;
    }
    public void setCell( Integer cell ){
        this.cell = cell;
    }
    public void setDuration( Integer duration ){
        this.duration = duration;
    }  
    public void setAll( String processId, Integer arrivalTime, Integer cell, Integer duration, char type ){
        setProcessId( processId );
        setArrivalTime( arrivalTime );
        setCell( cell );
        setDuration( duration );
        setEventType( type );
    }
    @Override
    public String toString(){
        return processId + " " + arrivalTime + " " + cell + " " + duration + " " + eventType;
    }
    public int compareTo( Process p ){
        Process rhs = (Process) p;
        return this.arrivalTime - rhs.arrivalTime;
    }

}