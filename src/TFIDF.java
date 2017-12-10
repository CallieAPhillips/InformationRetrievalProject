import java.util.HashMap;
import java.util.HashSet;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public class TFIDF {
	public static TObjectDoubleHashMap<String> getIdf(InstanceList data) {
		// get idf
		TObjectDoubleHashMap<String> idf = new TObjectDoubleHashMap<String>();

		for (Instance instance : data) {
			FeatureSequence original_tokens = (FeatureSequence) instance.getData();
			HashSet<String> words = new HashSet<String>();
			for (int jj = 0; jj < original_tokens.getLength(); jj++) {
				String word = (String) original_tokens.getObjectAtPosition(jj);
				words.add(word);
			}
			for (String word : words) {
				idf.adjustOrPutValue(word, 1, 1);
			}
		}

		int D = data.size();
		for (Object ob : idf.keys()) {
			String word = (String) ob;
			double value = D / (1 + idf.get(word));
			value = Math.log(value) + 1;
			idf.adjustValue(word, value);
		}

		System.out.println("Idf size: " + idf.size());
		return idf;
	}

	public static TObjectDoubleHashMap<String> computeTfidf(InstanceList data) {
		// get idf
		TObjectDoubleHashMap<String> idf = getIdf(data);

		// compute tf-idf for each word
		HashMap<String, HashSet<Double>> tfidf = new HashMap<String, HashSet<Double>>();

		for (Instance instance : data) {
			FeatureSequence original_tokens = (FeatureSequence) instance.getData();
			TObjectIntHashMap<String> tf = new TObjectIntHashMap();
			for (int jj = 0; jj < original_tokens.getLength(); jj++) {
				String word = (String) original_tokens.getObjectAtPosition(jj);
				tf.adjustOrPutValue(word, 1, 1);
			}
			for (Object ob : tf.keys()) {
				String word = (String) ob;
				HashSet<Double> values;
				if (tfidf.containsKey(word)) {
					values = tfidf.get(word);
				} else {
					values = new HashSet<Double>();
					tfidf.put(word, values);
				}
				double value = tf.get(word) * idf.get(word);
				values.add(value);
			}
		}

		// averaged tfidf
		TObjectDoubleHashMap<String> vocabtfidf = new TObjectDoubleHashMap();
		for (String word : tfidf.keySet()) {
			double sum = 0;
			int count = tfidf.get(word).size();
			for (double value : tfidf.get(word)) {
				sum += value;
			}
			sum = sum / count;
			vocabtfidf.put(word, sum);
		}
		System.out.println("vocab tfidf size: " + vocabtfidf.size());
		return vocabtfidf;
	}

}
