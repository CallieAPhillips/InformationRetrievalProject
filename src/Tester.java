
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;

public class Tester {

	private static void getAllFiles(File curDir) {

		File[] filesList = curDir.listFiles();
		for (File f : filesList) {
			if (f.isDirectory())
				System.out.println(f.getName());
			if (f.isFile()) {
				System.out.println(f.getName());
			}
		}

	}

	public static void outputSample() {

		HashMap<String, Integer> bow = new HashMap<>();
		String str = "Elizabeth Needham (died 3 May 1731), also known as Mother Needham, was an English procuress and brothel-keeper of 18th-century London, who has been identified as the bawd greeting Moll Hackabout in the first plate of William Hogarth's series of satirical etchings, A Harlot's Progress. Although Needham was notorious in London at the time, little is recorded of her life, and no genuine portraits of her survive. Her house was the most exclusive in London and her customers came from the highest strata of fashionable society, but she eventually crossed the moral reformers of the day and died as a result of the severe treatment she received after being sentenced to stand in the pillory.\r\n"
				+ "";
		StringBuilder s = new StringBuilder(str);
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '.') {
				s.deleteCharAt(i);
				i--;
			}
		}
		str = s.toString();
		String[] tokens = str.split(" ");
		for (String token : tokens) {
			if (bow.get(token) == null) {
				bow.put(token, 1);
			} else {
				Integer count = bow.get(token);
				bow.put(token, count + 1);
			}
		}

		ArrayList<String> keys = new ArrayList<String>(bow.keySet());
		Collections.sort(keys);
		for (String token : keys) {
			System.out.println(token + ": " + bow.get(token));
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
			// the first term is the category, the remaining is extracted as the tweet
			// itself
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

		Pipe pipe = buildPipe();
		InstanceList ilist = readFile(new File("sample.csv"), pipe);

		System.out.println("Corpus Dictionary:");
		Alphabet dict = ilist.get(0).getAlphabet();
		dict.dump();
		System.out.println();

		for (int i = 0; i < ilist.size(); i++) {
			TweetInstance tweet = (TweetInstance) ilist.get(i);
			FeatureSequence fs = (FeatureSequence) tweet.getData();
			// System.out.println("Iteration " + i);
			System.out.print("Name: " + tweet.getName() + "\nTarget: " + tweet.getTarget() + "\n");
			System.out.print("Data: ");

			for (int j = 0; j < fs.size(); j++) {
				System.out.print(dict.lookupObject(fs.getIndexAtPosition(j)) + " ");
			}
			System.out.println("\n");

		}
		
		int numTopics = 4;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

		model.addInstances(ilist);

		model.setNumThreads(1);

		model.setNumIterations(5000);
		try {
			model.estimate();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
