package jp.ac.osaka_u.ist.ipogBDD;

interface ConstraintHandler {
	boolean isPossible(Testcase test);
	boolean isPossible(int[] test);
}
