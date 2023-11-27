package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;
import Optimizer.CFG;

import java.util.*;

public class GCMWithBugs extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			gcm(function);
		}
		new IrRenamer(module).rename();
	}

	private CFG graph;
	private DomTree domTree;
	private Map<Instruction, BasicBlock> selected;
	private Map<Instruction, BasicBlock> latest;
	private Set<Instruction> visited;
	private Set<Instruction> toAdd;
	private void gcm(Function function) {
		graph = new CFG(function);
		domTree = new DomTree(graph);
		selected = new HashMap<>();
		latest = new HashMap<>();

		// 初始化
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				selected.put(instruction, basicBlock);
			}
		}

		visited = new HashSet<>();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (isPinnedInst(instruction)) {
					visited.add(instruction);
					for (Value value : instruction.getOperands()) {
						if (value instanceof Instruction) {
							scheduleEarly((Instruction) value);
						}
					}
				}
			}
		}

		visited = new HashSet<>();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (isPinnedInst(instruction)) {
					visited.add(instruction);
					for (Use use : instruction.getUseList()) {
						scheduleLate((Instruction) use.getUser());
					}
				}
			}
		}

		visited = new HashSet<>();
		toAdd = new LinkedHashSet<>();
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (Instruction instruction : basicBlock.getInstructions()) {
				if (!visited.contains(instruction)) {
					visited.add(instruction);
					for (Value value : instruction.getOperands()) {
						if (value instanceof Instruction) {
							schedule((Instruction) value);
						}
					}
					toAdd.add(instruction);
				}
			}
		}
		for (Instruction instruction : toAdd) {
			instruction.getBasicBlock().getInstructions().remove(instruction);
			selected.get(instruction).add(instruction);
		}
		for (Instruction instruction : toAdd) {
			if (instruction instanceof Br) {
				instruction.getBasicBlock().getInstructions().remove(instruction);
				selected.get(instruction).add(instruction);
			} else if (instruction instanceof Phi) {
				instruction.getBasicBlock().getInstructions().remove(instruction);
				selected.get(instruction).add(0, instruction);
			}
		}
	}

	private boolean isPinnedInst(Instruction instruction) {
		return instruction instanceof Phi || instruction instanceof Br ||
				instruction instanceof Ret || instruction instanceof Call ||
				instruction instanceof GetInt || instruction instanceof Putint ||
				instruction instanceof Putstr;
	}

	private void scheduleEarly(Instruction instruction) {
		if (visited.contains(instruction)) {
			return;
		}
		visited.add(instruction);

		if (!isPinnedInst(instruction)) {
			selected.put(instruction, domTree.root);
		}

		for (Value value : instruction.getOperands()) {
			if (value instanceof Instruction) {
				scheduleEarly((Instruction) value);
				int d1 = domTree.getDomDepth(selected.get(instruction));
				int d2 = domTree.getDomDepth(selected.get(value));
				if (d1 < d2 && !isPinnedInst(instruction)) {
					selected.put(instruction, selected.get(value));
				}
			}
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
			BasicBlock block = selected.get((Instruction) use.getUser());
			if (use.getUser() instanceof Phi) {
				Phi phi = (Phi) use.getUser();
				for (int i = 0; i < phi.getOperands().size(); i+=2) {
					if (phi.getOperands().get(i + 1) == instruction) {
						block = (BasicBlock) phi.getOperands().get(i);
						if (!isPinnedInst(instruction)) {
							lca = findLCA(lca, block);
						}
					}
				}
			}
			if (!isPinnedInst(instruction)) {
				lca = findLCA(lca, block);
			}
		}
		if (!isPinnedInst(instruction)) {
			latest.put(instruction, lca);
			selectBlock(instruction);
		}
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
		BasicBlock lca = latest.get(instruction);
		lca = lca == null ? selected.get(instruction) : lca;
		BasicBlock best = lca;
		while (lca != selected.get(instruction)) {
			if (domTree.getLoopDepth(lca) < domTree.getLoopDepth(best)) {
				best = lca;
			}
			lca = graph.getImmediateDoms().get(lca);
		}
		selected.put(instruction, best);
	}

	private void schedule(Instruction instruction) {
		if (visited.contains(instruction)) {
			return;
		}
		visited.add(instruction);

		for (Value value : instruction.getOperands()) {
			if (value instanceof Instruction) {
				schedule((Instruction) value);
			}
		}
		toAdd.add(instruction);
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
