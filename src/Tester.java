
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
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
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import gnu.trove.TObjectDoubleHashMap;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

public class Tester {

	// set the number of topics in the LDA
	public static final int NUM_TOPICS = 50;

	public static final String[] topics = { "Sports", "Politics & Social Issues", "Arts", "Science And Technology",
			"Business And Companies", "Environment", "Spiritual", "Other And Miscilleneous" };

	public static TObjectDoubleHashMap<String> idfValues;

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

		pipeList.add(new ProcessTweet());
		pipeList.add(new CharSequenceLowercase());
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
		Instances data = new Instances("myData", attributes, 12000);

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

	public static void main(String[] args) {

		long start = System.currentTimeMillis();

		try {
			defineHashtagMapping("result.txt");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		Pipe pipe = buildPipe();
		InstanceList ilist = readFile(new File("tweets.csv"), pipe);

		// shuffle the tweets for cross validation later on, seed = 37
		ilist.shuffle(new Random(37));

		ilist = ilist.subList(0, 500);

		// calculate the idf values for each word in the corpus
		idfValues = TFIDF.getIdf(ilist);

		ParallelTopicModel model = new ParallelTopicModel(NUM_TOPICS, 1.0, 0.01);
		model.addInstances(ilist);
		model.setNumThreads(1);
		model.setNumIterations(1000);

		try {

			// construct the LDA topic model
			model.estimate();

			// set up the Instances object for classification
			Instances data = defineFeatures(NUM_TOPICS + ilist.getAlphabet().size(), ilist.size());

			// construct the feature vector for each tweet
			// and store them in the weka Instances
			TopicInferencer inferencer = model.getInferencer();
			for (int i = 0; i < ilist.size(); i++) {

				if (i % 100 == 0) {
					System.out.println("Transforming tweet " + i);
				}

				// grab the current tweet
				TweetInstance tweet = (TweetInstance) ilist.get(i);
				// get the conditional topic distribution for each tweet instance
				double[] conditionalProbs = inferencer.getSampledDistribution(tweet, 10, 1, 5);

				// construct and retrieve the feature vector
				tweet.createFeatureVector(conditionalProbs);
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
			double[] err = new double[k];
			
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

				// build the classifier
				Classifier svm = (Classifier) new SMO();
				svm.buildClassifier(train);

				// classify the testing set
				Evaluation eval_train = new Evaluation(train);
				eval_train.evaluateModel(svm, test);

				System.out.println(eval_train.toSummaryString());
				err[i - 1] = eval_train.pctCorrect();

				// System.out.println("Fold " + i + ": \t[" + iStart + ":" + (iStart + toCopy -
				// 1) + "]");
			}

			System.out.println(mean(err));
			
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start) + " ms");

	}

}
