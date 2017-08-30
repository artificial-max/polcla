<h1> Polcla - A Polarity Classifier Incorporating Polarity Shifting for German</h1>

## Usage
```
# Clone this repository
$ git clone https://github.com/artificial-max/polcla.git

# Go into the repository
$ cd polcla/polcla

# Configure paths and properties
Open config/config.properties with a text editor of your choice.

# Install with maven
$ mvn install

# From the pocla/polcla directory, run the jar
java -jar target/polcla-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Preprocessing
This system expects the data to be processed to be already preprocessed by various NLP analyses (i.e. part-of-speech tagging, parsing and named-entity recognition).
The specific tools for this kind of preprocessing are not included in the release of this system.
However, you may find shellscripts for installing and running these tools in another project here:
https://github.com/miwieg/german-opinion-role-extractor

## Configuration File
```
# Example configuration

# Path specifications
TEXT_INPUT=src/main/resources/Input/raw_text.txt
DEPENDENCY_INPUT= src/main/resources/Input/dependency_parse.txt
CONSTITUENCY_INPUT=src/main/resources/Input/constituency_parse.xml
SENTIMENT_LEXICON_INPUT=src/main/resources/dictionaries/germanlex.txt
SHIFTER_LEXICON_INPUT=src/main/resources/dictionaries/shifter_lex.txt
INTENSIFIER_LEXICON_INPUT=src/main/resources/dictionaries/intensifier_lex.txt
OUTPUT=output/salsaResult.xml

###
# Optional path specifications
# If available, the position of subjective expressions in a sentence may be given to the system.
# The xml-file should be in the same format as the constituency parse file, with additional subjective expression frames.
# constituency_parse.preset_ses.xml provides an example.
# If not available, the sentiment lexicon (SENTIMENT_LEXICON_INPUT) is used to identify subjective expressions and their locations.
PRESET_SE_INPUT=src/main/resources/Input/constituency_parse.preset_ses.xml

# Option to use preset subjective expression information (TRUE), or to use
# the sentiment lexicon for subjective expression identification instead (FALSE).
# If no file is specified for PRESET_SE_INPUT, this option is meaningless.
# Default: TRUE
USE_PRESET_SE_INPUT=TRUE

# Option to also consider neutral entries of germanlex (labeled NEU).
# If set to TRUE, neutral expressions will be labeled as polar expressions,
# but won't affect sentence polarity.
# Default: FALSE
INCLUDE_NEUTRAL_EXPRESSIONS=FALSE
###

# Option for dependency parse normalization.
# Turns passive voice into active voice among other things. Should improve detection and classification.
# Default: TRUE
NORMALIZE=TRUE

# Option to consider POS tags for subjective expressions.
# Compares POS tags of words in a sentence with sentiment lexicon entries.
# If a mismatch between a lexicon entry's POS tag and a corresponding word is found, the word will not be considered to be a subjective
# expression.
# Only meaningful if USE_PRESET_SE_INPUT=FALSE.
# Default: TRUE
POS_LOOKUP_SENTIMENT=TRUE

# Option for pos lookup for shifters.
# The same rules as for POS_LOOKUP_SENTIMENT apply.
# Default: TRUE
POS_LOOKUP_SHIFTER=TRUE

# Option to take shifter orientation as given in the shifter lexicon (SHIFTER_LEXICON_INPUT) into account.
# Example:
    # Shifter: "heilen" with orientation "n" (on negative)" will only shift negative subjective expressions if this option is set to
    # TRUE.
# Default: FALSE = Higher Recall (and probably higher F-Score). TRUE = Higher Precision.
SHIFTER_ORIENTATION_CHECK=FALSE

###
# Baseline Module: Checks for shifter targets to the specified direction of a shifter within a given window-based scope.
# Default: FALSE
BASELINE_MODULE=FALSE
# The window size for shifter targets.
# Default size: 4
BASELINE_WINDOW=4
# The direction in which to search for shifter targets.
# Options: "LEFT", "RIGHT", "BOTH"
# Default: BOTH
BASELINE_DIRECTION=BOTH

# Baseline Rule Module: Checks for shifter targets without using specific dependency relations 
# filtered by lexicon scope entries, but instead by looking at all direct dependencies.
# Default: FALSE
BASELINE_RULE_MODULE=FALSE
###
```

## Usage/License
This software is licensed under the GNU GENERAL PUBLIC LICENSE.
Please refer to LICENSE.md for more detail.

If you intend to use this tool for your research, please acknowledge this software by citing by citing M. Wiegand, M. Wolf and J. Ruppenhofer (2017) 
(Full bibliographic information, see References.)

## Acknowledgements
This work was partially supported by the German Research Foundation (DFG) under grants RU 1873/2-1 and WI 4204/2-1.

This software package also includes the Java API to process the SALSA XML corpora:
http://www.coli.uni-saarland.de/projects/salsa/page.php?id=software
implemented by members of the SALSA-project at Saarland University.
The software package also includes the Zurcher Polart-sentiment lexicon (Klenner et al., 2009)
We thank the SALSA-project and the Department for Computational Linguistics at Zurich University (Manfred Klenner)
for letting us use and redistribute their resources.

## Contact Information
Maximilian Wolf                       email: M_S_Wolf@web.de

## References
Manfred Klenner, Angela Fahrni and Stefanos Petrakis
   "PolArt: A Robust Tool for Sentiment Analysis",
    in Proceedings of the Nordic Conference on Computational Linguistics (NoDaLiDa), pages 235-238, Odense, Denmark. 2009.
    
Michael Wiegand, Maximilian Wolf and Josef Ruppenhofer
  "Negation Modeling for German Polarity Classification", 
  in Proceedings of the German Society for Computational Linguistics and Language Technology (GSCL), Potsdam, Germany. 2017.
