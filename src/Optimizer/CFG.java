package Optimizer;

import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.Instruction.Br;
import MidCode.LLVMIR.Instruction.Instruction;

import java.util.*;

public class CFG {
	private final Function function;
	private final LinkedHashMap<BasicBlock, Set<BasicBlock>> nextNodes;
	private final HashMap<BasicBlock, Set<BasicBlock>> preNodes;
	private final LinkedHashMap<BasicBlock, Set<BasicBlock>> doms;
	private final LinkedHashMap<BasicBlock, Set<BasicBlock>> strictDoms;
	private final LinkedHashMap<BasicBlock, BasicBlock> immediateDoms;
	private final LinkedHashMap<BasicBlock, Set<BasicBlock>> dominanceFrontier;

	public CFG(Function function) {
		this.function = function;
		nextNodes = new LinkedHashMap<>();
		preNodes = new LinkedHashMap<>();
		doms = new LinkedHashMap<>();
		strictDoms = new LinkedHashMap<>();
		immediateDoms = new LinkedHashMap<>();
		dominanceFrontier = new LinkedHashMap<>();
		build();
		calculateDom();
		calculateStrictDom();
		calculateImmediateDom();
		calculateDominanceFrontier();
	}

	private void build() {
		for (int i = 0 ;i < function.getBasicBlocks().size(); i++) {
			BasicBlock basicBlock = function.getBasicBlocks().get(i);
			HashSet<BasicBlock> next = new LinkedHashSet<>();
			for (Instruction instruction : basicBlock.getValues()) {
				if (instruction instanceof Br) {
					instruction.getOperands().forEach(value -> {
						if (value instanceof BasicBlock) {
							BasicBlock node = (BasicBlock) value;
							next.add(node);

							if (!preNodes.containsKey(node)) {
								preNodes.put(node, new LinkedHashSet<>());
							}
							preNodes.get(node).add(basicBlock);
						}
					});
				}
			}
			nextNodes.put(basicBlock, next);
		}
	}

	private void calculateDom() {
		doms.put(function.getBasicBlocks().get(0), new LinkedHashSet<>());
		doms.get(function.getBasicBlocks().get(0)).add(function.getBasicBlocks().get(0));
		boolean change = true;
		while (change) {
			change = false;
			for (int i = 1; i < function.getBasicBlocks().size(); i++) {
				BasicBlock basicBlock = function.getBasicBlocks().get(i);
				Set<BasicBlock> pres = preNodes.get(basicBlock);
				Set<BasicBlock> dom = new LinkedHashSet<>(function.getBasicBlocks());
				for (BasicBlock pre : pres) {
					if (doms.get(pre) == null) {
						continue;
					}
					// 求前驱dom的交集
					dom.retainAll(doms.get(pre));
				}
				dom.add(basicBlock);
				if (!dom.equals(doms.get(basicBlock))) {
					change = true;
					doms.put(basicBlock, dom);
				}
			}
		}
	}

	private void calculateStrictDom() {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			Set<BasicBlock> strictDom = new LinkedHashSet<>(doms.get(basicBlock));
			strictDom.remove(basicBlock);
			strictDoms.put(basicBlock, strictDom);
		}
	}

	private void calculateImmediateDom() {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (BasicBlock dom : strictDoms.get(basicBlock)) {
				boolean flag = true;
				for (BasicBlock dom2 : strictDoms.get(basicBlock)) {
					if (strictDoms.get(dom2).contains(dom)) {
						flag = false;
						break;
					}
				}
				if (flag) {
					immediateDoms.put(basicBlock, dom);
					break;
				}
			}
		}
	}

	private void calculateDominanceFrontier() {
		for (BasicBlock a : nextNodes.keySet()) {
			for (BasicBlock b : nextNodes.get(a)) {
				BasicBlock x = a;
				while (!strictDoms.get(b).contains(x)) {
					if (!dominanceFrontier.containsKey(x)) {
						dominanceFrontier.put(x, new LinkedHashSet<>());
					}
					dominanceFrontier.get(x).add(b);
					x = immediateDoms.get(x);
				}
			}
		}
	}

	public HashMap<BasicBlock, Set<BasicBlock>> getNextNodes() {
		return nextNodes;
	}

	public HashMap<BasicBlock, Set<BasicBlock>> getPreNodes() {
		return preNodes;
	}

	public HashMap<BasicBlock, Set<BasicBlock>> getDoms() {
		return doms;
	}

	public HashMap<BasicBlock, Set<BasicBlock>> getStrictDoms() {
		return strictDoms;
	}

	public HashMap<BasicBlock, BasicBlock> getImmediateDoms() {
		return immediateDoms;
	}

	public HashMap<BasicBlock, Set<BasicBlock>> getDominanceFrontier() {
		return dominanceFrontier;
	}

	public void print() {
		for (BasicBlock basicBlock : doms.keySet()) {
			for (BasicBlock next : nextNodes.get(basicBlock)) {
				System.out.println(basicBlock.getName() + "->" + next.getName());
			}
		}
	}
}
