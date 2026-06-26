package jp.ac.osaka_u.ist.ipogBDD;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class InputFileData {
	PList parameterList;
	GList groupList;
	List<Node> constraintList;
	TreeSet<Integer> constrainedParameters;

	VONode ast; // top node of AST for variable ordering
	
	InputFileData(PList parameterList, GList groupList,
			List<Node> constraintList, TreeSet<Integer> constrainedParameters) {
		this.parameterList = parameterList;
		this.groupList = groupList;
		this.constraintList = constraintList;
		this.constrainedParameters = constrainedParameters;
	}
	
	InputFileData(String filename) {
		BufferedReader reader = openFile(filename);
		List<String> tokenList = makeTokenList(reader);
		TokenHandler t = new TokenHandler(tokenList);

		// Read parameters and values
		this.parameterList = readParameter(t);

		// Debug
		if (Main.debugMode) {
			for(Parameter p: parameterList) { System.err.print(p.name + ": ");
			for (String name : p.value_name) { System.err.print(name + ", "); }
			System.err.println(); }
		}

		// Read groups
		this.groupList = readGroup(t, parameterList);
		// Debug
		if (Main.debugMode) {
			for(Group g: groupList) { 
				for (int i = 0; i < g.size; i++)
					System.out.print(g.member[i] + ", "); 
				System.out.println(); 
			}
		}

		// preparation for reading constraints twice
		int currentToken = t.index;
		// Read constraints
		readConstraint(t, parameterList);	
		
		// Start: Read constraints again
		int currentToken2 = t.index;
		t.index = currentToken;
		// here goes the process of parsing constraints again
		this.ast = readConstraintForVariableOrdering(t, parameterList);	
		// this.ast.evaluate(parameterList.size());
		t.index = currentToken2;
		// End
		
		// Close file
		try {
			reader.close();
		} catch (IOException e) {
			Error.printError(Main.language == Main.Language.JP ? "入力ファイルにアクセスできません"
					: "Cannot access the input file");
		}
	}
	
	private static BufferedReader openFile(String filename) {
		BufferedReader reader = null;
		if (filename == null) {
			// default: standard input
			return new BufferedReader(new InputStreamReader(System.in));
		}

		try {
			reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			Error.printError(Main.language == Main.Language.JP ? "ファイル"
					+ filename + "が見つかりません．" : "Cannot find file " + filename);
		}
		return reader;
	}
	
	private static List<String> makeTokenList(BufferedReader reader) {
		List<String> tokenList = new ArrayList<String>();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				line = line.replaceAll("\\(", " ( ");
				line = line.replaceAll("\\)", " ) ");

				line = line.replaceAll("#", " # ");

				line = line.replaceAll("\\{", " { ");
				line = line.replaceAll("\\}", " } ");

				line = line.replaceAll("\\[", " [ ");
				line = line.replaceAll("\\]", " ] ");

				// line = line.replaceAll(":", " : ");
				line = line.replaceAll(";", " ; ");

				StringTokenizer st = new StringTokenizer(line);
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (token.equals("#"))
						break;
					tokenList.add(token);
				}
			}
			reader.close();
		} catch (IOException e) {
			Error.printError(e.getMessage());
		}
		return tokenList;
	}
	
	// Read parameters
	private static PList readParameter(TokenHandler t) {
		PList parameterList = new PList();

		while (true) {
			try {
				if (t.peepToken() == null || t.peepToken().equals("{")
						|| t.peepToken().equals("(")) {
					break;
				}

				// Parameter name. Should be non-null
				String parameter_name = t.getToken();
				checkParameterName(parameter_name);
				Parameter p = new Parameter(parameter_name);

				if (t.getToken().equals("(") == false) {
					Error.printError(Main.language == Main.Language.JP ? "( がありません．"
							: "( expected");
				}
				// Read level (value) names
				do {
					String level_name = t.getToken(); // チェックしてない
					checkLevelName(level_name);
					p.addValueName(level_name);
					if (t.peepToken() == null) {
						Error.printError(Main.language == Main.Language.JP ? "パラメータ指定に誤りがあります"
								: "Invalid parameters");
					}
				} while (t.peepToken().equals(")") == false);
				// Read ) (right parenthesis)
				t.getToken();

				// 値名の重複チェック
				p.check();

				parameterList.add(p);
			} catch (OutOfTokenStreamException e) {
				Error.printError(Main.language == Main.Language.JP ? "パラメータ指定に誤りがあります"
						: "Invalid parameters");
			}
		}

		// 因子名の重複チェック
		if (parameterList.checkNameDuplication())
			Error.printError(Main.language == Main.Language.JP ? "因子名が重複しています"
					: "Duplicated parameters");

		// # parameters must be at most 2
		if (parameterList.size() < 2)
			Error.printError(Main.language == Main.Language.JP ? "因子は2個以上必要です"
					: "Multiple parameters required");

		// Sort parameters in descending order of domain size
		parameterList.assignID1();
		Collections.sort(parameterList);
//		Collections.reverse(parameterList);
		
		return parameterList;
	}
	
	private static void checkParameterName(String name) {
		if (name.contains("(") || name.contains(")") || name.contains("{")
				|| name.contains("}") || name.contains("[")
				|| name.contains("]") || name.contains(";")
				|| name.contains(",")) {
			Error.printError(Main.language == Main.Language.JP ? "因子名に禁止文字が含まれています"
					: "Invalid symbol in parameter name");
		}
	}

	private static void checkLevelName(String name) {
		if (name.contains("(") || name.contains(")") || name.contains("{")
				|| name.contains("}") || name.contains("[")
				|| name.contains("]") || name.contains(";")
				|| name.contains(",")) {
			Error.printError(Main.language == Main.Language.JP ? "水準名に禁止文字が含まれています"
					: "Invalid symbol in parameter value");
		}
	}
	
	// グループの読み込み
	private static GList readGroup(TokenHandler t, PList parameterList) {
		GList groupList = new GList();
		while (true) {
			if (t.peepToken() == null || t.peepToken().equals("(")) {
				break;
			}
			try {
				if (t.getToken().equals("{") == false) {
					Error.printError("{ expected");
				}
			} catch (OutOfTokenStreamException e) {
				Error.printError(Main.language == Main.Language.JP ? "パラメータ指定に誤りがあります"
						: "Invalid parameter");
			}
			// グループのパラメータ
			Set<Integer> memberSet = new TreeSet<Integer>();
			do {
				String name = null;
				try {
					name = t.getToken(); // チェックしてない
				} catch (OutOfTokenStreamException e) {
					Error.printError(Main.language == Main.Language.JP ? "グループ指定に誤りがあります"
							: "Invalid grouping");
				}
				try {
					/*
					 * debug System.out.print(name + " " +
					 * parameterList.getID(name) + ", ");
					 */
					memberSet.add(Integer.valueOf(parameterList.getID(name)));
				} catch (NoParameterNameException e) {
					Error.printError(Main.language == Main.Language.JP ? "グループ指定で因子名に誤りがあります"
							: "Invalid parameter in group");
				}
				if (t.peepToken() == null) {
					Error.printError(Main.language == Main.Language.JP ? "グループ指定に誤りがあります"
							: "Invalid grouping");
				}
			} while (t.peepToken().equals("}") == false);
			Group g = new Group(memberSet);
			groupList.add(g);

			// } のよみこみ
			try {
				t.getToken();
			} catch (OutOfTokenStreamException e) {
				Error.printError(Main.language == Main.Language.JP ? "グループ指定に誤りがあります"
						: "Invalid grouping");
			}
		}
		// TODO groupの整列
		groupList.sort();
		// TODO 重複要素の削除

		return groupList;
	}
	
	private void readConstraint(TokenHandler t, PList parameterList) {
		List<Node> constraintList = new ArrayList<Node>();
		TreeSet<Integer> constrainedParameters = new TreeSet<Integer>();
		while (true) {
			if (t.peepToken() == null) {
				break;
			}
			//Node n = new Parse(t, parameterList).parseExpression();
			NodeAndConstrainedParameters res = new Parse(t, parameterList).extendedParseExpression();
			constraintList.add(res.node);
			constrainedParameters.addAll(res.constrainedParameters);
		}
		this.constraintList = constraintList;
		this.constrainedParameters = constrainedParameters;
	}
	
	private VONode readConstraintForVariableOrdering(TokenHandler t, PList parameterList) {
		VOBooleanMultinaryOperator res = new VOAndOperator();
		while (true) {
			if (t.peepToken() == null) {
				break;
			}
			VONode n = new ParseForVariableOrdering(t, parameterList).parseExpression();
			//	NodeAndConstrainedParameters res = new Parse(t, parameterList).extendedParseExpression();
			res.ChildList.add(n);
		}
		if (res.ChildList.size() <= 1) {
			return res.ChildList.get(0);
		}
		return res;
	}

	/*
	class LevelComparator implements java.util.Comparator<Parameter> {
		public int compare(Parameter s, Parameter t) {
			//               + (x > y)
			// compare x y = 0 (x = y)
			//               - (x < y)
//			return   s.value_name.size() - t.value_name.size();
			return 1;
		}
	}
	*/
}


