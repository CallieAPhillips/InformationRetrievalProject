import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class asdf {

	public static void main(String[] args) {
	
		double[] p = {0.05, 0.004, 0.002, 0.01, 0.0014, 0.06};
		System.out.println(mostProbableTopic(p));

	}
	
	public static int mostProbableTopic(double[] p) {
		double mostProbabale = -1.0;
		int index = -1;
		for(int i = 0; i < p.length; i++) {
			if(p[i] > mostProbabale) {
				mostProbabale = p[i];
				index = i;
			}
		}
		
		if(mostProbabale > 0.1) {
			return index;
		}
		
		return -1;	
	}

}
