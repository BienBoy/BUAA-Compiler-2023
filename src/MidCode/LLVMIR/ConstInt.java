package MidCode.LLVMIR;

public class ConstInt extends Value {
	private final int value;

	public ConstInt(int value) {
		super("" + value);
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	@Override
	public String getType() {
		return "i32";
	}
}
