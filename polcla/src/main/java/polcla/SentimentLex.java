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
 * SentimentLex object contains all informations from a given sentiment lexicon
 *
 */
public class SentimentLex {

  private final static Logger log = Logger.getLogger(SentimentLex.class.getName());

  List<SentimentUnit> sentimentList = new ArrayList<SentimentUnit>();

  Map<String, SentimentUnit> sentimentMap = new HashMap<String, SentimentUnit>();
  boolean flexibleMWEs = false;
  String[] collectSubjectiveExpressions = {""};
  private final boolean include_neutral;

  /**
   * Constructs a new SentimentLex
   *
   * @param flexMWE indicates if the multi word expressions in the lexicon
   * should be interpreted in a flexible way (more info see
   * {@link #mweFlexibility(SentimentUnit)}).
   * @param include_neutral If true, neutral expressions of german lex will be
   * taken into account too.
   */
  public SentimentLex(boolean flexMWE, boolean include_neutral) {
    log.setLevel(Level.FINE);
    this.flexibleMWEs = flexMWE;
    this.include_neutral = include_neutral;
  }

  // Lists containing words for flexible mwe interpretations.
  // Used in the method mweFlexibility of this class.
  /**
   * List containing reflexive nominatives for flexible interpretations. If mwe
   * contains "sich". "mich", "Mich", "dich", "Dich", "ihn", "Ihn", "es", "Es",
   * "euch", "Euch", "Sie"
   */
  private final List<String> sichAlternatives = Arrays.asList("mich", "Mich", "dich", "Dich", "ihn", "Ihn", "es", "Es",
          "euch", "Euch", "Sie");

  /**
   * List containing seinenAlternatives for flexible interpretations. If mwe
   * contains "seinen" or "seinem". "meine", "Meine", "deine", "Deine", "ihre",
   * "eure", "Ihre", "unsere", "Unsere"
   */
  private final List<String> seinenAlternatives = Arrays.asList("meine", "Meine", "deine", "Deine", "ihre", "eure", "Ihre",
          "unsere", "Unsere");

  /**
   * List containing words for flexible interpretations. If mwe contains "den".
   * "einen", "dem"
   */
  private final List<String> denAlternatives = Arrays.asList("einen", "dem");

  /**
   * List containing words for flexible interpretations. If mwe contains "ein".
   * "kein"
   */
  private final List<String> einAlternatives = Arrays.asList("kein");

  /**
   * List containing words for flexible interpretations. If mwe contains
   * "einen". "keinen"
   */
  private final List<String> einenAlternatives = Arrays.asList("keinen");

  /**
   * List containing words for flexible interpretations. If mwe contains "sein".
   * "werden"
   */
  private final List<String> seinAlternatives = Arrays.asList("werden");

  /**
   * Returns the SentimentUnit corresponding to the given name or null.
   *
   * @param name The name of the sentiment to look for.
   * @return SentimentUnit for a given name or null if no SentimentUnit with the
   * given name exists
   */
  public SentimentUnit getSentiment(String name) {
    for (int i = 0; i < sentimentList.size(); i++) {
      SentimentUnit tmp = (SentimentUnit) sentimentList.get(i);
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
   * Returns the SentimentUnits (more than one if there is more than one lexicon
   * entry) corresponding to the given name or null. Up to 50 entries possible
   * for one sentiment.
   *
   * @param name The name of the sentiment to look for.
   * @return SentimentUnit for a given name or null if no SentimentUnit with the
   * given name exists
   */
  public ArrayList<SentimentUnit> getAllSentiments(String name) {
    ArrayList<SentimentUnit> sentiments = new ArrayList<SentimentUnit>();
    for (SentimentUnit sentimentList1 : sentimentList) {
      SentimentUnit tmp = (SentimentUnit) sentimentList1;
      if (tmp.mwe) {
        String mweParts = new String();
        for (String mwePart : tmp.collocations) {
          mweParts += mwePart + "_";
        }
        mweParts += tmp.name;
        if (mweParts.equals(name)) {
          sentiments.add(tmp);
        }
      } else {
        if (tmp.name.equals(name)) {
          sentiments.add(tmp);
        }
      }
    }
    if (!sentiments.isEmpty()) {
      return sentiments;
    } else {
      return null;
    }
  }

  /**
   * Adds a sentiment expression to the internal lexicon, possibly interpreting
   * the with some flexibility (for MWEs).
   *
   * @param sentiment is added to SentimentLex
   */
  public void addSentiment(SentimentUnit sentiment) {
    if (!sentimentList.contains(sentiment)) {
      sentimentList.add(sentiment);
      addToMap(sentiment.name, sentiment);

      if (sentiment.mwe && this.flexibleMWEs) {
        for (SentimentUnit flex : mweFlexibility(sentiment)) {
          sentimentList.add(flex);
          addToMap(flex.name, flex);

        }
      }
    } else {
      System.err.println("Double entry in sentiment lexicon!: " + sentiment.name);
      log.log(Level.WARNING, "Double entry in sentiment lexicon!: {0}", sentiment.name);
    }
  }

  /**
   * Returns an ArrayList of SenimentUnits that are the result of flexible
   * interpretation of the given MWE SentimentUnit. Flexibility is applied
   * w.r.t. the following words that are found in the MWE: 1) Reflexive
   * pronouns: sich --> sich, mich, dich, ihn, ... etc 2) Possessive pronouns:
   * seinen --> seinen, meinen, deinen, ... etc 3) Ein(en)/Kein(en): ein(en) -->
   * ein(en), kein(en) 4) Sein/Werden: sein --> sein, werden 5) Other: den -->
   * den, einen, dem
   *
   * @param sentiment given a MWE SentimentUnit
   * @return ArrayList of SentimentUnits that are created by flexibility
   */
  public ArrayList<SentimentUnit> mweFlexibility(SentimentUnit sentiment) {
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

    for (int j = 0; j < sentiment.collocations.length; j++) {
      if (sentiment.collocations[j].equals("sich")) {
        containsSich = true;
        sichIndex = j;
      }
      if (sentiment.collocations[j].equals("seinen") || sentiment.collocations[j].equals("seinem")) {
        containsSeinen = true;
        seinenIndex = j;
      }
      if (sentiment.collocations[j].equals("ein")) {
        containsEin = true;
        einIndex = j;
      }
      if (sentiment.collocations[j].equals("einen")) {
        containsEinen = true;
        einenIndex = j;
      }
      if (sentiment.collocations[j].equals("den")) {
        containsDen = true;
        denIndex = j;
      }
      if (sentiment.collocations[j].equals("sein")) {
        containsSein = true;
        seinIndex = j;
      }
    }

    ArrayList<SentimentUnit> alternativeSUnits = new ArrayList<SentimentUnit>();
    if (containsSich) {
      String[] alternativeCollocations;
      SentimentUnit alternativeSentiment;

      for (String reflexive : sichAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[sichIndex] = reflexive;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }
    if (containsSeinen) {
      String[] alternativeCollocations;
      SentimentUnit alternativeSentiment;
      for (String reflexive : seinenAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[seinenIndex] = reflexive;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }

    if (containsDen) {
      SentimentUnit alternativeSentiment;
      String[] alternativeCollocations;
      for (String denOption : denAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[denIndex] = denOption;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }
    if (containsEin) {
      SentimentUnit alternativeSentiment;
      String[] alternativeCollocations;
      for (String einOption : einAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[einIndex] = einOption;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }
    if (containsEinen) {
      SentimentUnit alternativeSentiment;
      String[] alternativeCollocations;
      for (String einenOption : einenAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[einenIndex] = einenOption;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }
    if (containsSein) {
      SentimentUnit alternativeSentiment;
      String[] alternativeCollocations;
      for (String seinOption : seinAlternatives) {
        alternativeSentiment = new SentimentUnit(sentiment.name, sentiment.category, sentiment.value, sentiment.pos,
                sentiment.mwe);
        alternativeCollocations = sentiment.collocations.clone();
        alternativeCollocations[seinIndex] = seinOption;
        alternativeSentiment.collocations = alternativeCollocations;
        alternativeSUnits.add(alternativeSentiment);
      }
    }
    if (sentiment.name.equals("sein")) {
      SentimentUnit alternativeSentiment = new SentimentUnit("werden", sentiment.category, sentiment.value,
              sentiment.pos, sentiment.mwe);
      alternativeSentiment.collocations = sentiment.collocations.clone();
      alternativeSUnits.add(alternativeSentiment);
    }

    return alternativeSUnits;
  }

  /**
   * @param sentiment is removed from SentimentLex
   */
  public void removeSentiment(SentimentUnit sentiment) {
    sentimentList.remove(sentiment);
  }

  /**
   * Removes a sentiment from the SentimentLex based on the sentiment's name.
   *
   * @param wordFromInput
   */
  public void removeSentimentBasedOnWord(String wordFromInput) {
    for (SentimentUnit u : sentimentList) {
      if (u.name.equals(wordFromInput)) {
        this.removeSentiment(u);
        break;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String printer = "Name[Category][Value][Pos]";
    for (SentimentUnit sentimentList1 : sentimentList) {
      printer = printer + "\n";
      printer = printer + sentimentList1;
    }
    return printer;
  }

  /**
   * Reads in sentiment lexicon from filename
   *
   * @param filename
   */
  public void fileToLex(String filename) {
    String wordFromInput = new String();
    String category = new String();
    Double value = 0.0;
    String pos = new String();
    Boolean mwe = Boolean.FALSE;
    Scanner scanner;
    List<String> checkForMultiEntry = new ArrayList<>();
    List<String> neutralWords = new ArrayList<>();

    try {
      scanner = new Scanner(new File(filename), "UTF-8");
      scanner.useLocale(Locale.GERMANY);
      String inputLine;

      while (scanner.hasNext()) {
        inputLine = scanner.nextLine();

        // Ignore lines starting with "%%" (comments).
        if (!(inputLine.startsWith("%%"))) {

          // Ignore lines without the correct form.
          // Correct form example: fehlschlagen NEG=0.7 verben
          if (inputLine.matches("[\\w+[-_äöüÄÖÜß]*\\w*]+\\s\\w\\w\\w=\\d.?\\d?\\s\\w+")) {

            wordFromInput = inputLine.substring(0, inputLine.indexOf(" "));
            if (checkForMultiEntry.contains(wordFromInput)) {
              System.err.println("More than one (conflicting) entry in sentiment lexicon for: " + wordFromInput + ".\nNeutral versions will be ignored. Otherwise, the first entry from the top is used.");
              log.log(Level.WARNING, "More than one (conflicting) entry in sentiment lexicon for: {0}", wordFromInput);

              if (neutralWords.contains(wordFromInput)) {
                // A neutral sentiment unit is in the sentiment list. 
                // Overwrite it with the non-neutral version.
                neutralWords.remove(wordFromInput);
                this.removeSentimentBasedOnWord(wordFromInput);
                log.log(Level.FINE, "Removed neutral entry: {}", wordFromInput);
              } else { // Use the first encountered entry that's not neutral. Ignore the rest.
                continue;
              }
            }
            checkForMultiEntry.add(wordFromInput);

            category = inputLine.substring(inputLine.indexOf(" ") + 1, inputLine.indexOf("="));

            Locale original = Locale.getDefault();
            Locale.setDefault(new Locale("de", "DE"));
            try (Scanner doubleScanner = new Scanner(inputLine.substring(inputLine.indexOf("=") + 1).replace('.', ','))) {
              if (doubleScanner.hasNextDouble()) {
                value = doubleScanner.nextDouble();
              } else {
                log.log(Level.FINE, "no valueToSetFeatureTo has been found for: {0}", wordFromInput);
              }
            } finally {
              Locale.setDefault(original);
            }

            pos = inputLine.substring(inputLine.lastIndexOf(" ") + 1, inputLine.length());

            mwe = wordFromInput.contains("_");

            if (category != null && pos != null) {
              // Ignore Shifter
              if ((category.equals("NEG") || category.equals("POS"))) {
                SentimentUnit unit = new SentimentUnit(wordFromInput, category, value.toString(), pos, mwe);
                this.addSentiment(unit);
              } else if ((include_neutral && category.equals("NEU") && value == 0.0)) {
                SentimentUnit unit = new SentimentUnit(wordFromInput, category, value.toString(), pos, mwe);
                neutralWords.add(wordFromInput);
                this.addSentiment(unit);

              } // Faulty Lexicon entry cases
              else if (include_neutral && category.equals("NEU") && value != 0.0) {
                String message = String.format("Sentiment-Lexicon entry for %s %s %s %s %s is in a wrong format! ", wordFromInput, category, value.toString(), pos, mwe);
                System.err.println(message);
                log.log(Level.WARNING, message);
              }
            } else {
              System.err.println("Sentiment-Lexicon entry for " + wordFromInput + " is incomplete!");
              log.log(Level.WARNING, "Sentiment-Lexicon entry for {0} is incomplete!", wordFromInput);
            }

          } else {
            System.err.println("Line with wrong format in Sentiment-Lexicon, will be ignored: ");
            System.err.println("\"" + inputLine + "\"");
            log.log(Level.WARNING, "Line with wrong format in Sentiment-Lexicon, will be ignored:\n\"{0}\"", inputLine);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Builds a {@link #sentimentMap} from a given {@link #sentimentList}
   *
   * @param key
   * @param value
   */
  public void addToMap(String key, SentimentUnit value) {
    if (this.sentimentMap.containsKey(key)) {
      key = key + "+";
      addToMap(key, value);
    } else {
      this.sentimentMap.put(key, value);
    }
  }
}
