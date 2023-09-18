package Lexical;

public class Token {
	private String rawString; // 原始字符串
	private TokenType type; // 单词类别
	private int intValue; // 数值大小，仅在type==INTCON时使用
	private int line; // 所在行号

	/**
	 * 构造方法
	 * @param type 单词类别
	 * @param rawString 原始字符串
	 * @param line 所在行号
	 */
	public Token(TokenType type, String rawString, int line) {
		this.type = type;
		this.rawString = rawString;
		this.line = line;
		if (type.equals(TokenType.INTCON)) {
			this.intValue = Integer.parseInt(rawString);
		}
	}

	public String getRawString() {
		return rawString;
	}

	public TokenType getType() {
		return type;
	}

	public int getIntValue() {
		return intValue;
	}

	public int getLine() {
		return line;
	}

	@Override
	public String toString() {
		return this.type + " " + this.rawString;
	}
}
