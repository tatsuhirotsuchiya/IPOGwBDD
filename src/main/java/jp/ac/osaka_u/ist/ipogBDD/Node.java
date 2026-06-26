package jp.ac.osaka_u.ist.ipogBDD;

import jdd.bdd.*;
import bits.*;
import bits.exceptions.ProblemDenierException;

import java.util.*;

abstract class Node {
	abstract int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted);
	abstract IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables);
}

abstract class BooleanOperator extends Node {
}

abstract class BooleanUnaryOperator extends BooleanOperator {
	Node Child;
}

class NotOperator extends BooleanUnaryOperator {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int tmp = Child.buildBDD(bdd, parameters, restricted);
		int res = bdd.not(tmp);
		bdd.ref(res);
		bdd.deref(tmp);
		return res;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			IProblem child = Child.buildSAT(restricted, boolenVariables);
			res = new ProblemDenier(child);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		

		return res;
	}
}

abstract class BooleanBinaryOperator extends BooleanOperator {
	Node Left, Right;
}

class IfOperator extends BooleanBinaryOperator {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = Left.buildBDD(bdd, parameters, restricted);
		int f2 = Right.buildBDD(bdd, parameters, restricted);
		int f = bdd.imp(f1, f2);
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);
		return f;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			IProblem tmp = new ProblemDenier(Left.buildSAT(restricted, boolenVariables));
			res = new Disjunction(tmp, Right.buildSAT(restricted, boolenVariables)); 
		} catch (Exception e) {
			e.printStackTrace();			
			throw new RuntimeException();
		}

		return res;
	}
}

class EqualityOperator extends BooleanBinaryOperator {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = Left.buildBDD(bdd, parameters, restricted);
		int f2 = Right.buildBDD(bdd, parameters, restricted);
		int f = bdd.not(bdd.xor(f1, f2));
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);
		return f;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			res = new ProblemDenier(new ExclusiveDisjunction(Left.buildSAT(restricted, boolenVariables), 
					Right.buildSAT(restricted, boolenVariables)));		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		return res;
	}
}

class InequalityOperator extends BooleanBinaryOperator {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = Left.buildBDD(bdd, parameters, restricted);
		int f2 = Right.buildBDD(bdd, parameters, restricted);
		int f = bdd.xor(f1, f2);
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);
		return f;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			res = new ExclusiveDisjunction(Left.buildSAT(restricted, boolenVariables), 
					Right.buildSAT(restricted, boolenVariables));		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		return res;
	}
}

abstract class BooleanTrinaryOperator extends BooleanOperator {
	Node Left, Middle, Right;
}

class IfthenelseOperator extends BooleanTrinaryOperator {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = Left.buildBDD(bdd, parameters, restricted);
		int f2 = Middle.buildBDD(bdd, parameters, restricted);
		int f3 = Right.buildBDD(bdd, parameters, restricted);
		int f = bdd.ite(f1, f2, f3);
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);
		bdd.deref(f3);
		return f;
	}

	
	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		// if p then q else r = (!p || q) && (p || r)
		IProblem res = null;
		try {
			IProblem p = Left.buildSAT(restricted, boolenVariables);
			IProblem q = Middle.buildSAT(restricted, boolenVariables);
			IProblem r = Right.buildSAT(restricted, boolenVariables);
			IProblem ifPart = new Disjunction(new ProblemDenier(p), q);
			IProblem thenPart = new Disjunction(p, r);
			res = new Conjunction(ifPart, thenPart);		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return res; 
	}
}

abstract class BooleanMultinaryOperator extends BooleanOperator {
	List<Node> ChildList = new ArrayList<Node>();
}

class OrOperator extends BooleanMultinaryOperator {
	
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = ChildList.get(0).buildBDD(bdd, parameters, restricted);
		int f2 = ChildList.get(1).buildBDD(bdd, parameters, restricted);
		int f = bdd.or(f1, f2);
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);

		for (int i = 2; i < ChildList.size(); i++) {
			f1 = f;
			f2 = ChildList.get(i).buildBDD(bdd, parameters, restricted);
			f = bdd.or(f1, f2);
			bdd.ref(f);
			bdd.deref(f1);
			bdd.deref(f2);
		}
		return f;
	}

	@Override
//	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
//		IProblem res = null;
//		try {
//			res = new Disjunction(ChildList.get(0).buildSAT(restricted, boolenVariables), 
//				ChildList.get(1).buildSAT(restricted, boolenVariables));
//			for (int i = 2; i < ChildList.size(); i++) {
//				res = new Disjunction(res, ChildList.get(i).buildSAT(restricted, boolenVariables));
//			}
//			
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		return res;
//	}
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			IProblem[] problemArray = new IProblem[ChildList.size()];
			for (int i = 0; i < ChildList.size(); i++) {
				problemArray[i] = ChildList.get(i).buildSAT(restricted, boolenVariables);
			}
			res = new Disjunction(problemArray);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return res;
	}
	
}

class AndOperator extends BooleanMultinaryOperator {
	
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f1 = ChildList.get(0).buildBDD(bdd, parameters, restricted);
		int f2 = ChildList.get(1).buildBDD(bdd, parameters, restricted);
		int f = bdd.and(f1, f2);
		bdd.ref(f);
		bdd.deref(f1);
		bdd.deref(f2);

		for (int i = 2; i < ChildList.size(); i++) {
			f1 = f;
			f2 = ChildList.get(i).buildBDD(bdd, parameters, restricted);
			f = bdd.and(f1, f2);
			bdd.ref(f);
			bdd.deref(f1);
			bdd.deref(f2);
		}
		return f;
	}

//	@Override
//	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
//		IProblem res = null;
//		try {
//			res = new Conjunction(ChildList.get(0).buildSAT(restricted, boolenVariables), 
//				ChildList.get(1).buildSAT(restricted, boolenVariables));
//			for (int i = 2; i < ChildList.size(); i++) {
//				res = new Conjunction(res, ChildList.get(i).buildSAT(restricted, boolenVariables));
//			}
//			
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		return res;
//	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			IProblem[] problemArray = new IProblem[ChildList.size()];
			for (int i = 0; i < ChildList.size(); i++) {
				problemArray[i] = ChildList.get(i).buildSAT(restricted, boolenVariables);
			}
			res = new Conjunction(problemArray);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return res;
	}

}

abstract class AtomicExpression extends Node {
}

abstract class Constant extends AtomicExpression {
}

class TrueValue extends Constant {
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {

		int f = bdd.getOne();
		bdd.ref(f);
		return f;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			res = Problem.trivialProblem();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return res;
	}
//		IProblem res = null;
//		try {
//			IBooleanVariable dum = BooleanVariable.getBooleanVariable("dum");
//			res = new Problem(new IClause[]	{ new Clause().or(dum),	new Clause().orNot(dum) });		
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return res;
//	}
}

class FalseValue extends Constant {
	
	@Override
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int f = bdd.getZero();
		bdd.ref(f);
		return f;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		IProblem res = null;
		try {
			res = Problem.unsolvableProblem();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		return res;
	}
	
//	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
//		IProblem res = null;
//		try {
//			IBooleanVariable dum = BooleanVariable.getBooleanVariable("dum");
//			res = new Problem(new IClause[]	{ new Clause().or(dum),	new Clause().orNot(dum) });		
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return res;
//	}
}

abstract class ComparisonOfValueAndValue extends AtomicExpression {
	int v1;
	int v2;
}

/*
 * not used abstract class ComparisonOfParameterAndParameter extends
 * AtomicExpression { int p1; int p2; }
 */

abstract class ComparisonOfParameterAndValue extends AtomicExpression {
	int p;
	int v;
}

/*
 * class EqualityOfValueAndValue extends ComparisonOfValueAndValue { int
 * evaluate (BDD bdd, List<VariableAndBDD> parameters) { if (v1 == v2) return
 * bdd.getOne(); else return bdd.getZero(); } }
 */

class EqualityOfParameterAndValue extends ComparisonOfParameterAndValue {
	
	@Override
	int buildBDD(BDD bdd, List<VariableAndBDD> parameters, ArrayList<Integer> restricted) {
		int res = bdd.getOne();
		bdd.ref(res);
		// pは（絶対値で）パラメータの番号が既にはいっている
		int num = 0;
		for (Integer i: restricted) {
			if (i == this.p) 
				break;
			num++;
		}
		
		int[] var = parameters.get(num).var;
		for (int i = var.length - 1; i >= 0; i--) {
			if ((this.v & (0x01 << i)) > 0) {
				int tmp = bdd.ref(bdd.and(res, var[i]));
				bdd.deref(res);
				res = tmp;
			} else {
				int tmp = bdd.ref(bdd.and(res, bdd.not(var[i])));
				bdd.deref(res);
				res = tmp;
			}
		}
		bdd.ref(res);
		return res;
	}

	@Override
	IProblem buildSAT(ArrayList<ParaIDAndNumLevels> restricted, IBooleanVariable[][] boolenVariables) {
		// System.out.println("[" + p +"] = " + v);
		// pは（絶対値で）パラメータの番号が既にはいっている
		int num = 0;
		for (ParaIDAndNumLevels i: restricted) {
			if (i.getID() == p)  {
				break;
			}
			num++;
		}
		
		IProblem res = null;
		try {
			res = new BitFixer(boolenVariables[num][v], true);
			// System.out.println(res);		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		return res;
	}
}