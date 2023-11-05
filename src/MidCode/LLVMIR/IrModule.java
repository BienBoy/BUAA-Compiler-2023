package MidCode.LLVMIR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class IrModule {
	private final ArrayList<GlobalVariable> globalVariables;
	private final ArrayList<ConstString> constStrings;
	private final ArrayList<Function> functions;

	public IrModule() {
		globalVariables = new ArrayList<>();
		constStrings = new ArrayList<>();
		functions = new ArrayList<>();
	}

	public void addGlobalVariable(GlobalVariable globalVariable) {
		globalVariables.add(globalVariable);
	}

	public void addConstString(ConstString constString) {
		constStrings.add(constString);
	}

	public void addFunction(Function function) {
		functions.add(function);
	}

	public ArrayList<GlobalVariable> getGlobalVariables() {
		return globalVariables;
	}

	public ArrayList<Function> getFunctions() {
		return functions;
	}

	public ArrayList<ConstString> getConstStrings() {
		return constStrings;
	}

	public void output(BufferedWriter writer) throws IOException {
		// 输出库函数声明
		writer.write("declare i32 @getint()");
		writer.newLine();
		writer.write("declare void @putint(i32)");
		writer.newLine();
		writer.write("declare void @putch(i32)");
		writer.newLine();
		writer.write("declare void @putstr(i8*)");
		writer.newLine();

		boolean newline = true;
		for (GlobalVariable globalVariable : globalVariables) {
			if (newline) {
				newline = false;
				writer.newLine();
			}
			globalVariable.output(writer);
		}

		for (ConstString constString : constStrings) {
			if (newline) {
				newline = false;
				writer.newLine();
			}
			constString.output(writer);
		}

		for (Function function : functions) {
			function.output(writer);
		}
	}
}
