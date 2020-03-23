package moa.noveltydetection;

import java.util.ArrayList;
import java.util.Arrays;

public class MatrixUtils {
	ArrayList<ArrayList<Double>> knw;
	ArrayList<ArrayList<Double>> nvt;
	
	public static void main(String[] args) {
		ArrayList<Double> unknown = new ArrayList<Double>();
		
		unknown.add(5, 7.0);
		System.out.println(unknown.get(5));
	}
}
