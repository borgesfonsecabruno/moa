package moa.evaluation;

import java.util.ArrayList;

import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

public class NDMeasureCollection extends MeasureCollection {

	private static final long serialVersionUID = 1L;

	public NDMeasureCollection() {
		super();
	}
	
	@Override
	protected String[] getNames() {
		String[] names = {"Accuracy"};
        return names;

	}
	
	 @Override
	protected boolean[] getDefaultEnabled() {
	        boolean[] defaults = {true};
	        return defaults;
	 }

	@Override
	protected void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}
