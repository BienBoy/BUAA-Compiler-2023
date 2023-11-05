package MidCode.LLVMIR;

import java.io.BufferedWriter;
import java.io.IOException;

public class ConstString extends Value {
	private final String value;

	public ConstString(String name, String value) {
		super(name);
		this.value = value;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.write(name + " = private unnamed_addr constant ");
		writer.write(getShape() + " c\"");
		writer.write(value.replace("\n", "\\0A") + "\\00\"");
		writer.newLine();
	}

	public String getShape() {
		return "[" + (value.length() + 1) + " x i8]";
	}

	public String getMips() {
		StringBuilder builder = new StringBuilder();
		builder.append(getRawName()).append(": ").append(".asciiz ");
		builder.append("\"").append(value.replace("\n", "\\n")).append("\"");
		return builder.toString();
	}
}
