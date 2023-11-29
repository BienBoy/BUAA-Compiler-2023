package MidCode.LLVMIR;

import MidCode.LLVMIR.Instruction.Br;
import MidCode.LLVMIR.Instruction.Instruction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BasicBlock extends Value {
	private final ArrayList<Instruction> instructions; // 基本块内的指令

	public BasicBlock(String name) {
		super(name);
		instructions = new ArrayList<>();
	}

	public void add(Instruction value) {
		value.setBasicBlock(this);
		instructions.add(value);
	}

	public void add(int index, Instruction value) {
		value.setBasicBlock(this);
		instructions.add(index, value);
	}

	public ArrayList<Instruction> getInstructions() {
		return instructions;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		if (!useList.isEmpty()) {
			writer.write(name + ":");
			writer.newLine();
		}
		for (Value value : instructions) {
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
		Instruction instruction = instructions.get(instructions.size() - 1);
		if (!(instruction instanceof Br)) {
			throw new RuntimeException("BasicBlock最后一条语句不是跳转语句");
		}
		instruction.replaceUse(from, to);
	}

	// 将指令from替换为其他指令to(s)
	public void replaceInstruction(Instruction from, Instruction...to) {
		Integer position = null;
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i) == from) {
				position = i;
				break;
			}
		}
		if (position == null) {
			return;
		}
		from.delete();
		for (int i = 0; i < to.length; i++) {
			add(position + i, to[i]);
		}
	}
}
