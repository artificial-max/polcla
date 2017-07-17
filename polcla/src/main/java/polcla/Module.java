package polcla;

import salsa.corpora.elements.Frame;
import salsa.corpora.elements.Global;

import java.util.Collection;

/**
 * @author Erik Hahn
 *
 */
public interface Module {
	
	/**
	 * Globals are sentence flags. Stores each sentence's polarity.
	 * @return ArrayList<Global> globalsSentencePolarities
	 */
	 public Collection<Global> getGlobalsSentencePolarities();
	
	/**
	 *
	 * @param sentence
	 * @return A collection of {@link salsa.corpora.elements.Frame} objects. Each
	 *         represents a SubjectiveExpression. Each implementation of this
	 *         interface is expected to implement only a single strategy for
	 *         finding sentiment expressions. Their results are combined by
	 *         {@link SentimentChecker}.
	 */
	Collection<Frame> findFrames(SentenceObj sentence);
}
