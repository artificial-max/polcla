package polcla;

import java.util.Arrays;

/**
 * ShifterUnit object contains the informations of one shifter expression
 * 
 * Example: Verbesserung g [gmod, objp-*] nomen name = Verbesserung shifter_type
 * = g shifter_scope = [gmod, objp-*] shifter_pos = nomen mwe = false
 * collocations = []
 *
 */
public class ShifterUnit {
	String name;
	String shifter_type;
	String[] shifter_scope;
	String shifter_pos;
	Boolean mwe;
	String[] collocations;

	/**
	 * 
	 * @param name
	 *          Name of the Shifter.
	 * @param shifter_type
	 *          Either SHIFTER_TYPE_GENERAL, SHIFTER_TYPE_ON_POSITIVE or
	 *          SHIFTER_TYPE_ON_NEGATIVE (g, p, n).
	 * @param shifter_scope
	 *          The dependency relations used to find the shifter target.
	 * @param shifter_pos
	 *          POS-Tag.
	 * @param mwe
	 *          True if the shifter is a multi-word-expression.
	 */
	public ShifterUnit(String name, String shifter_type, String[] shifter_scope, String shifter_pos, Boolean mwe) {
		if (mwe) {
			String[] parts = name.split("_");
			this.name = parts[parts.length - 1];
			this.collocations = Arrays.copyOfRange(parts, 0, parts.length - 1);
		} else {
			this.name = name;
			this.collocations = new String[0];
		}
		this.shifter_type = shifter_type;
		this.shifter_scope = shifter_scope;
		this.shifter_pos = shifter_pos;
		this.mwe = mwe;
	}

	/**
	 * @param word
	 * @return true if the {@link #name} of the ShifterUnit equals the lemma of a
	 *         given Word Object
	 */
	public boolean equals(WordObj word) {
		if (this.name.equals(word.getLemma())) {
			return true;
		}
		return false;
	}

	/**
	 * @return {@link #shifter_type} of the ShifterUnit
	 */
	public String getType() {
		return this.shifter_type;
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
		printer.append(" ").append(this.shifter_type);
		printer.append(" ").append(Arrays.toString(this.shifter_scope));
		printer.append(" ").append(this.shifter_pos);
		return printer.toString();
	}
}
