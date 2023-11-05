package MidCode.LLVMIR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BasicBlock extends Value {
	private final ArrayList<Value> values; // 基本块内的指令

	public BasicBlock(String name) {
		super(name);
		values = new ArrayList<>();
	}

	public void add(Value value) {
		values.add(value);
	}

	public ArrayList<Value> getValues() {
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
}
