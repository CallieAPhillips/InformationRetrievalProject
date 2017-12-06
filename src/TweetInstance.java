import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private String topic;
	private String user;
	private String timeStamp;
	private ArrayList<String> hashtags;

	public TweetInstance(Object data, Object target, Object name, Object source) {
		super(data, target, name, source);
				
		this.topic = (String) target;

		this.hashtags = new ArrayList<String>();
	}

	public void addHashtag(String hashtag) {
		this.hashtags.add(hashtag);
	}
	
	public String getTopic() {
		return topic;
	}

	public void getTopic(String topic) {
		this.topic = topic;
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
