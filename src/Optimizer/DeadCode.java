package Optimizer;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;

import java.util.ArrayList;

public class DeadCode extends BaseOptimizer {
	private boolean change = true;
	@Override
	public void optimize(IrModule module) {
		while (change) {
			change = false;

			module.getGlobalVariables().removeIf(v-> {
				if (v.getUseList().isEmpty()) {
					change = true;
					return true;
				}
				return false;
			});

			module.getConstStrings().removeIf(s-> {
				if (s.getUseList().isEmpty()) {
					change = true;
					return true;
				}
				return false;
			});

			module.getFunctions().removeIf(f-> {
				if (f.getUseList().isEmpty() && !f.getRawName().equals("main")) {
					change = true;
					return true;
				}
				return false;
			});

			for (Function function : module.getFunctions()) {
				removeDeadInstructions(function);
			}
		}
		new IrRenamer(module).rename();
	}

	private void removeDeadInstructions(Function function) {
		// 删除不可达代码（未使用的块）
		function.getBasicBlocks().removeIf(b-> {
			if (!b.equals(function.getBasicBlocks().get(0)) &&
					b.getUseList().isEmpty()) {
				change = true;
				b.getValues().forEach(User::removeUse);
				return true;
			}
			return false;
		});

		// 删除每个块内未被使用的指令
		for (BasicBlock basicBlock : function.getBasicBlocks()) {
			removeDeadInstructions(basicBlock);
		}
	}

	private void removeDeadInstructions(BasicBlock basicBlock) {
		// 遍历删除计算结果未被使用的指令
		basicBlock.getValues().removeIf(v->{
			if (v.getUseList().isEmpty() && (v instanceof Add ||
					v instanceof Sub || v instanceof Mul || v instanceof Sdiv ||
					v instanceof Srem || v instanceof Icmp || v instanceof Zext ||
					v instanceof Load || v instanceof Alloca)) {
				change = true;
				v.removeUse();
				return true;
			}
			return false;
		});
	}
}
