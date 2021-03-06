/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, Heinrich
 * Heine Universitaet Duesseldorf This software is licenced under EPL 1.0
 * (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package de.prob.animator.command.notImplemented;

import de.prob.animator.IAnimator;
import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.ICommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

public class ClearMachineCommand implements ICommand {

	private static final ClearCmd CLEAR_CMD = new ClearCmd();
	private final GetPrologRandomSeed getRandomSeed;
	private final ComposedCommand cmd;

	public ClearMachineCommand() {
		this.getRandomSeed = new GetPrologRandomSeed();
		this.cmd = new ComposedCommand(getRandomSeed, CLEAR_CMD);
	}

	public void clearMachine(final IAnimator animator)
			 {
		animator.execute(new ClearMachineCommand());
	}

	public void processResult(
			final ISimplifiedROMap<String, PrologTerm> bindings)
			 {
		cmd.processResult(bindings);
//		 FIXME: REFACTOR for new animator!
//		final Animator animator = Animator.getAnimator();
//		animator.setRandomSeed(getRandomSeed.getSeed());
	}

	public void writeCommand(final IPrologTermOutput pto)
			 {
		cmd.writeCommand(pto);
	}

	private final static class ClearCmd implements ICommand {
		public void processResult(
				final ISimplifiedROMap<String, PrologTerm> bindings)
				{
		}

		public void writeCommand(final IPrologTermOutput pto) {
			pto.openTerm("clear_loaded_machines").closeTerm();
		}
	}

}
