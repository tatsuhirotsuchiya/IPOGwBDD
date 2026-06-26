package jp.ac.osaka_u.ist.ipogBDD;

import java.util.*;

public class Parameter implements Comparable<Parameter> {
	// name of factor
	String name;
	// names of levels
	List<String> value_name = new LinkedList<String>();
	// id for sort-resort
	int id1;
	// id for sort-resort
	int id2;
	
	Parameter(String name) {
		this.name = name;
	}

	void addValueName(String name) {
		value_name.add(name);
	}

	// Check the number of levels (values)
	void check() {
		if (value_name.size() <= 0 || value_name.size() > Main.MAX_LEVEL) {
			Error.printError(Main.language == Main.Language.JP ? "水準数に誤りがあります"
					: "Invalid number of values");
		}

		/* 水準名の重複を禁止-> comment out */
		/*
		 * for (int i = 0; i < value_name.size() - 1; i++) { for (int j = i+1; j
		 * < value_name.size(); j++) { if
		 * (value_name.get(i).equals(value_name.get(j)))
		 * Error.printError(Main.language == Main.Language.JP ? "水準名が重複しています" :
		 * "Overlap of parameter value name"); } }
		 */
	}

	/*
	 * int getID(String str) throws NoValueNameException { for (int i = 0; i <
	 * value_name.size(); i++) { if (value_name.get(i).equals(str)) return i; }
	 * throw new NoValueNameException(); }
	 */

	// get ID of level (=str)
	List<Integer> getID(String str) throws NoValueNameException {
		List<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < value_name.size(); i++) {
			if (value_name.get(i).equals(str))
				ids.add(i);
		}
		if (ids.size() == 0)
			throw new NoValueNameException();
		else
			return ids;
	}

	// numberと算術的に同じ水準のidをとりだす→つかってない
	List<Integer> getID(double number) {
		List<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < value_name.size(); i++) {
			double level;
			try {
				level = Double.parseDouble(value_name.get(i));
				if (level == number)
					ids.add(i);
			} catch (NumberFormatException e) {}
		}
		return ids;
	}
	
	// numberと算術的に関係のある水準のidをとりだす
	// level ～ number
	// get ID of level (=number)
	List<Integer> getID(double number, RelationOverDoublePair com) {
		List<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < value_name.size(); i++) {
			double level;
			try {
				level = Double.parseDouble(value_name.get(i));
				if (com.hasRelation(level, number))
					ids.add(i);
			} catch (NumberFormatException e) {}
		}
		return ids;
	}
	
	// for sorting parameters
	// seems not stable ???
	public int compareTo(Parameter previous) {
		if (this.value_name.size() > previous.value_name.size()) 
			return -1;
		else if (this.value_name.size() == previous.value_name.size()) 
			return 0;
		else 
			return 1;
//		return Integer.compare(this.value_name.size(), other.value_name.size());
	}
}

class PList extends LinkedList<Parameter> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	boolean checkNameDuplication() {
		for (int i = 0; i < this.size() - 1; i++)
			for (int j = i + 1; j < this.size(); j++) {
				if (this.get(i).name.equals(this.get(j).name)) {
					return true;
				}
			}
		return false;
	}

	int getID(String str) throws NoParameterNameException {
		for (int i = 0; i < this.size(); i++) {
			if (this.get(i).name.equals(str))
				return i;
		}
		throw new NoParameterNameException();
	}
	
	// useless?
	int getRestrictedID(String str, TreeSet<Integer> RestrictedParameters) 
		throws NoParameterNameException {
		try {
			int parameter = this.getID(str);
			int num = 0;
			for (Integer i: RestrictedParameters) {
				if (i == parameter)
					return num;
				num++;
			}
		} catch (NoParameterNameException e) {
			throw e;
		}
		// if the parameter is not a relevant one
		throw new NoParameterNameException();
	}

	// assign each parameter an integer from 0... 
	// execute before sort for the numbers to be used to recover original parameter sequence
	void assignID1() {
		for (int i = 0; i < this.size(); i++) {
			this.get(i).id1 = i;
		}
	}
	void assignID2() {
		for (int i = 0; i < this.size(); i++) {
			this.get(i).id2 = i;
		}
	}
}

class NoParameterNameException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6603037538755301907L;
}

class NoValueNameException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -92079148371461108L;
}
