package CompilerError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeSet;

public class ErrorRecord {
	private static TreeSet<CompilerError> errors = new TreeSet<>();
	private static boolean working = true;

	public static void add(CompilerError error) {
		if (working)
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
			writer.newLine();
		}
	}

	public static void print() {
		for (CompilerError error : errors) {
			System.out.println(error.toString() + " " + error.getDetail());
		}
	}

	public static void open() {
		working = true;
	}

	public static void close() {
		working = false;
	}
}
