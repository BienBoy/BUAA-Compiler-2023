import AST.ASTNode;
import CompilerError.ErrorRecord;
import Lexical.Lexer;
import Lexical.Token;
import Parser.Parser;
import SymbolTable.SymbolTable;

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
		String output = "./output.txt";
		String error = "./error.txt";
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

//		try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
//			root.output(writer);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		SymbolTable symbolTable = new SymbolTable();
		root.check(symbolTable);

		if (ErrorRecord.hasError()) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(error))) {
				ErrorRecord.output(writer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
