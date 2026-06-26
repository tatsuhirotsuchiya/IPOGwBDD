package jp.ac.osaka_u.ist.ipogBDD;

class VariableAndBDD {
	int[] var; // bdd nodes
	// TODO 名前 constraint -> 何かよいものに
	int constraint; // bdd for invalid values

	VariableAndBDD(int[] var, int constraint) {
		this.var = var;
		this.constraint = constraint;
	}
}
