package MidCode.LLVMIR;

/**
 * 记录Value的使用情况
 */
public class Use {
	private Value value; // 被使用的Value
	private User user; // 使用者

	public Use(Value value, User user) {
		this.value = value;
		this.user = user;
	}

	public Value getValue() {
		return value;
	}

	public User getUser() {
		return user;
	}
}
