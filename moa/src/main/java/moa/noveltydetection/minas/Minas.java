package moa.noveltydetection.minas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.MOAObject;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.Measurement;
import moa.noveltydetection.AbstractNoveltyDetection;
import moa.noveltydetection.MicroCluster;

public class Minas extends AbstractNoveltyDetection {

	private static final long serialVersionUID = 1L;

	public IntOption maxNumKernelsOption = new IntOption("maxNumKernels", 'k', "Number of micro kernels to use.", 100);

	public IntOption timeWindowOption = new IntOption("horizon", 'h', "Window size to forget outdated data.", 4000);

	public IntOption minExamplesUnknownOption = new IntOption("minExamplesUnknown", 'u',
			"Unknown size to online clustering process.", 2000);

	public FloatOption thresholdOption = new FloatOption("threshold", 't', "Threshold value.", 0.7);

	public MultiChoiceOption clusteringOfflineOption = new MultiChoiceOption("clusteringAlgorithmOffline", 'a',
			"Clustering algorithm to use", new String[] { "KMeans", "CluStream" },
			new String[] { "KMeans algorithm for clustering", "CluStream algorithm for clustering" }, 0);

	/*
	 * public MultiChoiceOption clusteringOnlineOption = new MultiChoiceOption(
	 * "clusteringAlgorithmOnline", 'b', "Clustering algorithm to use", new
	 * String[]{"KMeans", "CluStream"}, new
	 * String[]{"KMeans algorithm for clustering" }, 0);
	 */

	// normal classes number
	private boolean initialized; // 0: offline phase, 1: online phase
	private Map<Integer, LinkedList<Instance>> buffer;
	/*
	 * buffer of training instances mapped to their corresponding classes. Each
	 * class of the training set has a set of examples
	 */
	private List<MicroCluster> model; // model composed by a set of micro-clusters

	private Map<Long, Instance> unknownSet; // short-term memory of unknown examples and their corresponding timestamp
	private List<MicroCluster> sleepMemory; // mechanism to forget old micro-clusters

	private long timestamp;
	private int k; // micro-cluster number of each problem class

	private int bufferSize; // number of labeled examples used in the offline phase
	private int bufferedInst; /*
								 * quantos exemplos de treinamento j� foram processados, tive que colocar essa
								 * vari�vel porque o timestamp reseta
								 */
	private int minExamplesUnknown; // m�nimo de exemplos para a mem�ria unknown ser significativa e entrar no
									// processo de clustering.
	private int lastCheck; // used in the novelty detection process
	private int minExamplesMicro = 50; // m�nimo de exemplos que um micro-grupo precisa ter para ser adicionado ao
										// modelo
	private int noveltyId = 0; // novelty identifier

	private double threshold; // used to separate novelty patterns from extensions
	private int timeWindow;

	protected int[] clusters; /* ?? kmeans control: example-class */
	private double t; /* clustreamKernel param */
	private FileWriter ArqSaida; // lembrar de tirar

	public FileWriter getArqSaida() {
		return ArqSaida;
	}

	// lembrar de tirar
	String filePath = "C:\\out_\\out_\\MOA3";
	String arqsaida = filePath + "\\resultados" + "_fold" + 1 + "_moa";

	public void cria_arqsaida() throws IOException {
		String arqsaida = filePath + "\\resultados" + "_fold" + 1 + "_moa";
		ArqSaida = new FileWriter(new File(arqsaida), false);

		String cabecalho = "Resultados";
		ArqSaida.write(cabecalho);
		ArqSaida.write("\n\n \n\n");
	}

	public String getName() {
		return "MINAS";
	}

	@Override
	public void resetLearningImpl() {
		this.initialized = false;
		this.bufferedInst = 0;
		this.lastCheck = 0;
		this.threshold = this.thresholdOption.getValue();
		this.noveltyId = 1;
		this.timeWindow = this.timeWindowOption.getValue();

		this.k = this.maxNumKernelsOption.getValue();
		this.t = 2;
		this.timestamp = -1;
		this.bufferSize = this.numTrainExamplesOption.getValue();
		this.minExamplesUnknown = this.minExamplesUnknownOption.getValue();

		this.buffer = new HashMap<Integer, LinkedList<Instance>>();
		this.model = new LinkedList<MicroCluster>();
		this.unknownSet = new HashMap<Long, Instance>();
		this.sleepMemory = new LinkedList<MicroCluster>();

		try {
			cria_arqsaida();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {

		if (!this.initialized) {

			// is training over?
			if (this.bufferedInst < this.bufferSize) {

				int index = (int) inst.classValue();
				double[] instAux = inst.toDoubleArray();

				// remove class of features - could interfere distance calcule -
				DenseInstance instance = new DenseInstance(1, Arrays.copyOfRange(instAux, 0, instAux.length - 1));

				// is buffer has this class index?
				if (this.buffer.containsKey(index)) {
					this.buffer.get(index).add(instance);

				}
				// class not belongs buffer
				else {
					LinkedList<Instance> newClass = new LinkedList<Instance>();
					newClass.add(instance);
					this.buffer.put(index, newClass);
				}

				if (this.bufferedInst < this.bufferSize - 1) {
					this.bufferedInst++;
					return;
				}
				this.bufferedInst++;

			}

			if (this.clusteringOfflineOption.getChosenIndex() == 0) { // KMeans as clustering algorithm
				for (Map.Entry<Integer, LinkedList<Instance>> map : this.buffer.entrySet()) {
					LinkedList<Instance> examples = map.getValue();

					if (map.getKey() > this.getC())
						this.setC(map.getKey());

					MicroCluster[] kernels = createKmeansModel(examples, this.k);

					// add micro cluster to model
					for (int i = 0; i < kernels.length; i++)
						updateModel(kernels[i], "normal", map.getKey());

				}

			} else { // Clustream as clustering algorithm

				for (Map.Entry<Integer, LinkedList<Instance>> map : this.buffer.entrySet()) {
					LinkedList<Instance> examples = map.getValue();

					Clustering clustreamModel = createClustreamModel(examples, this.k);

					if (map.getKey() > this.getC())
						this.setC(map.getKey());

					for (int i = 0; i < clustreamModel.size(); i++) {
						ClustreamKernel clusKernel = (ClustreamKernel) clustreamModel.get(i);
						MicroCluster micro = new MicroCluster(clusKernel, this.t, this.k);
						updateModel(micro, "normal", map.getKey());
					}

				}
			}
			
			this.initialized = true;
			this.timestamp = 1;
			return;
		}

		// online phase
		unsupervisedModelUpdate(inst);

		if (this.timestamp % this.timeWindow == 0) {
			putMicroSleep();
			removeUnknown();

		}

		this.timestamp++;
	}

	public void unsupervisedModelUpdate(Instance inst) {
		// instance identified as unkown
		// unknown memory size is significant to novelty detection
		if ((this.unknownSet.size() >= this.minExamplesUnknown) && (this.lastCheck + 2000 < this.timestamp)) {
			this.lastCheck = (int) this.timestamp;
			this.minExamplesMicro = (int) ((float) 2000 / this.k);

			/*
			 * map to identify elements that belongs to new micro cluster, and later remove
			 * from unknown
			 */
			HashMap<Long, Integer> remove = new HashMap<Long, Integer>();
			MicroCluster[] unknownModel;

			unknownModel = clusteringOnline(this.unknownSet, this.k, remove);

			// candidate groups to increase or extend model
			for (int kernelTmpIndex = 0; kernelTmpIndex < unknownModel.length; kernelTmpIndex++) {
				MicroCluster micro = unknownModel[kernelTmpIndex];

				// representative group
				if (micro != null && micro.getN() >= this.minExamplesMicro) {
					// valid group
					if (verifySilhouette(micro) == true) {
						String[] info = closerMicro(micro);
						int pos = Integer.parseInt(info[0]);
						double shortDist = Double.parseDouble(info[1]);
						double vthreshold = Double.parseDouble(info[2]);
						SortedSet<Long> keys = new TreeSet<>(remove.keySet());
						for (Long timeKey : keys) {
							if (kernelTmpIndex == remove.get(timeKey)) {
								this.unknownSet.remove(timeKey);
								remove.remove(timeKey);
							}
						}

						String fileText = "";

						// centroid distance of new group to nearest centroid group is < than a
						// threshold
						if (shortDist < vthreshold) {
							// extension
							if (this.model.get(pos).getCategory().equalsIgnoreCase("normal")
									|| this.model.get(pos).getCategory().equalsIgnoreCase("ext")) {
								// normal extension
								updateModel(micro, "ext", this.model.get(pos).getClassId());
								fileText = "Thinking " + "Extensao1: " + "C " + this.model.get(pos).getClassId() + " - "
										+ (int) micro.getN() + " exemplos";
							} else {
								// novelty extension learned online
								updateModel(micro, "extNov", this.model.get(pos).getClassId());
								fileText = "Thinking " + "ExtensaoNovidade1: " + "N " + this.model.get(pos).getClassId()
										+ " - " + (int) micro.getWeight() + " exemplos";
							}

						} else {
							// novelty
							updateModel(micro, "nov", this.noveltyId);

							if (Double.toString(this.model.get(this.model.size() - 1).getClassId())
									.compareToIgnoreCase(Integer.toString(this.noveltyId) + ".0") == 0) {
								if (checkReocurrence(micro)) {
									// novelty reocurrence
									if (micro.getCategory().equalsIgnoreCase("ext"))
										fileText = "Thinking " + "Extensao2:" + "C " + (int)  micro.getClassId() + " - "
												+ micro.getN() + " exemplos";
									else
										fileText = "Thinking " + "ExtensaoNovidade2: " + "N " + (int) micro.getClassId()
												+ " - " + micro.getN() + " exemplos";
								} else {
									// novelty
									fileText = "ThinkingNov: " + "Novidade " + this.noveltyId + " - " + (int) micro.getN()
											+ " exemplos";
									this.noveltyId++;

								}
							} else {
								fileText = "Thinking " + "ExtensaoNovidade3: " + "N "
										+ (int) this.model.get(this.model.size() - 1).getClassId() + " - " + (int) micro.getN()
										+ " exemplos";
							}
						}

						try {
							ArqSaida.write(fileText);
							ArqSaida.write("\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					} // end: valid group
				} // end: representative group
			} // end: candidate groups
		} // end: significant number unknown examples
	}

	public boolean checkReocurrence(MicroCluster micro) {

		double dist, shortDist;

		if (this.sleepMemory.size() > 0) {
			shortDist = Minas.distance(this.sleepMemory.get(0).getCenter(), micro.getCenter());
			int pos = 0;

			for (int i = 1; i < this.sleepMemory.size(); i++) {
				dist = Minas.distance(this.sleepMemory.get(i).getCenter(), micro.getCenter());

				if (dist < shortDist) {
					shortDist = dist;
					pos = i;
				}
			}
			double vthreshold = this.sleepMemory.get(pos).getRadius() / 2 * this.threshold;

			if (shortDist <= vthreshold) {
				if (this.sleepMemory.get(pos).getCategory().equalsIgnoreCase("normal")
						|| this.sleepMemory.get(pos).getCategory().equalsIgnoreCase("ext"))
					micro.setCategory("ext");
				else
					micro.setCategory("extNov");

				micro.setClassId(this.sleepMemory.get(pos).getClassId());
				this.model.add((MicroCluster) this.sleepMemory.get(pos).copy());
				this.sleepMemory.remove(pos);

				return true;
			}
			return false;
		}
		return false;

	}

	public String[] doMinasPrediction(Instance inst) {
		double[] instAux = inst.toDoubleArray();
		DenseInstance instance = new DenseInstance(1, Arrays.copyOfRange(instAux, 0, instAux.length - 1));

		double dist = 0.0;
		double shortDist = Double.MAX_VALUE;
		int pos = 0;
		String[] info = new String[2];

		for (int i = 0; i < this.model.size(); i++) {
			dist = Minas.distance(this.model.get(i).getCenter(), instance.toDoubleArray());

			if (dist <= shortDist) {
				shortDist = dist;
				pos = i;
			}
		}

		try {

			double radius = this.model.get(pos).getRadius();
			String text = "Ex: " + this.timestamp + "\t Classe Real: " + inst.classValue() + "\t Classe MINAS: ";

			if (shortDist <= radius) {
				// model.get(pos).insert(instance, timestamp);
				info[0] = Double.toString(this.model.get(pos).getClassId());
				info[1] = this.model.get(pos).getCategory();
				this.model.get(pos).setTime((long) this.timestamp);

				// instance identified as part of model
				String strRet[] = ExPertenceClassConh(info);
				String fileText = text + strRet[0];

				try {
					ArqSaida.write(fileText);
					ArqSaida.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}

				return info;
			} else {
				// add instance at unknown map
				this.unknownSet.put((long) this.timestamp, inst);
				text = text + "n�o sei ";
				try {
					ArqSaida.write(text);
					ArqSaida.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (Exception E) {
			E.printStackTrace();
			System.out.println("pos" + pos);
			System.out.println("model size: " + this.model.size());

		}

		return null;

	}

	public String[] closerMicro(MicroCluster micro) {
		double shortDist = distance(this.model.get(0).getCenter(), micro.getCenter());
		int pos = 0;
		double dist;

		for (int i = 1; i < this.model.size(); i++) {
			dist = distance(this.model.get(i).getCenter(), micro.getCenter());

			if (dist < shortDist) {
				shortDist = dist;
				pos = i;
			}
		}

		double vthreshold = this.model.get(pos).getRadius() / 2 * this.threshold;

		String[] info = new String[3];
		info[0] = Integer.toString(pos);
		info[1] = Double.toString(shortDist);
		info[2] = Double.toString(vthreshold);

		return info;

	}

	protected Clustering createClustreamModel(LinkedList<Instance> examples, int kValue) {
		MinasClustream clusteringAlgo = new MinasClustream();
		clusteringAlgo.kernelRadiFactorOption.setValue(2);
		clusteringAlgo.maxNumKernelsOption.setValue(kValue);
		clusteringAlgo.resetLearning();
		clusteringAlgo.setBufferSize(10);

		for (Instance data : examples)
			clusteringAlgo.trainOnInstanceImpl(data);

		return clusteringAlgo.getMicroClusteringResult();
	}

	protected MicroCluster[] createKmeansModel(LinkedList<Instance> examples, int kValue) {
		int dim = examples.get(0).numValues();

		this.clusters = null;

		/*
		 * array with k initial centers kmeans TODO: randomize centers
		 */
		MicroCluster[] centers = new MicroCluster[kValue];

		/*
		 * each list input is an example transformed to MicroCluster kmeans works with
		 * microcluster
		 */
		List<MicroCluster> kmeansBuffer = new LinkedList<MicroCluster>();

		for (int i = 0; i < examples.size(); i++) {

			/* transforming in MicroCluster */
			MicroCluster data = new MicroCluster(examples.get(i), dim, this.timestamp, this.t, kValue);
			kmeansBuffer.add(data);

			// Centers unrandom..
			if (i < kValue) {
				centers[i] = (MicroCluster) data.copy();
			}
		}

		Clustering kmeans_clustering = kMeans(kValue, centers, kmeansBuffer);

		MicroCluster[] kernels = new MicroCluster[kmeans_clustering.size()];

		for (int i = 0; i < this.clusters.length; i++) {
			if (kernels[this.clusters[i]] == null)
				kernels[this.clusters[i]] = new MicroCluster(new DenseInstance(1.0, kmeansBuffer.get(i).getCenter()),
						kmeansBuffer.get(i).getCenter().length, this.timestamp, this.t, kValue);
			else
				kernels[this.clusters[i]].insert(new DenseInstance(1.0, kmeansBuffer.get(i).getCenter().clone()), i);
		}

		kmeansBuffer.clear();

		return kernels;
	}

	public MicroCluster[] clusteringOnline(Map<Long, Instance> examples, int k_online, HashMap<Long, Integer> remove) {
		SortedSet<Long> keys = new TreeSet<>(examples.keySet());
		MicroCluster[] kernels = null;
		// kmeans

		LinkedList<Instance> kmeansBuffer = new LinkedList<Instance>();

		for (Long timeKey : keys) {
			double[] instAux = examples.get(timeKey).toDoubleArray();
			int tam = instAux.length;
			DenseInstance instance = new DenseInstance(1.0, Arrays.copyOfRange(instAux, 0, tam - 1));
			kmeansBuffer.add(instance);

		}

		kernels = createKmeansModel(kmeansBuffer, k_online);

		// example list do remove from unknown
		SortedSet<Long> removeKeys = new TreeSet<>(examples.keySet());
		int j = 0;
		for (Long timeKey : removeKeys) {
			remove.put(timeKey, this.clusters[j]);
			j++;
		}

		// }

		return kernels;

	}

	public void updateModel(MicroCluster micro, String category, double classId) {
		micro.setClassId(classId);
		micro.setCategory(category);
		micro.setTime(this.timestamp);
		this.model.add(micro);

		// Group intercep a existent group (novelty case)
		ArrayList<double[]> centers = new ArrayList<double[]>();
		ArrayList<String> novelty = new ArrayList<String>();
		ArrayList<Double> radius = new ArrayList<Double>();

		if (category.equalsIgnoreCase("nov")) {
			// get hyperspheres (novelty)
			for (int i = 0; i < this.model.size(); i++) {
				String cat = this.model.get(i).getCategory();
				if (cat.equalsIgnoreCase("nov")) {
					novelty.add(Double.toString(this.model.get(i).getClassId()));
					radius.add(this.model.get(i).getRadius() / 2/* this.model.get(i).getRadius() */);
					centers.add(this.model.get(i).getCenter());
				}
			}
		}
		// if centers not empty, not first novelty
		if (!centers.isEmpty()) {
			double temp_dist = 0.0;
			// center new model
			double[] center = micro.getCenter();
			// calculate distance from new model center to center of existing model center
			// which are novelty
			for (int k = 0; k < centers.size() - 1; k++) {
				temp_dist = distance(center, centers.get(k));
				if (temp_dist < (radius.get(k) + micro.getRadius() / 2)) {
					this.model.get(this.model.size() - 1).setClassId(Double.parseDouble(novelty.get(k)));
					this.model.get(this.model.size() - 1).setCategory("extNov");
				}
			}
		}
	}

	public boolean verifySilhouette(MicroCluster micro) {
		double shortDistance = 0;
		double dist = 0;
		shortDistance = Minas.distance(this.model.get(0).getCenter(), micro.getCenter());

		for (int i = 1; i < this.model.size(); i++) {
			dist = distance(this.model.get(i).getCenter(), micro.getCenter());
			if (dist < shortDistance)
				shortDistance = dist;
		}

		double b = shortDistance;
		double a = micro.getRadius() / 2;
		double silhouette = (b - a) / (Math.max(b, a));
		if (silhouette > 0)
			return true;
		else
			return false;
	}

	public String[] ExPertenceClassConh(String retorno[]) {
		String textoArq = "", texto = "";

		if (retorno[1].equals("normal")) {
			textoArq = "C " + retorno[0];
			texto = retorno[0];
		}
		// exemplo pertence a extensao de uma classe conhecida
		else if (retorno[1].equals("ext")) {
			textoArq = "ExtCon " + retorno[0];
			texto = "Extensao da Classe " + retorno[0];
		}
		// exemplo pertence a uma classe aprendida na fase online (novidade)
		int retornoInt = (int) Math.round(Double.parseDouble(retorno[0]));
		if (retorno[1].equals("nov")) {
			textoArq = "N " + retorno[0];
			texto = "Novidade " + retornoInt;
		}
		// exemplo pertence a extensao de uma classe aprendida na fase online (novidade)
		if (retorno[1].equals("extNov")) {
			textoArq = "ExtNov " + retorno[0];
			texto = "Extensao da Novidade " + retornoInt;
		}

		String strRet[] = new String[2];
		strRet[0] = textoArq;
		strRet[1] = texto;
		return strRet;
	}

	public void removeUnknown() {
		SortedSet<Long> keys = new TreeSet<>(this.unknownSet.keySet());
		for (Long key : keys) {
			if (key < (this.timestamp - this.timeWindow)) {

				this.unknownSet.remove(key);
			}
		}
	}

	public void putMicroSleep() {
		for (int i = 0; i < this.model.size(); i++) {
			if (this.model.get(i).getTime() < (this.timestamp - 1 - this.timeWindow)) {
				this.sleepMemory.add((MicroCluster) this.model.get(i).copy());
				this.model.remove(i);
				i--;
			}
		}
	}

	@Override
	public Prediction getPredictionForInstance(Instance inst) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MOAObject getModel() {
		return null;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	public double[] getVotesForInstance(Instance inst) {

		int index = (int) this.getC() + this.noveltyIndex();
		double[] votes = new double[index];

		// Case is not possible classify an example, array = 0 in all positions
		Arrays.fill(votes, 0);
		String[] predict = doMinasPrediction(inst);
		if (predict != null) {
			double predictedClass = Double.parseDouble(predict[0]);
			boolean isNovelty = (predict[1] == "nov" || predict[1] == "extNov");
			if (predictedClass < index)
				if(!isNovelty)
					votes[(int) predictedClass] = 1;
				else
					votes[(int) predictedClass] = 2;

		}
		return votes;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}

	private static double distance(double[] pointA, double[] pointB) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}

	// K MEANS
	public Clustering kMeans(int k, List<? extends Cluster> data) {
		Random random = new Random(0);
		Cluster[] centers = new Cluster[k];
		for (int i = 0; i < centers.length; i++) {
			int rid = random.nextInt(k);
			centers[i] = new SphereCluster(data.get(rid).getCenter(), 0);
		}
		Clustering clustering = kMeans(k, centers, data);
		return clustering;
	}

	public Clustering kMeans(int k, Cluster[] centers, List<? extends Cluster> data) {
		assert (centers.length == k);
		assert (k > 0);

		int dimensions = centers[0].getCenter().length;
		this.clusters = new int[data.size()];

		ArrayList<ArrayList<Cluster>> clustering = new ArrayList<ArrayList<Cluster>>();
		for (int i = 0; i < k; i++) {
			clustering.add(new ArrayList<Cluster>());
		}

		int repetitions = 100;
		int count = 0;
		while (repetitions-- >= 0) {
			count = 0;
			// Assign points to clusters
			for (Cluster point : data) {
				double minDistance = distance(point.getCenter(), centers[0].getCenter());
				int closestCluster = 0;
				for (int i = 1; i < k; i++) {
					double distance = distance(point.getCenter(), centers[i].getCenter());

					if (distance < minDistance) {
						closestCluster = i;
						minDistance = distance;
					}
				}

				clustering.get(closestCluster).add(point);
				if (repetitions == -1)
					this.clusters[count] = closestCluster;
				count++;
			}

			// Calculate new centers and clear clustering lists
			SphereCluster[] newCenters = new SphereCluster[centers.length];
			for (int i = 0; i < k; i++) {
				newCenters[i] = calculateCenter(clustering.get(i), dimensions);
				if (repetitions != -1)
					clustering.get(i).clear();
			}
			centers = newCenters;
		}

		return new Clustering(centers);
	}

	private static SphereCluster calculateCenter(ArrayList<Cluster> cluster, int dimensions) {
		double[] res = new double[dimensions];
		for (int i = 0; i < res.length; i++) {
			res[i] = 0.0;
		}

		if (cluster.size() == 0) {
			return new SphereCluster(res, 0.0);
		}

		for (Cluster point : cluster) {
			double[] center = point.getCenter();
			for (int i = 0; i < res.length; i++) {
				res[i] += center[i];
			}
		}

		// Normalize
		for (int i = 0; i < res.length; i++) {
			res[i] /= cluster.size();
		}

		// Calculate radius
		double radius = 0.0;
		for (Cluster point : cluster) {
			double dist = distance(res, point.getCenter());
			if (dist > radius) {
				radius = dist;
			}
		}
		SphereCluster sc = new SphereCluster(res, radius);
		sc.setWeight(cluster.size());
		return sc;
	}

	@Override // Retirar daqui, da task e da noveltyabstract
	public void atFinal() {
		try {
			ArqSaida.close();
			noveltyIndex();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int noveltyIndex() {
		int max = 0;
		for (MicroCluster m : this.model) {
			if (m.getCategory() == "nov" && m.getClassId() > max)
				max = (int) m.getClassId();
		}
		return (int) max;
	}

}
