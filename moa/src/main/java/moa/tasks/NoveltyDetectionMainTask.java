package moa.tasks;

import java.util.ArrayList;

import moa.streams.clustering.ClusterEvent;

public abstract class NoveltyDetectionMainTask extends MainTask {

	private static final long serialVersionUID = 1L;
	
	protected ArrayList<ClusterEvent> events;

    protected void setEventsList(ArrayList<ClusterEvent> events) {
        this.events = events;
    }
    
    public ArrayList<ClusterEvent> getEventsList() {
        return this.events;
    }
    

}
