package MidCode.LLVMIR;

import SymbolTable.Array1D;
import SymbolTable.Array2D;
import SymbolTable.Symbol;
import SymbolTable.Variable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class Function extends Value {
	private final ArrayList<BasicBlock> basicBlocks;
	private final ArrayList<FunctionParam> params;
	private int primarySp; // 初始栈指针位置
	private int raAddr; // $ra在栈上的地址
	private Map<String, Integer> registerAddr; // 保存的现场的地址

	public Function(String name, Symbol symbol) {
		super(name, symbol);
		basicBlocks = new ArrayList<>();
		params = new ArrayList<>();
	}

	public void addBasicBlock(BasicBlock block) {
		basicBlocks.add(block);
	}

	public ArrayList<BasicBlock> getBasicBlocks() {
		return basicBlocks;
	}

	public void addParam(FunctionParam param) {
		params.add(param);
	}

	public ArrayList<FunctionParam> getParams() {
		return params;
	}

	@Override
	public void output(BufferedWriter writer) throws IOException {
		writer.newLine();
		writer.write("define dso_local ");

		if (((SymbolTable.Function) symbol).hasReturn()) {
			writer.write("i32 ");
		} else {
			writer.write("void ");
		}

		writer.write(name + "(");

		for (int i = 0; i < params.size(); i++) {
			if (params.get(i).symbol instanceof Variable) {
				writer.write("i32 " + params.get(i).name);
			} else if (params.get(i).symbol instanceof Array1D) {
				writer.write("i32* " + params.get(i).name);
			} else {
				Array2D array = (Array2D) params.get(i).symbol;

				writer.write("[" + array.getShapeY() + " x i32]* " + params.get(i).name);
			}

			if (i < params.size() - 1) {
				writer.write(", ");
			}
		}

		writer.write(") {");
		writer.newLine();

		for (BasicBlock basicBlock : basicBlocks) {
			basicBlock.output(writer);
		}

		writer.write("}");
		writer.newLine();
	}

	public int getPrimarySp() {
		return primarySp;
	}

	public void setPrimarySp(int primarySp) {
		this.primarySp = primarySp;
	}

	public int getRaAddr() {
		return raAddr;
	}

	public void setRaAddr(int raAddr) {
		this.raAddr = raAddr;
	}

	public Map<String, Integer> getRegisterAddr() {
		return registerAddr;
	}

	public void setRegisterAddr(Map<String, Integer> registerAddr) {
		this.registerAddr = registerAddr;
	}
}
