package moa.noveltydetection;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.Classifier;
import moa.core.Example;
import moa.learners.Learner;

public interface NoveltyDetection extends Learner<Example<Instance>>{
	
	public Classifier[] getSubClassifiers();
	
	public boolean correctlyClassifies(Instance inst);
	
	public double[] getVotesForInstance(Instance inst);
	
	public Prediction getPredictionForInstance(Instance inst);
	
	public void trainOnInstance(Instance inst);
	
	public void atFinal();


}
