package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.ISolver;

import bits.*;
import bits.exceptions.UnsolvableProblemException;

import org.sat4j.minisat.SolverFactory;

class SATConstraintHandler implements ConstraintHandler {

//	private ArrayList<Integer> constrainedParameters;
	private ArrayList<ParaIDAndNumLevels> parameterToIDAndNumLevels = new ArrayList<ParaIDAndNumLevels>();

	// BooleanVariables
	private IBooleanVariable[][] booleanVariables = null; //for suffuron
	private  int[][] sat4jvariables = null; // for sat4j
	
	// Constraints 
	private IProblem booleanConstraint = null;
	private org.sat4j.specs.IProblem  sat4jproblem;

	// suffuron 
	KSatReader reader = null; 

	// sat4j
	ISolver solver = null;
	
	SATConstraintHandler(PList parameterList, List<Node> constraintList,
			TreeSet<Integer> constrainedParametersArg) {
		// set -> arrayList
//		constrainedParameters = new ArrayList<Integer>();
		for (Integer factor: constrainedParametersArg) {
//			constrainedParameters.add(factor);
			ParaIDAndNumLevels pidlevels = new ParaIDAndNumLevels(factor, parameterList.get(factor).value_name.size());
			parameterToIDAndNumLevels.add(pidlevels);	
		}
		
		booleanVariables = new IBooleanVariable[parameterToIDAndNumLevels.size()][];
		
		// create BooleanVariables pi_j s.t. i:factor ID, j>=0
		try {
			int i = 0;
			for (ParaIDAndNumLevels pidlevels: parameterToIDAndNumLevels) {
				booleanVariables[i] = new IBooleanVariable[pidlevels.getNumLevels()];
				for (int j = 0; j < pidlevels.getNumLevels(); j++) {
					String str = "p" + pidlevels.getID() + "_" + j;
					booleanVariables[i][j] = BooleanVariable.getBooleanVariable(str);
				}
				i++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		booleanConstraint = setSATConstraint(constraintList);
		
//		solver = SolverFactory.newMiniSATHeap();
//		solver = SolverFactory.newLight();
		solver = SolverFactory.newDefault();
		reader = new KSatReader(solver);
		try {
			sat4jproblem = reader.parseInstance(booleanConstraint);
		} catch (UnsolvableProblemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// map BooleanVariables pi_j to sat4j variables
		sat4jvariables = new int[parameterToIDAndNumLevels.size()][];
		try {
			for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
				ParaIDAndNumLevels pidlevels = parameterToIDAndNumLevels.get(i);
				sat4jvariables[i] = new int[pidlevels.getNumLevels()];
				for (int j = 0; j < pidlevels.getNumLevels(); j++) {
					IBooleanVariable ib = booleanVariables[i][j];
					sat4jvariables[i][j] = reader.getSat4jVariable(ib);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	private IProblem setSATConstraint(List<Node> constraintList) {	
		// 制約式の論理積をとる
		ArrayList<IProblem> problemArrayList = new ArrayList<IProblem>();
		for (int i = 0; i < constraintList.size(); i++) {
			IProblem tmp = constraintList.get(i).buildSAT(parameterToIDAndNumLevels, booleanVariables);
			problemArrayList.add(tmp);
		}
		
		for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
			ArrayList<IBooleanVariable> bitArrayList = new ArrayList<IBooleanVariable>();
			ParaIDAndNumLevels pidlevels = parameterToIDAndNumLevels.get(i);
			for (int j = 0; j < pidlevels.getNumLevels(); j++) {
				bitArrayList.add(booleanVariables[i][j]);
			}
			try {
				IProblem problem = new BitExclusiveSelector(bitArrayList);
				problemArrayList.add(problem);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		IProblem res = null;
		try {
			res = new Conjunction(problemArrayList);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	@Override
	public boolean isPossible(Testcase test) {
		Boolean res = false;

//		ArrayList<IProblem> problemArrayList = new ArrayList<IProblem>();
//		problemArrayList.add(booleanConstraint);
//		
//		try {
//			for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
//				int p = parameterToIDAndNumLevels.get(i).getID();
//				int v = test.get(p);
//				if (v >= 0) {
//					problemArrayList.add(new BitFixer(booleanVariables[i][v], true));
//				}
//			}
//			IProblem constraintAndTest = new Conjunction(problemArrayList);
//			res = constraintAndTest.solve(Problem.defaultSolver());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		//
		List<org.sat4j.specs.IConstr> constList = new ArrayList <org.sat4j.specs.IConstr>();
		try {
			for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
				int p = parameterToIDAndNumLevels.get(i).getID();
				int v = test.get(p);
				if (v >= 0) {
					int [] clause = {sat4jvariables[i][v]}; 
					try {
						IConstr tmp = solver.addClause(new org.sat4j.core.VecInt(clause));
						if (tmp != null)
							constList.add(tmp);
					}
					catch (org.sat4j.specs.ContradictionException e) {
						res = false;
						for (org.sat4j.specs.IConstr cnst: constList) {
							solver.removeConstr(cnst);
						}
						return res;
					}
				}
			}
			org.sat4j.specs.IProblem problem = solver;
			res = problem.isSatisfiable();
			for (org.sat4j.specs.IConstr cnst: constList) {
				solver.removeConstr(cnst);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	
	@Override
	public boolean isPossible(int[] test) {
		Boolean res = false;
		
//		ArrayList<IProblem> problemArrayList = new ArrayList<IProblem>();
//		problemArrayList.add(booleanConstraint);
//		try {
//			for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
//				int p = parameterToIDAndNumLevels.get(i).getID();
//				int v = test[p];
//				if (v >= 0) {
//					problemArrayList.add(new BitFixer(booleanVariables[i][v], true));
//				}
//			}
//			IProblem constraintAndTest = new Conjunction(problemArrayList);
//			res = constraintAndTest.solve(Problem.defaultSolver());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		List<org.sat4j.specs.IConstr> constList = new ArrayList <org.sat4j.specs.IConstr>();

		try {
			for (int i = 0; i < parameterToIDAndNumLevels.size(); i++) {
				int p = parameterToIDAndNumLevels.get(i).getID();
				int v = test[p];
				if (v >= 0) {
					int [] clause = {sat4jvariables[i][v]}; 
					try {
						IConstr tmp = solver.addClause(new org.sat4j.core.VecInt(clause));
						if (tmp != null)
							constList.add(tmp);
					}
					catch (org.sat4j.specs.ContradictionException e) {
						res = false;
						for (org.sat4j.specs.IConstr cnst: constList) {
							solver.removeConstr(cnst);
						}
						return res;
					}
				}
			}
			org.sat4j.specs.IProblem problem = solver;
			res = problem.isSatisfiable();
			
			for (org.sat4j.specs.IConstr cnst: constList) {
				solver.removeConstr(cnst);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		return res;
	}

}
