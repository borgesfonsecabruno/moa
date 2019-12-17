package moa.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.DoubleStream;

import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class BasicNoveltyPerformanceEvaluator extends AbstractOptionHandler implements NDPerformanceEvaluator {

	private static final long serialVersionUID = 1L;

	protected Integer C;
	// sum = FE - exemplos classificados corretamente
	// len = N
	protected Estimator weightCorrect;
	// sum = FN - novidade classificados como normal
	// len = Nc
	protected Estimator mNew;
	// sum = FP - normal classificados como novidade
	// len = N-Nc
	protected Estimator fNew;
	// sum = FP + FN + FE
	// len = N
	protected Estimator err;
	// sum = peso dos elementos classificados como desconhecido
	// len = N
	protected Estimator unknownRate;
	// Soma dos pesos de todos os exemplos
	private double totalWeightObserved;

	public FlagOption accuracyOutputOption = new FlagOption("accuracyOutput",
            'a',
            "Outputs accuracy.");
	
	public FlagOption mNewOutputOption = new FlagOption("mNewOutput",
            'm',
            "Outputs MNew.");
	
	public FlagOption fNewOutputOption = new FlagOption("fNewOutput",
            'f',
            "Outputs FNew.");
	
	public FlagOption errOutputOption = new FlagOption("errOutput",
            'e',
            "Outputs Err.");
	
	public FlagOption unknowRateOption = new FlagOption("unknowRate",
            'o',
            "Unknow integrated (MNew, FNew, Err).");
	
	@Override
	public void reset(Integer normalClasses) {
		this.C = normalClasses;
		reset();
	}

	@Override
	public void reset() {
		this.weightCorrect = newEstimator();
		this.mNew = newEstimator();
		this.fNew = newEstimator();
		this.err = newEstimator();
		this.unknownRate = newEstimator();

		this.totalWeightObserved = 0;
	}

	static int i = 0;

	@Override
	public void addResult(Example<Instance> example, double[] classVotes) {
		Instance inst = example.getData();
		double weight = inst.weight();
		
		if (weight > 0.0) {
			int trueClass = (int) inst.classValue();
			int predictedClass = Utils.maxIndex(classVotes);
			this.totalWeightObserved += weight;

			boolean predictedIsUnknown = DoubleStream.of(classVotes).allMatch(x -> x == 0.0);
			boolean predictedClassIsNormal = (predictedClass <= this.C);
			boolean trueClassIsNormal = (trueClass <= this.C);
			
			// System.out.println(trueClass + "-" + predictedClass);
			if (predictedIsUnknown && this.unknowRateOption.isSet()) {
				this.weightCorrect.add(0);
				this.err.add(weight);
				this.unknownRate.add(weight);
				
				if(!trueClassIsNormal)
					this.mNew.add(weight);
				if(trueClassIsNormal)
					this.fNew.add(weight);
			} else {
				this.unknownRate.add(0);
				this.weightCorrect.add(predictedClass == trueClass ? weight : 0);
				this.err.add(predictedClass == trueClass ? 0 : weight);
				
				// Classe real é novidade
				if (!trueClassIsNormal) {
					// Verifica se a classe predita foi erroneamente rotulada com normal
					this.mNew.add(predictedClassIsNormal ? weight : 0);

					// real = predita = novidade, ie, não incrementa no FN,
					// mas pode incrementar no FE
					/*if (!predictedClassIsNormal) {
						this.err.add(trueClass == predictedClass ? 0 : weight);
					} else {
						// Nesse caso, o rótulo real é novidade e o predito é normal, ie,
						// incrementa FN, mas preciso verificar se a predição foi certa ou
						// errada para incrementar no FE
						this.err.add(trueClass == predictedClass ? weight : 2 * weight);
					}*/

				} else {
					// classe normal

					// Verifica se a classe predita foi erroneamente rotulada como novidade
					this.fNew.add(predictedClassIsNormal ? 0 : weight);		
					
					// real = predita = normal, ie, não incrementa no FP,
					// mas pode incrementar no FE
					/*if (predictedClassIsNormal) {
						this.err.add(trueClass == predictedClass ? 0 : weight);
					} else {
						// Nesse caso, o rótulo real é normal e o predito é novidade, ie,
						// incrementa FP, mas preciso verificar se a predição foi certa ou
						// errada para incrementar no FE
						this.err.add(trueClass == predictedClass ? 1 : 2);
					}*/
				}
			}
		}
		BasicNoveltyPerformanceEvaluator.i++;
	}

	@Override
	public void addResult(Example<Instance> testInst, Prediction prediction) {
		// TODO Auto-generated method stub

	}

	@Override
	public Measurement[] getPerformanceMeasurements() {
		ArrayList<Measurement> measurements = new ArrayList<Measurement>();
		double accuracy = 0.0;
		double fNew     = 0.0;
		double mNew     = 0.0;
		double err      = 0.0;
		
		if(this.accuracyOutputOption.isSet())
			accuracy = this.getFractionCorrectlyClassified();
		if(this.fNewOutputOption.isSet())
			fNew = this.getFNew();
		if(this.mNewOutputOption.isSet())
			mNew = this.getMNew();
		if(this.errOutputOption.isSet())
			err = this.getErr();
		measurements.add(new Measurement("fnew", fNew * 100.0));
		measurements.add(new Measurement("classified instances", this.getTotalWeightObserved()));
		measurements.add(
				new Measurement("classifications correct (percent)", accuracy * 100.0));
		measurements.add(new Measurement("mnew", mNew * 100.0));
		measurements.add(new Measurement("unknown rate", this.unknownRate.estimation() * 100));
		measurements.add(new Measurement("err", err * 100.0));

		Measurement[] result = new Measurement[measurements.size()];

		return measurements.toArray(result);
	}

	public double getFractionCorrectlyClassified() {
		return this.weightCorrect.estimation();
	}

	public double getTotalWeightObserved() {
		return this.totalWeightObserved;
	}

	public double getMNew() {
		// FN/Nc or sum/len
		return this.mNew.estimation();
	}

	public double getFNew() {
		// FP/(N - Nc) or sum/(len - Nc)
		return this.fNew.estimation();
	}

	public double getErr() {
		// (FP+ FN + FE)/N
		return err.estimation();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		Measurement.getMeasurementsDescription(getPerformanceMeasurements(), sb, indent);
	}

	public interface Estimator extends Serializable {

		void add(double value);

		double estimation();

	}

	public class BasicEstimator implements Estimator {

		private static final long serialVersionUID = 1L;

		protected double len;

		protected double sum;

		@Override
		public void add(double value) {
			if (!Double.isNaN(value)) {
				sum += value;
				len++;
			}
		}

		@Override
		public double estimation() {
			if (len != 0)
				return sum / len;
			return 0;
		}
	}

	protected Estimator newEstimator() {
		return new BasicEstimator();
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub
		
	}

}
