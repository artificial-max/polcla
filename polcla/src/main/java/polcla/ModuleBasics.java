package polcla;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import polcla.ConstituencyTree;
import polcla.SalsaAPIConnective;
import polcla.SentimentLex;
import polcla.SentimentUnit;
import polcla.ShifterLex;
import polcla.ShifterUnit;
import polcla.WordObj;

import salsa.corpora.elements.Fenode;
import salsa.corpora.elements.Frame;
import salsa.corpora.elements.Frames;
import salsa.corpora.elements.Global;
import salsa.corpora.elements.Graph;
import salsa.corpora.elements.Sentence;
import salsa.corpora.elements.Target;
import salsa.corpora.elements.Terminal;

/**
 * ModuleBasics provides helper methods that can be used by all Modules. E.g.
 * POS look-up, shifter orientation check, frame setting.
 *
 * @author Max
 */
public abstract class ModuleBasics {

  private final static Logger log = Logger.getLogger(ModuleBasics.class.getName());
  protected SalsaAPIConnective salsa;
  protected SentimentLex sentimentLex;
  protected ShifterLex shifterLex;
  protected Boolean posLookupSentiment;
  protected Boolean posLookupShifter;
  protected Boolean shifter_orientation_check;
  protected Boolean usePresetSELocations = false;
  protected ArrayList<String> missingInGermanLex = new ArrayList<String>();
  /**
   * Stores each sentence's polarity
   */
  protected Collection<Global> globalsSentencePolarities = new ArrayList<Global>();

  public ModuleBasics() {
    super();
    log.setLevel(Level.ALL);
  }

  /**
   * Globals are sentence flags. Stores each sentence's polarity.
   *
   * @return ArrayList<Global> globalsSentencePolarities
   */
  public Collection<Global> getGlobalsSentencePolarities() {
    return globalsSentencePolarities;
  }

  /**
   * Compares pos tags of found words in a sentence with shifter lexicon
   * entries.
   *
   * @param shifterList the list of found shifters in the current sentence.
   * @param word the current word's WordObj.
   * @param shifterLexEntry the entry for the shifter in the shifter lexicon.
   */
  public void posLookupShifter(ArrayList<WordObj> shifterList, WordObj word, ShifterUnit shifterLexEntry) {
    if ((word.getPos().startsWith("N") || word.getPos().equals("PIS")) && shifterLexEntry.shifter_pos.equals("nomen")) {
      shifterList.add(word);
    } else if (word.getPos().startsWith("ADJ") && shifterLexEntry.shifter_pos.equals("adj")) {
      shifterList.add(word);
    } else if (word.getPos().startsWith("ADV") && shifterLexEntry.shifter_pos.equals("adv")) {
      shifterList.add(word);
    } else if (word.getPos().startsWith("V") && shifterLexEntry.shifter_pos.equals("verb")) {
      shifterList.add(word);
    } else if (word.getPos().equals("PTKNEG")) {
      shifterList.add(word);
    } else if (word.getPos().equals("APPR") && shifterLexEntry.shifter_pos.equals("appr")) {
      shifterList.add(word);
    } else {
      ShifterUnit shifterLexEntryNew = shifterLex.getShifter(word.getName());
      if (shifterLexEntryNew != null && shifterLexEntry != shifterLexEntryNew) {
        posLookupShifter(shifterList, word, shifterLexEntryNew);
      } else {
        log.fine("Shifter POS-MISMATCH!");
        log.log(Level.FINE, "word: {0} pos: {1}", new Object[]{word.getName(), word.getPos()});
        log.log(Level.FINE, "shifterLex entry pos: {0}", shifterLexEntry.shifter_pos);
      }
    }
  }

  /**
   * Compares pos tags of found words in a sentence with sentiment lexicon
   * entries.
   *
   * @param sentimentList the list to which of found sentiments in the current
   * sentence are being added.
   * @param word the current word's WordObj.
   * @param sentLexEntry the entry for the sentiment word in the sentiment
   * lexicon.
   */
  public void posLookupSentiment(ArrayList<WordObj> sentimentList, WordObj word, SentimentUnit sentLexEntry) {
    if (word.getPos().startsWith("N") && sentLexEntry.pos.equals("nomen")) {
      sentimentList.add(word);
    } else if (word.getPos().startsWith("V") && sentLexEntry.pos.equals("verben")) {
      sentimentList.add(word);
    } else if (word.getPos().startsWith("A") && sentLexEntry.pos.equals("adj")) {
      sentimentList.add(word);
    } else {
      // Check for another possible sentLexEntry using the exact word instead of
      // its Lemma
      // Example: "abweisen" vs "abweisend" gets found this way.
      SentimentUnit sentLexEntryNew = sentimentLex.getSentiment(word.getName());
      if (sentLexEntryNew != null && sentLexEntry != sentLexEntryNew) {
        posLookupSentiment(sentimentList, word, sentLexEntryNew);
      } else {
        log.fine("Sentiment POS-MISMATCH!");
        log.log(Level.FINE, "word: {0} pos: {1}", new Object[]{word.getName(), word.getPos()});
        log.log(Level.FINE, "sentimentLex entry pos: {0}", sentLexEntry.pos);
      }
    }
  }

  /**
   * Checks whether the shifter orientation (on general/positive/negative)
   * matches the sentiment orientation (POS/NEG).
   *
   * @param shifter
   * @param shifterTarget
   * @return true if the orientations match or there is no lexicon entry for the
   * sentiment expression.
   */
  public Boolean orientationCheck(WordObj shifter, WordObj shifterTarget) {
    if (!shifter_orientation_check) {
      return true;
    }
    SentimentUnit shifterTargetUnit = sentimentLex.getSentiment(shifterTarget.getLemma());
    ShifterUnit shifterUnit = shifterLex.getShifter(shifter.getLemma());
    String shifterType = shifterUnit.getType(); // g,n,p

    // Can't compare the orientation if the sentiment expression does not have a
    // lexicon entry.
    if (shifterTargetUnit == null) {
      return true;
    }
    String sentimentType = shifterTargetUnit.getType(); // POS, NEG

    switch (shifterType) {
      case ShifterLex.SHIFTER_TYPE_GENERAL:
        return true;

      case ShifterLex.SHIFTER_TYPE_ON_NEGATIVE:
        if (sentimentType.equals("NEG")) {
          return true;
        } else {
          return false;
        }

      case ShifterLex.SHIFTER_TYPE_ON_POSITIVE:
        if (sentimentType.equals("POS")) {
          return true;
        } else {
          return false;
        }
    }
    return false;
  }

  /**
   * Helper method used to set Salsa Frames for subjective expressions.
   *
   * @param sentence The relevant sentence.
   * @param frames The Frame collection to be modified.
   * @param sentiment The relevant sentiment.
   * @param frame The Frame to be set.
   * @param target The target to set the Frame to.
   */
  protected void setFrames(SentenceObj sentence, final Collection<Frame> frames, WordObj sentiment, final Frame frame,
          final Target target) {
    target.addFenode(new Fenode(sentence.getTree().getTerminal(sentiment).getId()));

    // In case of mwe: add all collocations to the subjective expression
    // xml/frame
    SentimentUnit unit = sentimentLex.getSentiment(sentiment.getLemma());
    if (unit.mwe) {
      ArrayList<WordObj> matches = sentence.getGraph().getMweMatches(sentiment,
              new ArrayList<String>(Arrays.asList(unit.collocations)), true);
      for (WordObj match : matches) {
        target.addFenode(new Fenode(sentence.getTree().getTerminal(match).getId()));
      }
    }
    // add extra Fenode for particle of a verb if existent
    if (sentiment.getIsParticleVerb()) {
      WordObj particle = sentiment.getParticle();
      Fenode particleFeNode = new Fenode(sentence.getTree().getTerminal(particle).getId());
      if (!target.getFenodes().contains(particleFeNode)) {
        target.addFenode(particleFeNode);
      }
    }
    frame.setTarget(target);
    frames.add(frame);
  }

  /**
   * If preset SE locations are given, identifies and uses them. Adds sentiments
   * to the sentimentList.
   *
   * @param sentence The current sentence.
   * @param sentimentList List of subjective expressions in the current
   * sentence.
   */
  public void usePresetSELocations(SentenceObj sentence, ArrayList<WordObj> sentimentList) {
    ArrayList<Sentence> salsaSentences = salsa.getBody().getSentences();

    Sentence presetSentence = null;
    Graph presetGraph = null;
    ConstituencyTree presetTree = null;

    // Find the current sentence in the salsa corpus
    for (int i = 0; i < salsaSentences.size(); i++) {
      presetSentence = salsaSentences.get(i);
      presetGraph = presetSentence.getGraph();
      presetTree = new ConstituencyTree(presetGraph);
      if (sentence.getTree().toString().equals(presetTree.toString())) {
        break;
      }
    }

    // Collect the preset SEs
    ArrayList<Frames> presetFrames = presetSentence.getSem().getFrames();

    // First collect Ids of SE fenodes
    ArrayList<String> fenodeIds = new ArrayList<>();
    ArrayList<String> fenodeIdsMWE = new ArrayList<>();

    for (Frames allPresetFrames : presetFrames) {
      for (int i = 0; i < allPresetFrames.getFrames().size(); i++) {
        Frame presetFrame = allPresetFrames.getFrames().get(i);
        ArrayList<Fenode> fenodes = presetFrame.getTarget().getFenodes();
        if (fenodes.size() == 2) {
          for (Fenode fe : fenodes) {
            fenodeIdsMWE.add(fe.getIdref().getId());
          }
        } else {
          for (Fenode fe : fenodes) {
            fenodeIds.add(fe.getIdref().getId());
          }
        }
      }
    }

    // Compare fenodeIds with terminal Ids of the tree terminals to get to the
    // WordObjs.
    int wordIndex = 0;
    ArrayList<WordObj> particles = new ArrayList<WordObj>();

    for (Terminal terminal : presetTree.getTerminals()) {
      wordIndex += 1;
      String terminalId = terminal.getId().getId();
      for (String fenodeId : fenodeIds) {
        if (terminalId.equals(fenodeId)) {
          WordObj wordObj = sentence.getWordList().get(wordIndex - 1);
          // System.out.println("found preset SE: " + terminal.getWord());
          // System.out.println("with wordIndex: " + wordIndex);
          sentimentList.add(wordObj);
          // System.out.println("added given SE: " + wordObj);
          if (wordObj.getIsParticleVerb()) {
            particles.add(wordObj.getParticle());
          }
        }
      }
    }
    // In case of multi word expressions, things might be added twice.
    // Remove particles of particle words as they are already accounted for.
    sentimentList.removeAll(particles);

    // **************************CASE Particle MWE******************************
    // Compare fenodeIds with terminal Ids of the tree terminals to get to the
    // WordObjs.
    if (fenodeIdsMWE.size() == 2) {
      wordIndex = 0;
      int index = 0;
      WordObj wordObjFirst = null;
      WordObj wordObjSecond = null;

      for (Terminal terminal : presetTree.getTerminals()) {
        wordIndex += 1;
        String terminalId = terminal.getId().getId();
        for (String fenodeId : fenodeIdsMWE) {
          if (terminalId.equals(fenodeId)) {
            index++;
            if (fenodeIdsMWE.size() == 2) {
              if (index == 1) {
                wordObjFirst = sentence.getWordList().get(wordIndex - 1);
                wordObjFirst.setIsParticleVerb(true);
              }
              if (index == 2) {
                wordObjSecond = sentence.getWordList().get(wordIndex - 1);
                wordObjSecond.setIsParticleVerb(true);
              }
            }
          }
        }
        if (wordObjFirst != null && wordObjSecond != null && !sentimentList.contains(wordObjFirst)) {
          // System.out.println("added " + wordObjFirst + " with particle: " +
          // wordObjSecond);
          wordObjFirst.setParticle(wordObjSecond);
          sentimentList.add(wordObjFirst);
        }
      }
    }

    // Preset SEs might not have an entry as SentimentUnit in the SentimentLex,
    // with lemma, pos, value, etc.
    // Create dummy entries in those cases
    for (WordObj sentiment : sentimentList) {
      if (sentimentLex.getSentiment(sentiment.getLemma()) == null) {
        // System.out.println("no entry for: " + sentiment.getLemma());
        missingInGermanLex.add(sentiment.getLemma());
        SentimentUnit newUnit = new SentimentUnit(sentiment.getLemma(), "UNKNOWN", "0.0", sentiment.getPos(),
                Boolean.FALSE);
        sentimentLex.addSentiment(newUnit);
      }
    }
  }
}
