/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, Heinrich
 * Heine Universitaet Duesseldorf This software is licenced under EPL 1.0
 * (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package de.prob.animator.command.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.animator.command.CheckInitialisationStatusCommand;
import de.prob.animator.command.CheckInvariantStatusCommand;
import de.prob.animator.command.CheckMaxOperationReachedStatusCommand;
import de.prob.animator.command.CheckTimeoutStatusCommand;
import de.prob.animator.command.ICommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.parser.ResultParserException;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

/**
 * Command to retrieve a state property. Most likely you rather want one of the
 * specialized versions {@link CheckInitialisationStatusCommand},
 * {@link CheckInvariantStatusCommand} ,
 * {@link CheckMaxOperationReachedStatusCommand} or
 * {@link CheckTimeoutStatusCommand}
 */
public class CheckBooleanPropertyCommand implements ICommand {

	private final Logger logger = LoggerFactory
			.getLogger(CheckBooleanPropertyCommand.class);

	private static final String PROP_RESULT = "PropResult";

	private final String stateId;
	private final String propertyName;
	private Boolean result;

	protected CheckBooleanPropertyCommand(final String propertyName,
			final String stateId) {
		this.propertyName = propertyName;
		this.stateId = stateId;
	}

	@Override
	public void processResult(
			final ISimplifiedROMap<String, PrologTerm> bindings) {

		String functor = bindings.get(PROP_RESULT).getFunctor();
		checkIfBoolean(functor);
		result = Boolean.valueOf(functor);
	}

	private void checkIfBoolean(final String functor) {
		if (!"true".equals(functor) && !"false".equals(functor)) {
			result = null;
			logger.error("Expected true or false, but was: {}", functor);
			throw new ResultParserException("Expected true or false, but was: "+functor,null);
		}
	}

	private static void writeCommand(final IPrologTermOutput pto,
			final String propertyName, final String stateId) {
		pto.openTerm("state_property");
		pto.printAtom(propertyName);
		pto.printAtomOrNumber(stateId);
		pto.printVariable(PROP_RESULT);
		pto.closeTerm();
	}

	@Override
	public void writeCommand(final IPrologTermOutput pto) {
		writeCommand(pto, propertyName, stateId);
	}

	public boolean getResult() {
		if (result == null)
			throw new IllegalStateException(
					"Cannot get result before finishing query");
		return result;
	}

}
