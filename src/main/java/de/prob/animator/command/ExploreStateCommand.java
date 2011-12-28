/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, Heinrich
 * Heine Universitaet Duesseldorf This software is licenced under EPL 1.0
 * (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package de.prob.animator.command;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.ProBException;
import de.prob.cli.StateError;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

public final class ExploreStateCommand implements ICommand {

	Logger logger = LoggerFactory.getLogger(ExploreStateCommand.class);

	private final String stateId;
	private final GetEnabledOperationsCommand getOpsCmd;
	private final GetStateValuesCommand getValuesCmd;
	private final CheckBooleanPropertyCommand checkInitialisedCmd;
	private final CheckBooleanPropertyCommand checkInvCmd;
	private final CheckBooleanPropertyCommand checkMaxOpCmd;
	private final CheckBooleanPropertyCommand checkTimeoutCmd;
	private final GetStateBasedErrorsCommand getStateErrCmd;
	private final ComposedCommand allCommands;
	private final GetOperationsWithTimeout checkTimeoutOpsCmd;

	private boolean initialised;

	private boolean invariantOk;

	private boolean timeoutOccured;

	private boolean maxOperationsReached;

	private List<OpInfo> enabledOperations;

	private List<Variable> variables;

	private Collection<StateError> stateErrors;

	private Set<String> timeouts;

	public ExploreStateCommand(final String stateID) {
		stateId = stateID;
		getOpsCmd = new GetEnabledOperationsCommand(stateId);
		getValuesCmd = new GetStateValuesCommand(stateId);
		checkInitialisedCmd = new CheckInitialisationStatusCommand(stateId);
		checkInvCmd = new CheckInvariantStatusCommand(stateId);
		checkMaxOpCmd = new CheckMaxOperationReachedStatusCommand(stateId);
		checkTimeoutCmd = new CheckTimeoutStatusCommand(stateId);
		checkTimeoutOpsCmd = new GetOperationsWithTimeout(stateId);
		getStateErrCmd = new GetStateBasedErrorsCommand(stateId);
		this.allCommands = new ComposedCommand(getOpsCmd, getValuesCmd,
				checkInitialisedCmd, checkInvCmd, checkMaxOpCmd,
				checkTimeoutCmd, checkTimeoutOpsCmd, getStateErrCmd);

	}

	public String getStateID() {
		return stateId;
	}

	@Override
	public void processResult(
			final ISimplifiedROMap<String, PrologTerm> bindings)
			throws ProBException {
		allCommands.processResult(bindings);

		initialised = checkInitialisedCmd.getResult();
		invariantOk = checkInvCmd.getResult();
		timeoutOccured = checkTimeoutCmd.getResult();
		maxOperationsReached = checkMaxOpCmd.getResult();
		enabledOperations = getOpsCmd.getEnabledOperations();
		variables = getValuesCmd.getResult();
		stateErrors = getStateErrCmd.getResult();

		if (!initialised && enabledOperations.isEmpty() && !timeoutOccured) {
			logger.error("ProB could not find valid constants. This might be caused by the animation settings (e.g., Integer range or deferred set size) or by an inconsistency in the axioms");
		}

		timeouts = new HashSet<String>(checkTimeoutOpsCmd.getTimeouts());
	}

	@Override
	public void writeCommand(final IPrologTermOutput pto) throws ProBException {
		allCommands.writeCommand(pto);
	}

	public boolean isInitialised() {
		return initialised;
	}

	public boolean isInvariantOk() {
		return invariantOk;
	}

	public boolean isTimeoutOccured() {
		return timeoutOccured;
	}

	public boolean isMaxOperationsReached() {
		return maxOperationsReached;
	}

	public List<OpInfo> getEnabledOperations() {
		return enabledOperations;
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public Collection<StateError> getStateErrors() {
		return stateErrors;
	}

	public Set<String> getTimeouts() {
		return timeouts;
	}
}