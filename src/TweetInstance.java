import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;

public class TweetInstance extends Instance {
	
	/*
	 * A TweetInstance has the following fields:
	 *	from superclass Instance:
	 * 		data - when processed with the pipe ProcessTweet, this will hold the tweet's content minus the hashtags
	 * 		name
	 * 		target
	 * 		source
	 * 	new fields:
	 * 		topic - the assigned topic
	 * 		user - the username of the tweet's poster
	 * 		timeStamp - the timestamp on the tweet stored as a String
	 * 		hashtags - a list of the hashtags used in the tweet
	 */

	private String label;
	private String user;
	private String timeStamp;
	private ArrayList<String> hashtags;
	private double[] featureVector;
	private boolean isProcessed = false;

	public TweetInstance(Object data, Object target, Object name, Object source) {
		super(data, target, name, source);
		
		this.label = (String) target;
		this.hashtags = new ArrayList<String>();
	}

	public void addHashtag(String hashtag) {
		this.hashtags.add(hashtag);
	}
	
	/**
	 * Each TweetInstance will have a vector of doubles as its feature vector.
	 * The first K elements are the conditional topic distributions.
	 * The remaining V elements are BoW features (ideally using TF-IDF, if not just TF)
	 * 
	 * @param topicDistribution - conditional topic distribution
	 */
	public void createFeatureVector(double[] topicDistribution) {
		
		//make sure the data is processed before creating a feature vector
		if(!this.isProcessed) {
			System.out.println("Error: This tweet has not been run through a ProcessTweet pipe.");
			return;
		}
		
		//the number of topics
		int k = topicDistribution.length;
		
		//the total number of words in the vocabulary
		int v = getAlphabet().size();
		
		//the number of features in the vector (topics dist. and BoW)
		int p = k + v;
		featureVector = new double[p];
		
		//fill in the first k entries as the conditional topic probabilities for this document
		for(int i = 0; i < k; i++) {
			featureVector[i] = topicDistribution[i];
		}
		
		//fill in the remaining entries as the TF of each word 
		//using the feature index for each word 
		FeatureSequence fs = (FeatureSequence) this.getData();
		for(int i = 0; i < fs.size(); i++) {
			int featureIndex = fs.getIndexAtPosition(i);
			featureVector[k + featureIndex] += 1;
		}
		
//		for(int i = 0; i < fs.size(); i++) {
//			int featureIndex = fs.getIndexAtPosition(i);
//			String word = (String) getAlphabet().lookupObject(featureIndex);
//			featureVector[k + featureIndex] = featureVector[k + featureIndex] * Tester.idfValues.get(word);
//		}
		
		
	}
	
	public void setAsProcessed() {
		this.isProcessed = true;
	}

	public double[] getFeatureVector() {
		return featureVector;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public ArrayList<String> getHashtags() {
		return hashtags;
	}

	public void setHashtags(ArrayList<String> hashtags) {
		this.hashtags = hashtags;
	}
	
	
}
