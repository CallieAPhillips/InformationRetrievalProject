
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.sun.org.apache.bcel.internal.generic.POP;
import com.sun.xml.internal.fastinfoset.sax.Features;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class Tester {

	// set the number of topics in the LDA
	public static final int NUM_TOPICS = 50;

	// the public mapping of a hashtag to a certain topic
	// this is manually defined in the popular hashtag text file
	public static HashMap<String, String> popularHashtags;

	/**
	 * EDIT THE FILE READING ONCE THE HASHTAGS ARE LABELED! Read in the popular
	 * hashtags from the given file. Use the manually labeled classifications of
	 * each hashtags to create the public HashMap
	 * 
	 * @param filename
	 */
	public static void defineHashtagMapping(String filename) throws FileNotFoundException {

		popularHashtags = new HashMap<>();

		Scanner sc = new Scanner(new File(filename));
		while (sc.hasNext()) {
			String hashtag = sc.nextLine();
			popularHashtags.put(hashtag, hashtag);
		}

	}

	public static Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		pipeList.add(new ProcessTweet());
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence());
		pipeList.add(new TokenSequenceRemoveStopwords(new File("en.txt"), "UTF-8", false, false, false));
		pipeList.add(new TokenSequence2FeatureSequence());

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

	public static void main(String[] args) {

		try {
			defineHashtagMapping("popularHashtags.txt");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		Pipe pipe = buildPipe();
		InstanceList ilist = readFile(new File("tweets.csv"), pipe);

		System.out.println("Corpus Dictionary:");
		Alphabet dictionary = ilist.getAlphabet();
		dictionary.dump();
		System.out.println();

		// for (int i = 0; i < ilist.size(); i++) {
		// TweetInstance tweet = (TweetInstance) ilist.get(i);
		// FeatureSequence fs = (FeatureSequence) tweet.getData();
		// // System.out.println("Iteration " + i);
		// System.out.print("Name: " + tweet.getName() + "\nTarget: " +
		// tweet.getTarget() + "\n");
		// System.out.print("Data: ");
		//
		// for (int j = 0; j < fs.size(); j++) {
		// System.out.print(dictionary.lookupObject(fs.getIndexAtPosition(j)) + " ");
		// }
		// System.out.println("\n");
		//
		// }

		ParallelTopicModel model = new ParallelTopicModel(NUM_TOPICS, 1.0, 0.01);

		model.addInstances(ilist);

		model.setNumThreads(1);

		model.setNumIterations(10);
		try {

			// construct the LDA topic model
			model.estimate();

			// construct the feature vector for each tweet
			TopicInferencer inferencer = model.getInferencer();
			for (int i = 0; i < ilist.size(); i++) {
				if (i % 100 == 0) {
					System.out.println("Checking tweet " + i);
				}
				// grab the current tweet
				TweetInstance tweet = (TweetInstance) ilist.get(i);
				// get the conditional topic distribution for each tweet instance
				double[] conditionalProbs = inferencer.getSampledDistribution(tweet, 10, 1, 5);

				// construct and retrieve the feature vector
				tweet.createFeatureVector(conditionalProbs);
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

				// loop over all values and check if they match the origianl topic distribution
				// and bag-of-words
				for (int j = 0; j < fv.length; j++) {
					if (j < NUM_TOPICS) {
						if (fv[j] != conditionalProbs[j]) {
							System.out.println("Error with topic distribution in feature vector.");
						}
					} else {
						Double feature = (double) j - NUM_TOPICS;
						Double featureCount = bow.get(feature);
						if (featureCount == null) {
							if (fv[j] != 0.0) {
								System.out.println("Marked the count of a word that is not in the tweet as nonzero");
							}
						} else {
							if (fv[j] != featureCount) {
								System.out.println("Wrong count for word feature index = " + feature);
								System.out.println("Real Count = " + featureCount + ", FV Count = " + (int) fv[j]);
							}
						}
					}
				}

			}

			/* VIEW THE TOPIC ASSIGMENTS OF EACH WORD IN A GIVEN TWEET */
			// //output words and their topic assignment for the first document
			// Alphabet dataAlphabet = ilist.getDataAlphabet();
			// FeatureSequence tokens = (FeatureSequence)
			// model.getData().get(0).instance.getData();
			// LabelSequence topics = model.getData().get(0).topicSequence;
			// Formatter out = new Formatter(new StringBuilder(), Locale.US);
			// for (int position = 0; position < tokens.getLength(); position++) {
			// out.format("%s-%d ",
			// dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)),
			// topics.getIndexAtPosition(position));
			// }
			// System.out.println(out);
			//
			// // Estimate the topic distribution of the first instance,
			// // given the current Gibbs state.
			// double[] topicDistribution = model.getTopicProbabilities(0);

			/*
			 * VIEWING IMPORTANT MOST IMPORTANT WORDS ALONG WITH THEIR WEIGHTS IN EACH TOPIC
			 */
			// // Get an array of sorted sets of word ID/count pairs
			// ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
			//
			// // Show top 5 words in topics with proportions for the first document
			// for (int topic = 0; topic < numTopics; topic++) {
			// Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
			//
			// out = new Formatter(new StringBuilder(), Locale.US);
			// out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			// int rank = 0;
			// while (iterator.hasNext() && rank < 10) {
			// IDSorter idCountPair = iterator.next();
			// out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()),
			// idCountPair.getWeight());
			// rank++;
			// }
			// System.out.println(out);
			// }

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
