package polcla;

import java.util.Arrays;

/**
 * SentimentUnit object contains the informations of one sentiment expression
 * 
 * Example: froh_gestimmt POS=1 adj
 * name = froh
 * category = POS
 * value = 1
 * pos = adj
 * mwe = true
 * collocations = [gestimmt]
 *
 */
public class SentimentUnit {
	String name;
	String category;
	String value;
	String pos;
	Boolean mwe;
	String[] collocations;

	/**
	 * 
	 * @param name
	 * @param category
	 * @param value
	 * @param pos
	 * @param mwe
	 */
	public SentimentUnit(String name, String category, String value, String pos, Boolean mwe) {
		if (mwe) {
			String[] parts = name.split("_");
			this.name = parts[parts.length - 1];
			this.collocations = Arrays.copyOfRange(parts, 0, parts.length - 1);
		} else {
			this.name = name;
			this.collocations = new String[0];
		}
		this.category = category;
		this.pos = pos;
		this.mwe = mwe;
		this.value = value;
	}

	/**
	 * Constructs a new SentimentUnit
	 * 
	 * @param name
	 *          {@link #name} is set
	 */
	public SentimentUnit(String name) {
		this.name = name;

	}

	public SentimentUnit(String name, Boolean mwe) {
		this.mwe = mwe;
		if (mwe) {
			String[] parts = name.split("_");
			this.name = parts[parts.length - 1];
			this.collocations = Arrays.copyOfRange(parts, 0, parts.length - 1);
		}
	}

	/**
	 * @param word
	 * @return true if the {@link #name} of the SentimentUnit equals the lemma of
	 *         a given Word Object
	 */
	public boolean equals(WordObj word) {
		if (this.name.equals(word.getLemma())) {
			return true;
		}
		return false;
	}

	/**
	 * @return {@link #category} of the SentimentUnit
	 */
	public String getType() {
		return this.category;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */

	public String toString() {
		StringBuffer printer = new StringBuffer();
		printer.append(this.name);
		for (String col : this.collocations) {
			printer.append("_" + col);
		}
		printer.append(this.category);
		printer.append("=");
		printer.append(this.value);
		printer.append(" ");
		printer.append(this.pos);
		return printer.toString();
	}
}
