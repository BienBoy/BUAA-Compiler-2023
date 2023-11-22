package MidCode.LLVMIR.Instruction;

import MidCode.LLVMIR.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 并行复制指令，非LLVM IR指令。
 * 内部操作数依此为：phi指令、指令、phi指令、指令……
 */
public class PC extends Instruction {
	public PC() {
		super();
	}

	/**
	 * 并行复制串行化
	 */
	public Instruction[] sequential() {
		ArrayList<Instruction> copies = new ArrayList<>();
		// 去除a==b的
		for (int i = 0; i < operands.size();) {
			if (operands.get(i) == operands.get(i + 1)){
				Value b = operands.get(i);
				Value a = operands.get(i + 1);
				copies.add(new Move(b, a));
				operands.remove(i);
				operands.remove(i);
				continue;
			}
			i+=2;
		}

		while (!operands.isEmpty()) {
			assert operands.size() % 2 == 0;
			Integer pos = null;
			for (int i = 0; i < operands.size(); i+=2) {
				boolean flag = true;
				for (int j = 0; j < operands.size(); j+=2) {
					if (j == i) {
						continue;
					}
					if (operands.get(j + 1) == operands.get(i)) {
						flag = false;
						break;
					}
				}
				if (flag) {
					pos = i;
					break;
				}
			}
			if (pos != null) {
				copies.add(new Move(operands.get(pos), operands.get(pos + 1)));
				operands.remove(pos.intValue());
				operands.remove(pos.intValue());
			} else {
				Empty n = new Empty();
				copies.add(n);
				copies.add(new Move(n, operands.get(1)));
				operands.set(1, n);
			}
		}
		return copies.toArray(new Instruction[0]);
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write("\tPC(");
		for (int i = 0; i < operands.size(); i+=2) {
			if (i > 0) {
				writer.write("; ");
			}
			writer.write(operands.get(i).getName() + ", ");
			writer.write(operands.get(i + 1).getName());
		}
		writer.write(")");
		writer.newLine();
	}
}
