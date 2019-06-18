package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.core.Utils;
import moa.evaluation.LearningEvaluation;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.preview.LearningCurve;
import moa.noveltydetection.AbstractNoveltyDetection;
import moa.noveltydetection.NoveltyDetection;
import moa.options.ClassOption;
import moa.streams.InstanceStream;
import moa.streams.clustering.ClusteringStream;

public class EvaluateNoveltyDetection extends NoveltyDetectionMainTask implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Override
	public String getPurposeString() {
        return "Detect novelty detection on a stream by classifying then training with each example in sequence.";
    }

	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to evaluate on.", ClusteringStream.class,
            "FileStream");

	public ClassOption algorithmOption0 = new ClassOption("algorithm", 'm',
        "Algorithm to use.", AbstractNoveltyDetection.class, "minas.Minas");
	
	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            LearningPerformanceEvaluator.class,
            "WindowClassificationPerformanceEvaluator");
	
	public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
            "File to append output predictions to.", null, "pred", true);
			
	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv results to.", null, "csv", true);
	
	
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
			stream = (ClusteringStream) ClassOption.cliStringToObject(streamOption.getValueAsCLIString(), ClusteringStream.class, null);
			AbstractNoveltyDetection algorithm = (AbstractNoveltyDetection) ClassOption.cliStringToObject(algorithmOption0.getValueAsCLIString(), NoveltyDetection.class, null);
			LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
			LearningCurve learningCurve = new LearningCurve(
	                "learning evaluation instances");
			
			stream.prepareForUse();
			algorithm.resetLearning();
			
			monitor.setCurrentActivity("Training...", -1);
			
			File dumpFile = this.dumpFileOption.getFile();
	        PrintStream immediateResultStream = null;
	        if (dumpFile != null) {
	            try {
	                if (dumpFile.exists()) {
	                    immediateResultStream = new PrintStream(
	                            new FileOutputStream(dumpFile, true), true);
	                } else {
	                    immediateResultStream = new PrintStream(
	                            new FileOutputStream(dumpFile), true);
	                }
	            } catch (Exception ex) {
	                throw new RuntimeException(
	                        "Unable to open immediate result file: " + dumpFile, ex);
	            }
	        }
	        
	        File outputPredictionFile = this.outputPredictionFileOption.getFile();
	        PrintStream outputPredictionResultStream = null;
	        if (outputPredictionFile != null) {
	            try {
	                if (outputPredictionFile.exists()) {
	                    outputPredictionResultStream = new PrintStream(
	                            new FileOutputStream(outputPredictionFile, true), true);
	                } else {
	                    outputPredictionResultStream = new PrintStream(
	                            new FileOutputStream(outputPredictionFile), true);
	                }
	            } catch (Exception ex) {
	                throw new RuntimeException(
	                        "Unable to open prediction result file: " + outputPredictionFile, ex);
	            }
	        }
			
	        boolean firstDump = true;
	        
	        TimingUtils.enablePreciseTiming();
	        
	        double RAMHours = 0.0;
	        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
	        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
	        long lastEvaluateStartTime = evaluateStartTime;
	        boolean firstTesting = false;
	        
			while(stream.hasMoreInstances()) {
				Example<Instance> example = (Example<Instance>) stream.nextInstance();
				
				// if training
				if(instancesProcessed < (int) algorithm.numTrainExamplesOption.getValue()) {
					algorithm.trainOnInstance((Instance) example.getData());	
				}
				// else testing
				else {
					if(firstTesting == false) {
						firstTesting = true;
						monitor.setCurrentActivity("Testing...", -1);
					}
					
					double[] predictedClass = algorithm.getVotesForInstance(example);
					int trueClass = (int) ((Instance) example.getData()).classValue();
					
					evaluator.addResult(example, predictedClass);
					
					if (outputPredictionFile != null) {
		                outputPredictionResultStream.println((Arrays.stream(predictedClass).max().getAsDouble() == 0 ? "?" : Utils.maxIndex(predictedClass)) + "," +(
		                        ((Instance) example.getData()).classIsMissing() == true ? " ? " : trueClass));
		            }
							
					algorithm.trainOnInstance((Instance) example.getData());
					
					List<Measurement> measurements = new ArrayList<Measurement>(); 
					
					long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
	                double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
	                lastEvaluateStartTime = evaluateTime;
	                
	                measurements.add(new Measurement("learning evaluation instances", instancesProcessed));
	                measurements.add(new Measurement("evaluation time ("+ (preciseCPUTiming ? "cpu ": "") + "seconds)", time));
	                measurements.add(new Measurement("model cost (RAM-Hours)", 0.0));
	                
	                Measurement[] performanceMeasurements = evaluator.getPerformanceMeasurements();
		            for (Measurement measurement : performanceMeasurements) {
		            	String measurementName = measurement.getName();
		            	if(measurementName.toUpperCase().contains("KAPPA")) {
		            		measurement = new Measurement(measurementName, 0.0);
		            	}
		            	measurements.add(measurement);
		            }
		            
		            learningCurve.insertEntry(new LearningEvaluation(measurements.toArray(new Measurement[measurements.size()])));
	                
		            if (immediateResultStream != null) {
		                if (firstDump) {
		                    immediateResultStream.println(learningCurve.headerToString());
		                    firstDump = false;
		                }
		                immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
		                immediateResultStream.flush();
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
	                if (monitor.resultPreviewRequested()) {
	                    monitor.setLatestResultPreview(learningCurve.copy());
	                }
	            }
			   instancesProcessed++;
			}

			algorithm.atFinal();
			return learningCurve;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
