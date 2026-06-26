package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


abstract class VONode {
	
	int[] distanceToParameter; // create instance whenever update occurs
	// -1: unseen, 0,... : distance from the node to the parameter node 
	abstract void evaluate(int numOfParameters, int[][] mutual);
	// mutual: call-by-reference. no new instance is created
}

abstract class VOBooleanOperator extends VONode {
}

abstract class VOBooleanUnaryOperator extends VOBooleanOperator {
	VONode Child;
}

class VONotOperator extends VOBooleanUnaryOperator {
// method
	void evaluate(int numOfParameters, int[][] mutual) {
		Child.evaluate(numOfParameters, mutual);
		this.distanceToParameter = Child.distanceToParameter;
	}
}

abstract class VOBooleanBinaryOperator extends VOBooleanOperator {
	VONode Left, Right;
	void evaluate(int numOfParameters, int[][] mutual) {
		this.distanceToParameter = new int[numOfParameters];
		Left.evaluate(numOfParameters, mutual);
		Right.evaluate(numOfParameters, mutual);

		for(int i = 0; i < numOfParameters; i++) {
			if (Left.distanceToParameter[i] < 0 && Right.distanceToParameter[i] <0) {
				this.distanceToParameter[i] = -1;
			}
			else if (Left.distanceToParameter[i] < 0 && Right.distanceToParameter[i] >= 0) {
				this.distanceToParameter[i] = Right.distanceToParameter[i] + 1;
			}
			else if (Left.distanceToParameter[i] >= 0 && Right.distanceToParameter[i] < 0) {
				this.distanceToParameter[i] = Left.distanceToParameter[i] + 1;
			}
			else if (Left.distanceToParameter[i] < Right.distanceToParameter[i]){
				this.distanceToParameter[i] = Left.distanceToParameter[i];
			}
		}

		// if for some i, j, i\=j, two children see i and j respectively, then mutual is set
		for(int i=0; i < numOfParameters-1; i++) {
			for (int j=i+1; j< numOfParameters; j++) {
				if ((Left.distanceToParameter[i] > 0 && Right.distanceToParameter[j] > 0)) {
					int tmp = Left.distanceToParameter[i] + Right.distanceToParameter[j] + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if ((Left.distanceToParameter[j] > 0 && Right.distanceToParameter[i] > 0)) {
					int tmp = Left.distanceToParameter[j] + Right.distanceToParameter[i] + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}				
			}
		}
	}
}

class VOIfOperator extends VOBooleanBinaryOperator {
// method
	
}

class VOEqualityOperator extends VOBooleanBinaryOperator {
//
}

class VOInequalityOperator extends VOBooleanBinaryOperator {

}

abstract class VOBooleanTrinaryOperator extends VOBooleanOperator {
	VONode Left, Middle, Right;
	 void evaluate(int numOfParameters, int[][] mutual) {
		this.distanceToParameter = new int[numOfParameters];
		Left.evaluate(numOfParameters, mutual);
		Middle.evaluate(numOfParameters, mutual);
		Right.evaluate(numOfParameters, mutual);
		
		for(int i = 0; i < numOfParameters; i++) {
			int a0 = Left.distanceToParameter[i]; 
			int a1 = Middle.distanceToParameter[i]; 
			int a2 = Right.distanceToParameter[i];
			// sort descending order
			if (a0 < a1) {int tmp = a1; a1 = a0; a0 = tmp;}
			if (a1 < a2) {int tmp = a2; a2 = a1; a1 = tmp;}
			if (a0 < a1) {int tmp = a1; a1 = a0; a0 = tmp;}
			if (a0 < 0) { // all -1
				this.distanceToParameter[i] = -1;
			}
			else if (a1 < 0) { // two are -1
				this.distanceToParameter[i] = a0 + 1;
			}
			else if (a2 < 0) { // just one is -1 
				this.distanceToParameter[i] = a1 + 1;
			}
			else {
				this.distanceToParameter[i] = a2 + 1;
			}
		}
		
		// if for some i, j, i\=j, two of the three children see i and j respectively, then mutual is set
		for(int i=0; i < numOfParameters-1; i++) {
			for (int j=i+1; j< numOfParameters; j++) {
				int a0 = Left.distanceToParameter[i]; 
				int a1 = Middle.distanceToParameter[i]; 
				int a2 = Right.distanceToParameter[i];
				int b0 = Left.distanceToParameter[j]; 
				int b1 = Middle.distanceToParameter[j]; 
				int b2 = Right.distanceToParameter[j];
				
				if (a0 > 0 && b1 > 0) {
					int tmp = a0 + b1 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if (a0 > 0 && b2 > 0) {
					int tmp = a0 + b2 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if (a1 > 0 && b0 > 0) {
					int tmp = a1 + b0 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if (a1 > 0 && b2 > 0) {
					int tmp = a1 + b2 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if (a2 > 0 && b0 > 0) {
					int tmp = a2 + b0 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}
				if (a2 > 0 && b1 > 0) {
					int tmp = a2 + b1 + 2;
					if (mutual[i][j] > tmp) {
						mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
					}
				}		
			}
		}		
		
	}
}

class VOIfthenelseOperator extends VOBooleanTrinaryOperator {
//
}

abstract class VOBooleanMultinaryOperator extends VOBooleanOperator {
	List<VONode> ChildList = new ArrayList<VONode>();

	 void evaluate(int numOfParameters, int[][] mutual) {
		this.distanceToParameter = new int[numOfParameters];
		for (VONode v: ChildList) {
			v.evaluate(numOfParameters, mutual);
		}			
		for(int i = 0; i < numOfParameters; i++) {
			// choose minimum value at least equal to 0
			int min = -1;
			for (VONode v: ChildList) {
				if (v.distanceToParameter[i] >= 0) {
					if (min < 0 || (min >=0 && v.distanceToParameter[i] < min)) {
						min = v.distanceToParameter[i];
					}
				}
			}
			if (min == -1) 
				this.distanceToParameter[i] = -1;
			else 
				this.distanceToParameter[i] = min + 1;
		}
		
		// if for some i, j, i\=j, two of the children see i and j respectively, then mutual is set
		for(int i=0; i < numOfParameters-1; i++) {
			for (int j=i+1; j< numOfParameters; j++) {
				for (VONode v0: ChildList) {
					for (VONode v1: ChildList) {
						if (v0.equals(v1)) continue;
						if (v0.distanceToParameter[i] > 0 && v1.distanceToParameter[j] > 0) {
							int tmp = v0.distanceToParameter[i] + v1.distanceToParameter[j] + 2;
							if (mutual[i][j] > tmp) {
								mutual[i][j] = tmp; mutual[j][i] = tmp; // mutual[i][j]= mutual[j][i]
							}
						}					
					}
				}
				
			}
		}		
		
	}
}

class VOOrOperator extends VOBooleanMultinaryOperator {

}

class VOAndOperator extends VOBooleanMultinaryOperator {

}

abstract class VOAtomicExpression extends VONode {
}

class AtomicExpressionForVariableOrdering extends VOAtomicExpression {
	// for keeping parameters' number involved in the expression
	// if no parameters appear, p0 = p1 = null
	// if exactly one parameter occurs; p0 = the ID of the parameter, p1 = null
	Integer p0, p1;
	AtomicExpressionForVariableOrdering(int numOfParameters) {
		this.p0 = null; this.p1 = null; 
		this.distanceToParameter = new int[numOfParameters];
		for (int i=0; i < numOfParameters; i++) {
			distanceToParameter[i] = -1;
		}
	}
	AtomicExpressionForVariableOrdering(int parameter0,int numOfParameters) {
		this.p0 = parameter0; this.p1 = null; 
		this.distanceToParameter = new int[numOfParameters];
		for (int i=0; i < numOfParameters; i++) {
			distanceToParameter[i] = (i == parameter0 ? 1: -1);
		}
	}
	AtomicExpressionForVariableOrdering(int parameter0, int parameter1, int numOfParameters) {
		this.p0 = parameter0; this.p1 = parameter1; 
		this.distanceToParameter = new int[numOfParameters];
		for (int i=0; i < numOfParameters; i++) {
			distanceToParameter[i] = (i == parameter0 || i == parameter1 ? 1: -1);
		}
	}
	@Override
	void evaluate(int numOfParameters, int[][] mutual) {
		// operations on distanceToParameter in constructor can be moved here
		if (this.p0 != null && this.p1 != null) { // two children are both parameter nodes 
			mutual[p0][p1] = mutual[p1][p0] = 2;
		}
		
	}
	
}


class ParseForVariableOrdering {
	private TokenHandler t;
	private PList parameterList;

	private TreeSet<Integer> constrainedParameters = new TreeSet<Integer>();
	
	int distance[][]; // distance between two parameters
	
	ParseForVariableOrdering(TokenHandler t, PList parameterList) {
		this.t = t;
		this.parameterList = parameterList;
		this.distance = new int[parameterList.size()][parameterList.size()];
		for (int i=0; i< parameterList.size(); i++) {
			for (int j=0; j< parameterList.size(); j++) {
				distance[i][j] = -1; // -1 means infinity
			}
		}
	}

	VONode parseExpression() {
		String nextToken = t.peepToken();
		try {
			if (nextToken == null) {
				Error.printError(Main.language == Main.Language.JP ? "制約式に誤りがあります"
						: "Invalid constraints");
				return null;
			}
			else if (nextToken.equals("(")) {
				// debug: System.err.println(this.constrainedParameters.toString());
				return expressionWithParentheses();
			}
			else {
				// error
				Error.printError(Main.language == Main.Language.JP ? "制約に'('がありません"
						: "( expected in constraints");
				return null;
			}
		} catch (OutOfTokenStreamException e) {
			Error.printError(Main.language == Main.Language.JP ? "制約式に誤りがあります"
					: "Invalid constraints");
			return null;
		}
	}

	private VONode expressionWithParentheses() throws OutOfTokenStreamException {
		VONode res; // 戻り値
		String token = t.getToken();
		if (token.equals("(") == false) {
			// error
			// this block is unreachable
			Error.printError(Main.language == Main.Language.JP ? "制約に'('がありません"
					: "( expected in constraints");
			return null;
		}
		// expression :: (expression)
		if (t.peepToken() == null)
			throw new OutOfTokenStreamException();
		if (t.peepToken().equals("("))
			res = expressionWithParentheses();
		else
			// otherwise
			res = expressionBody();
		// closed with ')' ?
		if (t.getToken().equals(")") == false) {
			// error
			Error.printError(Main.language == Main.Language.JP ? "制約に')'がありません"
					: ") expected in constraints");
			return null;
		}
		return res;
	}

	private VONode expressionBody() throws OutOfTokenStreamException {
		// 演算子の次のトークンが ( か どうかで判断
		// case 1: ( <> (
		// case 2: ( <> [ foo, ( <> foo
		String nextNextToken = t.peepNextToken();
		if (nextNextToken == null)
			throw new OutOfTokenStreamException();
		if (nextNextToken.equals("("))
			return boolExpression();
		else
			return atomExpression();
	}

	private VONode boolExpression() throws OutOfTokenStreamException {
		// boolean expression with operator
		String token = t.peepToken();
		if (t.peepToken() == null)
			throw new OutOfTokenStreamException();
		if (token.equals("not"))
			return notExpression();
		else if (token.equals("=="))
			return equalityExpression();
		else if (token.equals("<>"))
			return inequalityExpression();
		else if (token.equals("or"))
			return orExpression();
		else if (token.equals("and"))
			return andExpression();
		else if (token.equals("if"))
			return ifExpression();
		else if (token.equals("ite"))
			return iteExpression();
		else
			Error.printError(token + " is not a valid operator");
		return null; // unreachable
	}

	private VONode notExpression() throws OutOfTokenStreamException {
		VOBooleanUnaryOperator res = new VONotOperator();
		t.getToken();
		res.Child = parseExpression();
		return res;
	}

	private VONode equalityExpression() throws OutOfTokenStreamException {
		VOBooleanBinaryOperator res = new VOEqualityOperator();
		t.getToken();
		res.Left = parseExpression();
		res.Right = parseExpression();
		return res;
	}

	private VONode inequalityExpression() throws OutOfTokenStreamException {
		VOBooleanBinaryOperator res = new VOInequalityOperator();
		t.getToken();
		res.Left = parseExpression();
		res.Right = parseExpression();
		return res;
	}

	private VONode orExpression() throws OutOfTokenStreamException {
		VOBooleanMultinaryOperator res = new VOOrOperator();
		t.getToken();
		res.ChildList.add(parseExpression());
		res.ChildList.add(parseExpression());
		if (t.peepToken() == null)
			throw new OutOfTokenStreamException();
		while (t.peepToken().equals(")") == false) {
			res.ChildList.add(parseExpression());
			if (t.peepToken() == null)
				throw new OutOfTokenStreamException();
		}
		return res;
	}

	private VONode andExpression() throws OutOfTokenStreamException {
		VOBooleanMultinaryOperator res = new VOAndOperator();
		t.getToken();
		res.ChildList.add(parseExpression());
		res.ChildList.add(parseExpression());
		if (t.peepToken() == null)
			throw new OutOfTokenStreamException();
		while (t.peepToken().equals(")") == false) {
			res.ChildList.add(parseExpression());
			if (t.peepToken() == null)
				throw new OutOfTokenStreamException();
		}
		return res;
	}

	private VONode ifExpression() throws OutOfTokenStreamException {
		VOBooleanBinaryOperator res = new VOIfOperator();
		t.getToken();
		res.Left = parseExpression();
		res.Right = parseExpression();
		return res;
	}

	private VONode iteExpression() throws OutOfTokenStreamException {
		VOBooleanTrinaryOperator res = new VOIfthenelseOperator();
		t.getToken();
		res.Left = parseExpression();
		res.Middle = parseExpression();
		res.Right = parseExpression();
		return res;
	}

	private VONode atomExpression() throws OutOfTokenStreamException {
		// 次のトークンをチェック: 演算子でないといけない
		String token = t.getToken();
		if (token.equals("=="))
			return equalityAtomExpression();
		else if (token.equals("<>"))
			return inequalityAtomExpression();
		else if (token.equals("==="))
			return arithmeticEqualityAtomExpression(new EqualTo(), new EqualTo());
		else if (token.equals("!=="))
			return artithmeticInequalityAtomExpression(new EqualTo(), new EqualTo());
		else if (token.equals("<"))
			return arithmeticEqualityAtomExpression(new LessThan(), new GreaterThan());
		else if (token.equals(">"))
			return arithmeticEqualityAtomExpression(new GreaterThan(), new LessThan());
		else if (token.equals("<="))
			return arithmeticEqualityAtomExpression(new LTE(), new GTE());
		else if (token.equals(">="))
			return arithmeticEqualityAtomExpression(new GTE(), new LTE());
		else
			Error.printError(Main.language == Main.Language.JP ? "制約式に == か <> が必要です"
					: "== or <> expected in constraints");
		return null;
	}

	private VONode artithmeticInequalityAtomExpression(RelationOverDoublePair com1,
			RelationOverDoublePair com2) throws OutOfTokenStreamException {
		VOBooleanUnaryOperator res = new VONotOperator();
		res.Child = arithmeticEqualityAtomExpression(com1, com2);
		return res;
	}

	private VONode inequalityAtomExpression() throws OutOfTokenStreamException {
		VOBooleanUnaryOperator res = new VONotOperator();
		res.Child = equalityAtomExpression();
		return res;
	}

	private VONode arithmeticEqualityAtomExpression(RelationOverDoublePair com1, RelationOverDoublePair com2)
			throws OutOfTokenStreamException {
		// case 1 val1 val2        com1
		// case 2 val1 [para1]     com2
		// case 3 [para1] val1     com1
		// case 4 [para1] [para2]  com1
		String val1, val2, para1, para2;
		String token1, token2;

		token1 = t.peepToken();
		token2 = t.peepNextToken();

		if (token1 == null || token2 == null)
			throw new OutOfTokenStreamException();

		// case 1
		if ((token1.equals("[") == false) && (token2.equals("[") == false)) {
			val1 = t.getToken();
			val2 = t.getToken();
			return new AtomicExpressionForVariableOrdering(this.parameterList.size());
		}

		// case 2 
		if ((token1.equals("[") == false) && (token2.equals("[") == true)) {
			val1 = t.getToken();
			t.getToken(); // must be [
			para1 = t.getToken();
			if (t.getToken().equals("]") == false) {
				Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
						: "] expected in constraints");
			}
			return compareArithmeticParameterAndValue(para1, val1, com2);
		}

		// case 3, 4 
		t.getToken(); // must be "["
		para1 = t.getToken();
		if (t.getToken().equals("]") == false) {
			Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
					: "] expected in constraints");
		}
		token1 = t.peepToken();
		if (token1 == null)
			throw new OutOfTokenStreamException();

		// case 3
		if (token1.equals("[") == false) {
			val1 = t.getToken();
			return compareArithmeticParameterAndValue(para1, val1, com1);
		}

		// case 4 
		t.getToken(); // must be [
		para2 = t.getToken();
		if (t.getToken().equals("]") == false) {
			Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
					: "] expected in constraints");
		}
		return compareArithmeticParameterAndParameter(para1, para2, com1);
	}


	private VONode equalityAtomExpression() throws OutOfTokenStreamException {
		// case 1 val1 val2
		// case 2 val1 [para1]
		// case 3 [para1] val1
		// case 4 [para1] [para2]
		String val1, val2, para1, para2;
		String token1, token2;

		token1 = t.peepToken();
		token2 = t.peepNextToken();

		if (token1 == null || token2 == null)
			throw new OutOfTokenStreamException();

		// case 1
		if ((token1.equals("[") == false) && (token2.equals("[") == false)) {
			val1 = t.getToken();
			val2 = t.getToken();
			return new AtomicExpressionForVariableOrdering(this.parameterList.size());
		}

		// case 2
		if ((token1.equals("[") == false) && (token2.equals("[") == true)) {
			val1 = t.getToken();
			t.getToken(); // must be [
			para1 = t.getToken();
			if (t.getToken().equals("]") == false) {
				Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
						: "] expected in constraints");
			}
			return compareParameterAndValue(para1, val1);
		}

		// case 3, 4
		t.getToken(); // must be "["
		para1 = t.getToken();
		if (t.getToken().equals("]") == false) {
			Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
					: "] expected in constraints");
		}
		token1 = t.peepToken();
		if (token1 == null)
			throw new OutOfTokenStreamException();

		// case 3
		if (token1.equals("[") == false) {
			val1 = t.getToken();
			return compareParameterAndValue(para1, val1);
		}

		// case 4
		t.getToken(); // must be [
		para2 = t.getToken();
		if (t.getToken().equals("]") == false) {
			Error.printError(Main.language == Main.Language.JP ? "制約式に]が必要です"
					: "] expected in constraints");
		}
		return compareParameterAndParameter(para1, para2);
	}
	
	private VONode compareParameterAndValue(String para, String val) {
		int parameterID = 0;
		// 因子名が正しいかチェック
		try {
			parameterID = parameterList.getID(para);
			this.constrainedParameters.add(parameterID);
		} catch (NoParameterNameException e) {
			Error.printError(Main.language == Main.Language.JP ? "制約中の因子名に誤りがあります"
					: "Invalid parameter name in constraints");
		}
		return new AtomicExpressionForVariableOrdering(parameterID, this.parameterList.size());
	}

	private VONode compareArithmeticParameterAndValue(String para, String val, RelationOverDoublePair com) {
		int parameterID = 0;

		// 因子名が正しいかチェック
		try {
			parameterID = parameterList.getID(para);
			this.constrainedParameters.add(parameterID);
		} catch (NoParameterNameException e) {
			Error.printError(Main.language == Main.Language.JP ? "制約中の因子名に誤りがあります"
					: "Invalid parameter name in constraints");
		}
		return new AtomicExpressionForVariableOrdering(parameterID, this.parameterList.size());
	}
	
	private VONode compareParameterAndParameter(String para1, String para2) {
		int parameterID1 = 0;
		int parameterID2 = 0;
		// 因子名が正しいかチェック
		try {
			parameterID1 = parameterList.getID(para1);
			parameterID2 = parameterList.getID(para2);
			this.constrainedParameters.add(parameterID1);
			this.constrainedParameters.add(parameterID2);
		} catch (NoParameterNameException e) {
			Error.printError(Main.language == Main.Language.JP ? "制約中の因子名に誤りがあります"
					: "Invalid parameter name in constraints");
		}
		return new AtomicExpressionForVariableOrdering(parameterID1, parameterID2, this.parameterList.size());
	}

	private VONode compareArithmeticParameterAndParameter(String para1, String para2, RelationOverDoublePair com) {
		int parameterID1 = 0;
		int parameterID2 = 0;
		// 因子名が正しいかチェック
		try {
			parameterID1 = parameterList.getID(para1);
			parameterID2 = parameterList.getID(para2);
			this.constrainedParameters.add(parameterID1);
			this.constrainedParameters.add(parameterID2);
		} catch (NoParameterNameException e) {
			Error.printError(Main.language == Main.Language.JP ? "制約中の因子名に誤りがあります"
					: "Invalid parameter name in constraints");
		}
		return new AtomicExpressionForVariableOrdering(parameterID1, parameterID2, this.parameterList.size());
	}
}


