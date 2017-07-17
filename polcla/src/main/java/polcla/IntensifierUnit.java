package polcla;

import java.util.Arrays;

/**
 * IntensifierUnit object contains the informations of one intensifier
 *
 * Example: tatsächlich g [subj,attr-rev] adj
 *
 */
public class IntensifierUnit {
	String name;
	String intensifier_type;
	String[] intensifier_scope;
	String intensifier_pos;
	Boolean mwe;
	String[] collocations;

	/**
	 * 
	 * @param name
	 *          Name of the Intensifier.
	 * @param intensifier_type
	 *          Either INTENSIFIER_TYPE_GENERAL, INTENSIFIER_TYPE_ON_POSITIVE or
	 *          INTENSIFIER_TYPE_ON_NEGATIVE (g, p, n).
	 * @param intensifier_scope
	 *          The dependency relations used to find the intensifier target.
	 * @param intensifier_pos
	 *          POS-Tag.
	 * @param mwe
	 *          True if the intensifier is a multi-word-expression.
	 */
	public IntensifierUnit(String name, String intensifier_type, String[] intensifier_scope, String intensifier_pos, Boolean mwe) {
		if (mwe) {
			String[] parts = name.split("_");
			this.name = parts[parts.length - 1];
			this.collocations = Arrays.copyOfRange(parts, 0, parts.length - 1);
		} else {
			this.name = name;
			this.collocations = new String[0];
		}
		this.intensifier_type = intensifier_type;
		this.intensifier_scope = intensifier_scope;
		this.intensifier_pos = intensifier_pos;
		this.mwe = mwe;
	}

	/**
	 * @param word
	 * @return true if the {@link #name} of the IntensifierUnit equals the lemma of a
	 *         given Word Object
	 */
	public boolean equals(WordObj word) {
		if (this.name.equals(word.getLemma())) {
			return true;
		}
		return false;
	}

	/**
	 * @return {@link #intensifier_type} of the IntensifierUnit
	 */
	public String getTypy() {
		return this.intensifier_type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */

        @Override
	public String toString() {
		StringBuilder printer = new StringBuilder();
		printer.append(this.name);
		for (String col : this.collocations) {
			printer.append("_").append(col);
		}
		printer.append(" ").append(this.intensifier_type);
		printer.append(" ").append(Arrays.toString(this.intensifier_scope));
		printer.append(" ").append(this.intensifier_pos);
		return printer.toString();
	}
}
