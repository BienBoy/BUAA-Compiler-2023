package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;
import Optimizer.CFG;

import java.util.*;

public class GCM extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			gcm(function);
		}
		new IrRenamer(module).rename();
	}

	private CFG graph;
	private DomTree domTree;
	private Map<Instruction, BasicBlock> latest;
	private Set<Instruction> visited;
	private Set<Instruction> toAdd;
	private void gcm(Function function) {
		graph = new CFG(function);
		domTree = new DomTree(graph);
		latest = new HashMap<>();

		List<Instruction> instructions = new ArrayList<>();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			instructions.addAll(basicBlock.getInstructions());
		}

		visited = new HashSet<>();
		for (Instruction instruction : instructions) {
			if (isPinnedInst(instruction)) {
				visited.add(instruction);
				for (Value value : instruction.getOperands()) {
					if (value instanceof Instruction) {
						scheduleEarly((Instruction) value);
					}
				}
			}
		}

		visited = new HashSet<>();
		for (Instruction instruction : instructions) {
			if (isPinnedInst(instruction)) {
				visited.add(instruction);
				for (Use use : instruction.getUseList()) {
					scheduleLate((Instruction) use.getUser());
				}
			}
		}
	}

	private boolean isPinnedInst(Instruction instruction) {
		return instruction instanceof Phi || instruction instanceof Br ||
				instruction instanceof Ret || instruction instanceof Call ||
				instruction instanceof GetInt || instruction instanceof Putint ||
				instruction instanceof Putstr || instruction instanceof Alloca ||
				instruction instanceof Load || instruction instanceof Getelementptr;
	}

	private void scheduleEarly(Instruction instruction) {
		if (visited.contains(instruction)) {
			return;
		}
		visited.add(instruction);

		BasicBlock block = domTree.root;

		for (Value value : instruction.getOperands()) {
			if (value instanceof Instruction) {
				scheduleEarly((Instruction) value);
				int d1 = domTree.getDomDepth(block);
				int d2 = domTree.getDomDepth(((Instruction) value).getBasicBlock());
				if (d1 < d2 && !isPinnedInst(instruction)) {
					block = ((Instruction) value).getBasicBlock();
				}
			}
		}

		if (!isPinnedInst(instruction)) {
			instruction.getBasicBlock().getInstructions().remove(instruction);
			int pos = 0;
			for (int i = 0; i < block.getInstructions().size(); i++) {
				Instruction ins = block.getInstructions().get(i);
				if (instruction.getOperands().contains(ins) ||
						ins instanceof Phi ||
						ins instanceof Alloca) {
					pos = i + 1;
				}
			}
			block.add(pos, instruction);
		}
	}

	private void scheduleLate(Instruction instruction) {
		if (visited.contains(instruction)) {
			return;
		}

		visited.add(instruction);
		BasicBlock lca = null;

		for (Use use : instruction.getUseList()) {
			scheduleLate((Instruction) use.getUser());
			BasicBlock block = ((Instruction) use.getUser()).getBasicBlock();
			if (use.getUser() instanceof Phi) {
				Phi phi = (Phi) use.getUser();
				for (int i = 0; i < phi.getOperands().size(); i+=2) {
					if (phi.getOperands().get(i + 1) == instruction) {
						block = (BasicBlock) phi.getOperands().get(i);
						lca = findLCA(lca, block);
					}
				}
			}
			lca = findLCA(lca, block);
		}

		latest.put(instruction, lca);
		selectBlock(instruction);
	}

	private BasicBlock findLCA(BasicBlock a, BasicBlock b) {
		if (a == null) {
			return b;
		}
		assert b != null;
		while (domTree.getDomDepth(a) > domTree.getDomDepth(b)) {
			a = graph.getImmediateDoms().get(a);
		}
		while (domTree.getDomDepth(b) > domTree.getDomDepth(a)) {
			b = graph.getImmediateDoms().get(b);
		}
		while (a != b) {
			a = graph.getImmediateDoms().get(a);
			b = graph.getImmediateDoms().get(b);
		}
		return a;
	}

	private void selectBlock(Instruction instruction) {
		if (isPinnedInst(instruction)) {
			return;
		}

		BasicBlock lca = latest.get(instruction);
		lca = lca == null ? instruction.getBasicBlock() : lca;
		BasicBlock best = lca;
		while (lca != instruction.getBasicBlock()) {
			if (domTree.getLoopDepth(lca) < domTree.getLoopDepth(best)) {
				best = lca;
			}
			lca = graph.getImmediateDoms().get(lca);
		}

		instruction.getBasicBlock().getInstructions().remove(instruction);
		int pos = 0;
		for (int i = 0; i < best.getInstructions().size(); i++) {
			Instruction ins = best.getInstructions().get(i);
			if (instruction.getOperands().contains(ins) ||
					ins instanceof Phi ||
					ins instanceof Alloca) {
				pos = i + 1;
			}
		}
		best.add(pos, instruction);
	}

	private static class DomTree {
		private CFG graph;
		private Map<BasicBlock, Set<BasicBlock>> children = new HashMap<>();
		private Map<BasicBlock, Integer> domDepth = new HashMap<>();
		private BasicBlock root;
		private Map<BasicBlock, Integer> loopDepth = new HashMap<>();

		private DomTree(CFG graph) {
			this.graph = graph;
			build();
			calculateDomDepth(root, 0);
			findLoop(root);
		}

		private void build() {
			Map<BasicBlock, BasicBlock> immediateDoms = graph.getImmediateDoms();
			for (BasicBlock basicBlock : immediateDoms.keySet()) {
				BasicBlock immediateDom = immediateDoms.get(basicBlock);
				if (!children.containsKey(immediateDom)) {
					children.put(immediateDom, new HashSet<>());
				}
				children.get(immediateDom).add(basicBlock);
			}
			root = graph.getFirst();
		}

		private void calculateDomDepth(BasicBlock basicBlock, int depth) {
			domDepth.put(basicBlock, depth);
			if (!children.containsKey(basicBlock)) {
				return;
			}
			for (BasicBlock child : children.get(basicBlock)) {
				calculateDomDepth(child, depth + 1);
			}
		}

		private int getDomDepth(BasicBlock basicBlock) {
			return domDepth.get(basicBlock);
		}

		private List<BasicBlock> visiting = new ArrayList<>();
		private void findLoop(BasicBlock basicBlock) {
			// 不具有普适性的一个方法
			if (basicBlock == null) {
				return;
			}
			if (!loopDepth.containsKey(basicBlock)) {
				loopDepth.put(basicBlock, 0);
			}
			visiting.add(basicBlock);
			if (children.containsKey(basicBlock)) {
				for (BasicBlock child : children.get(basicBlock)) {
					findLoop(child);
				}
			}
			for (BasicBlock next : graph.getNextNodes().get(basicBlock)) {
				if (visiting.contains(next)) {
					int pos = visiting.indexOf(next);
					for (int i = pos; i < visiting.size(); i++) {
						loopDepth.put(visiting.get(i), loopDepth.get(visiting.get(i)) + 1);
					}
					break;
				}
			}
			visiting.remove(basicBlock);
		}

		private int getLoopDepth(BasicBlock basicBlock) {
			return loopDepth.get(basicBlock);
		}
	}
}
