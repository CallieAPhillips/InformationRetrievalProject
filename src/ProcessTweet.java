import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

public class ProcessTweet extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * Process tweet so that the data of the instance only includes the tweet's
	 * content (without hashtags). Store other parts of the data in the
	 * TweetInstance's fields
	 */
	@Override
	public TweetInstance pipe(Instance carrier) {

		TweetInstance tweet = (TweetInstance) carrier;
		String data = (String) tweet.getData();

		//split the data using regex
		Pattern pattern = getPattern(data);
		Matcher matcher = pattern.matcher(data);
		matcher.matches();
		int numGroups = matcher.groupCount();

		// from the data, extract: user, timeStamp, content
		tweet.setUser(matcher.group(1));
		tweet.setTimeStamp(matcher.group(2));
		data = matcher.group(3);

		// accumulate all of the tweet's hashtags
		for (int i = 4; i <= numGroups; i++) {
			tweet.addHashtag(matcher.group(i));
		}

		ArrayList<String> hashtags = tweet.getHashtags();
		// remove all occurrences of the hashtags from the content
		for (int i = 0; i < hashtags.size(); i++) {
			String ht = "#" + hashtags.get(i);
			data = data.replaceAll(ht, "");
		}

		tweet.setData(data);
		
		return tweet;
	}

	/**
	 * Get the appropriate regex pattern to split the data
	 *  based on the number of hashtags in the tweet
	 */
	private Pattern getPattern(String data) {
		String regex = "";
		int numCommas = numberOfCommas(data);
		for (int i = 0; i < numCommas; i++) {
			regex += "(.*?)\\,\\s*";
		}
		regex += "(.*)";
		Pattern pattern = Pattern.compile(regex);
		return pattern;
	}

	/**
	 * Return the number of commas in the data
	 */
	private int numberOfCommas(String data) {
		int count = 0;
		for (int i = 0; i < data.length(); i++) {
			if (data.charAt(i) == ',') {
				count++;
			}
		}
		return count;
	}

}
