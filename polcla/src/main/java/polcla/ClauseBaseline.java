package polcla;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import salsa.corpora.elements.Fenode;
import salsa.corpora.elements.Flag;
import salsa.corpora.elements.Frame;
import salsa.corpora.elements.FrameElement;
import salsa.corpora.elements.Global;
import salsa.corpora.elements.Nonterminal;
import salsa.corpora.elements.Target;
import salsa.corpora.elements.Terminal;

public class ClauseBaseline extends ModuleBasics implements Module {

  private final static Logger log = Logger.getLogger(ClauseBaseline.class.getName());

  private final Collection<Global> globalsSentencePolaritiesB;

  /**
   * Globals are sentence flags. Stores each sentence's polarity.
   *
   * @return ArrayList<Global> globalsSentencePolaritiesB
   */
  @Override
  public Collection<Global> getGlobalsSentencePolarities() {
    return globalsSentencePolaritiesB;
  }

  /**
   * Constructs a new BaselineModule. Looks for shifter targets within the same
   * clause as the shifter.
   *
   * @param sentimentLex A SentimentLex object in which the rules for finding
   * sentiment sources and targets are saved as SentimentUnits.
   * @param shifterLex A ShifterLex object in which the rules for finding
   * shifter targets are saved as ShifterUnits.
   * @param pos_lookup_sentiment Option to do pos lookup for SEs.
   * @param pos_lookup_shifter Option to do pos lookup for shifters.
   * @param shifter_orientation_check Option to do orientation check for
   * shifters.
   */
  public ClauseBaseline(SentimentLex sentimentLex, ShifterLex shifterLex, Boolean pos_lookup_sentiment,
          Boolean pos_lookup_shifter, Boolean shifter_orientation_check) {
    this.globalsSentencePolaritiesB = new ArrayList<Global>();
    log.setLevel(Level.ALL);
    this.sentimentLex = sentimentLex;
    this.shifterLex = shifterLex;
    this.posLookupSentiment = pos_lookup_sentiment;
    this.posLookupShifter = pos_lookup_shifter;
    this.shifter_orientation_check = shifter_orientation_check;
  }

  /**
   * Constructs a new BaselineModule. Looks for shifter targets within the same
   * clause as the shifter.
   *
   * @param salsa Salsa API connective containing locations of SEs.
   * @param sentimentLex A SentimentLex object in which the rules for finding
   * sentiment sources and targets are saved as SentimentUnits.
   * @param shifterLex A ShifterLex object in which the rules for finding
   * shifter targets are saved as ShifterUnits.
   * @param pos_lookup_sentiment Option to do pos lookup for SEs.
   * @param pos_lookup_shifter Option to do pos lookup for shifters.
   * @param shifter_orientation_check Option to do orientation check for
   * shifters.
   */
  public ClauseBaseline(SalsaAPIConnective salsa, SentimentLex sentimentLex, ShifterLex shifterLex,
          Boolean pos_lookup_sentiment, Boolean pos_lookup_shifter, Boolean shifter_orientation_check) {
    this.globalsSentencePolaritiesB = new ArrayList<Global>();
    log.setLevel(Level.ALL);
    this.salsa = salsa;
    this.sentimentLex = sentimentLex;
    this.shifterLex = shifterLex;
    this.posLookupSentiment = pos_lookup_sentiment;
    this.posLookupShifter = pos_lookup_shifter;
    this.shifter_orientation_check = shifter_orientation_check;
    this.usePresetSELocations = true;
  }

  /**
   * Looks at a single SentenceObj to find any subjective expressions and adds
   * the information to the Salsa XML structure. First shifters and their
   * targets are located, afterwards subjective expressions outside the scope of
   * a shifter are considered.
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
    globalsSentencePolaritiesB.removeAll(globalsSentencePolaritiesB);

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

    // Iterate over every found shifter and search for targets in their scope.
    // Also set frames.
    for (WordObj shifter : shifterList) {
      // Look for the shifterTarget
      log.log(Level.FINE, "Shifter: {0}", shifter.toString());
      WordObj shifterTarget = findShifterTargetClauseBaseline(shifter, sentimentList, sentence);

      if (shifterTarget != null) {

        // Create Frame object for Salsa XML output
        final Frame frame = new Frame("SubjectiveExpression", frameIds.next());
        final FrameElementIds feIds = new FrameElementIds(frame);

        // Set Frames for the sentiment word
        final Target target = new Target();
        setFrames(sentence, frames, shifterTarget, frame, target);

        // Set FrameElement for the shifter
        // EVAL call "Shifter" "Target" for use with the evaluation tool.
        final FrameElement shifterElement = new FrameElement(feIds.next(), "Shifter");
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
    // final Frame sentenceFrame = new Frame("Sentence");
    // final Flag polaritySumFlag = new Flag("Sentence polarity: " +
    // String.format("%.2f", polaritySum), "sentence");
    // final Target sentenceTarget = new Target();
    // sentenceTarget.addFenode(new
    // Fenode(sentence.getTree().getTrueRoot().getId()));
    // sentenceFrame.addFlag(polaritySumFlag);
    // sentenceFrame.setTarget(sentenceTarget);
    // frames.add(sentenceFrame);

    final Global sentencePolarity = new Global("INTERESTING");
    sentencePolarity.setParam(String.format("%.2f", polaritySum));
    sentencePolarity.setText("The sentence polarity.");
    globalsSentencePolaritiesB.add(sentencePolarity);

    return frames;
  }

  /**
   * This method is used to look for the target of a shifter using dependency
   * relations. The found shifter target must be contained in the sentimentList
   * in order to be returned by this method. Ignores scopes.
   *
   * @param shifter The shifter for which a target is searched for.
   * @param sentimentList A list of found sentiments in the current sentence.
   * These are the potential shifter target candidates.
   * @param sentence The sentence the shifter is in.
   * @return The WordObj corresponding to the found shifter target, or null.
   */
  private WordObj findShifterTargetClauseBaseline(WordObj shifter, ArrayList<WordObj> sentimentList,
          SentenceObj sentence) {
    WordObj shifterTarget = null;
    ShifterUnit shifterUnit = shifterLex.getShifter(shifter.getLemma());

    HashSet<Edge> edges = sentence.getGraph().getEdges();

    String[] scopeEntry = shifterUnit.shifter_scope;
    // "Clause" case
    if (Arrays.asList(scopeEntry).contains("clause") || true) { // || true to use clause case for all instances.
      final ConstituencyTree tree = sentence.getTree();
      final Terminal shifterNode = tree.getTerminal(shifter);
      final Nonterminal containingClause;

      int shifterPos = sentence.getWordPosition(shifter);

      if (tree.hasDominatingNode(shifterNode, "S")) {
        containingClause = tree.getLowestDominatingNode(shifterNode, "S");
      } else {
        // This shouldn't happen except in case of parsing errors
        containingClause = tree.getTrueRoot();
      }

      // First collect all terminals in the containing clause (childrenT)
      ArrayList<Object> children = tree.getChildren(containingClause);
      ArrayList<Object> childrenT = new ArrayList<>();
      for (int i = 0; i < children.size(); i++) {
        Object child = children.get(i);
        // System.out.println("child: " + child.toString());
        if (child instanceof Terminal) {
          childrenT.add(child);
        } else if (child instanceof Nonterminal) {
          children.add(tree.getChildren((Nonterminal) child));
        } else {
          for (Object part : (ArrayList<Object>) child) {
            children.add(part);
          }
        }
      }
			// System.err.println("Children expanded: " + childrenNT);

      // Now consider all SE-terminals to be potential shifterTargets.
      HashMap<WordObj, Integer> shifterTargets = new HashMap<>();
      for (Object c : childrenT) {
        Terminal childT = null;
        if (c instanceof Terminal) {
          childT = (Terminal) c;
          // Compare Id with terminal Ids of the tree terminals to get to the
          // WordObjs.
          int wordIndex = 0;
          for (Terminal terminal : tree.getTerminals()) {
            wordIndex += 1;
            String terminalId = terminal.getId().getId();
            if (terminalId.equals(childT.getId().getId())) {
              WordObj wordObj = sentence.getWordList().get(wordIndex - 1);
              // Check if the word is a SE in the current sentence.
              if (sentimentList.contains(wordObj)) {
                shifterTargets.put(wordObj, wordIndex - 1);
              }
            }
          }
        } else {
          log.log(Level.SEVERE, "WARNING, Terminal expected for: {0}", c.toString());
        }
      }
      // Find the closest shifterTarget from the list
      log.log(Level.FINE, "Potential targets in clause case: {0}", shifterTargets.keySet().toString());
      int bestDistance = Integer.MAX_VALUE;
      if (!shifterTargets.isEmpty()) {
        for (WordObj shifterCandidate : shifterTargets.keySet()) {
          int distance = Math.abs(shifterPos - shifterTargets.get(shifterCandidate));
          if (distance < bestDistance && distance != 0) {
            bestDistance = distance;
            shifterTarget = shifterCandidate;
            log.log(Level.FINE, "current candidate: {0}", shifterTarget);
          }
        }
      }
      log.log(Level.FINE, "final candidate: {0}", shifterTarget);

      if (sentimentList.contains(shifterTarget) && !shifterTarget.equals(shifter)) {
        if (shifter_orientation_check) {
          if (orientationCheck(shifter, shifterTarget)) {
            return shifterTarget;
          }
        } else {
          return shifterTarget;
        }
      }
    }
    return null;
  }
}
