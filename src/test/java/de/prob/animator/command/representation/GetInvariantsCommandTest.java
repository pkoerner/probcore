package de.prob.animator.command.representation;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import de.be4.classicalb.core.parser.analysis.prolog.NodeIdAssignment;
import de.prob.model.classicalb.ClassicalBEntity;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.StructuredPrologOutput;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.IntegerPrologTerm;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;

public class GetInvariantsCommandTest {

	@Test
	public void testWriteCommand()  {
		StructuredPrologOutput prologTermOutput = new StructuredPrologOutput();
		GetInvariantsCommand command = new GetInvariantsCommand(
				new NodeIdAssignment());
		command.writeCommand(prologTermOutput);
		prologTermOutput.fullstop().flush();
		Collection<PrologTerm> sentences = prologTermOutput.getSentences();
		PrologTerm next = sentences.iterator().next();

		assertNotNull(next);
		assertTrue(next instanceof CompoundPrologTerm);
		CompoundPrologTerm t = (CompoundPrologTerm) next;

		assertEquals("get_invariants", t.getFunctor());
		assertEquals(1, t.getArity());
		PrologTerm argument = t.getArgument(1);
		assertTrue(argument.isVariable());
	}

	@Test
	public void testProcessResult()  {
		@SuppressWarnings("unchecked")
		ISimplifiedROMap<String, PrologTerm> map = mock(ISimplifiedROMap.class);
		ListPrologTerm lpt = new ListPrologTerm(new CompoundPrologTerm("inv1",
				new CompoundPrologTerm("active /\\ waiting"),
				new IntegerPrologTerm(1)), new CompoundPrologTerm("inv2",
				new CompoundPrologTerm("active /\\ ready"),
				new IntegerPrologTerm(2)));
		when(map.get("LIST")).thenReturn(lpt);

		NodeIdAssignment nia = mock(NodeIdAssignment.class);
		when(nia.lookupById(anyInt())).thenReturn(null);
		GetInvariantsCommand command = new GetInvariantsCommand(nia);
		command.processResult(map);

		List<ClassicalBEntity> invariants = command.getInvariants();
		assertEquals("active /\\ waiting", invariants.get(0).getIdentifier());
		assertNull(invariants.get(0).getIdentifierExpression());
		assertEquals("active /\\ ready", invariants.get(1).getIdentifier());
		assertNull(invariants.get(1).getIdentifierExpression());
	}
}
