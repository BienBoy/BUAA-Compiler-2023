package MidCode.LLVMIR;

/**
 * 记录Value的使用情况
 */
public class Use {
	private Value value; // 被使用的Value
	private User user; // 使用者
	private int pos; // 位置

	public Use(Value value, User user, int pos) {
		this.value = value;
		this.user = user;
		this.pos = pos;
	}
}
