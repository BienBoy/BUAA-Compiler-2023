package MidCode.LLVMIR;

import MidCode.LLVMIR.Instruction.Br;
import MidCode.LLVMIR.Instruction.Instruction;
import MidCode.LLVMIR.Instruction.PC;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BasicBlock extends Value {
	private final ArrayList<Instruction> values; // 基本块内的指令

	public BasicBlock(String name) {
		super(name);
		values = new ArrayList<>();
	}

	public void add(Instruction value) {
		values.add(value);
	}

	public void add(int index, Instruction value) {
		values.add(index, value);
	}

	public ArrayList<Instruction> getValues() {
		return values;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		if (!useList.isEmpty()) {
			writer.write(name + ":");
			writer.newLine();
		}
		for (Value value : values) {
			value.output(writer);
		}
	}

	@Override
	public String getRawName() {
		return getName();
	}

	public String getMipsLabel(String prefix) {
		return prefix + "_" + getName();
	}

	// 替换下个基本块
	public void replaceNext(BasicBlock from, BasicBlock to) {
		Instruction instruction = values.get(values.size() - 1);
		if (!(instruction instanceof Br)) {
			throw new RuntimeException("BasicBlock最后一条语句不是跳转语句");
		}
		instruction.replaceUse(from, to);
	}

	// 将PC替换为其他指令to(s)
	public void replacePC(PC from, Instruction...to) {
		Integer position = null;
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) == from) {
				position = i;
				break;
			}
		}
		if (position == null) {
			return;
		}
		values.remove(position.intValue());
		for (int i = 0; i < to.length; i++) {
			values.add(position + i, to[i]);
		}
	}
}
