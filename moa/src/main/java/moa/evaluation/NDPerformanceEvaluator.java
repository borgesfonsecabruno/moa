package moa.evaluation;

import java.util.List;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Example;

/**
 * Interface implemented by Novelty Detection evaluators to monitor
 * the predicted class of the novelty process.
 **/
public interface NDPerformanceEvaluator extends LearningPerformanceEvaluator<Example<Instance>> {
	
	public void reset(Integer normal);

}
