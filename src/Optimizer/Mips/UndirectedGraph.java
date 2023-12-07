package Optimizer.Mips;

import MidCode.LLVMIR.Value;

import java.util.*;

public class UndirectedGraph implements Cloneable {
	private Map<Value, Set<Value>> adjs = new LinkedHashMap<>();

	public void add(Value a, Value b) {
		if (!adjs.containsKey(a)) {
			adjs.put(a, new HashSet<>());
		}
		if (!adjs.containsKey(b)) {
			adjs.put(b, new HashSet<>());
		}
		if (a == b) {
			// 无自环
			return;
		}
		adjs.get(a).add(b);
		adjs.get(b).add(a);
	}

	public void add(Value a) {
		if (!adjs.containsKey(a)) {
			adjs.put(a, new HashSet<>());
		}
	}

	public void remove(Value a) {
		for (Value adj : adjs.get(a)) {
			adjs.get(adj).remove(a);
		}
		adjs.remove(a);
	}

	public void coalesce(Value a, Value b) {
		// 将b并入a
		if (hasEdge(a,b)) {
			adjs.get(b).remove(a);
			adjs.get(a).remove(b);
		}
		Set<Value> bAdjs = adjs.get(b);
		for (Value bAdj : bAdjs) {
			adjs.get(bAdj).remove(b);
			adjs.get(bAdj).add(a);
		}
		adjs.get(a).addAll(bAdjs);
		adjs.remove(b);
	}

	public boolean contains(Value a) {
		return adjs.containsKey(a);
	}

	public Map<Value, Set<Value>> getAdjs() {
		return adjs;
	}

	public Set<Value> getAdjs(Value a) {
		return adjs.get(a);
	}

	public boolean hasEdge(Value a, Value b) {
		return adjs.get(a).contains(b);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		UndirectedGraph cloned = (UndirectedGraph) super.clone();
		cloned.adjs = new HashMap<>(adjs);
		cloned.adjs.replaceAll((v, value) -> new HashSet<>(value));
		return cloned;
	}

	public boolean isEmpty() {
		return adjs.isEmpty();
	}

	public boolean isUndirected() {
		for (Value value : adjs.keySet()) {
			for (Value b:adjs.get(value)) {
				if (!adjs.get(b).contains(value)) {
					return false;
				}
			}
		}
		return true;
	}
}
