package AST;

public class LOrExp extends RewrittenBranchNode {
	@Override
	public void reorganize() {
		if (children.size() < 3)
			return;
		LOrExp temp = new LOrExp();
		temp.append(children.get(0));
		children.set(0, temp);
		while (children.size() > 3) {
			temp = new LOrExp();
			for (int i = 0; i < 3; i++) {
				temp.append(children.get(0));
				children.remove(0);
			}
			children.add(0, temp);
		}
	}
}
