package polcla;

import salsa.corpora.elements.Semantics;
import salsa.corpora.elements.Flags;
import salsa.corpora.elements.Meta;
import salsa.corpora.elements.Frames;
import salsa.corpora.elements.Flag;
import salsa.corpora.elements.Frame;
import salsa.corpora.elements.Globals;
import salsa.corpora.elements.CorpusId;
import salsa.corpora.elements.Global;
import salsa.corpora.elements.Element;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks for sentiment expressions in every {@link SentenceObj} and adds the
 * sentiment information to the Tiger XML document.
 *
 */
public class SentimentChecker {

  private final static Logger log = Logger.getLogger(SentimentChecker.class.getName());
  private final SalsaAPIConnective salsaCon;
  private final SentenceList list;
  private final Set<Module> modules;

  /**
   *
   * @param salsaCon A {@link SalsaAPIConnective} object used to add the
   * sentiment information to the Tiger XML corpus.
   * @param modules The modules that will be used to find sentiment expressions
   * @param list
   * <!--TODO-->
   */
  public SentimentChecker(SalsaAPIConnective salsaCon, SentenceList list, Set<Module> modules) {
    log.setLevel(Level.ALL);
    this.salsaCon = salsaCon;
    this.list = list;
    this.modules = modules;
  }

  /**
   * Calls the <code>findFrames</code> method of each enabled {@link Module} and
   * combines their output into a single {@link Semantics} object.
   *
   * @param sentence The {@link SentenceObj} that will be passed to each module
   * @return A {@link Semantics} object
   */
  private Semantics findSentiment(SentenceObj sentence) {
    /*
     * The SALSA API and SALTO can handle multiple Frames objects in one
     * sentence but evaltool can't. Thus we merge all frames into a single
     * Frames object.
     */
    final Frames frames = new Frames();
    final Globals globals = new Globals();
    for (Module module : modules) {

      for (Frame frame : module.findFrames(sentence)) {
        frames.addFrame(frame);
      }
      for (Global global : module.getGlobalsSentencePolarities()) {
        globals.addGlobal(global);
      }
    }
    final Semantics sem = new Semantics();
    sem.addFrames(frames);
    sem.addGlobals(globals);
    return sem;
  }

  /**
   * Calls {@link #findSentiment(SentenceObj)} for every {@link SentenceObj} in
   * {@link SentenceList} and exports the Salsa XML structure to filename. Also
   * adds general specification of frames to the Salsa XML structure.
   *
   * @param filename The path of the output file.
   */
  public void findSentiments(String filename) {

    Frames hframes = new Frames();
    Frame f1 = new Frame("SubjectiveExpression");
    Element e1 = new Element("Shifter", "true");
    f1.addElement(e1);
    hframes.addFrame(f1);
    Global global = new Global("Polarity");
    Flags hflags = new Flags();
    Flag hflag1 = new Flag("Polarity without shift", "subjExpr");
    Flag hflag2 = new Flag("Polarity after shift", "subjExpr");
    Flag hflag3 = new Flag("Type", "shifter");
    Flag polarityFlag = new Flag("Polarity", "sentence");
    hflags.addFlag(hflag1);
    hflags.addFlag(hflag2);
    hflags.addFlag(hflag3);
    hflags.addFlag(polarityFlag);
    this.salsaCon.getHead().setFlags(hflags);
    this.salsaCon.getHead().setFrames(hframes);

    // Set meta: corpusId is necessary for the evaluation tool
    CorpusId corpusId = new CorpusId();
    corpusId.setId("1");
    Meta meta = new Meta();
    meta.setCorpus_id(corpusId);
    this.salsaCon.getHead().setMeta(meta);

    int listSize = list.sentenceList.size();

    for (int i = 0; i < listSize; i++) {
      SentenceObj sentence = list.sentenceList.get(i);
      log.log(Level.INFO, sentence.toString());
      Semantics sem = findSentiment(sentence);
      this.salsaCon.getSentences().get(i).setSem(sem);
      String msg = "Sentence " + (i + 1) + " of " + list.sentenceList.size() + " done.";
      System.out.println(msg);
      log.log(Level.INFO, "{0}\n", msg);
    }

    System.out.println((list.sentenceList.size()) + " sentences have been analysed successfully.");
    // Statistics for logging.
    try {
      SubjectiveExpressionModule m = ((SubjectiveExpressionModule) modules.toArray()[0]);
      String result = String.format(
              "obja: %d, objp: %d, subj: %d, gmod: %d, governor: %d, objd: %d, clause: %d, ohne: %d, attr-rev: %d, objg: %d, obji: %d, dependent: %d",
              m.obja, m.objp, m.subj, m.gmod, m.governor, m.objd, m.clause, m.ohne, m.attr_rev, m.objg, m.obji, m.dependent);
      log.fine(result);
    } catch (ClassCastException e) {

    }

    MyFileWriter writer = new MyFileWriter(filename);
    try {
      writer.writeToFile(this.salsaCon.getCorpus().toString());
    } catch (IOException e) {

      e.printStackTrace();
    }
  }
}
