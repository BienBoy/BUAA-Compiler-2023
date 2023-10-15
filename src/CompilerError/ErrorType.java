package CompilerError;

import java.util.HashMap;

public enum ErrorType {
	ILLEGAL_CHAR, REDEFINED, UNDEFINED, MISMATCHED_FUNCTION_ARGS_NUM,
	MISMATCHED_FUNCTION_ARGS_TYPE, MISMATCHED_RETURN, MISSING_RETURN,
	MODIFIED_CONSTANT, MISSING_SEMICOLON, MISSING_PARENTHESIS, MISSING_BRACKET,
	MISMATCHED_PRINTF_ARGS_NUM, INCORRECT_BREAK_CONTINUE, OTHER;

	private final static HashMap<ErrorType, String> codes = new HashMap<ErrorType, String>() {{
		put(ILLEGAL_CHAR, "a");
		put(REDEFINED, "b");
		put(UNDEFINED, "c");
		put(MISMATCHED_FUNCTION_ARGS_NUM, "d");
		put(MISMATCHED_FUNCTION_ARGS_TYPE, "e");
		put(MISMATCHED_RETURN, "f");
		put(MISSING_RETURN, "g");
		put(MODIFIED_CONSTANT, "h");
		put(MISSING_SEMICOLON, "i");
		put(MISSING_PARENTHESIS, "j");
		put(MISSING_BRACKET, "k");
		put(MISMATCHED_PRINTF_ARGS_NUM, "l");
		put(INCORRECT_BREAK_CONTINUE, "m");
		put(OTHER, "o");
	}};

	public String getCode() {
		return codes.get(this);
	}
}
