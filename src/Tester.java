
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelSequence;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

public class Tester {

	// set the number of topics in the LDA
	public static final int NUM_TOPICS = 50;

	// number of top words extracted from each LDA topic
	public static final int samplePerTopic = 200;

	public static double[] idfValues;

	public static final String[] topics = { "Sports", "Politics & Social Issues", "Arts", "Science And Technology",
			"Business And Companies", "Environment", "Spiritual", "Other And Miscilleneous" };

	// the public mapping of a hashtag to a certain topic
	// this is manually defined in the popular hashtag text file
	public static HashMap<String, String> popularHashtags;

	/**
	 * Read in the hashtags from the given file. Use the manually labeled
	 * classifications of each hashtags to create the public HashMap
	 *
	 * @param filename
	 */
	public static void defineHashtagMapping(String filename) throws FileNotFoundException {

		popularHashtags = new HashMap<>();

		Scanner sc = new Scanner(new File(filename));
		while (sc.hasNextLine()) {
			String hashtag = sc.nextLine();
			List<String> tagAndTopic = Arrays.asList(hashtag.split(","));
			popularHashtags.put(tagAndTopic.get(0), tagAndTopic.get(1));
		}
	}

	public static Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new ProcessTweet());
		pipeList.add(new CharSequence2TokenSequence());
		pipeList.add(new TokenSequenceRemoveStopwords(new File("en.txt"), "UTF-8", false, false, false));
		pipeList.add(new TokenSequence2FeatureSequence());
		pipeList.add(new Target2Label());

		return new SerialPipes(pipeList);
	}

	public static InstanceList readFile(File f, Pipe pipe) {
		InstanceList trainInstances;
		TweetIterator iter = null;
		try {
			// the first term is the category,
			// the remaining is extracted as the tweet itself
			// will later be processed to extract user, date, content, and hashtags
			iter = new TweetIterator(new FileReader(f), Pattern.compile("(.*?)\\,\\s*(.*)"), 2, 1, -1);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		trainInstances = new InstanceList(pipe);
		trainInstances.addThruPipe(iter);
		return trainInstances;
	}

	public static boolean checkFeatureVectors(InstanceList ilist, TopicInferencer inferencer) {
		boolean isOK = true;
		for (int i = 0; i < ilist.size(); i++) {

			if (i % 100 == 0) {
				System.out.println("Checking tweet " + i);
			}

			// grab the current tweet
			TweetInstance tweet = (TweetInstance) ilist.get(i);
			double[] fv = tweet.getFeatureVector();

			// find the actual counts of each word feature in the tweet's feature sequence
			HashMap<Double, Double> bow = new HashMap<>();
			FeatureSequence fs = (FeatureSequence) tweet.getData();
			for (int j = 0; j < fs.size(); j++) {
				Double key = (double) fs.getIndexAtPosition(j);
				Double count = bow.get(key);
				if (count == null) {
					bow.put(key, 1.0);
				} else {
					bow.put(key, count + 1);
				}
			}

			// loop over all values and check if they match the original topic distribution
			// and bag-of-words
			for (int j = 0; j < fv.length; j++) {
				if (j >= NUM_TOPICS) {

					Double feature = (double) j - NUM_TOPICS;
					Double featureCount = bow.get(feature);
					if (featureCount == null) {
						if (fv[j] != 0.0) {
							System.out.println("Marked the count of a word that is not in the tweet as nonzero");
							isOK = false;
						}
					} else {
						if (fv[j] != featureCount) {
							System.out.println("Wrong count for word feature index = " + feature);
							System.out.println("Real Count = " + featureCount + ", FV Count = " + (int) fv[j]);
							isOK = false;
						}
					}
				}
			}

		}
		return isOK;
	}

	/**
	 * Define the Weka Instances object that will store all of the tweet feature
	 * vectors and labels. This object will be used in the classification later on.
	 * 
	 * @param numOfFeatures
	 *            number of columns in the feature vector (does not include the
	 *            label)
	 * @param tweetCapacity
	 *            number of tweets used
	 * @return a list of the tweet's features and label as a Weka Instances object
	 */
	public static Instances defineFeatures(int numOfFeatures, int tweetCapacity) {

		ArrayList<Attribute> attributes = new ArrayList<>();

		// for all the features, define a numeric (double type) attribute
		for (int i = 0; i < numOfFeatures; i++) {
			if (i < NUM_TOPICS) {
				// will hold the topic probabilities
				attributes.add(new Attribute("p" + i));
			} else {
				// will hold the BoW value for each word
				attributes.add(new Attribute("w" + (i - NUM_TOPICS)));
			}
		}

		// define the tweet's label as the final (nominal) attribute
		ArrayList<String> labelValues = new ArrayList<String>();
		for (int i = 0; i < topics.length; i++) {
			labelValues.add(topics[i]);
		}
		attributes.add(new Attribute("label", labelValues));

		// define the Instances object
		Instances data = new Instances("myData", attributes, tweetCapacity);

		// set the tweet's label as the class attribute
		data.setClassIndex(data.numAttributes() - 1);

		return data;
	}

	public static double mean(double[] arr) {
		if (arr.length == 0) {
			System.out.println("Cannot calculate mean. Empty array.");
		}
		double mean = 0;
		for (double d : arr) {
			mean += d;
		}
		return mean / arr.length;
	}

	public static void writeAlphabet(Alphabet a, String file) {
		try {
			PrintWriter p = new PrintWriter(file);
			a.dump(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int mostProbableTopic(double[] p) {
		double mostProbabale = -1.0;
		int index = -1;
		for (int i = 0; i < p.length; i++) {
			if (p[i] > mostProbabale) {
				mostProbabale = p[i];
				index = i;
			}
		}

		if (mostProbabale > 0.1) {
			return index;
		}

		return -1;
	}

	public static HashSet<String> getTopWords(ParallelTopicModel model, int numWords) {

		HashSet<String> topWords = new HashSet<>();
		Alphabet alphabet = model.getAlphabet();

		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Print results for each topic
		for (int topic = 0; topic < NUM_TOPICS; topic++) {
			TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);
			int word = 0;
			Iterator<IDSorter> iterator = sortedWords.iterator();

			while (iterator.hasNext() && word < numWords) {
				IDSorter info = iterator.next();
				topWords.add((String) alphabet.lookupObject(info.getID()));
				word++;
			}

		}

		return topWords;

	}

	/**
	 * Return the idf value of a given word
	 * 
	 * @param ilist
	 *            list of documents
	 * @param word
	 * @return the IDF value of that word
	 */
	public static double idf(InstanceList ilist, int word) {
		double n = 0;

		for (int i = 0; i < ilist.size(); i++) {
			TweetInstance t = (TweetInstance) ilist.get(i);
			FeatureSequence fs = (FeatureSequence) t.getData();
			for (int j = 0; j < fs.size(); j++) {
				int featureIndex = fs.getIndexAtPosition(j);
				if (featureIndex == word) {
					n++;
					break;
				}
			}
		}

		return Math.log((double) ilist.size() / n) + 1;
	}

	public static void main(String[] args) throws Exception {

		long start = System.currentTimeMillis();

		try {
			defineHashtagMapping("result.txt");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		Pipe pipe = buildPipe();
		InstanceList ilist = readFile(new File("tweets.csv"), pipe);

		writeAlphabet(ilist.getAlphabet(), "vocabulary.txt");

		// shuffle the tweets for cross validation later on, seed = 37
		ilist.shuffle(new Random(37));

		ParallelTopicModel model = new ParallelTopicModel(NUM_TOPICS, 1.0, 0.01);
		model.addInstances(ilist);
		model.setNumThreads(1);
		model.setNumIterations(2000);

		try {

			// construct the LDA topic model
			model.estimate();
			TopicInferencer inferencer = model.getInferencer();

			HashSet<String> topWords = getTopWords(model, samplePerTopic);
			ArrayList<Integer> topWordIndeces = new ArrayList<Integer>();
			for (String word : topWords) {
				int featureIndex = ilist.getAlphabet().lookupIndex(word);
				topWordIndeces.add(featureIndex);
			}

			/* IDF was found not to improve the accuracy */
//			 idfValues = new double[topWordIndeces.size()];
//			 for(int i = 0; i < topWordIndeces.size(); i++) {
//			 idfValues[i] = idf(ilist, topWordIndeces.get(i));
//			 }

			/* Led to OutOfMemoryError due to growIfNecessary() function in FeatureSequence*/
//			// boosting important words
//			for (int i = 0; i < ilist.size(); i++) {
//				TweetInstance ti = (TweetInstance) ilist.get(i);
//				FeatureSequence fs = (FeatureSequence) ti.getData();
//				for (int j = 0; j < fs.size(); j++) {
//					int featureIndex = fs.getIndexAtPosition(j);
//					if (topWordIndeces.contains(featureIndex)) {
//						for (int rep = 0; rep < 2; rep++)
//							fs.add(featureIndex);
//					}
//				}
//			}

			// set up the Instances object for classification
			Instances data = defineFeatures(NUM_TOPICS + topWords.size(), ilist.size());

			// construct the feature vector for each tweet
			// and store them in the weka Instances
			for (int i = 0; i < ilist.size(); i++) {

				if (i % 1000 == 0) {
					System.out.println("Working on tweet " + i);
				}

				// grab the current tweet
				TweetInstance tweet = (TweetInstance) ilist.get(i);
				// get the conditional topic distribution for each tweet instance
				double[] conditionalProbs = inferencer.getSampledDistribution(tweet, 2000, 1, 5);

				// construct and retrieve the feature vector
				tweet.createFeatureVector(conditionalProbs, topWordIndeces);
				double[] featureVector = tweet.getFeatureVector();

				// grab the label for this tweet
				String label = ((Label) tweet.getTarget()).toString();

				// a Weka Instance that will store the feature vector
				// SparseInstance is used because most words in BoW will be 0
				SparseInstance si = new SparseInstance(featureVector.length + 1);

				// set the value for all numeric attributes in si
				for (int j = 0; j < featureVector.length; j++) {
					si.setValue(data.attribute(j), featureVector[j]);
				}

				// set the label attribute in si
				si.setValue(data.classAttribute(), label);

				// add the feature vector to the Instances object
				data.add(si);

			}

			// 10-fold cross validation
			int k = 10;
			int len = data.size() / k;
			int remainder = data.size() % k;
			double[] accuracy = new double[k];

			int iStart = 0, toCopy = 0;
			for (int i = 1; i <= k; i++) {
				iStart += toCopy;
				if (i <= remainder) {
					toCopy = len + 1;
				} else {
					toCopy = len;
				}

				Instances test = new Instances(data, iStart, toCopy);
				Instances train = new Instances(data, 0, iStart);
				for (int j = iStart + toCopy; j < data.size(); j++) {
					train.add(data.get(j));
				}

				Classifier classifier = (Classifier) new NaiveBayesMultinomial();
				classifier.buildClassifier(train);

				// classify the testing set
				Evaluation eval_train = new Evaluation(train);
				eval_train.evaluateModel(classifier, test);

				accuracy[i - 1] = eval_train.pctCorrect();

			}

			System.out.println(mean(accuracy) + "%");

			System.out.println("Feature vector length = " + ((TweetInstance) ilist.get(0)).getFeatureVector().length);

		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start) + " ms");

	}

}
