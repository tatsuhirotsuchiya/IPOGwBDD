package jp.ac.osaka_u.ist.ipogBDD;

// Naive implementation of Knuth's algorithm
public class CombinationGenerator {
	private int c[];
	private int j;
	private final int t;
	
	/** 
	 * @param n number of things 
	 * @param t combination size 
	 * n > t must hold
	 */
	CombinationGenerator(int n, int t) {
		this.c = new int[t + 2];
		this.t = t;
		for (int i = 1; i <= t; i++) 
			c[i - 1] = i - 1;
		c[t] = n; c[t+1] = 0;
		this.j = t;
	}
	
	int get(int i) {
		if (i < t)
			return c[i];
		else {
			throw new RuntimeException();
			// return 0;
		}
	}
	
	boolean visitNext() {
		int x;
		if (j > 0) {
			c[j-1] = j; j--; return true;
		}
		if (c[0]+1 < c[1])  {
			c[0]++; return true;
		} else {
			j = 2;
		}
		do {
			c[j-2] = j - 2;
			x = c[j-1] + 1;
			if (x == c[j]) {
				j++; 
			} else 
				break;
		} while (true);
		if (j > t) 
			return false;
		c[j-1] = x; j--;
		return true;
	}

	int getID() {
		int sum = 0;
		for (int i = 1; i <= t; i++) {
			int ci = c[i-1];
			// compute ci C i and add it to sum
			if (ci >= i) {
				int tmp = ci;
				for (int j = ci - 1; j >= ci-i+1; j--)
					tmp *= j;
				for (int j = i; j > 1; j--)
					tmp /= j;
				sum += tmp;
			}
		}
		return sum;
	}

	private static int getCombinationID(int[] c) {
		int sum = 0;
		for (int i = 1; i <= c.length; i++) {
			int ci = c[i-1];
			// compute ci C i and add it to sum
			if (ci >= i) {
				int tmp = ci;
				for (int j = ci - 1; j >= ci-i+1; j--)
					tmp *= j;
				for (int j = i; j > 1; j--)
					tmp /= j;
				sum += tmp;
			}
		}
		return sum;
	}

	static int getTotalNumber(int n, int t) {
		int[] comb = new int[t];
		// create the last combination
		for (int i = 0; i < t; i++) {
			comb[i] = n - t + i;
		}
		return getCombinationID(comb) + 1;
	}
}