package polcla;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;


/**
 * ShifterLex object contains all informations from a given shifter lexicon
 *
 */
public class ShifterLex {
	private final static Logger log = Logger.getLogger(ShifterLex.class.getName());
	
	// Constants
	public static final String SHIFTER_TYPE_GENERAL = "general";
	public static final String SHIFTER_TYPE_ON_POSITIVE = "on positive";
	public static final String SHIFTER_TYPE_ON_NEGATIVE = "on negative";

	List<ShifterUnit> shifterList = new ArrayList<ShifterUnit>();
	Map<String, ShifterUnit> shifterMap = new HashMap<String, ShifterUnit>();
	boolean flexibleMWEs = false;
	String[] collectSubjectiveExpressions = { "" };

	/**
	 * Constructs a new ShifterLex
	 *
	 * @param flexMWE
	 *          indicates if the multi word expressions in the lexicon should be
	 *          interpreted in a flexible way (more info see
	 *          {@link #mweFlexibility(ShifterUnit)}).
	 */
	public ShifterLex(boolean flexMWE) {
		this.flexibleMWEs = flexMWE;
	}

	// Lists containing words for flexible mwe interpretations.
	// Used in the method mweFlexibility of this class.
	/**
	 * List containing reflexive nominatives for flexible interpretations. If mwe
	 * contains "sich". "mich", "Mich", "dich", "Dich", "ihn", "Ihn", "es", "Es",
	 * "euch", "Euch", "Sie"
	 */
	private List<String> sichAlternatives = Arrays.asList("mich", "Mich", "dich", "Dich", "ihn", "Ihn", "es", "Es",
			"euch", "Euch", "Sie");

	/**
	 * List containing seinenAlternatives for flexible interpretations. If mwe
	 * contains "seinen" or "seinem". "meine", "Meine", "deine", "Deine", "ihre",
	 * "eure", "Ihre", "unsere", "Unsere"
	 */
	private List<String> seinenAlternatives = Arrays.asList("meine", "Meine", "deine", "Deine", "ihre", "eure", "Ihre",
			"unsere", "Unsere");

	/**
	 * List containing words for flexible interpretations. If mwe contains "den".
	 * "einen", "dem"
	 */
	private List<String> denAlternatives = Arrays.asList("einen", "dem");

	/**
	 * List containing words for flexible interpretations. If mwe contains "ein".
	 * "kein"
	 */
	private List<String> einAlternatives = Arrays.asList("kein");

	/**
	 * List containing words for flexible interpretations. If mwe contains
	 * "einen". "keinen"
	 */
	private List<String> einenAlternatives = Arrays.asList("keinen");

	/**
	 * List containing words for flexible interpretations. If mwe contains "sein".
	 * "werden"
	 */
	private List<String> seinAlternatives = Arrays.asList("werden");

	/**
	 * Returns the ShifterUnit corresponding to the given name or null.
	 *
	 * @param name
	 *          The name of the shifter to look for.
	 * @return ShifterUnit for a given name or null if no ShifterUnit with the
	 *         given name exists
	 */
	public ShifterUnit getShifter(String name) {
		for (int i = 0; i < shifterList.size(); i++) {
			ShifterUnit tmp = (ShifterUnit) shifterList.get(i);
			if (tmp.mwe) {
				String mweParts = new String();
				for (String mwePart : tmp.collocations) {
					mweParts += mwePart + "_";
				}
				mweParts += tmp.name;
				if (mweParts.equals(name)) {
					return tmp;
				}
			} else {
				if (tmp.name.equals(name)) {
					return tmp;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the ShifterUnits (more than one if there is more than one lexicon
	 * entry) corresponding to the given name or null. Up to 50 entries possible
	 * for one shifter.
	 *
	 * @param name
	 *          The name of the shifter to look for.
	 * @return ShifterUnit for a given name or null if no ShifterUnit with the
	 *         given name exists
	 */
	public ArrayList<ShifterUnit> getAllShifters(String name) {
		ArrayList<ShifterUnit> shifters = new ArrayList<ShifterUnit>();
		for (int i = 0; i < shifterList.size(); i++) {
			ShifterUnit tmp = (ShifterUnit) shifterList.get(i);
			if (tmp.mwe) {
				String mweParts = new String();
				for (String mwePart : tmp.collocations) {
					mweParts += mwePart + "_";
				}
				mweParts += tmp.name;
				if (mweParts.equals(name)) {
					shifters.add(tmp);
				}
			} else {
				if (tmp.name.equals(name)) {
					shifters.add(tmp);
				}
			}
		}
		if (!shifters.isEmpty()) {
			return shifters;
		} else {
			return null;
		}
	}

	/**
	 * Adds a shifter expression to the internal lexicon, possibly interpreting
	 * the with some flexibility (for MWEs).
	 *
	 * @param shifter
	 *          is added to ShifterLex
	 * @return nothing (void)
	 */
	public void addShifter(ShifterUnit shifter) {
		shifterList.add(shifter);
		addToMap(shifter.name, shifter);

		if (shifter.mwe && this.flexibleMWEs) {
			for (ShifterUnit flex : mweFlexibility(shifter)) {
				shifterList.add(flex);
				addToMap(flex.name, flex);
			}
		}
	}

	/**
	 * Returns an ArrayList of SenimentUnits that are the result of flexible
	 * interpretation of the given MWE ShifterUnit. Flexibility is applied w.r.t.
	 * the following words that are found in the MWE: 1) Reflexive pronouns: sich
	 * --> sich, mich, dich, ihn, ... etc 2) Possessive pronouns: seinen -->
	 * seinen, meinen, deinen, ... etc 3) Ein(en)/Kein(en): ein(en) --> ein(en),
	 * kein(en) 4) Sein/Werden: sein --> sein, werden 5) Other: den --> den,
	 * einen, dem
	 *
	 * @param shifter
	 *          given a MWE ShifterUnit
	 * @return ArrayList of ShifterUnits that are created by flexibility
	 */
	public ArrayList<ShifterUnit> mweFlexibility(ShifterUnit shifter) {
		boolean containsSich = false; // sich > mich, dich, ...
		int sichIndex = -1;
		boolean containsSeinen = false; // seinen > meinen, deinen, ...
		int seinenIndex = -1;
		boolean containsDen = false; // den > einen, dem
		int denIndex = -1;
		boolean containsEin = false; // ein > kein
		int einIndex = -1;
		boolean containsEinen = false; // einen > keinen
		int einenIndex = -1;
		boolean containsSein = false; // sein > werden
		int seinIndex = -1;

		for (int j = 0; j < shifter.collocations.length; j++) {
			if (shifter.collocations[j].equals("sich")) {
				containsSich = true;
				sichIndex = j;
			}
			if (shifter.collocations[j].equals("seinen") || shifter.collocations[j].equals("seinem")) {
				containsSeinen = true;
				seinenIndex = j;
			}
			if (shifter.collocations[j].equals("ein")) {
				containsEin = true;
				einIndex = j;
			}
			if (shifter.collocations[j].equals("einen")) {
				containsEinen = true;
				einenIndex = j;
			}
			if (shifter.collocations[j].equals("den")) {
				containsDen = true;
				denIndex = j;
			}
			if (shifter.collocations[j].equals("sein")) {
				containsSein = true;
				seinIndex = j;
			}
		}

		ArrayList<ShifterUnit> alternativeSUnits = new ArrayList<ShifterUnit>();
		if (containsSich) {
			String[] alternativeCollocations;
			ShifterUnit alternativeShifter;

			for (String reflexive : sichAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[sichIndex] = reflexive;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}
		if (containsSeinen) {
			String[] alternativeCollocations;
			ShifterUnit alternativeShifter;
			for (String reflexive : seinenAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[seinenIndex] = reflexive;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}

		if (containsDen) {
			ShifterUnit alternativeShifter;
			String[] alternativeCollocations;
			for (String denOption : denAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[denIndex] = denOption;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}
		if (containsEin) {
			ShifterUnit alternativeShifter;
			String[] alternativeCollocations;
			for (String einOption : einAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[einIndex] = einOption;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}
		if (containsEinen) {
			ShifterUnit alternativeShifter;
			String[] alternativeCollocations;
			for (String einenOption : einenAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[einenIndex] = einenOption;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}
		if (containsSein) {
			ShifterUnit alternativeShifter;
			String[] alternativeCollocations;
			for (String seinOption : seinAlternatives) {
				alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
						shifter.shifter_pos, shifter.mwe);
				alternativeCollocations = shifter.collocations.clone();
				alternativeCollocations[seinIndex] = seinOption;
				alternativeShifter.collocations = alternativeCollocations;
				alternativeSUnits.add(alternativeShifter);
			}
		}
		if (shifter.name.equals("sein")) {
			ShifterUnit alternativeShifter = new ShifterUnit(shifter.name, shifter.shifter_type, shifter.shifter_scope,
					shifter.shifter_pos, shifter.mwe);
			alternativeShifter.collocations = shifter.collocations.clone();
			alternativeSUnits.add(alternativeShifter);
		}

		return alternativeSUnits;
	}

	/**
	 * @param shifter
	 *          is removed from ShifterLex
	 */
	public void removeShifter(ShifterUnit shifter) {
		shifterList.remove(shifter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String printer = "Name[Type][Scope][Pos]";
		for (int i = 0; i < shifterList.size(); i++) {
			printer = printer + "\n";
			printer = printer + shifterList.get(i);
		}
		return printer;
	}

	/**
	 * Reads in shifter lexicon from filename
	 *
	 * @param filename
	 */
	public void fileToLex(String filename) {

		String shifterStr = new String();
		String shifter_type = new String();
		String shifter_scope_str = new String();
		String[] shifter_scope;
		String shifter_pos = new String();
		Boolean mwe = Boolean.FALSE;
		Scanner scanner;

		try {
			scanner = new Scanner(new File(filename), "UTF-8");
			scanner.useLocale(Locale.GERMANY);
			String inputLine;

			while (scanner.hasNext()) {
				inputLine = scanner.nextLine();

				// Ignore lines starting with "%%" (comments).
				if (!(inputLine.startsWith("%%"))) {

					// Ignore lines without the correct form.
					// Correct form example: Fehlschlag p [subj,attr-rev] nomen
					if (inputLine.matches("[\\w+[-_äöüÄÖÜß]*\\w*]+\\s\\w\\s\\[[\\w+[-\\*,\"]*\\s*]+\\]\\s\\w+")) {

						shifterStr = inputLine.substring(0, inputLine.indexOf(" "));
						// System.out.println("shifter: " + shifterStr);

						shifter_type = inputLine.substring(inputLine.indexOf(" ") + 1, inputLine.indexOf(" ") + 2);
						// System.out.println("shifter_type: " + shifter_type);
						String shifter_type_written_out = new String();
						switch (shifter_type) {
						case "g":
							shifter_type_written_out = SHIFTER_TYPE_GENERAL;
							break;
						case "p":
							shifter_type_written_out = SHIFTER_TYPE_ON_POSITIVE;
							break;
						case "n":
							shifter_type_written_out = SHIFTER_TYPE_ON_NEGATIVE;
							break;
						default:
							//System.err.println("Warning: unknown shifter_type: " + shifter_type + " for shifter: " + shifterStr);
							log.warning("Warning: unknown shifter_type: " + shifter_type + " for shifter: " + shifterStr);
							shifter_type_written_out = shifter_type;
						}

						shifter_scope_str = inputLine.substring(inputLine.lastIndexOf("[") + 1, inputLine.lastIndexOf("]")).replaceAll("\\s+", "");
						shifter_scope = shifter_scope_str.split(",");
						// System.out.println("shifter-scope: " +
						// Arrays.toString(shifter_scope));

						shifter_pos = inputLine.substring(inputLine.lastIndexOf(" ") + 1, inputLine.length());
						// System.out.println("shifter_pos: " + shifter_pos);

						mwe = shifterStr.contains("_");

						if (shifter_type != null && shifter_scope != null && shifter_pos != null) {
							ShifterUnit shifter = new ShifterUnit(shifterStr, shifter_type_written_out, shifter_scope, shifter_pos,
									mwe);
							this.addShifter(shifter);

						} else {
							//System.err.println("Shifter-Lexicon entry for " + shifterStr + " is incomplete!");
							log.warning("Shifter-Lexicon entry for " + shifterStr + " is incomplete!");
						}

					} else {
						//System.err.println("Line with wrong format in Shifter-Lexicon, will be ignored: ");
						//System.err.println("\"" + inputLine + "\"");
						log.warning("Line with wrong format in Shifter-Lexicon, will be ignored:\n\""+ inputLine + "\"");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds a {@link #shifterMap} from a given {@link #shifterList}
	 *
	 * @param key
	 * @param value
	 */
	public void addToMap(String key, ShifterUnit value) {
		if (this.shifterMap.containsKey(key)) {
			key = key + "+";
			addToMap(key, value);
		} else {
			this.shifterMap.put(key, value);
		}
	}
}
