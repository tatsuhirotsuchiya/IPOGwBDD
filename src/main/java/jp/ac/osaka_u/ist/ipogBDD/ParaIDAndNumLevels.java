package jp.ac.osaka_u.ist.ipogBDD;

class ParaIDAndNumLevels {
	private int id; // id of a (restricted) parameter among all parameters
	private int numLevels; // num of levels of the parameter

	ParaIDAndNumLevels(int id, int numLevels) {
		this.id = id;
		this.numLevels = numLevels;
	}
	
	int getID() {
		return id;
	}
	int getNumLevels() {
		return numLevels;
	}
}
