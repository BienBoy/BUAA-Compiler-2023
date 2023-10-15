package CompilerError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeSet;

public class ErrorRecord {
	private static TreeSet<CompilerError> errors = new TreeSet<>();

	public static void add(CompilerError error) {
		errors.add(error);
	}

	public static TreeSet<CompilerError> getErrors() {
		return errors;
	}

	public static boolean hasError() {
		return !errors.isEmpty();
	}

	public static void output(BufferedWriter writer) throws IOException {
		for (CompilerError error : errors) {
			writer.write(error.toString());
//			writer.write(" " + error.getDetail());
			writer.newLine();
		}
	}
}
