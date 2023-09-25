package AST;

public class LAndExp extends RewrittenBranchNode {
	@Override
	public void reorganize() {
		if (children.size() < 3)
			return;
		LAndExp temp = new LAndExp();
		temp.append(children.get(0));
		children.set(0, temp);
		while (children.size() > 3) {
			temp = new LAndExp();
			for (int i = 0; i < 3; i++) {
				temp.append(children.get(0));
				children.remove(0);
			}
			children.add(0, temp);
		}
	}
}
