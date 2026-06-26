package jp.ac.osaka_u.ist.ipogBDD;

public class ParameterModel {
	// 因子の数
	// number of factors
	final int size;
	// 各因子のレベル数
	// number of levels for each factor
	final byte[] range;

	ParameterModel(PList plist) {
		size = plist.size();
		this.range = new byte[size];
		for (int i = 0; i < size; i++) {
			range[i] = (byte) plist.get(i).value_name.size();
		}
	}

}
