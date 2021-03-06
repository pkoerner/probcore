package de.prob.model.classicalb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DirectedMultigraph;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.AIdentifierExpression;
import de.be4.classicalb.core.parser.node.AImplementationMachineParseUnit;
import de.be4.classicalb.core.parser.node.AImportsMachineClause;
import de.be4.classicalb.core.parser.node.AMachineReference;
import de.be4.classicalb.core.parser.node.ARefinementMachineParseUnit;
import de.be4.classicalb.core.parser.node.ASeesMachineClause;
import de.be4.classicalb.core.parser.node.AUsesMachineClause;
import de.be4.classicalb.core.parser.node.PExpression;
import de.be4.classicalb.core.parser.node.PMachineReference;
import de.be4.classicalb.core.parser.node.Start;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.RefType;
import de.prob.model.representation.RefType.ERefType;

public class DependencyWalker extends DepthFirstAdapter {

	private final DirectedMultigraph<String, RefType> graph;
	private final String src;
	private final Map<String, Start> map;
	private final HashMap<String, AbstractElement> components;

	public DependencyWalker(final String machine,
			final HashMap<String, AbstractElement> comps,
			final DirectedMultigraph<String, RefType> graph,
			final Map<String, Start> map) {
		src = machine;
		this.components = comps;
		this.graph = graph;
		this.map = map;
	}

	@Override
	public void caseASeesMachineClause(final ASeesMachineClause node) {
		registerMachineNames(node.getMachineNames(), new RefType(ERefType.SEES));
	}

	@Override
	public void caseAUsesMachineClause(final AUsesMachineClause node) {
		registerMachineNames(node.getMachineNames(), new RefType(ERefType.USES));
	}

	@Override
	public void caseAImportsMachineClause(final AImportsMachineClause node) {
		LinkedList<PMachineReference> machineReferences = node
				.getMachineReferences();
		for (PMachineReference r : machineReferences) {
			String dest = extractMachineName(((AMachineReference) r)
					.getMachineName());
			ClassicalBMachine newMachine = makeMachine(dest);
			String name = newMachine.name();
			components.put(name, newMachine);
			graph.addVertex(name);
			graph.addEdge(src, name, new RefType(ERefType.IMPORTS));
		}
	}

	private ClassicalBMachine makeMachine(final String dest) {
		// FIXME: find the NodeIdAssignment and initialize the ClassicalBMachine
		// with it
		ClassicalBMachine dst = new ClassicalBMachine(null);
		DomBuilder builder = new DomBuilder(dst);
		Start start = map.get(dest);
		start.apply(builder);
		return dst;
	}

	@Override
	public void caseAMachineReference(final AMachineReference node) {
		String dest = extractMachineName(node.getMachineName());
		ClassicalBMachine newMachine = makeMachine(dest);
		String name = newMachine.name();
		components.put(name, newMachine);
		graph.addVertex(name);
		graph.addEdge(src, name, new RefType(ERefType.INCLUDES));
	}

	@Override
	public void outARefinementMachineParseUnit(
			final ARefinementMachineParseUnit node) {
		registerRefinementMachine(node.getRefMachine());
	}

	@Override
	public void outAImplementationMachineParseUnit(
			final AImplementationMachineParseUnit node) {
		registerRefinementMachine(node.getRefMachine());
	}

	private void registerRefinementMachine(final TIdentifierLiteral refMachine) {
		String dest = refMachine.getText();
		ClassicalBMachine newMachine = makeMachine(dest);
		String name = newMachine.name();
		components.put(name, newMachine);
		graph.addVertex(name);
		graph.addEdge(src, name, new RefType(ERefType.REFINES));
	}

	private void registerMachineNames(final List<PExpression> machineNames,
			final RefType depType) {
		for (PExpression machineName : machineNames) {
			if (machineName instanceof AIdentifierExpression) {
				AIdentifierExpression identifier = (AIdentifierExpression) machineName;
				String dest = extractMachineName(identifier.getIdentifier());
				ClassicalBMachine newMachine = makeMachine(dest);
				String name = newMachine.name();
				components.put(name, newMachine);
				graph.addVertex(name);
				graph.addEdge(src, name, depType);
			}
		}
	}

	private String extractMachineName(final LinkedList<TIdentifierLiteral> list) {
		return list.getLast().getText();
	}

}
