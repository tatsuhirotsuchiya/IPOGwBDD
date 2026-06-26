package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;

import jp.ac.osaka_u.ist.ipogBDD.Main.Randstar;

public class IPOG extends Generator {

	protected int matrix[][]; 
	protected int strength;
	protected boolean bitmap[][];
	protected int numOfRows; 

	// used for horizontal extension
	private int positions[];
	
	IPOG(ParameterModel parametermodel, GList groupList,
			ConstraintHandler constrainthandler, List<Testcase> seed,
			long randomseed, int strength) {
		super(parametermodel, groupList, constrainthandler, seed, randomseed);
		this.strength = strength;
		this.matrix = new int [MaxNumOfTestcases][parametermodel.size];
		// initialize test suite to empty one
		for (int i = 0; i < MaxNumOfTestcases; i++) {
			for (int j = 0; j < parametermodel.size; j++) {
				matrix[i][j] = -1;
			}
		} 
	}
	
	@Override
	List<Testcase> generate() throws OutOfMaxNumOfTestcasesException {

		buildFirstColumnsDebug();
		// buildFirstColumns();
		List<Testcase> res = new ArrayList<Testcase>();

		for (int factorID = strength; factorID < parametermodel.size; factorID++) {

			initializeBitmap(factorID); 
			// horizontalExtention(factorID);
			horizontalExtentionNew(factorID);
			
			// verticalExtentionDebug(factorID);
			verticalExtention(factorID);
		}

		// fill in don't care entries (stars)
		if (Main.randstar == Randstar.OFF) {
			fillInStars();
		}
		
		for (int i = 0; i < numOfRows; i++) {
			Testcase test = new Testcase(parametermodel.size);
			for (int j = 0; j < parametermodel.size; j++) {
				test.set(j, matrix[i][j]);
				// System.out.print((matrix[i][j]>= 0? "~" : "") + matrix[i][j]);
			}
			res.add(test);
			// System.out.println();
			if (res.size() > MaxNumOfTestcases)
				throw new OutOfMaxNumOfTestcasesException();
		}
		return res;
	}

	private void fillInStars() {
		// Testcase onerow = new Testcase(parametermodel.size); 
		for (int row = 0; row < numOfRows; row++) {
			// onerow.initiallize();
			// for (int column = 0; column<parametermodel.size; column++) {
			// 	onerow.set(column, matrix[row][column]);
			// }			
			for (int column = 0; column<parametermodel.size; column++) {
				if (matrix[row][column] < 0) {
					int range = parametermodel.range[column];
					int basevalue = this.rnd.nextInt(range); 
					for (int i = 0; i < range; i++) {
						int value = (basevalue + i) % range; 
						// assert 0 <= value  < range
						// onerow.set(column, value);
						// if (constrainthandler.isPossible(onerow)) {
						//	matrix[row][column] = value;
						//	break;
						// }
						matrix[row][column] = value;
						if (constrainthandler.isPossible(matrix[row]))
							break;
						// revert the change
						matrix[row][column] = -1;
						// onerow.set(column, (byte) -1); // can be omitted if error free
					}
					//assert matrix[row][column] >= 0
					if (matrix[row][column] < 0) {
						System.err.println("fillin star error!");
					}
				}
			}	
		}
	}
	/**
	 * 
	 * @param factorID factor currently working on (>= 0, < # factors)
	 */
	private void initializeBitmap(int factorID) {
		// initialize links to bit maps
		// needs C(#factors - 1, strength - 1) bitmaps
		final int totalColumnCombinations = CombinationGenerator.getTotalNumber(factorID, strength-1);
		bitmap = new boolean[totalColumnCombinations][];
		this.positions = new int[totalColumnCombinations];
		CombinationGenerator cg = new CombinationGenerator(factorID, strength-1); 
		// compute number of tuples and initialize bit maps
		//  if cg.c = [0, 2] and factorID = 3, then D_0 * D_2 * D_3 bits are needed 
		do {
			int numOfTuples = parametermodel.range[factorID];
			for (int i = 0; i < strength -1; i++) {
				numOfTuples *= parametermodel.range[cg.get(i)]; 
			}
			bitmap[cg.getID()] = new boolean[numOfTuples];
		} while (cg.visitNext());
	}

	private void verticalExtention(int factor) {
		CombinationGenerator cg = new CombinationGenerator(factor, strength-1);	
		do {
			// int bases[] = new int[strength -1];
			int bases[] = getBases(cg, strength - 1, factor);
			// debug
			// System.out.println("bases " + Arrays.toString(bases) + ", comb: " + Arrays.toString(cg.c));
			
			for (int i=0; i < bitmap[cg.getID()].length; i++) {
				if (bitmap[cg.getID()][i] == false) {
					// tuple needs bases.length + 1 (= strength) in length
					// bases.length does not contain the current factor
					
					// tuple: the one to be covered
					// before adding the tuple, check its validity
					int[] tuple = getTupleFromID (i, bases);	
					// forbidden ?
					Testcase tmp = new Testcase(parametermodel.size);
					tmp.initiallize();
					for (int j =0; j< strength -1; j++) {
						tmp.set(cg.get(j), tuple[j]);
					}
					tmp.set(factor, tuple[strength-1]);
					boolean isValid = constrainthandler.isPossible(tmp); 				
					if (isValid)
						addTupleNew(tuple, cg, factor); 
					bitmap[cg.getID()][i] = true;
				}
			}
		} while (cg.visitNext());	
		// System.out.println();
	}

	private void addTupleNew(int tuple[], CombinationGenerator cg, int factor) {
		
		// search for a row that contains the tuple 
		boolean isIdentical; 		
		for (int row = 0; row < this.numOfRows; row++) {
			if (tuple[strength-1] != matrix[row][factor])
				continue;
			isIdentical = true; 
			for (int i = tuple.length -2; i >= 0; i--) {
				if (!(tuple[i] == matrix[row][cg.get(i)])) {
					isIdentical = false; break;
				}
			}
			if (isIdentical == true)
				return;
		}
		
		for (int row = 0; row < MaxNumOfTestcases; row++) {
			//		for (int row = numOfRows-1; row < MaxNumOfTestcases;
			//				row = (row == 0)? numOfRows : row-1) {

			iteration:
			{
				if (!(matrix[row][factor] < 0 || tuple[strength-1] == matrix[row][factor]))
					break iteration; 
				for (int i = tuple.length -2; i >= 0; i--)
					if (!(matrix[row][cg.get(i)] < 0 || tuple[i] == matrix[row][cg.get(i)])) 
						break iteration;

				/* validity check required!!!*/
				Testcase tmp = new Testcase(parametermodel.size);
				tmp.initiallize();
				for (int j = 0; j <strength -1; j++) {
					tmp.set(cg.get(j), tuple[j]);
				}
				tmp.set(factor, tuple[strength -1]);
				for (int j = 0; j < factor; j++) {
					if (tmp.get(j) < 0)
						tmp.set(j, matrix[row][j]);
				}
				if (constrainthandler.isPossible(tmp) == false)
					break iteration;
				
				// now a row has been found where the tuple fits in
				matrix[row][factor] = tuple[strength-1];
				assert(tuple[strength -1] < parametermodel.range[factor]);

				for (int i = tuple.length -2; i >= 0; i--) {
					matrix[row][cg.get(i)] = tuple[i];
					assert(tuple[i] < parametermodel.range[cg.get(i)]);
				}
				
				if (row >= numOfRows) {
					assert(row == numOfRows);
					numOfRows = row +1; 
				}
				return;
			}
		} // for
		// assert(row >= MaxNumOfTestcases);
		Error.printError(Main.language == Main.Language.JP ? "上限"
				+ MaxNumOfTestcases + "を超えるテストケースが必要です"
				: "The number of test cases exceeds the upper bound "
				+ MaxNumOfTestcases);
	}
	/**
	 * 
	 * @param factor factor currently working on (>= 0, < #factors) 
	 */
	private void horizontalExtentionNew(int factor) {
	
		final int levelsOfCurrentFactor = parametermodel.range[factor];

		// number of occurrences for each value
		int occurrence[] = new int[levelsOfCurrentFactor];
		
		// record positions in bitmap
		// final int maxLengthOfpositions = 50000;
		// int positions[] = new int[maxLengthOfpositions];
		
		for (int row = 0; row < numOfRows; row++) {
			// check validity when value v is appended
			boolean validity[] = new boolean[levelsOfCurrentFactor];			
			for (int v = 0; v < levelsOfCurrentFactor; v++) {
				matrix[row][factor] = v;
				validity[v] = constrainthandler.isPossible(matrix[row]); 
			}
			matrix[row][factor] = -1;
			
			// count the number of newly covered interactions
			int count[] = new int[levelsOfCurrentFactor];			
			CombinationGenerator cg = new CombinationGenerator(factor, strength-1); 
		
			do {  
				// pre processing for computing unique id for interaction
				int id = 0;
				int multiplier = levelsOfCurrentFactor;
				boolean isDontcareContained = false;

				// compute id
				// id: the baseline position of a total of 
				// levelsOfCurrentFactor tuples in the bitmap
				for (int i = strength - 2; i >= 0; i--) {
					int column = cg.get(i);
					if (matrix[row][column] < 0) {
						isDontcareContained = true; break;
					}
					id += matrix[row][column]*multiplier; 
					multiplier *= parametermodel.range[column];
				}	
				
				// if the tuple is not completed due to a don't care, 
				// record it in positions array as -1
				// otherwise record the (baseline) position of the tuples
				// in the bitmap
				// cg.getID(): combination of factors |-> integer
				int cg_getID = cg.getID();			
				if (isDontcareContained) {
					positions[cg_getID] = -1; 
					continue;
				}
				else {
					positions[cg_getID] = id;
				}
				
				for (int v = 0; v < levelsOfCurrentFactor; v++) {
					/* 
					 * check if a new tuple is covered or not for each level for the factor
					 */
					if (validity[v] == false) {
						count[v] = -1;
						continue;
					}
					if(bitmap[cg_getID][id + v] == false) {
						count[v]++;
					}
				}
			} while (cg.visitNext());

			int maxv = -1; 
			int maxcount = -1;
			int min_occurrence = 0;

			//int offset = row < levelsOfCurrentFactor ?
			//		1 : factor; 
			for (int v = 0; v < levelsOfCurrentFactor; v++) {
				//v = (offset + i) % (levelsOfCurrentFactor);
				if (((count[v] > maxcount) && (validity[v] == true)) ||
						((count[v] == maxcount)&& (validity[v] == true) && (occurrence[v]<min_occurrence))){
					maxcount = count[v];
					maxv = v;
					min_occurrence = occurrence[v];
				}
			}
			// no value can be assigned
			// or no new tuples can cover
			if (maxcount <= 0)
				continue;

			// append maxv
			// System.out.println("row " + row + " " + Arrays.toString(matrix[row]));
			matrix[row][factor] = maxv; assert(maxv < levelsOfCurrentFactor);
						
			// System.out.println("sel: " + (maxv+1) + " count" + Arrays.toString(count) + " occ" + Arrays.toString(occurrence));
			occurrence[maxv]++;
			// System.out.println(Arrays.toString(matrix[row]) + "\n ---");

			// update bitmap
			for (int i=0; i < positions.length; i++) {
				int id = positions[i];
				if (id >= 0) {
					bitmap[i][id + maxv] = true;
				}
			}
		}
	}

	protected void buildFirstColumnsDebug() throws OutOfMaxNumOfTestcasesException {
		int numOfCombinations = 1;
		int row = 0;
		for (int i = 0; i < strength; i++) {
			numOfCombinations *= parametermodel.range[i];
		}
		boolean isTupleValid = false;
		
		int[] tuple = new int[strength];
		// first row is 0000...0
		for (int i = 0; i < strength; i++) {
			matrix[row][i] = tuple[i] = 0;
		}
		if (constrainthandler.isPossible(matrix[row])) {
			row++;
		}

		// matrixにtupleを同時に書き込む
		for (int i = 1; i < numOfCombinations; i++) {
			for (int j = strength - 1; j >= 0; j--) {
				if (tuple[j] + 1 >= parametermodel.range[j]) { // けたあげ
					matrix[row][j] = (tuple[j] = 0);
				}
				else {
					matrix[row][j] = (tuple[j] += 1);
					for (int k = j-1; k >=0; k--) {
						matrix[row][k] = tuple[k];
					}
					break;
				}
			}
			if ((isTupleValid = constrainthandler.isPossible(matrix[row]))==true) {
				if (row + 1 > MaxNumOfTestcases) {
					Error.printError(Main.language == Main.Language.JP ? "特定因子の全網羅に上限"
							+ MaxNumOfTestcases + "を超えるテストケースが必要です"
							: "The number of test cases exceeds the upper bound "
							+ MaxNumOfTestcases);
				}
				else { 
					row++;
				}
			}
		}
		if (isTupleValid == false) { // revert the last row
			for (int i=0; i<strength; i++) {
				matrix[row][i] = -1;
			}
		}
		numOfRows = row; 
	}

	protected void buildFirstColumns() throws OutOfMaxNumOfTestcasesException {
		int numOfCombinations = 1;
		int row = 0;
		for (int i = 0; i < strength; i++) {
			numOfCombinations *= parametermodel.range[i];
		}

		Testcase tmptest = new Testcase(parametermodel.size);
		tmptest.initiallize();
		for (int i = 0; i < strength; i++) {
			tmptest.set(i, 0);
		}
		if (constrainthandler.isPossible(tmptest)) {
			for (int j = 0; j < strength; j++)
				matrix[row][j] = tmptest.get(j);	
			row++;
		}

		for (int i = 1; i < numOfCombinations; i++) {
			tmptest = tmptest.makeClone();
			for (int j = strength - 1; j >= 0; j--) {
				if (tmptest.get(j) + 1 >= parametermodel.range[j]) // けたあげ
					tmptest.set(j, 0);
				else {
					tmptest.set(j, tmptest.get(j) + 1);
					break;
				}
			}
			if (constrainthandler.isPossible(tmptest)) {
				if (row + 1 > MaxNumOfTestcases)
					Error.printError(Main.language == Main.Language.JP ? "特定因子の全網羅に上限"
							+ MaxNumOfTestcases + "を超えるテストケースが必要です"
							: "The number of test cases exceeds the upper bound "
							+ MaxNumOfTestcases);
				for (int j = 0; j < strength; j++) {
					matrix[row][j] = tmptest.get(j); assert(tmptest.get(j) < parametermodel.range[j]);
				}
				row++;				
			}
		}
		numOfRows = row; 
	}
	/*
	 * returns reference to bases for c0 c1 c_size-1 
	 * c[] combination
	 * size size of combination excluding the current factor
	 * factor factor being currently added
	 */
	private int[] getBases(CombinationGenerator cg, int size, int factor) {
		int base = parametermodel.range[factor];
		int[] bases = new int[size];
		for (int i = size - 1; i >= 0; i--) {
			bases[i] = base;
			base *= parametermodel.range[cg.get(i)];
		}
		return bases;
	}

	// tuple needs bases.length + 1 (= strength) in length
	// bases.length does not contain the current factor
	private int[] getTupleFromID (int id, int bases[]) {
		int[] tuple = new int[bases.length + 1];
		for (int i = 0; i < bases.length; i++) {
			tuple[i] = id / bases[i];
			id %= bases[i];
		}
		tuple[bases.length] = id;
		return tuple;
	}

}

