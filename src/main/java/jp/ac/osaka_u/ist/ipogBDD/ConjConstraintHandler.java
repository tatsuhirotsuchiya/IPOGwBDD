package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/*
 * This class implements a naive checking method where the conjunction of 
 * the constraint BDD and the test case
 */

class ConjConstraintHandler extends BDDConstraintHandler implements ConstraintHandler {
	List<VariableAndBDD> parameterToVariablesAndBDD = null;

	// With constrainedParameters BDD is reduced by excluding irrelevant parameters
	ConjConstraintHandler(PList parameterList, List<Node> constraintList, TreeSet<Integer> constrainedParametersArg, VONode ast) {
		super(parameterList, constraintList, constrainedParametersArg, ast);
		parameterToVariablesAndBDD = setBDDforConstrainedParameter(parameterList);
		
		// contrainListから、ノードを呼ぶ
		bddConstraint = setBddConstraint(constraintList);
	}

//	private void printConstraintBDD() {
//		bdd.printSet(bddConstraint);
//	}

	// 各パラメータにboolean変数を割り当て．総数も計算
	private List<VariableAndBDD> setBDDforConstrainedParameter(PList parameterList) {
		List<VariableAndBDD> res = new ArrayList<VariableAndBDD>();
		this.numOfBDDvariables = 0;

		for (Integer constrainedParameter : this.constrainedParameters) {
			Parameter p = parameterList.get(constrainedParameter);
			// BDD変数の設定  2^num <= size (not size + 1)
			// 必要分ぴったりにするには　< を <= に！
			int num_vars = 1;
			for (int levels = 2;; levels *= 2) {
				if (p.value_name.size() <= levels)
					break;
				num_vars++;
			}
			// BDD変数の総数を計算
			numOfBDDvariables += num_vars;

			// boolean variables
			// 生成された順に getVar(v): 0, 1, 2, ..
			int[] var = new int[num_vars];
			for (int i = num_vars - 1; i >= 0; i--) {
				var[i] = bdd.createVar();
			}

			// 制約のBDDの設定
			// constraint for invalid values
			int f = bdd.getZero();
			bdd.ref(f);
			// domain-1より小さい数
			for (int i = var.length - 1; i >= 0; i--) {
				if ((p.value_name.size() - 1 & (0x01 << i)) > 0) {
					int g = bdd.getOne();
					bdd.ref(g);
					for (int j = var.length - 1; j > i; j--) {
						if ((p.value_name.size() - 1 & (0x01 << j)) > 0) {
							int tmp = bdd.ref(bdd.and(g, var[j]));
							bdd.deref(g);
							g = tmp;
						} else {
							int tmp = bdd.ref(bdd.and(g, bdd.not(var[j])));
							bdd.deref(g);
							g = tmp;
						}
					}
					int tmp = bdd.ref(bdd.and(g, bdd.not(var[i])));
					bdd.deref(g);
					g = tmp;
					tmp = bdd.ref(bdd.or(f, g));
					bdd.deref(g);
					f = tmp;
				}
			}

			// domain - 1自身
			int g = bdd.getOne();
			bdd.ref(g);
			for (int i = var.length - 1; i >= 0; i--) {
				if ((p.value_name.size() - 1 & (0x01 << i)) > 0) {
					int tmp = bdd.ref(bdd.and(g, var[i]));
					bdd.deref(g);
					g = tmp;
				} else {
					int tmp = bdd.ref(bdd.and(g, bdd.not(var[i])));
					bdd.deref(g);
					g = tmp;
				}
			}
			int d = bdd.or(f, g);
			bdd.ref(d);
			bdd.deref(f);
			bdd.deref(g);
			// var, d を listに追加
			res.add(new VariableAndBDD(var, d));
		}
		return res;
	}

	private int setBddConstraint(List<Node> constraintList) {
		int f = bdd.getOne();
		bdd.ref(f);

		// パラメータでつかわない値をとった場合にfalseとなるようにする
		for (VariableAndBDD vb : parameterToVariablesAndBDD) {
			int tmp = bdd.ref(bdd.and(f, vb.constraint));
			bdd.deref(f);
			f = tmp;
		}

		// 制約式の論理積をとる
		for (Node n : constraintList) {		
			int g = n.buildBDD(bdd, parameterToVariablesAndBDD, constrainedParameters);
			int tmp = bdd.ref(bdd.and(f, g));
			bdd.deref(f);
			bdd.deref(g);
			f = tmp;
		}
		return f;
	}

	public boolean isPossible(Testcase test) {
		int[] bv = binarizeReduced(test);
		// full test
		if (bv[bv.length-1] == 1) {
			return isPossibleFulltest(bv);
		}

		// create a BDD representing the partial test 	
		int bddtest = bdd.getOne();
		int b_var_id = 0;
//		for (@SuppressWarnings("unused") Integer factor: constrainedParameters) {
		for (VariableAndBDD p: parameterToVariablesAndBDD) {
			// parametersはすでに順番になっているので，前からとっていく 			
			for (int j = p.var.length -1; j >= 0; j--) {
				if (bv[b_var_id] == 1) {
					int tmp = bdd.ref(bdd.and(bddtest, p.var[j]));
					bdd.deref(bddtest);
					bddtest = tmp;				
				}
				else if (bv[b_var_id] == 0) {
					int tmp = bdd.ref(bdd.and(bddtest, bdd.not(p.var[j])));
					bdd.deref(bddtest);
					bddtest = tmp;									
				}
				else {
					// -1: skip
				}
				//			System.out.print(bdd.getVar(p.var[j]) + " ");
				b_var_id ++;
			}
		}
		//	System.out.println(bv.length + " " + b_var_id);
		// Anding Constraint and Partial Test
		int tmp = bdd.ref(bdd.and(bddConstraint, bddtest));
		bdd.deref(bddtest);
		if (tmp == 0) {
			bdd.deref(tmp); return false;
		}
		else {
			bdd.deref(tmp); return true;
		}
	}

	public boolean isPossible(int[] test) {
		int[] bv = binarizeReduced(test);
		// full test
		if (bv[bv.length-1] == 1) {
			return isPossibleFulltest(bv);
		}

		// create a BDD representing the partial test 	
		int bddtest = bdd.getOne();
		int i = 0; // should be integrated into "factor" below
		int b_var_id = 0;
		for (@SuppressWarnings("unused") Integer factor: constrainedParameters) {
			VariableAndBDD p = parameterToVariablesAndBDD.get(i);
			// parametersはすでに順番になっているので，前からとっていく 
			// つまり，get(factor)とはやってはいけない
			// for (p : parameters)でよいような
			
			for (int j = p.var.length -1; j >= 0; j--) {
				if (bv[b_var_id] == 1) {
					int tmp = bdd.ref(bdd.and(bddtest, p.var[j]));
					bdd.deref(bddtest);
					bddtest = tmp;				
				}
				else if (bv[b_var_id] == 0) {
					int tmp = bdd.ref(bdd.and(bddtest, bdd.not(p.var[j])));
					bdd.deref(bddtest);
					bddtest = tmp;									
				}
				else {
					// -1: skip
				}
				//			System.out.print(bdd.getVar(p.var[j]) + " ");
				b_var_id ++;
			}
			i++;
		}
		//	System.out.println(bv.length + " " + b_var_id);
		// Anding Constraint and Partial Test
		int tmp = bdd.ref(bdd.and(bddConstraint, bddtest));
		bdd.deref(bddtest);
		if (tmp == 0) {
			bdd.deref(tmp); return false;
		}
		else {
			bdd.deref(tmp); return true;
		}
	}
	
	private boolean isPossibleFulltest(int[] bv) {
		int node = bddConstraint;
		//		int[] bv = binarizeReduced(test);

		while (true) {
			// 恒真，恒偽
			if (node == 0)
				return false;
			else if (node == 1)
				return true;

			// このposの0, 1はノードなし
			if (bv[bdd.getVar(node)] == 1)
				node = bdd.getHigh(node);
			else
				node = bdd.getLow(node);
		}
	}
	// TreeSet<Integer> constrainedParameters にあるparameterだけを2値化
	private int[] binarizeReduced(Testcase test) {
		// the last element is used to specify whether it is  a full test case (1) or not (0)
		// note: this is NOT a good practice!
		int[] res = new int[numOfBDDvariables + 1];
		res[numOfBDDvariables] = 1; // possibly full test case

		int pos = 0;
		int i = 0;
		for (Integer factor: constrainedParameters) {
			// VariableAndBDD p = parameters.get(i); <- 
			// parameters が relevantなものだけになれば，上記に変更
			VariableAndBDD p = parameterToVariablesAndBDD.get(i);
			// parametersはすでに順番になっているので，前からとっていく 
			// つまり，get(factor)とはやってはいけない
			// for (p : parameters)でよいような
			
			int lv = test.get(factor);
			if (lv < 0) {
				res[numOfBDDvariables] = 0;
				for (int j = 0; j < p.var.length; j++) 
					res[pos + j] = -1;
			} else {
				int j0 = 0;
				// 左のbitからみている
				for (int j = p.var.length -1; j >=0; j--) {
					if ((lv & (0x01 << j)) > 0) 
						res[pos + j0] = 1;
					else
						res[pos + j0] = 0;
					j0++;
				}
			}
			pos += p.var.length;
			i++;
		}			 
		return res;
	}
	
	// TreeSet<Integer> constrainedParameters にあるparameterだけを2値化
	private int[] binarizeReduced(int test[]) {
		// the last element is used to specify whether it is  a full test case (1) or not (0)
		// note: this is NOT a good practice!
		int[] res = new int[numOfBDDvariables + 1];
		res[numOfBDDvariables] = 1; // possibly full test case

		int pos = 0;
		int i = 0;
		for (Integer factor: constrainedParameters) {
			// VariableAndBDD p = parameters.get(i); <- 
			// parameters が relevantなものだけになれば，上記に変更
			VariableAndBDD p = parameterToVariablesAndBDD.get(i);
			// parametersはすでに順番になっているので，前からとっていく 
			// つまり，get(factor)とはやってはいけない
			// for (p : parameters)でよいような
			
			int lv = test[factor];
			if (lv < 0) {
				res[numOfBDDvariables] = 0;
				for (int j = 0; j < p.var.length; j++) 
					res[pos + j] = -1;
			} else {
				int j0 = 0;
				// 左のbitからみている
				for (int j = p.var.length -1; j >=0; j--) {
					if ((lv & (0x01 << j)) > 0) 
						res[pos + j0] = 1;
					else
						res[pos + j0] = 0;
					j0++;
				}
			}
			pos += p.var.length;
			i++;
		}				 
		return res;
	}
}