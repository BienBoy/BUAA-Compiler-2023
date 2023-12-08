package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.BasicBlock;
import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

public class Phi extends Instruction {
	private Alloca var;

	public Phi(String name, Value...operands) {
		// 用于函数内联时构造phi指令
		super(name, operands);
	}

	public Phi(Alloca var) {
		super();
		this.var = var;
	}

	public Phi(Alloca var, Value[] operands) {
		super(operands);
		this.var = var;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write("\t" + name + " = phi i32 ");
		boolean flag = false;
		for (int i = 0; i < operands.size(); i+=2) {
			if (flag) {
				writer.write(", ");
			}
			flag = true;
			writer.write("[ " + operands.get(i+1).getName() + ", %");
			writer.write(operands.get(i).getName() + " ]");
		}
		writer.newLine();
	}

	public Alloca getVar() {
		return var;
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
