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
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SphereCluster;
import moa.clusterers.clustream.Clustream;
import moa.clusterers.clustream.ClustreamKernel;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
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

	protected int C = 0;
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
								 * quantos exemplos de treinamento já foram processados, tive que colocar essa
								 * variável porque o timestamp reseta
								 */
	private int minExamplesUnknown; // mínimo de exemplos para a memória unknown ser significativa e entrar no
									// processo de clustering.
	private int lastCheck; // used in the novelty detection process
	private int minExamplesMicro = 50; // mínimo de exemplos que um micro-grupo precisa ter para ser adicionado ao
										// modelo
	private int noveltyId = 0; // novelty identifier

	private double threshold; // used to separate novelty patterns from extensions
	private int timeWindow;

	protected int[] clusters; /* ?? kmeans control: example-class */
	private double t; /* clustreamKernel param */
	private FileWriter ArqSaida; // lembrar de tirar

	protected DoubleVector observedClassDistribution; // ?

	protected AutoExpandVector<AttributeClassObserver> attributeObservers;

	public FileWriter getArqSaida() {
		return ArqSaida;
	}

	// lembrar de tirar
	String filePath = "C:\\Users\\bf_04\\Documents";
	String arqsaida = filePath + "\\resultados" + "_fold" + 1;

	public void cria_arqsaida() throws IOException {
		String arqsaida = filePath + "\\resultados" + "_fold" + 1;
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
		this.threshold = thresholdOption.getValue();
		this.noveltyId = 1;
		this.observedClassDistribution = new DoubleVector();
		this.timeWindow = timeWindowOption.getValue();

		this.k = maxNumKernelsOption.getValue();
		this.t = 2;
		this.timestamp = -1;
		this.bufferSize = numTrainExamplesOption.getValue();
		this.minExamplesUnknown = minExamplesUnknownOption.getValue();

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

		if (!initialized) {

			// is training over?
			if (bufferedInst < bufferSize) {

				int index = (int) inst.classValue();
				double[] instAux = inst.toDoubleArray();

				// remove class of features - could interfere distance calcule -
				DenseInstance instance = new DenseInstance(1, Arrays.copyOfRange(instAux, 0, instAux.length - 1));

				// is buffer has this class index?
				if (buffer.containsKey(index)) {
					buffer.get(index).add(instance);

				}
				// class not belongs buffer
				else {
					LinkedList<Instance> newClass = new LinkedList<Instance>();
					newClass.add(instance);
					buffer.put(index, newClass);
				}

				if (bufferedInst < bufferSize - 1) {
					this.bufferedInst++;
					return;
				}
				this.bufferedInst++;

			}

			if (clusteringOfflineOption.getChosenIndex() == 0) { // KMeans as clustering algorithm
				for (Map.Entry<Integer, LinkedList<Instance>> map : buffer.entrySet()) {
					LinkedList<Instance> examples = map.getValue();

					if (map.getKey() > C)
						C = map.getKey();

					MicroCluster[] kernels = createKmeansModel(examples, this.k);

					// add micro cluster to model
					for (int i = 0; i < kernels.length; i++)
						updateModel(kernels[i], "normal", map.getKey());

				}

			} else { // Clustream as clustering algorithm

				for (Map.Entry<Integer, LinkedList<Instance>> map : buffer.entrySet()) {
					LinkedList<Instance> examples = map.getValue();

					Clustering clustreamModel = createClustreamModel(examples, this.k);

					if (map.getKey() > C)
						C = map.getKey();

					for (int i = 0; i < clustreamModel.size(); i++) {
						ClustreamKernel clusKernel = (ClustreamKernel) clustreamModel.get(i);
						MicroCluster micro = new MicroCluster(clusKernel, this.t, this.k);
						updateModel(micro, "normal", map.getKey());
					}

				}
			}
			initialized = true;
			timestamp = 1;
<<<<<<< HEAD
<<<<<<< HEAD
=======

>>>>>>> 3bd4a55... oooooooops
=======
>>>>>>> 83b6781... stable
			return;
		}

		// online phase
		unsupervisedModelUpdate(inst);

<<<<<<< HEAD
<<<<<<< HEAD
=======

>>>>>>> 3bd4a55... oooooooops
=======
>>>>>>> 83b6781... stable
		//
		if (timestamp % timeWindow == 0) {

			putMicroSleep();
			removeUnknown();

		}

		timestamp++;
	}

	public void unsupervisedModelUpdate(Instance inst) {
		// instance identified as unkown
		// unknown memory size is significant to novelty detection
		if ((unknownSet.size() >= minExamplesUnknown) && (lastCheck + 2000 < timestamp)) {
			lastCheck = (int) timestamp;
			minExamplesMicro = (int) ((float) 2000 / this.k);

			/*
			 * map to identify elements that belong to the new micro cluster, and later
			 * remove from unknown
			 */
			HashMap<Long, Integer> remove = new HashMap<Long, Integer>();
			MicroCluster[] unknownModel;

			unknownModel = clusteringOnline(unknownSet, this.k, remove);

			// candidate groups to increase or extend model
			for (int kernelTmpIndex = 0; kernelTmpIndex < unknownModel.length; kernelTmpIndex++) {
				MicroCluster micro = unknownModel[kernelTmpIndex];

				// representative group
				if (micro != null && micro.getN() >= minExamplesMicro) {
					// valid group
					if (verifySilhouette(micro) == true) {
						String[] info = closerMicro(micro);
						int pos = Integer.parseInt(info[0]);
						double shortDist = Double.parseDouble(info[1]);
						double vthreshold = Double.parseDouble(info[2]);

						SortedSet<Long> keys = new TreeSet<>(remove.keySet());
						for (Long timeKey : keys) {
							if (kernelTmpIndex == remove.get(timeKey)) {
								unknownSet.remove(timeKey);
								remove.remove(timeKey);
							}
						}

						String fileText = "";

						// centroid distance of new group to nearest centroid group is < than a threshold
						if (shortDist < vthreshold) {
							// extension
							if (model.get(pos).getCategory().equalsIgnoreCase("normal")
									|| model.get(pos).getCategory().equalsIgnoreCase("ext")) {
								// normal extension
								updateModel(micro, "ext", model.get(pos).getClassId());
								fileText = "Thinking " + "Extensao1: " + "C " + model.get(pos).getClassId() + " - "
										+ (int) micro.getN() + " exemplos";
							} else {
								// novelty extension learned online
								updateModel(micro, "extNov", model.get(pos).getClassId());
								fileText = "Thinking " + "ExtensaoNovidade1: " + "N " + model.get(pos).getClassId()
										+ " - " + (int) micro.getWeight() + " exemplos";
							}

						} else {
							// novelty
							updateModel(micro, "nov", noveltyId);

							if (checkReocurrence(micro)) {
								// novelty reocurrence
								if (micro.getCategory().equalsIgnoreCase("ext"))
									fileText = "Thinking " + "Extensao2:" + "C " + micro.getClassId() + " - "
											+ micro.getN() + " exemplos";
								else
									fileText = "Thinking " + "ExtensaoNovidade2: " + "N " + micro.getClassId() + " - "
											+ micro.getN() + " exemplos";
							} else {
								// novelty
								fileText = "ThinkingNov: " + "Novidade " + noveltyId + " - " + (int) micro.getN()
										+ " exemplos";
								noveltyId++;
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

		if (sleepMemory.size() > 0) {
			shortDist = Minas.distance(sleepMemory.get(0).getCenter(), micro.getCenter());
			int pos = 0;

			for (int i = 1; i < sleepMemory.size(); i++) {
				dist = Minas.distance(sleepMemory.get(i).getCenter(), micro.getCenter());

				if (dist < shortDist) {
					shortDist = dist;
					pos = i;
				}
			}
			double vthreshold = sleepMemory.get(pos).getRadius() / 2 * this.threshold;

			if (shortDist <= vthreshold) {
				if (sleepMemory.get(pos).getCategory().equalsIgnoreCase("normal")
						|| sleepMemory.get(pos).getCategory().equalsIgnoreCase("ext"))
					micro.setCategory("ext");
				else
					micro.setCategory("extNov");

				micro.setClassId(sleepMemory.get(pos).getClassId());
				model.add((MicroCluster) sleepMemory.get(pos).copy());
				sleepMemory.remove(pos);

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

		for (int i = 0; i < model.size(); i++) {
			dist = Minas.distance(model.get(i).getCenter(), instance.toDoubleArray());

			if (dist <= shortDist) {
				shortDist = dist;
				pos = i;
			}
		}

		try {

			double radius = model.get(pos).getRadius();
			String text = "Ex: " + timestamp + "\t Classe Real: " + inst.classValue() + "\t Classe MINAS: ";

			if (shortDist <= radius) {
				// model.get(pos).insert(instance, timestamp);
				info[0] = Double.toString(model.get(pos).getClassId());
				info[1] = model.get(pos).getCategory();
				model.get(pos).setTime((long) timestamp);

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
				unknownSet.put((long) timestamp, inst);
				text = text + "não sei ";
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
			System.out.println("model size: " + model.size());

		}

		return null;

	}

	public String[] closerMicro(MicroCluster micro) {
		double shortDist = distance(model.get(0).getCenter(), micro.getCenter());
		;
		int pos = 0;
		double dist;

		for (int i = 1; i < model.size(); i++) {
			dist = distance(model.get(i).getCenter(), micro.getCenter());

			if (dist < shortDist) {
				shortDist = dist;
				pos = i;
			}
		}

		double vthreshold = model.get(pos).getRadius() / 2 * threshold;

		String[] info = new String[3];
		info[0] = Integer.toString(pos);
		info[1] = Double.toString(shortDist);
		info[2] = Double.toString(vthreshold);

		return info;

	}

	protected Clustering createClustreamModel(LinkedList<Instance> examples, int kValue) {
		Clustream clusteringAlgo = new Clustream();
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
			MicroCluster data = new MicroCluster(examples.get(i), dim, timestamp, t, kValue);
			kmeansBuffer.add(data);

			// Centers unrandom..
			if (i < kValue) {
				centers[i] = (MicroCluster) data.copy();
			}
		}

		Clustering kmeans_clustering = kMeans(kValue, centers, kmeansBuffer);

		MicroCluster[] kernels = new MicroCluster[kmeans_clustering.size()];

		for (int i = 0; i < clusters.length; i++) {
			if (kernels[clusters[i]] == null)
				kernels[clusters[i]] = new MicroCluster(new DenseInstance(1.0, kmeansBuffer.get(i).getCenter()),
						kmeansBuffer.get(i).getCenter().length, timestamp, t, kValue);
			else
				kernels[clusters[i]].insert(new DenseInstance(1.0, kmeansBuffer.get(i).getCenter().clone()), i);
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
			remove.put(timeKey, clusters[j]);
			j++;
		}

		// }

		return kernels;

	}

	public void updateModel(MicroCluster micro, String category, double classId) {
		micro.setClassId(classId);
		micro.setCategory(category);
		micro.setTime(timestamp);
		model.add(micro);

		// Group intercep a existent group (novelty case)
		ArrayList<double[]> centers = new ArrayList<double[]>();
		ArrayList<String> novelty = new ArrayList<String>();
		ArrayList<Double> radius = new ArrayList<Double>();

		if (category.equalsIgnoreCase("nov")) {
			// get hyperspheres (novelty)
			for (int i = 0; i < model.size(); i++) {
				String cat = model.get(i).getCategory();
				if (cat.equalsIgnoreCase("nov")) {
					novelty.add(Double.toString(model.get(i).getClassId()));
					radius.add(model.get(i).getRadius() / 2/* model.get(i).getRadius() */);
					centers.add(model.get(i).getCenter());
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
					model.get(model.size() - 1).setClassId(Double.parseDouble(novelty.get(k)));
					model.get(model.size() - 1).setCategory("extNov");
				}
			}
		}
	}

	public boolean verifySilhouette(MicroCluster micro) {
		double shortDistance = 0;
		double dist = 0;
		shortDistance = Minas.distance(model.get(0).getCenter(), micro.getCenter());

		for (int i = 1; i < model.size(); i++) {
			dist = distance(model.get(i).getCenter(), micro.getCenter());
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
		SortedSet<Long> keys = new TreeSet<>(unknownSet.keySet());
		for (Long key : keys) {
			if (key < (timestamp - timeWindow)) {
				unknownSet.remove(key);
			}
		}
	}

	public void putMicroSleep() {
		for (int i = 0; i < model.size(); i++) {
			if (model.get(i).getTime() < (timestamp - 1 - timeWindow)) {
				sleepMemory.add((MicroCluster) model.get(i).copy());
				model.remove(i);
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
		/*
		 * moa uses an array, where each position its a class for now, votes = offline
		 * phase model
		 */

		double[] votes = new double[(int) C + 1];

		// Case is not possible classify an example, array = 0 in all positions
		Arrays.fill(votes, 0);
		String[] predict = doMinasPrediction(inst);
		if (predict != null) {
			double predictedClass = Double.parseDouble(predict[0]);
			System.out.println();
			if (predictedClass <= C)
				votes[(int) predictedClass] = 1;

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
		clusters = new int[data.size()];

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
					clusters[count] = closestCluster;
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
