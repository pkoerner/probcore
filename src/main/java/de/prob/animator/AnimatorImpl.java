package de.prob.animator;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.inject.Inject;

import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.GetErrorsCommand;
import de.prob.animator.command.ICommand;
import de.prob.cli.ProBInstance;
import de.prob.exception.CliException;
import de.prob.exception.UnexpectedResultException;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.term.PrologTerm;

class AnimatorImpl implements IAnimator {

	private final ProBInstance cli;
	private final Logger logger = LoggerFactory.getLogger(AnimatorImpl.class);
	private final CommandProcessor processor;
	private final GetErrorsCommand getErrors;

	@Inject
	public AnimatorImpl(@Nullable final ProBInstance cli,
			final CommandProcessor processor, final GetErrorsCommand getErrors) {
		this.cli = cli;
		this.processor = processor;
		this.getErrors = getErrors;
		processor.configure(cli, logger);
	}

	@Override
	public void execute(final ICommand command) {
		if (cli == null) {
			// System.out.println("Probcli is missing. Try \"upgrade\".");
			logger.error("Probcli is missing. Try \"upgrade\".");
			throw new CliException("no cli found");
		}
		ISimplifiedROMap<String, PrologTerm> bindings = null;
		try {
			bindings = processor.sendCommand(command);
			command.processResult(bindings);
		} catch (RuntimeException e) {
			logger.error("Runtime error while executing query.", e);
			throw new ProBException(e);
		} finally {
			getErrors();
		}
	}

	private void getErrors() {
		ISimplifiedROMap<String, PrologTerm> errorbindings;
		List<String> errors;
		try {
			errorbindings = processor.sendCommand(getErrors);
			getErrors.processResult(errorbindings);
		} catch (RuntimeException e) {
			logger.error("Runtime error while executing query.", e);
			throw new ProBException(e);
		}
		errors = getErrors.getErrors();
		if (errors != null && !errors.isEmpty()) {
			String msg = Joiner.on('\n').join(errors);
			logger.error("ProB raised exception(s):\n", msg);
			throw new ProBException(msg);
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(AnimatorImpl.class).addValue(cli)
				.toString();
	}

	@Override
	public void execute(final ICommand... commands) throws ProBException,
			UnexpectedResultException {
		execute(new ComposedCommand(commands));
	}

	@Override
	public void sendInterrupt() {
		cli.sendInterrupt();
	}

}
