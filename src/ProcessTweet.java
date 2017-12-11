import java.util.ArrayList;
import java.util.HashMap;
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

		// split the data using regex
		Pattern pattern = getPattern(data);
		Matcher matcher = pattern.matcher(data);
		matcher.matches();
		int numGroups = matcher.groupCount();

		// from the data, extract: user, timeStamp, content
		tweet.setUser(matcher.group(1));
		tweet.setTimeStamp(matcher.group(2));
		data = matcher.group(3);
		tweet.setSource(data);

		// accumulate all of the tweet's hashtags
		for (int i = 4; i <= numGroups; i++) {
			tweet.addHashtag(matcher.group(i));
		}

		// fetch all hashtags
		ArrayList<String> hashtags = tweet.getHashtags();
		// fetch the collection of popular hashtags
		HashMap<String, String> popular = Tester.popularHashtags;
		for (String h : hashtags) {
			h = "#" + h;
			// if a mapping exists from a hashtag to a classification
			// then that hashtag is a popular and must be removed
			if (popular.get(h) != null) {
				data = data.replaceAll(h, "");
			}
		}

		// remove http urls from the content
		data = removeURLs(data);

//		// remove the mentions from the content of the tweet
//		try {
//			data = remove(data, "@");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		tweet.setData(data);
		tweet.setAsProcessed();
		classifyByHashtags(carrier);

		return tweet;

	}

	/**
	 * Get the appropriate regex pattern to split the data based on the number of
	 * hashtags in the tweet
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

	/**
	 * @param content
	 * @return content will all http urls removed
	 */
	private String removeURLs(String content) {
		// remove all urls of the form 'http...'
		// does not remove a url at the end of the string with no trailing space
		content = content.replaceAll("https?://.*?\\s+", "");

		StringBuilder s = new StringBuilder(content);
		for (int i = 0; i < s.length() - 4; i++) {
			// if current four letter substring is http
			if (s.substring(i, i + 4).equals("http")) {
				boolean spaceAtEnd = false;
				// search to make sure there is no space in the remaining text
				// meaning this http url is the last word in the content
				for (int j = i + 4; j < s.length(); j++) {
					// if space is found, make note of it
					if (s.charAt(j) == ' ' || s.charAt(j) == '\t' || s.charAt(j) == '\n') {
						spaceAtEnd = true;
					}
				}
				// if there was no space, remove the remaining substring containing the url
				if (!spaceAtEnd) {
					s.replace(i, s.length(), "");
					break;
				}
			}
		}
		return s.toString();
	}

	public void classifyByHashtags(Instance isntance) {

		TweetInstance tweet = (TweetInstance) isntance;
		ArrayList<String> tags = tweet.getHashtags();
		// tweetList = [category, user_name, date, contents, hashtags..]
		int minTopic = 8;
		for (String tag : tags) {
			tag = "#" + tag;
			int topic = Tester.popularHashtags.get(tag) != null
					? (int) Integer.parseInt(Tester.popularHashtags.get(tag))
					: 9;
			if (topic < minTopic) {
				minTopic = topic;
			}
		}
		tweet.setTarget(Tester.topics[minTopic - 1]);

	}

	/**
	 * 
	 * @param content
	 * @return content without specified word types (hash-tags or mentions)
	 */
	private String remove(String content, String removalType) throws Exception {

		if (!removalType.equals("#") && !removalType.equals("@")) {
			throw new Exception("Wrong input for removalType");
		}

		// remove all @ mentions of the form '@user1'
		// does not remove if at the end of the string with no trailing space
		content = content.replaceAll(removalType + ".*?\\s+", "");

		StringBuilder s = new StringBuilder(content);
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == removalType.charAt(0)) {
				boolean spaceAtEnd = false;
				// search to make sure there is no space in the remaining text
				for (int j = i; j < s.length(); j++) {
					// if space is found, make note of it
					if (s.charAt(j) == ' ' || s.charAt(j) == '\t' || s.charAt(j) == '\n') {
						spaceAtEnd = true;
					}
				}
				// if there was no space, remove the remaining substring containing the url
				if (!spaceAtEnd) {
					s.replace(i, s.length(), "");
					break;
				}
			}
		}
		return s.toString();
	}

}
