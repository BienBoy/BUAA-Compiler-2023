package CompilerError;

public class CompilerError implements Comparable<CompilerError> {
	private final int line;
	private final ErrorType type;
	private final String detail;

	public CompilerError(int line, ErrorType type) {
		this.line = line;
		this.type = type;
		this.detail = type.toString();
	}

	public CompilerError(int line, ErrorType type, String detail) {
		this.line = line;
		this.type = type;
		this.detail = detail;
	}

	public int getLine() {
		return line;
	}

	public ErrorType getType() {
		return type;
	}

	@Override
	public int compareTo(CompilerError o) {
		if (line < o.line)
			return -1;
		if (line > o.line)
			return 1;
		return type.getCode().compareTo(o.getType().getCode());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CompilerError that = (CompilerError) o;
		return line == that.line && type == that.type;
	}

	public String getDetail() {
		return detail;
	}

	@Override
	public String toString() {
		return line + " " + type.getCode();
	}
}
