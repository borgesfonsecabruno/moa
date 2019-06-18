package moa.evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.AbstractMOAObject;
import moa.core.Example;
import moa.core.Measurement;

public class BasicNoveltyPerformanceEvaluator extends AbstractMOAObject implements NDPerformanceEvaluator {

	private static final long serialVersionUID = 1L;
	
	double sumAccuracy = 0.0;
	double sumExamples = 0.0;
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(getPerformanceMeasurements(),
                sb, indent);
	}

	@Override
	public void reset() {
		sumAccuracy = 0.0;
		sumExamples = 0.0;
	}


	@Override
	public Measurement[] getPerformanceMeasurements() {
		 Measurement m[] = new Measurement[]{
	                new Measurement("classifications correct (percent)", (double) sumAccuracy/sumExamples),

	        };
	        // reset
	        // reset();
	        return m;
	}

	@Override
	public void addResult(Example<Instance> example, int predictedClass) {
		sumExamples++;
		Instance inst = example.getData();
		if((int) inst.classValue() == predictedClass) {
			sumAccuracy++;
		}
		
	}
	
	@Override
	public void addResult(Example<Instance> testInst, Prediction prediction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addResult(Example<Instance> example, double[] classVotes) {
		// TODO Auto-generated method stub
		
	}



}
