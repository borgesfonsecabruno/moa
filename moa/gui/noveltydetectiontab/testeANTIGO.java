package moa.gui.noveltydetectiontab;

import moa.noveltydetection.NoveltyDetection;
import moa.streams.clustering.ClusteringStream;

public class testeANTIGO {
	protected ClusteringStream stream;
	protected NoveltyDetection algorithm;

	public testeANTIGO() {
		
	}
	
	public void start(ClusteringStream c, NoveltyDetection algo) {
		this.stream = c;
		this.algorithm = algo;
		this.begin();
	} 
	
	public void begin() {
		System.out.println("Comeco");
		stream.prepareForUse();
		algorithm.resetLearning();
		
		while(stream.hasMoreInstances()) {
			algorithm.trainOnInstance(stream.nextInstance().getData());
		}

		algorithm.atFinal();
		
		
	}
}
