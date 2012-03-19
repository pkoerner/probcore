package de.prob.animator.command;

import java.util.List;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;

/**
 * @author Jens Bendisposto
 * 
 */
public class GetPrintableAtomsList implements ICommand {

	private static final String PROLOG_VARIABLE = "L";
	private final String prologPredicate;
	private List<String> list;

	/**
	 * Executes the query: prologPredicate(L). Expects L to be a list of
	 * printable atoms
	 * 
	 * @param animator
	 * @param prologPredicate
	 * @return
	 * @throws ProBException
	 */

	public List<String> getList() {
		return list;
	}

	private GetPrintableAtomsList(final String prologPredicate) {
		this.prologPredicate = prologPredicate;
	}

	public void processResult(
			final ISimplifiedROMap<String, PrologTerm> bindings) {
		list = PrologTerm.atomicStrings((ListPrologTerm) bindings
				.get(PROLOG_VARIABLE));
	}

	public void writeCommand(final IPrologTermOutput pto) {
		pto.openTerm(prologPredicate);
		pto.printVariable(PROLOG_VARIABLE);
		pto.closeTerm();
	}

}
