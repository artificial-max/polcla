package polcla;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {

  private final static Logger log = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) {
    // System.setProperty("src.main.resources", "logging.properties");
    System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties");
    try {
      LogManager.getLogManager().readConfiguration();
    } catch (IOException | SecurityException e) {
      e.printStackTrace();
    }

    // Read config
    Properties prop = new Properties();
    java.nio.file.Path configPath = java.nio.file.Paths.get("config", "config.properties");
    System.out.println("Reading config from " + configPath.toString());
    log.log(Level.INFO, "Reading config from {0}", configPath.toString());
    try {
      InputStream configInput = new FileInputStream(configPath.toString());
      prop.load(configInput);

      String output = prop.getProperty("OUTPUT");
      String text_input = prop.getProperty("TEXT_INPUT");
      String dependency_input = prop.getProperty("DEPENDENCY_INPUT");
      String constituency_input = prop.getProperty("CONSTITUENCY_INPUT");
      String sentiment_lexicon_input = prop.getProperty("SENTIMENT_LEXICON_INPUT");
      String shifter_lexicon_input = prop.getProperty("SHIFTER_LEXICON_INPUT");
      String intensifier_lexicon_input = prop.getProperty("INTENSIFIER_LEXICON_INPUT");
      String preset_se_input = prop.getProperty("PRESET_SE_INPUT");
      String baseline_direction = prop.getProperty("BASELINE_DIRECTION");
      Boolean use_preset_se_input = Boolean.valueOf(prop.getProperty("USE_PRESET_SE_INPUT"));
      Boolean normalize = Boolean.valueOf(prop.getProperty("NORMALIZE"));
      Boolean window_baseline_module = Boolean.valueOf(prop.getProperty("WINDOW_BASELINE_MODULE"));
      Boolean clause_baseline_module = Boolean.valueOf(prop.getProperty("CLAUSE_BASELINE_MODULE"));
      Boolean pos_lookup_sentiment = Boolean.valueOf(prop.getProperty("POS_LOOKUP_SENTIMENT"));
      Boolean pos_lookup_shifter = Boolean.valueOf(prop.getProperty("POS_LOOKUP_SHIFTER"));
      Boolean shifter_orientation_check = Boolean.valueOf(prop.getProperty("SHIFTER_ORIENTATION_CHECK"));
      Boolean include_neutral_expressions = Boolean.valueOf(prop.getProperty("INCLUDE_NEUTRAL_EXPRESSIONS"));
      int baseline_window = Integer.valueOf(prop.getProperty("BASELINE_WINDOW"));

      // If more than one module is set to TRUE, warn the user and return.
      if (window_baseline_module && clause_baseline_module){
        System.err.println("Both baseline modules are set to TRUE at the same time. Please turn one module off.");
        return;
      }
      
      // Make sure that correct properties are set in case of baseline module.
      if (window_baseline_module) {
        if (baseline_window < 1) {
          System.err.println("n must be bigger than 0 for the baseline module to work!");
          System.err.println("Entered number: n=" + baseline_window);
          log.severe("n must be bigger than 0 for the baseline module to work!");
          log.log(Level.SEVERE, "Entered number: n={0}", baseline_window);
          return;
        }
        if (!(baseline_direction.equals("RIGHT") || baseline_direction.equals("LEFT")
                || baseline_direction.equals("BOTH"))) {
          System.err.println("baseline direction must either be \"LEFT\", \"RIGHT\", or \"BOTH\".");
          System.err.println("Given direction: " + baseline_direction);
          log.severe("Baseline direction must either be \"LEFT\", \"RIGHT\", or \"BOTH\".");
          log.log(Level.SEVERE, "Given direction: {0}", baseline_direction);
          return;
        }
      }

      // If no other module is turned on, use the standard
      // subjective expression module.
      Boolean subjective_expression_module = (window_baseline_module.equals(false) && clause_baseline_module.equals(false));

      // Only one module should be turned on at the same time.
      if (window_baseline_module && clause_baseline_module) {
        System.err.println("WARNING: Both Baseline Modules are turned on!");
        System.err.println("Check the config file!");
        log.warning("WARNING: Both Baseline Modules are turned on!");
        log.warning("Check the config file!");
      }

      // Read in raw input text and create SentenceList based on it.
      System.out.println("Reading raw text from : " + text_input);
      log.log(Level.INFO, "Reading raw text from : {0}", text_input);
      SentenceList sentences = new SentenceList();
      sentences.rawToSentenceList(text_input);

      // Read in dependency parse file and create a DependencyGraph object for
      // each sentence.
      System.out.println("Reading dependency data from " + dependency_input + "...");
      System.out.println("Creating dependency graph...");
      log.log(Level.INFO, "Reading dependency data from {0}...", dependency_input);
      log.info("Creating dependency graph...");
      sentences.readDependencyParse(dependency_input);

      // Normalize DependencyGraph objects.
      if (normalize) {
        System.out.println("Normalizing dependency graph...");
        log.info("Normalizing dependency graph...");
        sentences.normalizeDependencyGraphs();
      } else {
        System.out.println("Normalizing of dependency graph set to FALSE.");
        log.info("Normalizing of dependency graph set to FALSE.");
      }

      // Read in Salsa / Tiger XML file and create a ConstituencyTree object for
      // every sentence.
      System.out.println("Reading constituency data from " + constituency_input + "...");
      System.out.println("Creating constituency tree...");
      log.log(Level.INFO, "Reading constituency data from {0}...", constituency_input);
      log.info("Creating constituency tree...");
      SalsaAPIConnective salsa = new SalsaAPIConnective(constituency_input, sentences);

      // Read in sentiment lexicon.
      System.out.println("Reading sentiment lexicon from " + sentiment_lexicon_input + "...");
      log.log(Level.INFO, "Reading sentiment lexicon from {0}...", sentiment_lexicon_input);
      SentimentLex sentimentLex = new SentimentLex(true, include_neutral_expressions);
      sentimentLex.fileToLex(sentiment_lexicon_input);

      // Read in shifter lexicon.
      System.out.println("Reading shifter lexicon from " + shifter_lexicon_input + "...");
      log.log(Level.INFO, "Reading shifter lexicon from {0}...", shifter_lexicon_input);
      ShifterLex shifterLex = new ShifterLex(true);
      shifterLex.fileToLex(shifter_lexicon_input);

      // Read in intensifier lexicon.
      if (!intensifier_lexicon_input.isEmpty()) {
        System.out.println("Reading intensifier lexicon from " + shifter_lexicon_input + "...");
        log.log(Level.INFO, "Reading intensifier lexicon from {0}...", shifter_lexicon_input);
        IntensifierLex intensifierLex = new IntensifierLex(true);
        intensifierLex.fileToLex(intensifier_lexicon_input);
      }

      // Read in preset se file
      Boolean got_preset_se_file = false;
      if (!preset_se_input.isEmpty()) {
        if (use_preset_se_input) {
          System.out.println("Reading preset se file from " + preset_se_input + "...");
          log.log(Level.INFO, "Reading preset se file from {0}...", preset_se_input);
          SalsaAPIConnective salsa_preset = new SalsaAPIConnective(preset_se_input, sentences);
          got_preset_se_file = true;
          salsa = salsa_preset;
        } else {
          System.err.println("A preset se file path has been specified, but the option to use it is turned off.");
          System.err.println("See USE_PRESET_SE_INPUT in the configuration file.");
          log.severe("A preset se file path has been specified, but the option to use it is turned off.");
          log.severe("See USE_PRESET_SE_INPUT in the configuration file.");
        }
      } else if (use_preset_se_input) {
        System.err.println("USE_PRESET_SE_INPUT is set to TRUE but no preset-se-file-path was specified!");
        log.severe("USE_PRESET_SE_INPUT is set to TRUE but no preset-se-file-path was specified!");
        return;
      }

      // Look for subjective expressions and shifters using the according
      // modules.
      final Set<Module> modules = new HashSet<Module>();

      // Standard Module
      if (subjective_expression_module && got_preset_se_file && use_preset_se_input) {
        final SubjectiveExpressionModule subjectiveExpressionModule;
        subjectiveExpressionModule = new SubjectiveExpressionModule(salsa, sentimentLex, shifterLex,
                pos_lookup_sentiment, pos_lookup_shifter, shifter_orientation_check);
        modules.add(subjectiveExpressionModule);
      } else if (subjective_expression_module) {
        final SubjectiveExpressionModule subjectiveExpressionModule;
        subjectiveExpressionModule = new SubjectiveExpressionModule(sentimentLex, shifterLex, pos_lookup_sentiment,
                pos_lookup_shifter, shifter_orientation_check);
        modules.add(subjectiveExpressionModule);
      } else {
        System.err.println("Warning! Subjective Expression Module turned off!");
        log.severe("Warning! Subjective Expression Module turned off!");
      }

      // Baseline Module
      if (window_baseline_module && got_preset_se_file && use_preset_se_input) {
        final WindowBaseline baselineModule;
        baselineModule = new WindowBaseline(salsa, sentimentLex, shifterLex, baseline_window, baseline_direction,
                pos_lookup_sentiment, pos_lookup_shifter, shifter_orientation_check);
        modules.add(baselineModule);
      } else if (window_baseline_module) {
        final WindowBaseline baselineModule;
        baselineModule = new WindowBaseline(sentimentLex, shifterLex, baseline_window, baseline_direction,
                pos_lookup_sentiment, pos_lookup_shifter, shifter_orientation_check);
        modules.add(baselineModule);
      }

      // Baseline Rule Module
      if (clause_baseline_module && got_preset_se_file && use_preset_se_input) {
        final ClauseBaseline baselineRuleModule;
        baselineRuleModule = new ClauseBaseline(salsa, sentimentLex, shifterLex,
                pos_lookup_sentiment, pos_lookup_shifter, shifter_orientation_check);
        modules.add(baselineRuleModule);
      } else if (clause_baseline_module) {
        final ClauseBaseline baselineRuleModule;
        baselineRuleModule = new ClauseBaseline(sentimentLex, shifterLex, pos_lookup_sentiment,
                pos_lookup_shifter, shifter_orientation_check);
        modules.add(baselineRuleModule);
      }

      // Search for sentiment expressions and write results to the output file
      // specified in the configuration file
      final SentimentChecker sentcheck = new SentimentChecker(salsa, sentences, modules);
      System.out.println("Looking for sentiment expressions...");
      log.info("Looking for sentiment expressions...\n");

      sentcheck.findSentiments(output);
    } catch (FileNotFoundException e) {
      System.out.println("No config found at this config path: " + configPath);
      log.log(Level.SEVERE, "No config found at this config path: {0}", configPath);
    } catch (IOException e) {
      System.out.println("Could not read config file from " + configPath);
      log.log(Level.SEVERE, "Could not read config file from {0}", configPath);
    }
  }
}
