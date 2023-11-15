package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;
import Optimizer.CFG;

import java.util.*;

public class Mem2reg extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			CFG graph = new CFG(function);
			insertPhi(function, graph);
			renameVar(function, graph);
		}
		new IrRenamer(module).rename();
	}

	private void insertPhi(Function function, CFG graph) {
		// 获取所有变量
		BasicBlock firstBlock = function.getBasicBlocks().get(0);
		ArrayList<Alloca> vars = new ArrayList<>();
		for (Instruction i : firstBlock.getValues()) {
			if (i instanceof Alloca && ((Alloca) i).isLocalVariable()) {
				vars.add((Alloca) i);
				continue;
			}
		}

		for (Alloca var : vars) {
			// 获取含有var定义的基本块
			ArrayList<BasicBlock> defs = new ArrayList<>();
			for (BasicBlock b : function.getBasicBlocks()) {
				for (Instruction i : b.getValues()) {
					if (i instanceof Store && i.getOperands().contains(var)) {
						defs.add(b);
						break;
					}
				}
			}

			// 插入phi语句
			Set<BasicBlock> F = new HashSet<>();
			Set<BasicBlock> W = new LinkedHashSet<>(defs);
			while (!W.isEmpty()) {
				BasicBlock X = W.iterator().next();
				W.remove(X);
				if (graph.getDominanceFrontier().get(X) == null) {
					continue;
				}
				for (BasicBlock Y : graph.getDominanceFrontier().get(X)) {
					if (F.contains(Y)) {
						continue;
					}
					// 先插入phi指令，参数后续再填入
					Phi phi = new Phi(var);
					Y.add(0, phi);
					F.add(Y);
					if (!defs.contains(Y)) {
						W.add(Y);
					}
				}
			}
		}
		// 必须预先插入phi指令，再填充其参数：插入的phi指令可能作为其他phi指令的参数
		fillPhiParams(function, graph);
	}

	private void fillPhiParams(Function function, CFG graph) {
		for (BasicBlock b : function.getBasicBlocks()) {
			for (Instruction i : b.getValues()) {
				if (i instanceof Phi) {
					ArrayList<Value> choices = new ArrayList<>();
					Set<BasicBlock> pres = graph.getPreNodes().get(b);
					for (BasicBlock pre : pres) {
						choices.add(pre);
						choices.add(findDef(((Phi) i).getVar(), pre, graph));
					}
					i.addOperand(choices.toArray(new Value[0]));
				}
			}
		}
	}

	private void renameVar(Function function, CFG graph) {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			ArrayList<Instruction> instructions = basicBlock.getValues();
			Iterator<Instruction> iterator = instructions.iterator();
			while (iterator.hasNext()) {
				Instruction instruction = iterator.next();
				if (instruction instanceof Alloca && ((Alloca) instruction).isLocalVariable()) {
					// 对非数组的Alloca可以直接移除
					instruction.removeUse();
					iterator.remove();
				} else if (instruction instanceof Load && ((Load) instruction).isLocalVariable()) {
					// 对于非数组的Load，取消内存读取
					instruction.replaceUsed(findDef(instruction,
							(Alloca) ((Load) instruction).getVar(), basicBlock, graph));
					iterator.remove();
				}
			}
		}

		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			ArrayList<Instruction> instructions = basicBlock.getValues();
			Iterator<Instruction> iterator = instructions.iterator();
			while (iterator.hasNext()) {
				Instruction instruction = iterator.next();
				if (instruction instanceof Store && ((Store) instruction).isLocalVariable()) {
					// 对于非数组的Store，将对其的使用替换为要更新的值
					instruction.replaceUsed(((Store) instruction).getNewValue());
					iterator.remove();
				}
			}
		}
	}

	private Value findDef(Alloca var, BasicBlock basicBlock, CFG graph) {
		return findDef(null, var, basicBlock, graph);
	}

	private Value findDef(Value from, Alloca var, BasicBlock basicBlock, CFG graph) {
		ArrayList<Instruction> instructions = basicBlock.getValues();
		boolean flag = from != null;
		for (int i = instructions.size() - 1; i >= 0; i--) {
			if (flag) {
				if (instructions.get(i).equals(from)) {
					flag = false;
				}
				continue;
			}
			Instruction instruction = instructions.get(i);
			if (instruction instanceof Store && instruction.getOperands().contains(var)) {
				return instruction;
			}
			if (instruction instanceof Phi && ((Phi) instruction).getVar().equals(var)) {
				return instruction;
			}
		}

		if (graph.getImmediateDoms().get(basicBlock) == null) {
			return new Undef();
		}
		return findDef(var, graph.getImmediateDoms().get(basicBlock), graph);
	}
}
