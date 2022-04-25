package dfa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RandomIterator<T> implements Iterator<T> {
	private final Iterator<T> iterator;

	public RandomIterator(final Iterator<T> i) {
		final List<T> items;

		items = new ArrayList<T>();

		while (i.hasNext()) {
			final T item;

			item = i.next();
			items.add(item);
		}

		Collections.shuffle(items);
		iterator = items.iterator();
	}

	@Override
	public boolean hasNext() {
		return (iterator.hasNext());
	}

	@Override
	public T next() {
		return (iterator.next());
	}

	@Override
	public void remove() {
		iterator.remove();
	}
}