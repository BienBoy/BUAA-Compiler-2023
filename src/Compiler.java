import AST.ASTNode;
import Lexical.Lexer;
import Lexical.Token;
import Parser.Parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

public class Compiler {
	public static void main(String[] args) {
		String input = "./testfile.txt";
		StringBuilder program = new StringBuilder();
		try (Stream<String> lines = Files.lines(Paths.get(input))) {
			lines.forEach(line -> program.append(line).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Lexer lexer = new Lexer(program.toString());
		ArrayList<Token> tokens = lexer.analyze();

		Parser parser = new Parser(tokens);
		ASTNode root = parser.analyze();

		String output = "./output.txt";
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
			root.output(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
