package Optimizer.MidCode;

import MidCode.IrRenamer;
import MidCode.LLVMIR.*;
import MidCode.LLVMIR.Instruction.*;
import Optimizer.BaseOptimizer;

import java.util.*;

public class GVN extends BaseOptimizer {
	@Override
	public void optimize(IrModule module) {
		for (Function function : module.getFunctions()) {
			gvn(function);
		}
		new IrRenamer(module).rename();
	}

	private void gvn(Function function) {
		boolean change = true;
		while (change) {
			change = false;
			Map<GVN.Wrapper, Instruction> hashMap = new HashMap<>();
			Map<Instruction, Instruction> toReplace = new HashMap<>();
			for (BasicBlock basicBlock : function.getBasicBlocks()) {
				for (Instruction instruction : basicBlock.getInstructions()) {
					if (!instruction.hasResult() || instruction instanceof Alloca ||
							instruction instanceof Empty || instruction instanceof Call ||
							instruction instanceof Phi || instruction instanceof Load ||
							instruction instanceof GetInt) {
						continue;
					}
					GVN.Wrapper wrapper = new GVN.Wrapper(instruction);
					Instruction equivalent = hashMap.get(wrapper);
					if (equivalent == null) {
						hashMap.put(wrapper, wrapper.instruction);
					} else {
						toReplace.put(instruction, equivalent);
					}
				}
			}

			for (Instruction ins : toReplace.keySet()) {
				change = true;
				ins.replaceUsed(toReplace.get(ins));
				ins.delete();
			}
		}
	}

	private static class Wrapper {
		private final Instruction instruction;

		public Wrapper(Instruction instruction) {
			this.instruction = instruction;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			GVN.Wrapper wrapper = (GVN.Wrapper) o;
			return valueEqual(instruction, wrapper.instruction);
		}

		@Override
		public int hashCode() {
			return hash(instruction);
		}

		private int hash(Value value) {
			if (value instanceof ConstInt) {
				return ((ConstInt) value).getValue();
			}

			if (!(value instanceof Instruction)) {
				return Objects.hash(value);
			}

			Instruction ins = (Instruction) value;

			if (!ins.hasResult() || ins instanceof Alloca ||
					ins instanceof Empty || ins instanceof Call ||
					ins instanceof Phi || ins instanceof Load ||
					ins instanceof GetInt) {
				return Objects.hash(value);
			}

			if (ins.isCommutative()) {
				// 符合交换律
				int code = 0;
				for (Value operand : ins.getOperands()) {
					code += hash(operand);
				}
				return code;
			}
			int code = 1;
			for (Value operand : ins.getOperands())
				code = 31 * code + hash(operand);
			return code;
		}

		private boolean valueEqual(Value a, Value b) {
			if (a.getClass() != b.getClass()) {
				return false;
			}

			if (a instanceof ConstInt) {
				return ((ConstInt) a).getValue() == ((ConstInt) b).getValue();
			}

			if (!(a instanceof Instruction)) {
				return Objects.equals(a, b);
			}

			Instruction insA = (Instruction) a;
			Instruction insB = (Instruction) b;

			if (!insA.hasResult() || insA instanceof Alloca ||
					insA instanceof Empty || insA instanceof Call ||
					insA instanceof Phi || insA instanceof Load) {
				return Objects.equals(a, b);
			}

			if (insA.isCommutative()) {
				List<Value> operandsB = new ArrayList<>(insB.getOperands());
				for (Value operand1 : insA.getOperands()) {
					boolean flag = true;
					for (int i = 0; i < operandsB.size(); i++) {
						Value operand2 = operandsB.get(i);
						if (valueEqual(operand1, operand2)) {
							operandsB.remove(i);
							flag = false;
							break;
						}
					}
					if (flag) {
						return false;
					}
				}
				return true;
			}

			for (Value operand1 : insA.getOperands()) {
				for (Value operand2 : insB.getOperands()) {
					if (!valueEqual(operand1, operand2)) {
						return false;
					}
				}
			}
			return true;
		}
	}
}
