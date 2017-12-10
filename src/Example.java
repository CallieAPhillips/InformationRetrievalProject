import java.util.ArrayList;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

public class Example {

	public static void main(String[] args) throws Exception {

		ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			if (i < 50) {
				attributes.add(new Attribute("p" + i));
			} else {
				attributes.add(new Attribute("w" + i));
			}
		}

		ArrayList<String> labelValues = new ArrayList<String>();
		labelValues.add("Sports");
		labelValues.add("Politics & Social Issues");
		labelValues.add("Arts");
		// labelValues.add("Science And Technology");
		// labelValues.add("Business And Companies");
		// labelValues.add("Environment");
		// labelValues.add("Spiritual");
		// labelValues.add("Other And Miscilleneous");
		attributes.add(new Attribute("label", labelValues));

		Instances train = new Instances("myData", attributes, 100);

		// set the last attribute as the label
		train.setClassIndex(train.numAttributes() - 1);

		long start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {

			SparseInstance di = new SparseInstance(10001);
			
			double randLabel = Math.random();
			String label = "";
			if (randLabel < 0.333) {
				label = "Sport";
			} else if (randLabel < 0.666) {
				label = "Politics";
			} else {
				label = "Arts";
			}

			for (int j = 0; j < 50; j++) {
				double prob;
				int number;
				if (label.equals("Sport")) {
					Random generator = new Random();
					number = generator.nextInt(41);
				} else if (label.equals("Politics")) {
					Random generator = new Random();
					number = generator.nextInt(41) + 30;
				} else {
					Random generator = new Random();
					number = generator.nextInt(41) + 60;
				}
				prob = number / 100.0;
				di.setValue(attributes.get(j), prob);
			}
			for (int j = 50; j < attributes.size() - 1; j++) {
				di.setValue(attributes.get(j), Math.random() < 0.05 ? 1.0 : 0.0);
			}

			if (label.equals("Sport")) {
				di.setValue(attributes.get(attributes.size()-1), "Sports");
			} else if (label.equals("Politics")) {
				di.setValue(attributes.get(attributes.size()-1), "Politics & Social Issues");
			} else {
				di.setValue(attributes.get(attributes.size()-1), "Arts");
			}

			train.add(di);
		}

		for (int i = 0; i < train.size(); i++) {
		}

		long end = System.currentTimeMillis();

		System.out.println("Training set constructed");

		System.out.println("Time to construct training set = " + (end - start) + "ms for " + train.size() + " rows");

		Instances test = new Instances("test", attributes, 100);

		// set the last attribute as the label
		test.setClassIndex(test.numAttributes() - 1);

		for (int i = 0; i < 100; i++) {
			SparseInstance di = new SparseInstance(10001);
			double randLabel = Math.random();
			String label = "";
			if (randLabel < 0.333) {
				label = "Sport";
			} else if (randLabel < 0.666) {
				label = "Politics";
			} else {
				label = "Arts";
			}

			for (int j = 0; j < 50; j++) {
				double prob;
				int number;
				if (label.equals("Sport")) {
					Random generator = new Random();
					number = generator.nextInt(41);
				} else if (label.equals("Politics")) {
					Random generator = new Random();
					number = generator.nextInt(41) + 30;
				} else {
					Random generator = new Random();
					number = generator.nextInt(41) + 60;
				}
				prob = number / 100.0;
				di.setValue(attributes.get(j), prob);
			}
			for (int j = 50; j < attributes.size() - 1; j++) {
				di.setValue(attributes.get(j), Math.random() < 0.05 ? 1.0 : 0.0);
			}

			if (label.equals("Sport")) {
				di.setValue(attributes.get(attributes.size()-1), "Sports");
			} else if (label.equals("Politics")) {
				di.setValue(attributes.get(attributes.size()-1), "Politics & Social Issues");
			} else {
				di.setValue(attributes.get(attributes.size()-1), "Arts");
			}
			test.add(di);
		}

		System.out.println("Testing set constructed");

		// Build the classifier
		Classifier model = (Classifier) new SMO();
		model.buildClassifier(train);

		System.out.println("Training done.");

		Evaluation eval_train = new Evaluation(train);
		eval_train.evaluateModel(model, test);
//		eval_train.crossValidateModel(model, train, 10, new Random());
		System.out.println(eval_train.toSummaryString());
		System.out.println(eval_train.correct());
		System.out.println(eval_train.errorRate());
		System.out.println(eval_train.incorrect());
		System.out.println(eval_train.rootMeanSquaredError());
		System.out.println(eval_train.kappa());
		System.out.println(eval_train.pctCorrect());

		 
//		System.out.println("-------------------------------");
//		for (int i = 0; i < test.size(); i++) {
//			double prediction = eval_train.evaluateModelOnce(model, test.get(i));
//			System.out.println("*******************");
//			System.out.println("E Predicted: " + test.classAttribute().value((int) prediction));
//			System.out.println("Actual: " + test.classAttribute().value((int) test.get(i).classValue()));
//
//		}

	}

}
