package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;

import javax.swing.JOptionPane;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.core.Utils;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.NDPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.evaluation.preview.PreviewCollectionLearningCurveWrapper;
import moa.noveltydetection.AbstractNoveltyDetection;
import moa.noveltydetection.NoveltyDetection;
import moa.options.ClassOption;
import moa.streams.clustering.ClusteringStream;

public class EvaluateNoveltyDetection extends NoveltyDetectionMainTask implements Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public String getPurposeString() {
		return "Detect novelty detection on a stream by classifying then training with each example in sequence.";
	}

	public ClassOption streamOption = new ClassOption("stream", 's', "Stream to evaluate on.", ClusteringStream.class,
			"FileStream");

	public ClassOption algorithmOption = new ClassOption("algorithm", 'm', "Algorithm to use.",
			AbstractNoveltyDetection.class, "minas.Minas");

	/*
	 * public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
	 * "Classification performance evaluation method.",
	 * LearningPerformanceEvaluator.class,
	 * "WindowClassificationPerformanceEvaluator");
	 */
	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
			"Classification performance evaluation method.", NDPerformanceEvaluator.class,
			"BasicNoveltyPerformanceEvaluator");

	public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
			"File to append output predictions to.", null, "pred", true);

	public FileOption dumpFileOption = new FileOption("dumpFile", 'd', "File to append intermediate csv results to.",
			null, "csv", true);

	public IntOption sampleFrequencyOption = new IntOption("sampleFrequency", 'f',
			"How many instances between samples of the learning performance.", 1000, 0, Integer.MAX_VALUE);

	public EvaluateNoveltyDetection() {
	}

	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		long instancesProcessed = 0;

		ClusteringStream stream;

		try {
			NoveltyDetection learner = (NoveltyDetection) getPreparedClassOption(this.algorithmOption);
			stream = (ClusteringStream) ClassOption.cliStringToObject(streamOption.getValueAsCLIString(),
					ClusteringStream.class, null);
			AbstractNoveltyDetection algorithm = (AbstractNoveltyDetection) ClassOption
					.cliStringToObject(algorithmOption.getValueAsCLIString(), NoveltyDetection.class, null);
			// LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator)
			// getPreparedClassOption(this.evaluatorOption);
			NDPerformanceEvaluator evaluator = (NDPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
			LearningCurve learningCurve = new LearningCurve("learning evaluation instances");

			if(learner == null) {
				JOptionPane.showMessageDialog(null, "Learner missing", "Info: " + "Param missing", JOptionPane.INFORMATION_MESSAGE);
				return null;
			} else if(stream == null) {
				JOptionPane.showMessageDialog(null, "Stream missing", "Info: " + "Param missing", JOptionPane.INFORMATION_MESSAGE);
				return null;
			} else if(algorithm == null) {
				JOptionPane.showMessageDialog(null, "Algorithm missing", "Info: " + "Param missing", JOptionPane.INFORMATION_MESSAGE);
				return null;
			} else if (evaluator == null) {
				JOptionPane.showMessageDialog(null, "Evaluator missing", "Info: " + "Param missing", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			
			stream.prepareForUse();
			algorithm.resetLearning();

			monitor.setCurrentActivity("Training...", -1);

			File dumpFile = this.dumpFileOption.getFile();
			PrintStream immediateResultStream = null;
			if (dumpFile != null) {
				try {
					if (dumpFile.exists()) {
						immediateResultStream = new PrintStream(new FileOutputStream(dumpFile, true), true);
					} else {
						immediateResultStream = new PrintStream(new FileOutputStream(dumpFile), true);
					}
				} catch (Exception ex) {
					throw new RuntimeException("Unable to open immediate result file: " + dumpFile, ex);
				}
			}

			File outputPredictionFile = this.outputPredictionFileOption.getFile();
			PrintStream outputPredictionResultStream = null;
			if (outputPredictionFile != null) {
				try {
					if (outputPredictionFile.exists()) {
						outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile, true),
								true);
					} else {
						outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile),
								true);
					}
				} catch (Exception ex) {
					throw new RuntimeException("Unable to open prediction result file: " + outputPredictionFile, ex);
				}
			}

			boolean firstDump = true;

			TimingUtils.enablePreciseTiming();

			boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
			long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
			boolean firstTesting = false;

			while (stream.hasMoreInstances()) {
				Example<Instance> example = (Example<Instance>) stream.nextInstance();

				// if training
				if (instancesProcessed < (int) algorithm.numTrainExamplesOption.getValue()) {
					algorithm.trainOnInstance((Instance) example.getData());
				}
				// else testing
				else {
					if (firstTesting == false) {
						evaluator.reset(algorithm.getC());
						firstTesting = true;
						monitor.setCurrentActivity("Testing...", -1);
					}

					double[] predictedClass = algorithm.getVotesForInstance(example);
					int trueClass = (int) ((Instance) example.getData()).classValue();
					evaluator.addResult(example, predictedClass);

					if (outputPredictionFile != null) {
						outputPredictionResultStream.println((Arrays.stream(predictedClass).max().getAsDouble() == 0
								? "?"
								: Utils.maxIndex(predictedClass)) + ","
								+ (((Instance) example.getData()).classIsMissing() == true ? " ? " : trueClass));
					}

					algorithm.trainOnInstance((Instance) example.getData());

					if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
		                    || stream.hasMoreInstances() == false) {
						long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
						double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);

						learningCurve.insertEntry(new LearningEvaluation(new Measurement[] {
								new Measurement("learning evaluation instances", instancesProcessed), 
								new Measurement("evaluation time (" + (preciseCPUTiming ? "cpu " : "") + "seconds)", time) },
								evaluator, learner));
						if (immediateResultStream != null) {
							if (firstDump) {
								immediateResultStream.println(learningCurve.headerToString());
								firstDump = false;
							}
							immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
							immediateResultStream.flush();
						}
					}
				}

				if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
					if (monitor.taskShouldAbort()) {
						return null;
					}
					long estimatedRemainingInstances = stream.estimatedRemainingInstances();

					monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
							: (double) instancesProcessed
									/ (double) (instancesProcessed + estimatedRemainingInstances));
					if (monitor.resultPreviewRequested() && firstTesting) {
						monitor.setLatestResultPreview(new PreviewCollectionLearningCurveWrapper((LearningCurve)learningCurve.copy(), this.getClass()));
					}
				}
				instancesProcessed++;
			}

			return new PreviewCollectionLearningCurveWrapper(learningCurve, this.getClass());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
