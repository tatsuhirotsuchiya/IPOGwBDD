package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

class NewBDDConstraintHandler extends BDDConstraintHandler implements ConstraintHandler{
	List<VariableAndBDD> parameterToVariablesAndBDD = null;
	
	NewBDDConstraintHandler(PList parameterList, List<Node> constraintList, 
			TreeSet<Integer> constrainedParametersArg, VONode ast) {
		super(parameterList, constraintList, constrainedParametersArg, ast);
		parameterToVariablesAndBDD = setBDDforConstrainedParameter(parameterList);
		// contrainListから、ノードを呼ぶ
		bddConstraint = setBddConstraint(constraintList);
	}
	
	void printConstraintBDD() {
		bdd.printSet(bddConstraint);
	}

	// 各パラメータにboolean変数を割り当て．総数も計算
	private List<VariableAndBDD> setBDDforConstrainedParameter(PList parameterList) {
		List<VariableAndBDD> res = new ArrayList<VariableAndBDD>();
		this.numOfBDDvariables = 0;

		for (Integer constrainedParameter : this.constrainedParameters) {
			Parameter p = parameterList.get(constrainedParameter);
			// BDD変数の設定
			int num_vars = 1;
			for (int levels = 2;; levels *= 2) {
				if (p.value_name.size() < levels)
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
			// domain-1以下の数字のみ有効
			// bool variables の数はdomain-1をあらわせるだけはある
			//
			// domain-1の2進表現では，最上位の変数にあたるビットは常に1とは限らない
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
					bdd.deref(g); bdd.deref(f);
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

		long middle = System.currentTimeMillis();
		// *を付加
		f = extendBddConstraint(f);
		
		long end = System.currentTimeMillis();
		if (Main.verbose > 0) {
			System.out.println("BDD quantification " + (end - middle)  + "ms");			
		}	
		return f;
		
//		// new code..  may be logically buggy...
//		int rangeConst = f;
//		int newf = bdd.ref(bdd.getOne());
//		for (Node n: constraintList) {
//			int g = n.buildBDD(bdd,  parameterToVariablesAndBDD, constrainedParameters); // g reffed
//			int grange = bdd.ref(bdd.and(g, rangeConst)); 
//			bdd.deref(g);
//			int tmp = extendBddConstraint(grange); // grange dereffed then tmp reffed
//			int tmp2 = bdd.ref(bdd.and(newf, tmp)); // tmp2 reffed
//			bdd.deref(newf);
//			bdd.deref(tmp);
//			newf = tmp2;
//		}
//		return newf;
	}
		
	private int extendBddConstraint(int constraint) {
		int f = constraint;
		
		if (Main.handler == Main.Handler.UP)  
			Collections.reverse(parameterToVariablesAndBDD);

		for (VariableAndBDD p : parameterToVariablesAndBDD) {
			int cube = p.var[0];
			bdd.ref(cube);
			for (int i = 1; i < p.var.length; i++) {
				int tmp = bdd.ref(bdd.and(cube, p.var[i]));
				bdd.deref(cube);
				cube = tmp;
			}
			int tmp0 = bdd.ref(bdd.exists(f, cube)); // tmp0 reffed
			int tmp = bdd.ref(bdd.and(tmp0, cube)); // tmp reffed
			bdd.deref(cube);		
			bdd.deref(tmp0);

			int newf = bdd.ref(bdd.or(f, tmp));
			bdd.deref(tmp);
			bdd.deref(f);

			f = newf;
		}
		if (Main.handler == Main.Handler.UP)  
			Collections.reverse(parameterToVariablesAndBDD);
	
		return f;
	}

	// テストケースが制約を満たすか
	public boolean isPossible(Testcase test) {
		int node = bddConstraint;
		boolean[] bv = binarizeReduced(test);

		while (true) {
			// 恒真，恒偽
			if (node == 0)
				return false;
			else if (node == 1)
				return true;

			// このposの0, 1はノードなし
			if (bv[bdd.getVar(node)] == true)
				node = bdd.getHigh(node);
			else
				node = bdd.getLow(node);
		}
	}
	
	public boolean isPossible(int[] test) {
		int node = bddConstraint;
		boolean[] bv = binarizeReduced(test);

		while (true) {
			// 恒真，恒偽
			if (node == 0)
				return false;
			else if (node == 1)
				return true;

			// このposの0, 1はノードなし
			if (bv[bdd.getVar(node)] == true)
				node = bdd.getHigh(node);
			else
				node = bdd.getLow(node);
		}
	}
	
	// TreeSet<Integer> constrainedParameters にあるparameterだけを2値化
	private boolean[] binarizeReduced(Testcase test) {
		
		boolean[] res = new boolean[numOfBDDvariables];
		int pos = 0;
		int i = 0;
		for (Integer factor: constrainedParameters) {
			// VariableAndBDD p = parameters.get(i); <- 
			// parameters が relevantなものだけになれば，上記に変更
			VariableAndBDD p = parameterToVariablesAndBDD.get(i);
			// parametersはすでに順番になっているので，前からとっていく 
			// つまり，get(factor)とはやってはいけない

			int lv = test.get(factor);
			if (lv < 0) {
				for (int j = 0; j < p.var.length; j++) 
					res[pos + j] = true;
			} else {
				int j0 = 0;
				for (int j = p.var.length -1; j >=0; j--) {
					if ((lv & (0x01 << j)) > 0) 
						res[pos + j0] = true;
					else
						res[pos + j0] = false;
					j0++;
				}
			}
			pos += p.var.length;
			i++;
		}	 
		return res;
	}
	
	// TreeSet<Integer> constrainedParameters にあるparameterだけを2値化
	private boolean[] binarizeReduced(int[] test) {
		
		boolean[] res = new boolean[numOfBDDvariables];
		int pos = 0;
		int i = 0;
		for (Integer factor: constrainedParameters) {
			// VariableAndBDD p = parameters.get(i); <- 
			// parameters が relevantなものだけになれば，上記に変更
			VariableAndBDD p = parameterToVariablesAndBDD.get(i);
			// parametersはすでに順番になっているので，前からとっていく 
			// つまり，get(factor)とはやってはいけない

			int lv = test[factor];
			if (lv < 0) {
				for (int j = 0; j < p.var.length; j++) 
					res[pos + j] = true;
			} else {
				int j0 = 0;
				for (int j = p.var.length -1; j >=0; j--) {
					if ((lv & (0x01 << j)) > 0) 
						res[pos + j0] = true;
					else
						res[pos + j0] = false;
					j0++;
				}
			}
			pos += p.var.length;
			i++;
		}
		return res;
	}
}

class NoConstraintHandler implements ConstraintHandler {
	public boolean isPossible(Testcase test) { return true;}
	public boolean isPossible(int[] test) { return true;}
}
