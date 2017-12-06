import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Instance;

public class TweetIterator extends CsvIterator {

	LineNumberReader reader;
	Pattern lineRegex;
	int uriGroup, targetGroup, dataGroup;
	String currentLine;

	public TweetIterator(Reader input, Pattern lineRegex, int dataGroup, int targetGroup, int uriGroup) {
		super(input, lineRegex, dataGroup, targetGroup, uriGroup);
	}

	public TweetIterator(Reader input, String lineRegex, int dataGroup, int targetGroup, int uriGroup) {
		super(input, lineRegex, dataGroup, targetGroup, uriGroup);
	}

	public TweetIterator(String filename, String lineRegex, int dataGroup, int targetGroup, int uriGroup)
			throws FileNotFoundException {
		super(filename, lineRegex, dataGroup, targetGroup, uriGroup);
	}

	@Override
	public TweetInstance next() {
		Instance carrier = super.next();
		TweetInstance tweet = new TweetInstance(carrier.getData(), carrier.getTarget(), carrier.getName(),
				carrier.getSource());
		return tweet;
	}

}
