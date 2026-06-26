package jp.ac.osaka_u.ist.ipogBDD;

import java.util.ArrayList;
import java.util.List;

import jp.ac.osaka_u.ist.ipogBDD.Main.Randstar;

class IPOGnoconst extends Generator {

	protected int matrix[][]; 
	protected int strength;
	protected boolean bitmap[][];
	protected int numOfRows; 

	IPOGnoconst(ParameterModel parametermodel, GList groupList,
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
		List<Testcase> res = new ArrayList<Testcase>();

		for (int factorID = strength; factorID < parametermodel.size; factorID++) {

			initializeBitmap(factorID); 
			horizontalExtention(factorID);
			
			verticalExtention(factorID);
			// verticalExtention(factorID);
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
		Testcase onerow = new Testcase(parametermodel.size); 
		for (int row = 0; row < numOfRows; row++) {
			onerow.initiallize();
			for (int column = 0; column<parametermodel.size; column++) {
				onerow.set(column, matrix[row][column]);
			}			
			for (int column = 0; column<parametermodel.size; column++) {
				if (matrix[row][column] < 0) {
					int range = parametermodel.range[column];
					byte basevalue = (byte) this.rnd.nextInt(range); 
					for (byte i = 0; i < range; i++) {
						byte value = (byte) ((basevalue + i) % range); 
						// assert 0 <= value  < range
						onerow.set(column, value);
						if (constrainthandler.isPossible(onerow)) {
							matrix[row][column] = value;
							break;
						}
						onerow.set(column, (byte) -1); // can be omitted if error free
					}
					//assert matrix[row][column] >= 0
					if (matrix[row][column] < 0) {
						System.err.println("error");
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
		bitmap = new boolean[CombinationGenerator.getTotalNumber(factorID, strength-1)][];
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
			int bases[] = new int[strength -1];
			bases = getBases(cg, strength - 1, factor);
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
					addTuple(tuple, cg, factor); 
					bitmap[cg.getID()][i] = true;
				}
			}
		} while (cg.visitNext());	
		// System.out.println();
	}

	private void addTuple(int tuple[], CombinationGenerator cg, int factor) {

		for (int row = 0; row <= MaxNumOfTestcases; row++) {
			//		for (int row = numOfRows-1; row < MaxNumOfTestcases;
			//				row = (row == 0)? numOfRows : row-1) {

			if (row >= MaxNumOfTestcases) {
				Error.printError(Main.language == Main.Language.JP ? "上限"
						+ MaxNumOfTestcases + "を超えるテストケースが必要です"
						: "The number of test cases exceeds the upper bound "
						+ MaxNumOfTestcases);
			}
			iteration:
			{
				// System.out.println(row);
				if (!(matrix[row][factor] < 0 || (byte)(tuple[strength-1]) == matrix[row][factor]))
					break iteration;
				for (int i = tuple.length -2; i >= 0; i--)
					if (!(matrix[row][cg.get(i)] < 0 || (byte)(tuple[i]) == matrix[row][cg.get(i)])) 
						break iteration;
				
				matrix[row][factor] =  tuple[strength-1];
				assert(tuple[strength -1] < parametermodel.range[factor]);

				for (int i = tuple.length -2; i >= 0; i--) {
					matrix[row][cg.get(i)] =  tuple[i];
					assert(tuple[i] < parametermodel.range[cg.get(i)]);
				}
				
				if (row >= numOfRows) {
					assert(row == numOfRows);
					numOfRows = row +1; 
				}
				return;
			}
		}
		Error.printError("Exceeds the limit");
	}
	

	/**
	 * 
	 * @param factor factor currently working on (>= 0, < #factors) 
	 */
	private void horizontalExtention(int factor) {

		// number of occurrences for each value
		int occurrence[] = new int[parametermodel.range[factor]];

		for (int row = 0; row < numOfRows; row++) {
			// validity[v] = true for all v with no constraints
			
			// count the number of newly covered interactions
			int count[] = new int[parametermodel.range[factor]];			
			CombinationGenerator cg = new CombinationGenerator(factor, strength-1); 
			boolean isNotLastComb = true;

			while (isNotLastComb) {  
				// pre processing for computing unique id for interaction
				int id = 0;
				int multiplier = parametermodel.range[factor];
				boolean isDontcareContained = false;

				for (int i = strength - 2; i >= 0; i--) {
					if (matrix[row][cg.get(i)] < 0) {
						isDontcareContained = true; break;
					}
					id += matrix[row][cg.get(i)]*multiplier; 
					multiplier *= parametermodel.range[cg.get(i)];
				}
				if (isDontcareContained) {
					isNotLastComb = cg.visitNext();
					continue;
				}		
				for (byte v = 0; v < parametermodel.range[factor]; v++) {
					/* 
					 * check if a new tuple is covered or not for each level for the factor
					 */
					if(bitmap[cg.getID()][id + v] == false) {
						count[v]++;
					}
				}
				isNotLastComb = cg.visitNext();
			} 	

			/*
			// tie breaking using random numbers
			//　find the value to append
			int max = -1;
			for (int i = 0; i < count.length; i++)
				if (count[i] > max)
					max = count[i];
			// cover no new tuples 
			// if (max == 0)
			// 	continue;
			// decide a value with count >= 0
			int numOfCandVals = 0;
			for (int i = 0; i < count.length; i++) 
				if (count[i] == max)
					numOfCandVals++;
			int indexCandVal = this.rnd.nextInt(numOfCandVals);
			byte maxv = 0;
			for (int i = 0; i < count.length; i++) {
				if (count[i] < max) continue;
				if (indexCandVal == 0) {
					maxv = (byte) i; 
					break;
				}
				indexCandVal--;
			}
			 */

			int maxv = -1; 
			int max = -1;
			int min_occurrence = 0;
			int v = 0;
			//int offset = row < parametermodel.range[factor] ?
			//		1 : factor; 
			for (int i = 0; i < parametermodel.range[factor]; i++) {
				//v = (offset + i) % (parametermodel.range[factor]);
				if (((count[v] > max)) ||
						((count[v] == max)&& (occurrence[v]<min_occurrence))){
					max = count[v];
					maxv =  v;
					min_occurrence = occurrence[v];
				}
				v++;
			}
			// no value can be assigned
			// or no new tuples can cover
			if (max <= 0)
				continue;

			// append maxv
			// System.out.println("row " + row + " " + Arrays.toString(matrix[row]));
			matrix[row][factor] = maxv; assert(maxv < parametermodel.range[factor]);
						
			// System.out.println("sel: " + (maxv+1) + " count" + Arrays.toString(count) + " occ" + Arrays.toString(occurrence));
			occurrence[maxv]++;

			// System.out.println(Arrays.toString(matrix[row]) + "\n ---");

			// update bitmap
			cg = new CombinationGenerator(factor, strength-1); 
			isNotLastComb = true;
			while (isNotLastComb) {
				int id = 0;
				int multiplier = parametermodel.range[factor];
				boolean isDontcareContained = false;
				for (int i=strength-2; i>=0; i--) {
					if (matrix[row][cg.get(i)] < 0) { // dont'care
						isDontcareContained = true;
						break;
					}
					id += matrix[row][cg.get(i)]*multiplier; 
					multiplier *= parametermodel.range[cg.get(i)];
				}
				if (isDontcareContained) {
					isNotLastComb = cg.visitNext();
					continue;
				}
				id += matrix[row][factor]; // should be equal to maxv
				bitmap[cg.getID()][id] = true;
				isNotLastComb = cg.visitNext();
			}
		}
		// System.out.println("factor " + factor + ", row " + numOfRows);
	}


	protected void buildFirstColumnsDebug() throws OutOfMaxNumOfTestcasesException {
		int numOfCombinations = 1;
		int row = 0;
		for (int i = 0; i < strength; i++) {
			numOfCombinations *= parametermodel.range[i];
		}
	
		byte[] tuple = new byte[strength];
		// first row is 0000...0
		for (int i = 0; i < strength; i++) {
			matrix[row][i] = tuple[i] = (byte) 0;
		}
		row++;

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
			if (row + 1 > MaxNumOfTestcases)
				Error.printError(Main.language == Main.Language.JP ? "特定因子の全網羅に上限"
						+ MaxNumOfTestcases + "を超えるテストケースが必要です"
						: "The number of test cases exceeds the upper bound "
						+ MaxNumOfTestcases);
			row++;	
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
