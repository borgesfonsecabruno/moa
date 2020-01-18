package moa.noveltydetection;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.classifiers.Classifier;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.gui.AWTRenderer;
import moa.learners.Learner;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public abstract class AbstractNoveltyDetection extends AbstractOptionHandler implements NoveltyDetection {

	private static final long serialVersionUID = 1L;

	public IntOption numTrainExamplesOption = new IntOption("numTrainExamples", 'n',
			"Number of train examples used in the offline phase", 10000);

	protected int randomSeed = 1;

	protected IntOption randomSeedOption;

	protected InstancesHeader modelContext;

	protected double trainingWeightSeenByModel = 0.0;

	public Random noveltyRandom;

	protected int C;

	public AbstractNoveltyDetection() {
		if (isRandomizable()) {
			this.randomSeedOption = new IntOption("randomSeed", 'r',
					"Seed for random behaviour of the Novelty Detecction.", 1);
		}
	}

	@Override
	public String getPurposeString() {
		return "MOA Novelty Detection: " + getClass().getCanonicalName();
	}

	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		if (this.randomSeedOption != null) {
			this.randomSeed = this.randomSeedOption.getValue();
		}
		if (!trainingHasStarted()) {
			resetLearning();
		}
	}

	@Override
	public void setModelContext(InstancesHeader ih) {
		if ((ih != null) && (ih.classIndex() < 0)) {
			throw new IllegalArgumentException("Context for a Novelty Detection must include a class to learn");
		}
		if (trainingHasStarted() && (this.modelContext != null)
				&& ((ih == null) || !contextIsCompatible(this.modelContext, ih))) {
			throw new IllegalArgumentException("New context is not compatible with existing model");
		}
		this.modelContext = ih;
	}

	@Override
	public InstancesHeader getModelContext() {
		return this.modelContext;
	}

	@Override
	public double[] getVotesForInstance(Example<Instance> example) {
		return getVotesForInstance(example.getData());
	}

	@Override
	public abstract double[] getVotesForInstance(Instance inst);

	@Override
	public Prediction getPredictionForInstance(Example<Instance> example) {
		return getPredictionForInstance(example.getData());
	}

	@Override
	public void setRandomSeed(int s) {
		this.randomSeed = s;
		if (this.randomSeedOption != null) {
			// keep option consistent
			this.randomSeedOption.setValue(s);
		}
	}

	@Override
	public boolean trainingHasStarted() {
		return this.trainingWeightSeenByModel > 0.0;
	}

	@Override
	public double trainingWeightSeenByModel() {
		return this.trainingWeightSeenByModel;
	}

	@Override
	public void resetLearning() {
		this.trainingWeightSeenByModel = 0.0;
		if (isRandomizable()) {
			this.noveltyRandom = new Random(this.randomSeed);
		}
		resetLearningImpl();
	}

	@Override
	public void trainOnInstance(Instance inst) {
		trainOnInstanceImpl(inst);
	}

	@Override
	public Learner<?>[] getSublearners() {
		return getSubClassifiers();
	}

	@Override
	public Classifier[] getSubClassifiers() {
		return null;
	}

	@Override
	public NoveltyDetection copy() {
		return (NoveltyDetection) super.copy();
	}

	@Override
	public void trainOnInstance(Example<Instance> example) {
		trainOnInstance(example.getData());
	}

	@Override
	public boolean correctlyClassifies(Instance inst) {
		return Utils.maxIndex(getVotesForInstance(inst)) == (int) inst.classValue();
	}

	public String getClassNameString() {
		return InstancesHeader.getClassNameString(this.modelContext);
	}

	public String getClassLabelString(int classLabelIndex) {
		return InstancesHeader.getClassLabelString(this.modelContext, classLabelIndex);
	}

	public String getAttributeNameString(int attIndex) {
		return InstancesHeader.getAttributeNameString(this.modelContext, attIndex);
	}

	public String getNominalValueString(int attIndex, int valIndex) {
		return InstancesHeader.getNominalValueString(this.modelContext, attIndex, valIndex);
	}

	@Override
	public AWTRenderer getAWTRenderer() {
		// TODO should return a default renderer here
		// - or should null be interpreted as the default?
		return null;
	}

	public static boolean contextIsCompatible(InstancesHeader originalContext, InstancesHeader newContext) {

		if (newContext.numClasses() < originalContext.numClasses()) {
			return false; // rule 1
		}
		if (newContext.numAttributes() < originalContext.numAttributes()) {
			return false; // rule 2
		}
		int oPos = 0;
		int nPos = 0;
		while (oPos < originalContext.numAttributes()) {
			if (oPos == originalContext.classIndex()) {
				oPos++;
				if (!(oPos < originalContext.numAttributes())) {
					break;
				}
			}
			if (nPos == newContext.classIndex()) {
				nPos++;
			}
			if (originalContext.attribute(oPos).isNominal()) {
				if (!newContext.attribute(nPos).isNominal()) {
					return false; // rule 4
				}
				if (newContext.attribute(nPos).numValues() < originalContext.attribute(oPos).numValues()) {
					return false; // rule 3
				}
			} else {
				assert (originalContext.attribute(oPos).isNumeric());
				if (!newContext.attribute(nPos).isNumeric()) {
					return false; // rule 4
				}
			}
			oPos++;
			nPos++;
		}
		return true; // all checks clear
	}

	@Override
	public Measurement[] getModelMeasurements() {
		List<Measurement> measurementList = new LinkedList<Measurement>();
		Measurement[] modelMeasurements = getModelMeasurementsImpl();
		if (modelMeasurements != null) {
			measurementList.addAll(Arrays.asList(modelMeasurements));
		}
		// add average of sub-model measurements
		Learner<?>[] subModels = getSublearners();
		if ((subModels != null) && (subModels.length > 0)) {
			List<Measurement[]> subMeasurements = new LinkedList<Measurement[]>();
			for (Learner<?> subModel : subModels) {
				if (subModel != null) {
					subMeasurements.add(subModel.getModelMeasurements());
				}
			}
			Measurement[] avgMeasurements = Measurement
					.averageMeasurements(subMeasurements.toArray(new Measurement[subMeasurements.size()][]));
			measurementList.addAll(Arrays.asList(avgMeasurements));
		}
		return measurementList.toArray(new Measurement[measurementList.size()]);
	}

	public abstract void resetLearningImpl();

	public abstract void getModelDescription(StringBuilder out, int indent);

	protected abstract Measurement[] getModelMeasurementsImpl();

	public abstract void trainOnInstanceImpl(Instance inst);

	protected static int modelAttIndexToInstanceAttIndex(int index, Instance inst) {
		return inst.classIndex() > index ? index : index + 1;
	}

	protected static int modelAttIndexToInstanceAttIndex(int index, Instances insts) {
		return insts.classIndex() > index ? index : index + 1;
	}

	public int getC() {
		return this.C;
	}

	public void setC(int C) {
		this.C = C;
	}


	
	

}
