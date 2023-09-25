package AST;

public class RelExp extends RewrittenBranchNode {
	@Override
	public void reorganize() {
		if (children.size() < 3)
			return;
		RelExp temp = new RelExp();
		temp.append(children.get(0));
		children.set(0, temp);
		while (children.size() > 3) {
			temp = new RelExp();
			for (int i = 0; i < 3; i++) {
				temp.append(children.get(0));
				children.remove(0);
			}
			children.add(0, temp);
		}
	}
}
