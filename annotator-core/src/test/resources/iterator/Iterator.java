package test;

import javax.annotation.Nullable;

public class Iterator<T> {
  static final class Item<T> {
    @SuppressWarnings("NullAway.Init")
    public T payload;

    public Item<T> next;
    public Item<T> prev;
  }

  private Item<T> head;
  private Item<T> tail;
  private Item<T> iter;
  @Nullable private Item<T> curr;
  private int size = 0;

  private final Pool<Item<T>> pool;

  public Iterator(int maxPoolSize) {
    this.pool =
        new Pool<Item<T>>(16, maxPoolSize) {
          @Override
          protected Item<T> newObject() {
            return new Item<T>();
          }
        };
  }

  public void add(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.next = null;
    item.prev = null;
    if (head == null) {
      head = item;
      tail = item;
      size++;
      return;
    }
    item.prev = tail;
    tail.next = item;
    tail = item;
    size++;
  }

  public void addFirst(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.next = head;
    item.prev = null;
    if (head != null) {
      head.prev = item;
    } else {
      tail = item;
    }
    head = item;
    size++;
  }

  public int size() {
    return size;
  }

  public void iter() {
    iter = head;
  }

  public void iterReverse() {
    iter = tail;
  }

  @Nullable
  public T next() {
    if (iter == null) return null;
    T payload = iter.payload;
    curr = iter;
    iter = iter.next;
    return payload;
  }

  @Nullable
  public T previous() {
    if (iter == null) return null;
    T payload = iter.payload;
    curr = iter;
    iter = iter.prev;
    return payload;
  }

  public void remove() {
    if (curr == null) return;
    size--;
    Item<T> c = curr;
    Item<T> n = curr.next;
    Item<T> p = curr.prev;
    pool.free(curr);
    curr = null;
    if (size == 0) {
      head = null;
      tail = null;
      return;
    }
    if (c == head) {
      n.prev = null;
      head = n;
      return;
    }
    if (c == tail) {
      p.next = null;
      tail = p;
      return;
    }
    p.next = n;
    n.prev = p;
  }

  @Nullable
  public T removeLast() {
    if (tail == null) {
      return null;
    }

    T payload = tail.payload;

    size--;

    Item<T> p = tail.prev;
    pool.free(tail);

    if (size == 0) {
      head = null;
      tail = null;
    } else {
      tail = p;
      tail.next = null;
    }

    return payload;
  }

  public void clear() {
    iter();
    T v = null;
    while ((v = next()) != null) remove();
  }
}
