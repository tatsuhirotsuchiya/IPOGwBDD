/**
* Copyright (c) 2018-2019, Tatsuhiro Tsuchiya
* This software is distributed under the zlib license.
*/

package jp.ac.osaka_u.ist.ipogBDD;

import java.util.List;

public class Main {
	static int randomSeed = -1;
	static String modelFile;
	static int numOfIterations = 1;
	static String seedFile;
	static String outputFile;
	static int strength = 2; // default strength

	static final int MAX_LEVEL = 63;

	static final int MAX_ITERATIONS = 100000;
	static final int MAX_STRENGTH = 5;	
	static final int Max_RandomSeed = 65535;

	static boolean debugMode = false;

	enum Language {JP, EN};	
	static Language language = Language.JP;

	enum Handler {NO, UP, DOWN, CONJ, SAT};
	static Handler handler = Handler.UP;
//	static Handler handler = Handler.CONJ;
	
	enum Randstar {ON, OFF, EMPTY};
	static Randstar randstar = Randstar.OFF; 
	static int verbose = 0;
	
	// Start the whole process
	public static void main(String[] args) {

		long start = System.currentTimeMillis();
		try {
			// Process command line options
			String errorMessage = processCommandArgument(args);

			// Specify error output
			Error.setOutputFile(outputFile);

			// Print error message
			if (errorMessage != null)
				Error.printError(errorMessage);	

			// Read input model
			InputFileData inputfiledata = new InputFileData(modelFile);
			
			if (verbose > 0) {
				System.out.println("Parameters :\t" + inputfiledata.parameterList.size());
				System.out.println("Strength:\t" + strength);
			}
			// Build BDD for constraint handling
			ConstraintHandler conhndl = null;
			if (handler == Handler.UP || handler == Handler.DOWN) {
				long startbdd = System.currentTimeMillis();
				conhndl = new NewBDDConstraintHandler(
						inputfiledata.parameterList, inputfiledata.constraintList, 
						inputfiledata.constrainedParameters, inputfiledata.ast);
				// DEBUG: Show the BDD
				// if (debugMode == true)
				//	conhndl.printConstraintBDD(); 
				// Record time
				long endbdd = System.currentTimeMillis();
				
				if (verbose > 0) {
					System.out.println("BDD tm:\t" + (endbdd - startbdd) + "ms");
				}
			} 
			// Ignore constraint
			else if (handler == Handler.NO) {
				conhndl = new NoConstraintHandler();
			} 
			// Build BDD of direct constraint representation
			//   With this BDD, an AND operation needs to take place whenever checking SAT
			else if (handler == Handler.CONJ) {
				long startbdd = System.currentTimeMillis();
				conhndl = new ConjConstraintHandler(
						inputfiledata.parameterList, inputfiledata.constraintList, 
						inputfiledata.constrainedParameters, inputfiledata.ast);
				long endbdd = System.currentTimeMillis();

				if (verbose > 0) {
					System.out.println("BDD tm:\t" + (endbdd - startbdd) + "ms");
				}
			}
			else if (handler == Handler.SAT) {
				conhndl = new SATConstraintHandler(
						inputfiledata.parameterList, inputfiledata.constraintList, 
						inputfiledata.constrainedParameters);
			}
			else {
				Error.printError("something bad happened");
			}

			// Read seed file for predefined tests
			// currently not supported!!!
			//  List<Testcase> seed = SeedReader.readSeed(seedFile, inputfiledata);
			List<Testcase> seed = null;
			
			// Generate test cases
			List<Testcase> testSet = null;
			// Exhaustive test suite
			if (strength == -1) {
				try {
					testSet = GeneratorAll.generate(new ParameterModel(
							inputfiledata.parameterList), conhndl);
				} catch (OutOfMaxNumOfTestcasesException e) {
					Error.printError(Main.language == Main.Language.JP ? "テストケース数が上限"
							+ Generator.MaxNumOfTestcases + "を超えました"
							: "The number of test cases exceeded the upper bound "
									+ Generator.MaxNumOfTestcases);
				}

				new Outputer(outputFile).outputResult(testSet, inputfiledata,
						modelFile, outputFile);
			} 
			// Usual combinatorial test suite
			else { // strength >= 2
				Generator generator = GeneratorFactory.newGenerator(
						new ParameterModel(inputfiledata.parameterList),
						inputfiledata.groupList, conhndl, seed, randomSeed,
						strength);
				try {
					testSet = generator.generate();
				} catch (OutOfMaxNumOfTestcasesException e) {
					testSet = null;
				}
			
				if (debugMode) {
					System.err.println("random seed: " + randomSeed);
				}
				
				// 繰り返す場合
				/*
				for (int i = 2; i < numOfIterations; i++) {
					int nextRandomSeed = (int) Math.floor(Math.random()
							* (Max_RandomSeed + 1));
					generator = GeneratorFactory.newGenerator(
							new ParameterModel(inputfiledata.parameterList),
							inputfiledata.groupList, conhndl, seed,
							nextRandomSeed, strength);

					if (debugMode)
						System.err.println("random seed: " + nextRandomSeed);

					List<Testcase> nextTestSet = null;
					try {
						nextTestSet = generator.generate();
					} catch (OutOfMaxNumOfTestcasesException e) {
						nextTestSet = null;
					}

					if (testSet != null && nextTestSet != null) {
						if (nextTestSet.size() < testSet.size()) {
							testSet = nextTestSet;
							randomSeed = nextRandomSeed;
						}
					} else if (testSet == null && nextTestSet != null) {
						testSet = nextTestSet;
						randomSeed = nextRandomSeed;
					}
				}
				*/
				if (testSet == null)
					Error.printError(Main.language == Main.Language.JP ? "テストケース数が上限"
							+ Generator.MaxNumOfTestcases + "を超えました"
							: "The number of test cases exceeded the upper bound "
									+ Generator.MaxNumOfTestcases);

				new Outputer(outputFile).outputResult(testSet, inputfiledata,
						randomSeed, modelFile, seedFile, outputFile, strength,
						numOfIterations);
				System.out.println("# Number of Tests : " + testSet.size());
			}

			if (debugMode) {
				System.err.println("test set size: " + testSet.size());
			}
		} catch (OutOfMemoryError e) {
			Error.printError(Main.language == Main.Language.JP ? "メモリ不足です．"
					: "Out of memory");
		} catch (Exception e) {
			Error.printError(Main.language == Main.Language.JP ? "プログラムが異常終了しました．"
					: "Abnormal termination");
		}
		
		long end = System.currentTimeMillis();
		System.out.println("# Time (seconds) : " + (((double)(end - start))/1000));
		
	//	System.err.println("Time:\t" + (end - start) + "ms");
	}
	
	/**
	 * This method is used to process command arguments.
	 * @param array of arguments
	 * @return error message or null if no error occurs
	 */
	private static String processCommandArgument(String[] args) {
		if (args.length == 0) {
			Error.printError("usage: java -jar Program.jar [-i input] [-o output] [-policy] ...");
		}

		// Show license
		if (args.length == 1 && args[0].equals("-policy")) {
			System.out
					.println("This software is distributed under the zlib license.\n"
							+ "The software contains Java classes from JDD, a Java BDD library "
							+ "developed by Arash Vahidi.\n"
							+ "JDD is free software distributed under the zlib license.\n"
							+ "\n"
							+ "Copyright (c) 2018, Tatsuhiro Tsuchiya\n"
							+ "This software is provided 'as-is', without any express or implied \n"
							+ "warranty. In no event will the authors be held liable for any damages \n"
							+ "arising from the use of this software. \n"
							+ "\n"
							+ "Permission is granted to anyone to use this software for any purpose, \n"
							+ "including commercial applications, and to alter it and redistribute it \n"
							+ "freely, subject to the following restrictions: \n"
							+ " \n"
							+ "   1. The origin of this software must not be misrepresented; you must not \n"
							+ "   claim that you wrote the original software. If you use this software \n"
							+ "   in a product, an acknowledgment in the product documentation would be \n"
							+ "   appreciated but is not required. \n"
							+ "   \n"
							+ "   2. Altered source versions must be plainly marked as such, and must not be \n"
							+ "   misrepresented as being the original software. \n"
							+ "   \n"
							+ "   3. This notice may not be removed or altered from any source \n"
							+ "   distribution. \n");
			System.exit(0);
		}

		// This is necessary to postpone error message output till the error output file is specified
		String errorMessage = null;

		// Every option is followed by a value for it
		//   The last argument is ignored if the number of arguments is odd
		for (int i = 0; i + 1 < args.length; i += 2) {
			String option = args[i];
			String str = args[i + 1];
			if (option.equals("-i")) {
				modelFile = str;
			} else if (option.equals("-o")) {
				outputFile = str;
			} else if (option.equals("-random")) {
				try {
					randomSeed = Integer.parseInt(str);
				} catch (NumberFormatException e) {
					errorMessage = Main.language == Main.Language.JP ? "ランダムシードに無効な値が指定されています．"
							: "Invalid random seed";
					continue;
				}
				randomSeed = Math.abs(randomSeed) % (Max_RandomSeed + 1);
			} else if (option.equals("-c")) {
				if (str.equals("all")) {
					// Exhaustive testing is represented as strength = -1
					strength = -1;
				} else {
					try {
						strength = Integer.parseInt(str);
					} catch (NumberFormatException e) {
						errorMessage = Main.language == Main.Language.JP ? "網羅度に無効な値が指定されています．"
								: "Invalid strength";
						continue;
					}
					if (strength < 2 || MAX_STRENGTH < strength) {
						errorMessage = Main.language == Main.Language.JP ? "網羅度に無効な値が指定されています．"
								: "Invalid strength";
						continue;
					}
				}
			}
			// times of repetition (not used when IPOG is used)
			else if (option.equals("-repeat")) {
				try {
					numOfIterations = Integer.parseInt(str);
				} catch (NumberFormatException e) {
					errorMessage = Main.language == Main.Language.JP ? "くり返し数に無効な値が指定されています．"
							: "Invalid number of repetition times";
					continue;
				}
				if (numOfIterations <= 0 || numOfIterations > MAX_ITERATIONS) {
					errorMessage = Main.language == Main.Language.JP ? "くり返し数に無効な値が指定されています．"
							: "Invalid number of repetition times";
				}
			} else if (option.equals("-s")) {
				seedFile = str;
			} else if (option.equals("-debug")) {
				debugMode = true;
				// the command argument following it is simply ignored
			} else if (option.equals("-lang")) {
				if (str.matches("JP|jp")) {
					Main.language = Main.Language.JP;
				} else if (str.matches("EN|en")) {
					Main.language = Main.Language.EN;
				} else {
					errorMessage = "Invalid Language";
				}
			} else if (option.equals("-chandler")) {
				if (str.equalsIgnoreCase("no")) handler = Handler.NO;
				else if (str.equalsIgnoreCase("down")) handler = Handler.DOWN;
				else if (str.equalsIgnoreCase("up")) handler = Handler.UP; // default
				else if (str.equalsIgnoreCase("conj")) handler = Handler.CONJ;
				else if (str.equalsIgnoreCase("sat")) handler = Handler.SAT;
				else {
					errorMessage = Main.language == Main.Language.JP ? "無効な制約処理が指定されています．"
							: "Invalid Constraint handler";
				}
			} else if (option.equals("-randstar")) {
				if (str.equalsIgnoreCase("off")) randstar = Randstar.OFF;
				else if (str.equalsIgnoreCase("on")) randstar = Randstar.ON;
				else if (str.equals("empty")) randstar = Randstar.EMPTY;
				else {
					errorMessage = Main.language == Main.Language.JP ? "無効なドントケア処理が指定されています．"
							: "Invalid don't care handling option";
				}
			} else if (option.equals("-verbose")) {
				try {
					verbose = Integer.parseInt(str);
				} catch (NumberFormatException e) {
					errorMessage = "Invalid verbose level";					
				}
			}
			else {
				errorMessage = Main.language == Main.Language.JP ? "無効なオプションが指定されています．"
						: "Invalid option";
			}
		}

		if (randomSeed == -1) {
			randomSeed = (int) Math.floor(Math.random() * (Max_RandomSeed + 1));
		}

		return errorMessage;
	}
}
