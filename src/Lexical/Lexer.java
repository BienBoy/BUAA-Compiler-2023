package Lexical;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 词法分析器
 */
public class Lexer {
	// 待分析的程序
	private String program;
	// 当前分析到的位置
	private int position = 0;
	// 当前行号
	private int line = 1;
	// 保留字
	private final static HashMap<String, TokenType> reservedWords = new HashMap<String, TokenType>(){{
		// 通过匿名内部类实现创建时添加数据
		put("main", TokenType.MAINTK);
		put("const", TokenType.CONSTTK);
		put("int", TokenType.INTTK);
		put("break", TokenType.BREAKTK);
		put("continue", TokenType.CONTINUETK);
		put("if", TokenType.IFTK);
		put("else", TokenType.ELSETK);
		put("for", TokenType.FORTK);
		put("getint", TokenType.GETINTTK);
		put("printf", TokenType.PRINTFTK);
		put("return", TokenType.RETURNTK);
		put("void", TokenType.VOIDTK);
	}};

	/**
	 * 构造器
	 * @param program 待分析的程序
	 */
	public Lexer(String program) {
		this.program = program;
	}

	/**
	 * 对整个程序进行词法分析，返回一个ArrayList，包含划分的所有单词
	 * @return 划分的所有单词
	 */
	public ArrayList<Token> analyze() {
		ArrayList<Token> tokens = new ArrayList<>();
		while (hasNext()) {
			Token token = next();
			if (token != null) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	/**
	 * 判断是否读取到结尾
	 * @return 是否读取到结尾
	 */
	public boolean hasNext() {
		return this.position < program.length();
	}

	/**
	 * 获取下个单词，若为注释或已到达结尾，则返回null
	 * @return 下个单词或null
	 */
	public Token next() {
		skipBlank(); // 跳过空白
		if (!hasNext()) {
			return null;
		}
		char ch = program.charAt(position);
		if (Character.isLetter(ch) || ch == '_') {
			return getIdent();
		}
		if (Character.isDigit(ch)) {
			return getInteger();
		}
		if (ch == '"') {
			return getFormatString();
		}

		if (ch == '+') {
			position++;
			return new Token(TokenType.PLUS, "+", line);
		}
		if (ch == '-') {
			position++;
			return new Token(TokenType.MINU, "-", line);
		}
		if (ch == '*') {
			position++;
			return new Token(TokenType.MULT, "*", line);
		}
		if (ch == ';') {
			position++;
			return new Token(TokenType.SEMICN, ";", line);
		}
		if (ch == '%') {
			position++;
			return new Token(TokenType.MOD, "%", line);
		}
		if (ch == ',') {
			position++;
			return new Token(TokenType.COMMA, ",", line);
		}
		if (ch == '(') {
			position++;
			return new Token(TokenType.LPARENT, "(", line);
		}
		if (ch == ')') {
			position++;
			return new Token(TokenType.RPARENT, ")", line);
		}
		if (ch == '[') {
			position++;
			return new Token(TokenType.LBRACK, "[", line);
		}
		if (ch == ']') {
			position++;
			return new Token(TokenType.RBRACK, "]", line);
		}
		if (ch == '{') {
			position++;
			return new Token(TokenType.LBRACE, "{", line);
		}
		if (ch == '}') {
			position++;
			return new Token(TokenType.RBRACE, "}", line);
		}

		position++;
		char nextCh = hasNext() ? program.charAt(position) : '\0';

		if (ch == '!' && nextCh == '=') {
			position++;
			return new Token(TokenType.NEQ, "!=", line);
		}
		if (ch == '!') {
			return new Token(TokenType.NOT, "!", line);
		}

		if (ch == '=' && nextCh == '=') {
			position++;
			return new Token(TokenType.EQL, "==", line);
		}
		if (ch == '=') {
			return new Token(TokenType.ASSIGN, "=", line);
		}

		if (ch == '<' && nextCh == '=') {
			position++;
			return new Token(TokenType.LEQ, "<=", line);
		}
		if (ch == '<') {
			return new Token(TokenType.LSS, "<", line);
		}

		if (ch == '>' && nextCh == '=') {
			position++;
			return new Token(TokenType.GEQ, ">=", line);
		}
		if (ch == '>') {
			return new Token(TokenType.GRE, ">", line);
		}

		if (ch == '&' && nextCh == '&') {
			position++;
			return new Token(TokenType.AND, "&&", line);
		}
		if (ch == '|' && nextCh == '|') {
			position++;
			return new Token(TokenType.OR, "||", line);
		}

		if (ch == '/' && (nextCh == '/' || nextCh == '*')) {
			position--;
			skipComment();
			return null;
		}
		if (ch == '/') {
			return new Token(TokenType.DIV, "/", line);
		}

		// TODO 抛出错误
		return null;
	}

	/**
	 * 跳过空白
	 */
	private void skipBlank() {
		while (hasNext() && isBlank(program.charAt(position))) {
			if (program.charAt(position) == '\n') {
				line++;
			}
			position++;
		}
	}

	/**
	 * 判断是否为空白字符
	 * @param ch 要判断的字符
	 * @return 是否为空白字符
	 */
	private boolean isBlank(char ch) {
		return ch == ' ' || ch == '\n' || ch == '\t';
	}

	/**
	 * 跳过注释
	 */
	private void skipComment() {
		char nextCh = program.charAt(++position);
		if (nextCh == '/') {
			while (hasNext() && program.charAt(position) != '\n') {
				position++;
			}
			return;
		}
		while (hasNext()) {
			if (program.charAt(position) == '\n') {
				line++;
			}
			position++;
			if (position + 1 >= program.length()) {
				break;
			}
			if (program.charAt(position) == '*' && program.charAt(position + 1) == '/') {
				position += 2;
				return;
			}
		}
		// TODO 抛出错误，未闭合的多行注释
	}

	/**
	 * 读取标识符和关键字，不对首字母进行检查，调用前需确保首字母为_或字母
	 * @return 标识符或关键字
	 */
	private Token getIdent() {
		StringBuilder word = new StringBuilder();
		while (hasNext() &&
				(Character.isLetterOrDigit(program.charAt(position)) ||
				program.charAt(position) == '_')) {
			word.append(program.charAt(position));
			position++;
		}
		String raw = word.toString();
		return new Token(reservedWords.getOrDefault(raw, TokenType.IDENFR), raw, line);
	}

	/**
	 * 读取数字
	 * @return 数字
	 */
	private Token getInteger() {
		StringBuilder word = new StringBuilder();
		while (hasNext() && Character.isDigit(program.charAt(position))) {
			word.append(program.charAt(position));
			position++;
		}
		return new Token(TokenType.INTCON, word.toString(), line);
	}

	/**
	 * 读取格式化字符串常量
	 * @return 格式化字符串
	 */
	private Token getFormatString() {
		StringBuilder word = new StringBuilder();
		word.append(program.charAt(position));
		position++;
		while (program.charAt(position) != '"') {
			char ch = program.charAt(position);
			if (ch != 32 && ch != 33 && ch != '%' && (ch < 40 || ch > 126)) {
				// TODO 抛出错误，不合法的字符
				return null;
			}
			word.append(ch);
			position++;
			if (!hasNext()) {
				// TODO 抛出错误，字符串未闭合
				return null;
			}
		}
		word.append(program.charAt(position));
		position++;
		return new Token(TokenType.STRCON, word.toString(), line);
	}
}
