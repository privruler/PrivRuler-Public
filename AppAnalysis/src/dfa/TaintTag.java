package dfa;

import soot.tagkit.*;
import java.io.*;

public class TaintTag implements Tag, Serializable {
	private static final long serialVersionUID = -7646013529298629555L;

	int value;
	String secondaryName;

	public TaintTag(int value) {
		this.value = value;
		this.secondaryName = "NA";
	}

	public TaintTag(int value, String secondaryName) {
		this.value = value;
		this.secondaryName = secondaryName;
	}

	public String getName() {
		return "mySoot.TaintTag";
	}

	public String getSecondaryName() {
		return this.secondaryName;
	}

	public int getLabel() {
		return this.value;
	}

	public byte[] getValue() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(value);
			dos.flush();
		} catch (IOException e) {
			System.err.println(e);
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}
}
