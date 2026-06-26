package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jdd.bdd.*;

abstract class BDDConstraintHandler {
	static final int sizeOfNodetable = 10000;
	static final int sizeOfCache = 10000;
	
	BDD bdd;
	int bddConstraint;
	int numOfBDDvariables;
	ArrayList<Integer> constrainedParameters; // reflect variable ordering
	
	BDDConstraintHandler(PList parameterList, List<Node> constraintList, 
			TreeSet<Integer> constrainedParametersArg, VONode ast) {
		if (Main.debugMode == false) {
			bdd = new BDD(sizeOfNodetable, sizeOfCache);
		} else {
			bdd = new jdd.bdd.debug.DebugBDD(1000,1000);
		}
		
		// set -> arrayList
		constrainedParameters = new ArrayList<Integer>();
		for (Integer factor: constrainedParametersArg) {
			constrainedParameters.add(factor);
		}
		
		// variable ordering goes here
		// System.out.println(constrainedParameters);
		// printOrdering(parameterList);
		// variable ordering
		variableOrdering(parameterList, ast);
		// printOrdering(parameterList);
	}
	
	private void printOrdering(PList parameterList) {
		for (Integer p: constrainedParameters) {
			System.out.print(parameterList.get(p).name + "(" + parameterList.get(p).id1 + "),");
		}
		System.out.println();
	}
	
	private void variableOrdering(PList parameterList, VONode ast) {
		// order elements in constrainedParameters
		final int veryBigvalue = 10000000;
		int[][] mutualDistance = new int[parameterList.size()][parameterList.size()];
		for (int i=0; i < parameterList.size(); i++) {
			for (int j=0; j< parameterList.size(); j++) {
				mutualDistance[i][j] = veryBigvalue;
			}
		}
		ast.evaluate(parameterList.size(), mutualDistance);
		
		ArrayList<Integer> ordering = new ArrayList<Integer>();	
		// pick the first parameter
		int firstparameter = this.constrainedParameters.get(0);//just for safety
		int minsumdist = veryBigvalue;
		for (Integer i: this.constrainedParameters) {
			int sum = 0;
			for (Integer j: this.constrainedParameters) {
				if (i == j) continue;
				sum += mutualDistance[i][j];
			}
			if (sum < minsumdist) {
				minsumdist = sum;
				firstparameter = i;
			}
		}
		ordering.add(firstparameter);
		ArrayList<Integer> rest = new ArrayList<Integer>();	
		for (Integer i: this.constrainedParameters) {
			if (i != firstparameter) {
				rest.add(i);
			}
		}
		
		// choose one by one
		while (rest.isEmpty() == false) {
			int selectedid = 0;
			Integer selected = null;
			minsumdist = veryBigvalue;

			for (int i = 0; i < rest.size(); i++) {
				int sum = 0;
				int newone = rest.get(i);
				for (Integer fixed: ordering) {
					sum += mutualDistance[newone][fixed];
				}
				if (sum < minsumdist) {
					minsumdist = sum;
					selected = newone;
					selectedid = i;
				}
			}
			ordering.add(selected);
			assert(selected == rest.get(selectedid));
			rest.remove(selectedid);
		}		
		constrainedParameters = ordering;	
	}
}
