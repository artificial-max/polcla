package polcla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import polcla.SalsaAPIConnective;
import polcla.SentimentLex;
import polcla.SentimentUnit;
import polcla.ShifterLex;
import polcla.ShifterUnit;
import polcla.WordObj;

import salsa.corpora.elements.Fenode;
import salsa.corpora.elements.Flag;
import salsa.corpora.elements.Frame;
import salsa.corpora.elements.FrameElement;
import salsa.corpora.elements.Global;
import salsa.corpora.elements.Target;

public class BaselineModule extends ModuleBasics implements Module {

  /**
   * Defines the window in which shifter targets are searched for.
   */
  private final int window;

  /**
   * Defines the direction in which shifter targets are searched for.
   */
  private final String direction;
  private static final String LEFT = "LEFT";
  private static final String RIGHT = "RIGHT";
  private static final String BOTH = "BOTH";

  /**
   * Stores each sentence's polarity
   */
  private final Collection<Global> globalsSentencePolarities = new ArrayList<Global>();

  /**
   * Globals are sentence flags. Stores each sentence's polarity.
   *
   * @return ArrayList<Global> globalsSentencePolarities
   */
  @Override
  public Collection<Global> getGlobalsSentencePolarities() {
    return globalsSentencePolarities;
  }

  /**
   * Constructs a new BaselineModule. Looks for shifter targets within the
   * specified window to the right of the shifter. Does not use dependency
   * information
   *
   * @param sentimentLex A SentimentLex object in which the rules for finding
   * sentiment sources and targets are saved as SentimentUnits.
   * @param shifterLex A ShifterLex object in which the rules for finding
   * shifter targets are saved as ShifterUnits.
   * @param window Defines the window/window in which shifter targets are
   * searched for.
   * @param direction Determines the direction in which to look for shifter
   * targets. Either left or right of the shifter.
   * @param pos_lookup_sentiment Option to do pos lookup for SEs.
   * @param pos_lookup_shifter Option to do pos lookup for shifters.
   * @param shifter_orientation_check Option to take shifter orientation into
   * account.
   */
  public BaselineModule(SentimentLex sentimentLex, ShifterLex shifterLex, int window, String direction,
          Boolean pos_lookup_sentiment, Boolean pos_lookup_shifter, Boolean shifter_orientation_check) {
    this.sentimentLex = sentimentLex;
    this.shifterLex = shifterLex;
    this.window = window;
    this.direction = direction;
    this.posLookupSentiment = pos_lookup_sentiment;
    this.posLookupShifter = pos_lookup_shifter;
    this.shifter_orientation_check = shifter_orientation_check;
  }

  /**
   * Constructs a new BaselineModule. Looks for shifter targets within the
   * specified window to the right of the shifter. Does not use dependency
   * information. Uses preset SE locations.
   *
   * @param salsa Salsa API connective containing locations of SEs.
   * @param sentimentLex A SentimentLex object in which the rules for finding
   * sentiment sources and targets are saved as SentimentUnits.
   * @param shifterLex A ShifterLex object in which the rules for finding
   * shifter targets are saved as ShifterUnits.
   * @param window Defines the window/window in which shifter targets are
   * searched for.
   * @param direction Determines the direction in which to look for shifter
   * targets. Either left or right of the shifter.
   * @param pos_lookup_sentiment Option to do pos lookup for SEs.
   * @param pos_lookup_shifter Option to do pos lookup for shifters.
   * @param shifter_orientation_check Option to take shifter orientation into
   * account.
   */
  public BaselineModule(SalsaAPIConnective salsa, SentimentLex sentimentLex, ShifterLex shifterLex, int window,
          String direction, Boolean pos_lookup_sentiment, Boolean pos_lookup_shifter, Boolean shifter_orientation_check) {
    this.salsa = salsa;
    this.sentimentLex = sentimentLex;
    this.shifterLex = shifterLex;
    this.window = window;
    this.direction = direction;
    this.posLookupSentiment = pos_lookup_sentiment;
    this.posLookupShifter = pos_lookup_shifter;
    this.shifter_orientation_check = shifter_orientation_check;
    this.usePresetSELocations = true;
  }

  /**
   * Looks at a single SentenceObj to find any subjective expressions and adds
   * the information to the Salsa XML structure. First shifters and their
   * targets are located, afterwards subjective expressions outside the window
   * of a shifter are considered.
   *
   * @param sentence The SentenceObj which is to be checked for subjective
   * expressions.
   */
  @Override
  public Collection<Frame> findFrames(SentenceObj sentence) {
    /**
     * Collects the found frames
     */
    final Collection<Frame> frames = new ArrayList<Frame>();
    final FrameIds frameIds = new FrameIds(sentence, "se");
    globalsSentencePolarities.removeAll(globalsSentencePolarities);

    ArrayList<WordObj> sentimentList = new ArrayList<WordObj>();
    ArrayList<WordObj> shifterList = new ArrayList<WordObj>();

    if (usePresetSELocations) {
      usePresetSELocations(sentence, sentimentList);
    }

    Double polaritySum = 0.0;
    Double polarityOfWord = 0.0;

    // Look up every word of the sentence in the shifterLex and sentimentLex
    // lexicons and add them to the shifterList or sentimentList.
    for (WordObj word : sentence.getWordList()) {
      ArrayList<ShifterUnit> shifterLexEntries = shifterLex.getAllShifters(word.getLemma());
      if (shifterLexEntries != null) {
        for (ShifterUnit shifterLexEntry : shifterLexEntries) {
          if (shifterLexEntry != null) {
            if (posLookupShifter) {
              posLookupShifter(shifterList, word, shifterLexEntry);
            } else {
              shifterList.add(word);
              break;
            }
          }
        }
      }

      if (!usePresetSELocations) {
        ArrayList<SentimentUnit> sentLexEntries = sentimentLex.getAllSentiments(word.getLemma());
        if (sentLexEntries != null) {
          for (SentimentUnit sentLexEntry : sentLexEntries) {
            if (sentLexEntry != null) {
              if (posLookupSentiment) {
                posLookupSentiment(sentimentList, word, sentLexEntry);
              } else {
                sentimentList.add(word);
                break;
              }
            }
          }
        }
      }
    }

    OUTER:
    for (WordObj shifter : shifterList) {
      WordObj shifterTarget = null;
      // Look for the shifterTarget
      switch (direction) {
        case RIGHT:
          shifterTarget = findShifterTargetBaselineRight(shifter, sentimentList, sentence, window);
          break;
        case LEFT:
          shifterTarget = findShifterTargetBaselineLeft(shifter, sentimentList, sentence, window);
          break;
        case BOTH:
          shifterTarget = findShifterTargetBaselineBoth(shifter, sentimentList, sentence, window);
          break;
        default:
          System.err.println("False direction input for Baseline given!");
          System.err.println("Direction given: " + direction);
          System.err.println("Check the config properties!");
          break OUTER;
      }
      if (shifterTarget != null) {

        // Create Frame object for Salsa XML output
        final Frame frame = new Frame("SubjectiveExpression", frameIds.next());
        final FrameElementIds feIds = new FrameElementIds(frame);

        // Set Frames for the sentiment word
        final Target target = new Target();
        setFrames(sentence, frames, shifterTarget, frame, target);

        // Set FrameElement for the shifter
        // TODO call it shifter after evaluation tool is not needed anymore!
        final FrameElement shifterElement = new FrameElement(feIds.next(), "Target");
        shifterElement.addFenode(new Fenode(sentence.getTree().getTerminal(shifter).getId()));

        // Set Frame element flag for the shifter
        String shifterType = shifterLex.getShifter(shifter.getLemma()).shifter_type;
        final Flag shifterFlag = new Flag(shifterType, "shifter");
        shifterElement.addFlag(shifterFlag);

        // Set Flag for the Frame stating the starting polarity value
        String polarityValueStr = sentimentLex.getSentiment(shifterTarget.getLemma()).value;
        String polarityCategory = sentimentLex.getSentiment(shifterTarget.getLemma()).category;
        String valueAndCat = polarityCategory + " " + polarityValueStr;
        final Flag polarityWithoutShift = new Flag("polarity without shift: " + valueAndCat, "subjExpr");
        frame.addFlag(polarityWithoutShift);

        final double SHIFT_AMOUNT = 1.3;

        // Compute the polarity value after a shift and invert the category.
        polarityOfWord = Double.valueOf(polarityValueStr);
        if (!polarityCategory.equals("UNKNOWN")) {
          switch (shifterType) {
            case ShifterLex.SHIFTER_TYPE_ON_NEGATIVE:
              polarityCategory = "POS";
              polarityOfWord = polarityOfWord * -1.0 + SHIFT_AMOUNT;
              break;
            case ShifterLex.SHIFTER_TYPE_ON_POSITIVE:
              polarityCategory = "NEG";
              polarityOfWord = polarityOfWord - SHIFT_AMOUNT;
              break;
            case ShifterLex.SHIFTER_TYPE_GENERAL:
              switch (polarityCategory) {
                case "POS":
                  polarityCategory = "NEG";
                  polarityOfWord = polarityOfWord - SHIFT_AMOUNT;
                  break;
                case "NEG":
                  polarityCategory = "POS";
                  polarityOfWord = polarityOfWord * -1.0 + SHIFT_AMOUNT;
              }
          }
        }
        polaritySum += polarityOfWord;
        polarityValueStr = Double.toString(Math.abs(polarityOfWord));

        valueAndCat = polarityCategory + " " + polarityValueStr;
        final Flag polarityAfterShift = new Flag("polarity after shift: " + valueAndCat, "subjExpr");
        frame.addFlag(polarityAfterShift);

        frame.addFe(shifterElement);

        // Remove the shifterTarget from the sentiment list so it doesn't get
        // another frame when iterating over the remaining sentiments.
        sentimentList.remove(shifterTarget);
      }
    }

    // *********************sentiments without shifter*************************
    // Iterate over every found sentiment and set frames.
    for (WordObj sentiment : sentimentList) {
      // Create Frame object
      final Frame frame = new Frame("SubjectiveExpression", frameIds.next());

      // Set Target for the sentiment word
      final Target target = new Target();
      setFrames(sentence, frames, sentiment, frame, target);

      // Set Flag for the Frame stating the starting polarity value
      String polarityValueStr = sentimentLex.getSentiment(sentiment.getLemma()).value;
      String polarityCategory = sentimentLex.getSentiment(sentiment.getLemma()).category;
      String valueAndCat = polarityCategory + " " + polarityValueStr;
      polarityOfWord = Double.valueOf(polarityValueStr);
      if (polarityCategory.equals("NEG")) {
        polarityOfWord = polarityOfWord * -1.0;
      }
      polaritySum += polarityOfWord;
      final Flag polarityWithoutShift = new Flag("polarity without shift: " + valueAndCat, "subjExpr");
      frame.addFlag(polarityWithoutShift);
    }

    final Global sentencePolarity = new Global("INTERESTING");
    sentencePolarity.setParam(String.format("%.2f", polaritySum));
    sentencePolarity.setText("The sentence polarity.");
    globalsSentencePolarities.add(sentencePolarity);

    return frames;
  }

  /**
   * This method is used to look for the target of a shifter using a given
   * window and lexicon look-up. The found shifter target must be contained in
   * the sentimentList in order to be returned by this method. Looks to the
   * right of the shifter.
   *
   * @param shifter The shifter for which a target is searched for.
   * @param sentimentList A list of found sentiments in the current sentence.
   * These are the potential shifter target candidates.
   * @param sentence The sentence the shifter is in.
   * @return The WordObj corresponding to the found shifter target, or null.
   */
  private WordObj findShifterTargetBaselineRight(WordObj shifter, ArrayList<WordObj> sentimentList,
          SentenceObj sentence, int window) {
    WordObj shifterTarget = null;
    LinkedList<WordObj> wordList = sentence.getWordList();
    int shifterPos = sentence.getWordPosition(shifter);

    for (int i = 1; i <= window; i++) {
      if (wordList.size() > shifterPos + i) {
        shifterTarget = wordList.get(shifterPos + i);
        if (sentimentList.contains(shifterTarget)) {
          if (shifter_orientation_check) {
            if (orientationCheck(shifter, shifterTarget)) {
              return shifterTarget;
            }
          } else {
            return shifterTarget;
          }
        }
      }
    }
    return null;
  }

  /**
   * This method is used to look for the target of a shifter using a given
   * window and lexicon look-up. The found shifter target must be contained in
   * the sentimentList in order to be returned by this method. Looks to the left
   * of the shifter.
   *
   * @param shifter The shifter for which a target is searched for.
   * @param sentimentList A list of found sentiments in the current sentence.
   * These are the potential shifter target candidates.
   * @param sentence The sentence the shifter is in.
   * @return The WordObj corresponding to the found shifter target, or null.
   */
  private WordObj findShifterTargetBaselineLeft(WordObj shifter, ArrayList<WordObj> sentimentList, SentenceObj sentence,
          int window) {
    WordObj shifterTarget = null;
    LinkedList<WordObj> wordList = sentence.getWordList();
    int shifterPos = sentence.getWordPosition(shifter);

    for (int i = 1; i <= window; i++) {
      if (shifterPos - i >= 0) {
        shifterTarget = wordList.get(shifterPos - i);
        if (sentimentList.contains(shifterTarget)) {
          if (shifter_orientation_check) {
            if (orientationCheck(shifter, shifterTarget)) {
              return shifterTarget;
            }
          } else {
            return shifterTarget;
          }
        }
      }
    }
    return null;
  }

  /**
   * This method is used to look for the target of a shifter using a given
   * window and lexicon look-up. The found shifter target must be contained in
   * the sentimentList in order to be returned by this method. Looks to both
   * sides of the shifter in alternating fashion, starting right.
   *
   * @param shifter The shifter for which a target is searched for.
   * @param sentimentList A list of found sentiments in the current sentence.
   * These are the potential shifter target candidates.
   * @param sentence The sentence the shifter is in.
   * @return The WordObj corresponding to the found shifter target, or null.
   */
  private WordObj findShifterTargetBaselineBoth(WordObj shifter, ArrayList<WordObj> sentimentList, SentenceObj sentence,
          int window) {
    WordObj shifterTarget = null;
    LinkedList<WordObj> wordList = sentence.getWordList();
    int shifterPos = sentence.getWordPosition(shifter);

    for (int i = 1; i <= window; i++) {
      if (wordList.size() > shifterPos + i) {
        shifterTarget = wordList.get(shifterPos + i);
        if (sentimentList.contains(shifterTarget)) {
          if (shifter_orientation_check) {
            if (orientationCheck(shifter, shifterTarget)) {
              return shifterTarget;
            }
          } else {
            return shifterTarget;
          }
        }
      }
      if (shifterPos - i >= 0) {
        shifterTarget = wordList.get(shifterPos - i);
        if (sentimentList.contains(shifterTarget)) {
          if (shifter_orientation_check) {
            if (orientationCheck(shifter, shifterTarget)) {
              return shifterTarget;
            }
          } else {
            return shifterTarget;
          }
        }
      }
    }
    return null;
  }
}
