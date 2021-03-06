package de.prob.webconsole;

public class ResultObject {

	public final String output;
	public final boolean continued;
	public final String[] imports;

	public ResultObject(String output, boolean continued, String[] imports) {
		this.output = output;
		this.continued = continued;
		this.imports = imports;
	}

	public ResultObject(String output) {
		this(output, false, new String[0]);
	}
}
