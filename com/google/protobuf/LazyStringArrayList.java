package com.google.protobuf;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public class LazyStringArrayList extends AbstractProtobufList<String> implements LazyStringList, RandomAccess {
    public static final LazyStringList EMPTY = EMPTY_LIST;
    private static final LazyStringArrayList EMPTY_LIST = new LazyStringArrayList();
    private final List<Object> list;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public /* bridge */ /* synthetic */ boolean isModifiable() {
        return super.isModifiable();
    }

    public /* bridge */ /* synthetic */ boolean remove(Object obj) {
        return super.remove(obj);
    }

    public /* bridge */ /* synthetic */ boolean removeAll(Collection collection) {
        return super.removeAll(collection);
    }

    public /* bridge */ /* synthetic */ boolean retainAll(Collection collection) {
        return super.retainAll(collection);
    }

    static {
        EMPTY_LIST.makeImmutable();
    }

    static LazyStringArrayList emptyList() {
        return EMPTY_LIST;
    }

    public LazyStringArrayList() {
        this(10);
    }

    public LazyStringArrayList(int intialCapacity) {
        this((ArrayList<Object>) new ArrayList(intialCapacity));
    }

    public LazyStringArrayList(LazyStringList from) {
        this.list = new ArrayList(from.size());
        addAll(from);
    }

    public LazyStringArrayList(List<String> from) {
        this((ArrayList<Object>) new ArrayList(from));
    }

    private LazyStringArrayList(ArrayList<Object> list2) {
        this.list = list2;
    }

    public LazyStringArrayList mutableCopyWithCapacity(int capacity) {
        if (capacity >= size()) {
            ArrayList<Object> newList = new ArrayList<>(capacity);
            newList.addAll(this.list);
            return new LazyStringArrayList(newList);
        }
        throw new IllegalArgumentException();
    }

    public String get(int index) {
        Object o = this.list.get(index);
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof ByteString) {
            ByteString bs = (ByteString) o;
            String s = bs.toStringUtf8();
            if (bs.isValidUtf8()) {
                this.list.set(index, s);
            }
            return s;
        }
        byte[] ba = (byte[]) o;
        String s2 = Internal.toStringUtf8(ba);
        if (Internal.isValidUtf8(ba)) {
            this.list.set(index, s2);
        }
        return s2;
    }

    public int size() {
        return this.list.size();
    }

    public String set(int index, String s) {
        ensureIsMutable();
        return asString(this.list.set(index, s));
    }

    public void add(int index, String element) {
        ensureIsMutable();
        this.list.add(index, element);
        this.modCount++;
    }

    /* access modifiers changed from: private */
    public void add(int index, ByteString element) {
        ensureIsMutable();
        this.list.add(index, element);
        this.modCount++;
    }

    /* access modifiers changed from: private */
    public void add(int index, byte[] element) {
        ensureIsMutable();
        this.list.add(index, element);
        this.modCount++;
    }

    public boolean addAll(Collection<? extends String> c) {
        return addAll(size(), c);
    }

    public boolean addAll(int index, Collection<? extends String> c) {
        ensureIsMutable();
        boolean ret = this.list.addAll(index, c instanceof LazyStringList ? ((LazyStringList) c).getUnderlyingElements() : c);
        this.modCount++;
        return ret;
    }

    public boolean addAllByteString(Collection<? extends ByteString> values) {
        ensureIsMutable();
        boolean ret = this.list.addAll(values);
        this.modCount++;
        return ret;
    }

    public boolean addAllByteArray(Collection<byte[]> c) {
        ensureIsMutable();
        boolean ret = this.list.addAll(c);
        this.modCount++;
        return ret;
    }

    public String remove(int index) {
        ensureIsMutable();
        Object o = this.list.remove(index);
        this.modCount++;
        return asString(o);
    }

    public void clear() {
        ensureIsMutable();
        this.list.clear();
        this.modCount++;
    }

    public void add(ByteString element) {
        ensureIsMutable();
        this.list.add(element);
        this.modCount++;
    }

    public void add(byte[] element) {
        ensureIsMutable();
        this.list.add(element);
        this.modCount++;
    }

    public Object getRaw(int index) {
        return this.list.get(index);
    }

    public ByteString getByteString(int index) {
        Object o = this.list.get(index);
        ByteString b = asByteString(o);
        if (b != o) {
            this.list.set(index, b);
        }
        return b;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v0, resolved type: java.util.List<java.lang.Object>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v1, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v0, resolved type: byte[]} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] getByteArray(int r4) {
        /*
            r3 = this;
            java.util.List<java.lang.Object> r0 = r3.list
            java.lang.Object r0 = r0.get(r4)
            byte[] r1 = asByteArray(r0)
            if (r1 == r0) goto L_0x0011
            java.util.List<java.lang.Object> r2 = r3.list
            r2.set(r4, r1)
        L_0x0011:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.protobuf.LazyStringArrayList.getByteArray(int):byte[]");
    }

    public void set(int index, ByteString s) {
        setAndReturn(index, s);
    }

    /* access modifiers changed from: private */
    public Object setAndReturn(int index, ByteString s) {
        ensureIsMutable();
        return this.list.set(index, s);
    }

    public void set(int index, byte[] s) {
        setAndReturn(index, s);
    }

    /* access modifiers changed from: private */
    public Object setAndReturn(int index, byte[] s) {
        ensureIsMutable();
        return this.list.set(index, s);
    }

    private static String asString(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof ByteString) {
            return ((ByteString) o).toStringUtf8();
        }
        return Internal.toStringUtf8((byte[]) o);
    }

    /* access modifiers changed from: private */
    public static ByteString asByteString(Object o) {
        if (o instanceof ByteString) {
            return (ByteString) o;
        }
        if (o instanceof String) {
            return ByteString.copyFromUtf8((String) o);
        }
        return ByteString.copyFrom((byte[]) o);
    }

    /* access modifiers changed from: private */
    public static byte[] asByteArray(Object o) {
        if (o instanceof byte[]) {
            return (byte[]) o;
        }
        if (o instanceof String) {
            return Internal.toByteArray((String) o);
        }
        return ((ByteString) o).toByteArray();
    }

    public List<?> getUnderlyingElements() {
        return Collections.unmodifiableList(this.list);
    }

    public void mergeFrom(LazyStringList other) {
        ensureIsMutable();
        for (Object next : other.getUnderlyingElements()) {
            if (next instanceof byte[]) {
                byte[] b = (byte[]) next;
                this.list.add(Arrays.copyOf(b, b.length));
            } else {
                this.list.add(next);
            }
        }
    }

    private static class ByteArrayListView extends AbstractList<byte[]> implements RandomAccess {
        private final LazyStringArrayList list;

        ByteArrayListView(LazyStringArrayList list2) {
            this.list = list2;
        }

        public byte[] get(int index) {
            return this.list.getByteArray(index);
        }

        public int size() {
            return this.list.size();
        }

        public byte[] set(int index, byte[] s) {
            Object o = this.list.setAndReturn(index, s);
            this.modCount++;
            return LazyStringArrayList.asByteArray(o);
        }

        public void add(int index, byte[] s) {
            this.list.add(index, s);
            this.modCount++;
        }

        public byte[] remove(int index) {
            Object o = this.list.remove(index);
            this.modCount++;
            return LazyStringArrayList.asByteArray(o);
        }
    }

    public List<byte[]> asByteArrayList() {
        return new ByteArrayListView(this);
    }

    private static class ByteStringListView extends AbstractList<ByteString> implements RandomAccess {
        private final LazyStringArrayList list;

        ByteStringListView(LazyStringArrayList list2) {
            this.list = list2;
        }

        public ByteString get(int index) {
            return this.list.getByteString(index);
        }

        public int size() {
            return this.list.size();
        }

        public ByteString set(int index, ByteString s) {
            Object o = this.list.setAndReturn(index, s);
            this.modCount++;
            return LazyStringArrayList.asByteString(o);
        }

        public void add(int index, ByteString s) {
            this.list.add(index, s);
            this.modCount++;
        }

        public ByteString remove(int index) {
            Object o = this.list.remove(index);
            this.modCount++;
            return LazyStringArrayList.asByteString(o);
        }
    }

    public List<ByteString> asByteStringList() {
        return new ByteStringListView(this);
    }

    public LazyStringList getUnmodifiableView() {
        if (isModifiable()) {
            return new UnmodifiableLazyStringList(this);
        }
        return this;
    }
}
