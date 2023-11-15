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
		Empty n = new Empty();
		ArrayList<Instruction> copies = new ArrayList<>();
		ArrayList<Value> ready = new ArrayList<>();
		ArrayList<Value> to_do = new ArrayList<>();
		HashMap<Value, Value> pred = new HashMap<>();
		HashMap<Value, Value> loc = new HashMap<>();
		for (int i = 0; i < operands.size(); i+=2) {
			loc.put(operands.get(i + 1), operands.get(i + 1));
			pred.put(operands.get(i), operands.get(i + 1));
			to_do.add(operands.get(i));
		}

		for (int i = 0; i < operands.size(); i+=2) {
			if (!loc.containsKey(operands.get(i))) {
				ready.add(operands.get(i));
			}
		}

		while (!to_do.isEmpty()) {
			while (!ready.isEmpty()) {
				Value b = ready.get(0);
				ready.remove(0);
				Value a = pred.get(b);
				Value c = loc.get(a);
				copies.add(new Move(b, c));
				loc.put(a, b);
				if (a == c && pred.containsKey(a)) {
					ready.add(a);
				}
			}

			Value b = to_do.get(0);
			to_do.remove(0);
			if (b == loc.get(pred.get(b))) {
				copies.add(new Move(n, b));
				loc.put(b, n);
				ready.add(b);
			}
		}

		if (!n.getUseList().isEmpty()) {
			copies.add(0, n);
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
