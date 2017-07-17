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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IntensifierLex object contains all informations from a given intensifier
 * lexicon
 *
 */
public class IntensifierLex {

  private final static Logger log = Logger.getLogger(IntensifierLex.class.getName());
  
  // Constants
  public static final String INTENSIFIER_TYPE_GENERAL = "general";
  public static final String INTENSIFIER_TYPE_ON_POSITIVE = "on positive";
  public static final String INTENSIFIER_TYPE_ON_NEGATIVE = "on negative";

  List<IntensifierUnit> intensifierList = new ArrayList<IntensifierUnit>();
  Map<String, IntensifierUnit> intensifierMap = new HashMap<String, IntensifierUnit>();
  boolean flexibleMWEs = false;
  String[] collectSubjectiveExpressions = {""};

  /**
   * Constructs a new IntensifierLex
   *
   * @param flexMWE indicates if the multi word expressions in the lexicon
   * should be interpreted in a flexible way (more info see
   * {@link #mweFlexibility(IntensifierUnit)}).
   */
  public IntensifierLex(boolean flexMWE) {
    log.setLevel(Level.FINE);
    this.flexibleMWEs = flexMWE;
  }

  /**
   * Returns the IntensifierUnit corresponding to the given name or null.
   *
   * @param name The name of the intensifier to look for.
   * @return IntensifierUnit for a given name or null if no IntensifierUnit with
   * the given name exists
   */
  public IntensifierUnit getIntensifier(String name) {
    for (IntensifierUnit intensifierList1 : intensifierList) {
      IntensifierUnit tmp = (IntensifierUnit) intensifierList1;
      if (tmp.name.equals(name)) {
        return tmp;
      }
    }
    return null;
  }

  /**
   * Returns the IntensifierUnits (more than one if there is more than one
   * lexicon entry) corresponding to the given name or null. Up to 50 entries
   * possible for one intensifier.
   *
   * @param name The name of the intensifier to look for.
   * @return IntensifierUnit for a given name or null if no IntensifierUnit with
   * the given name exists
   */
  public ArrayList<IntensifierUnit> getAllIntensifiers(String name) {
    ArrayList<IntensifierUnit> intensifiers = new ArrayList<IntensifierUnit>();
    for (IntensifierUnit intensifierList1 : intensifierList) {
      IntensifierUnit tmp = (IntensifierUnit) intensifierList1;
      if (tmp.name.equals(name)) {
        intensifiers.add(tmp);
      }
    }
    if (!intensifiers.isEmpty()) {
      return intensifiers;
    } else {
      return null;
    }
  }

  /**
   * Adds an intensifier expression to the internal lexicon.
   *
   * @param intensifier is added to Intensifier
   */
  public void addIntensifier(IntensifierUnit intensifier) {
    intensifierList.add(intensifier);
    addToMap(intensifier.name, intensifier);
  }

  /**
   * @param intensifier is removed from Intensifier
   */
  public void removeIntensifier(IntensifierUnit intensifier) {
    intensifierList.remove(intensifier);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String printer = "Name[Type][Scope][Pos]";
    for (IntensifierUnit intensifierList1 : intensifierList) {
      printer = printer + "\n";
      printer = printer + intensifierList1;
    }
    return printer;
  }

  /**
   * Reads in intensifier lexicon from filename
   *
   * @param filename
   */
  public void fileToLex(String filename) {

		String intensifierStr = new String();
		String intensifier_type = new String();
		String intensifier_scope_str = new String();
		String[] intensifier_scope;
		String intensifier_pos = new String();
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

						intensifierStr = inputLine.substring(0, inputLine.indexOf(" "));

						intensifier_type = inputLine.substring(inputLine.indexOf(" ") + 1, inputLine.indexOf(" ") + 2);
						String intensifier_type_written_out = new String();
						switch (intensifier_type) {
						case "g":
							intensifier_type_written_out = INTENSIFIER_TYPE_GENERAL;
							break;
						case "p":
							intensifier_type_written_out = INTENSIFIER_TYPE_ON_POSITIVE;
							break;
						case "n":
							intensifier_type_written_out = INTENSIFIER_TYPE_ON_NEGATIVE;
							break;
						default:
							log.log(Level.WARNING, "Warning: unknown intensifier_type: {0} for intensifier: {1}", new Object[]{intensifier_type, intensifierStr});
							intensifier_type_written_out = intensifier_type;
						}

						intensifier_scope_str = inputLine.substring(inputLine.lastIndexOf("[") + 1, inputLine.lastIndexOf("]")).replaceAll("\\s+", "");
						intensifier_scope = intensifier_scope_str.split(",");

						intensifier_pos = inputLine.substring(inputLine.lastIndexOf(" ") + 1, inputLine.length());

						mwe = intensifierStr.contains("_");

						if (intensifier_type != null && intensifier_scope != null && intensifier_pos != null) {
							IntensifierUnit intensifier = new IntensifierUnit(intensifierStr, intensifier_type_written_out, intensifier_scope, intensifier_pos,
									mwe);
							this.addIntensifier(intensifier);

						} else {
							log.log(Level.WARNING, "Intensifier-Lexicon entry for {0} is incomplete!", intensifierStr);
						}

					} else {
						log.log(Level.WARNING, "Line with wrong format in Intensifier-Lexicon, will be ignored:\n\"{0}\"", inputLine);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

  /**
   * Builds a {@link #intensifierMap} from a given {@link #intensifierList}
   *
   * @param key
   * @param value
   */
  public void addToMap(String key, IntensifierUnit value) {
    if (this.intensifierMap.containsKey(key)) {
      key = key + "+";
      addToMap(key, value);
    } else {
      this.intensifierMap.put(key, value);
    }
  }
}
