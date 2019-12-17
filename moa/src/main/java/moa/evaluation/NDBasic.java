package moa.evaluation;

import java.util.ArrayList;

import moa.cluster.Clustering;
import moa.gui.visualization.DataPoint;

public class NDBasic extends MeasureCollection implements NDMeasureCollection {

	private static final long serialVersionUID = 1L;

	public NDBasic() {
		super();
	}
	
	@Override
	protected String[] getNames() {
		String[] names = {"Accuracy", "MNew", "FNew", "Err", "Unknown rate"};
        return names;

	}
	
	 @Override
	protected boolean[] getDefaultEnabled() {
	        boolean[] defaults = {true, true, true, true, true};
	        return defaults;
	 }

	@Override
	protected void evaluateClustering(Clustering clustering, Clustering trueClustering, ArrayList<DataPoint> points)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}
