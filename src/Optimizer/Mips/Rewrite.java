package Optimizer.Mips;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.Call;
import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.Instruction.Load;
import MidCode.LLVMIR.Instruction.PushStack;
import Optimizer.BaseOptimizer;

/**
 * 改写LLVM IR，便于后续的寄存器分配。
 * 主要改写内容为：每次使用函数参数前，先进行Load；
 * 函数调用前添加压栈操作
 */
public class Rewrite extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			rewrite(function);
		}
		new IrRenamer(module).rename();
	}

	public void rewrite(Function function) {
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			for (int i = 0; i < basicBlock.getInstructions().size(); i++) {
				Instruction instruction = basicBlock.getInstructions().get(i);
				for (int j = 0; j < instruction.getOperands().size(); j++) {
					Value operand = instruction.getOperands().get(j);
					if (operand instanceof FunctionParam) {
						Load load = new Load("%-1", operand);
						basicBlock.add(i++, load);
						instruction.getOperands().set(j, load);
					}
				}

				if (instruction instanceof Call) {
					for (int j = 1; j < instruction.getOperands().size(); j++) {
						Value operand = instruction.getOperands().get(j);
						basicBlock.add(i++, new PushStack(operand));
					}
					// 清空参数
					Value temp = instruction.getOperands().get(0);
					instruction.getOperands().clear();
					instruction.getOperands().add(temp);
				}
			}
		}
	}
}
