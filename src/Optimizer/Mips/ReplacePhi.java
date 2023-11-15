package Optimizer.Mips;

import MidCode.IrRenamer;
import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.Function;
import MidCode.LLVMIR.Instruction.*;
import MidCode.LLVMIR.IrModule;
import MidCode.LLVMIR.Value;
import Optimizer.BaseOptimizer;
import Optimizer.CFG;

import java.util.*;

public class ReplacePhi extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			replacePhi(function);
		}
		new IrRenamer(module).rename();
	}

	private void replacePhi(Function function) {
		CFG graph = new CFG(function);
		int num = function.getBasicBlocks().size(); // 新增的块不再分析
		for (int i = 1; i < num; i++) {
			BasicBlock basicBlock = function.getBasicBlocks().get(i);
			// 没有phi指令可以跳过
			if (!(basicBlock.getValues().get(0) instanceof Phi)) {
				continue;
			}

			Set<BasicBlock> pres = graph.getPreNodes().get(basicBlock);
			Map<BasicBlock, PC> pcs = new HashMap<>();
			for (BasicBlock pre : pres) {
				PC pc = new PC();
				pcs.put(pre, pc);
				if (graph.getNextNodes().get(pre).size() > 1) {
					BasicBlock newBlock = new BasicBlock(String.valueOf(function.getBasicBlocks().size()));
					function.getBasicBlocks().add(newBlock);
					newBlock.add(pc);
					newBlock.add(new Br(basicBlock));
					pre.replaceNext(basicBlock, newBlock);
				} else {
					// 在br语句前添加PC
					pre.add(pre.getValues().size() - 1, pc);
				}
			}

			for (int j = 0; j < basicBlock.getValues().size(); j++) {
				Instruction instruction = basicBlock.getValues().get(j);
				if (!(instruction instanceof Phi)) {
					break;
				}
				ArrayList<Value> operands = instruction.getOperands();
				for (int k = 0; k < operands.size(); k += 2) {
					PC pc = pcs.get((BasicBlock) operands.get(k));
					pc.addOperand(instruction, operands.get(k + 1));
				}
				// phi指令替换为empty
				Empty empty = new Empty();
				instruction.replaceUsed(empty);
				basicBlock.getValues().set(j, empty);
			}
		}

		// PC串行化
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (int i = 0; i < basicBlock.getValues().size(); i++) {
				Instruction instruction = basicBlock.getValues().get(i);
				if (instruction instanceof PC) {
					PC pc = (PC) instruction;
					basicBlock.replacePC(pc, pc.sequential());
				}
			}
		}
	}
}
