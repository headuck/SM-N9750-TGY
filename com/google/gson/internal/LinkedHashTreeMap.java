package com.google.gson.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class LinkedHashTreeMap<K, V> extends AbstractMap<K, V> implements Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final Comparator<Comparable> NATURAL_ORDER = new Comparator<Comparable>() {
        public int compare(Comparable a, Comparable b) {
            return a.compareTo(b);
        }
    };
    Comparator<? super K> comparator;
    private LinkedHashTreeMap<K, V>.EntrySet entrySet;
    final Node<K, V> header;
    private LinkedHashTreeMap<K, V>.KeySet keySet;
    int modCount;
    int size;
    Node<K, V>[] table;
    int threshold;

    public LinkedHashTreeMap() {
        this(NATURAL_ORDER);
    }

    public LinkedHashTreeMap(Comparator<? super K> comparator2) {
        this.size = 0;
        this.modCount = 0;
        this.comparator = comparator2 != null ? comparator2 : NATURAL_ORDER;
        this.header = new Node<>();
        this.table = new Node[16];
        Node<K, V>[] nodeArr = this.table;
        this.threshold = (nodeArr.length / 2) + (nodeArr.length / 4);
    }

    public int size() {
        return this.size;
    }

    public V get(Object key) {
        Node<K, V> node = findByObject(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return findByObject(key) != null;
    }

    public V put(K key, V value) {
        if (key != null) {
            Node<K, V> created = find(key, true);
            V result = created.value;
            created.value = value;
            return result;
        }
        throw new NullPointerException("key == null");
    }

    public void clear() {
        Arrays.fill(this.table, (Object) null);
        this.size = 0;
        this.modCount++;
        Node<K, V> header2 = this.header;
        Node<K, V> e = header2.next;
        while (e != header2) {
            Node<K, V> next = e.next;
            e.prev = null;
            e.next = null;
            e = next;
        }
        header2.prev = header2;
        header2.next = header2;
    }

    public V remove(Object key) {
        Node<K, V> node = removeInternalByKey(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public Node<K, V> find(K key, boolean create) {
        int comparison;
        Node<K, V> nearest;
        Node<K, V> created;
        int i;
        Comparable<Object> comparable = key;
        Comparator<? super K> comparator2 = this.comparator;
        Node<K, V>[] table2 = this.table;
        int hash = secondaryHash(key.hashCode());
        int index = hash & (table2.length - 1);
        Node<K, V> nearest2 = table2[index];
        if (nearest2 != null) {
            Comparable<Object> comparableKey = comparator2 == NATURAL_ORDER ? comparable : null;
            while (true) {
                if (comparableKey != null) {
                    i = comparableKey.compareTo(nearest2.key);
                } else {
                    i = comparator2.compare(comparable, nearest2.key);
                }
                int comparison2 = i;
                if (comparison2 == 0) {
                    return nearest2;
                }
                Node<K, V> child = comparison2 < 0 ? nearest2.left : nearest2.right;
                if (child == null) {
                    nearest = nearest2;
                    comparison = comparison2;
                    break;
                }
                nearest2 = child;
            }
        } else {
            nearest = nearest2;
            comparison = 0;
        }
        if (!create) {
            return null;
        }
        Node<K, V> header2 = this.header;
        if (nearest != null) {
            created = new Node<>(nearest, key, hash, header2, header2.prev);
            if (comparison < 0) {
                nearest.left = created;
            } else {
                nearest.right = created;
            }
            rebalance(nearest, true);
        } else if (comparator2 != NATURAL_ORDER || (comparable instanceof Comparable)) {
            created = new Node<>(nearest, key, hash, header2, header2.prev);
            table2[index] = created;
        } else {
            throw new ClassCastException(key.getClass().getName() + " is not Comparable");
        }
        int i2 = this.size;
        this.size = i2 + 1;
        if (i2 > this.threshold) {
            doubleCapacity();
        }
        this.modCount++;
        return created;
    }

    /* access modifiers changed from: package-private */
    public Node<K, V> findByObject(Object key) {
        if (key == null) {
            return null;
        }
        try {
            return find(key, false);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public Node<K, V> findByEntry(Map.Entry<?, ?> entry) {
        Node<K, V> mine = findByObject(entry.getKey());
        if (mine != null && equal(mine.value, entry.getValue())) {
            return mine;
        }
        return null;
    }

    private boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static int secondaryHash(int h) {
        int h2 = h ^ ((h >>> 20) ^ (h >>> 12));
        return ((h2 >>> 7) ^ h2) ^ (h2 >>> 4);
    }

    /* access modifiers changed from: package-private */
    public void removeInternal(Node<K, V> node, boolean unlink) {
        if (unlink) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
        }
        Node<K, V> left = node.left;
        Node<K, V> right = node.right;
        Node<K, V> originalParent = node.parent;
        if (left == null || right == null) {
            if (left != null) {
                replaceInParent(node, left);
                node.left = null;
            } else if (right != null) {
                replaceInParent(node, right);
                node.right = null;
            } else {
                replaceInParent(node, (Node<K, V>) null);
            }
            rebalance(originalParent, false);
            this.size--;
            this.modCount++;
            return;
        }
        Node<K, V> adjacent = left.height > right.height ? left.last() : right.first();
        removeInternal(adjacent, false);
        int leftHeight = 0;
        Node<K, V> left2 = node.left;
        if (left2 != null) {
            leftHeight = left2.height;
            adjacent.left = left2;
            left2.parent = adjacent;
            node.left = null;
        }
        int rightHeight = 0;
        Node<K, V> right2 = node.right;
        if (right2 != null) {
            rightHeight = right2.height;
            adjacent.right = right2;
            right2.parent = adjacent;
            node.right = null;
        }
        adjacent.height = Math.max(leftHeight, rightHeight) + 1;
        replaceInParent(node, adjacent);
    }

    /* access modifiers changed from: package-private */
    public Node<K, V> removeInternalByKey(Object key) {
        Node<K, V> node = findByObject(key);
        if (node != null) {
            removeInternal(node, true);
        }
        return node;
    }

    private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
        Node<K, V> parent = node.parent;
        node.parent = null;
        if (replacement != null) {
            replacement.parent = parent;
        }
        if (parent == null) {
            int i = node.hash;
            Node<K, V>[] nodeArr = this.table;
            nodeArr[i & (nodeArr.length - 1)] = replacement;
        } else if (parent.left == node) {
            parent.left = replacement;
        } else {
            parent.right = replacement;
        }
    }

    private void rebalance(Node<K, V> unbalanced, boolean insert) {
        for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
            Node<K, V> left = node.left;
            Node<K, V> right = node.right;
            int rightLeftHeight = 0;
            int leftHeight = left != null ? left.height : 0;
            int rightHeight = right != null ? right.height : 0;
            int delta = leftHeight - rightHeight;
            if (delta == -2) {
                Node<K, V> rightLeft = right.left;
                Node<K, V> rightRight = right.right;
                int rightRightHeight = rightRight != null ? rightRight.height : 0;
                if (rightLeft != null) {
                    rightLeftHeight = rightLeft.height;
                }
                int rightDelta = rightLeftHeight - rightRightHeight;
                if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
                    rotateLeft(node);
                } else {
                    rotateRight(right);
                    rotateLeft(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 2) {
                Node<K, V> leftLeft = left.left;
                Node<K, V> leftRight = left.right;
                int leftRightHeight = leftRight != null ? leftRight.height : 0;
                if (leftLeft != null) {
                    rightLeftHeight = leftLeft.height;
                }
                int leftDelta = rightLeftHeight - leftRightHeight;
                if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
                    rotateRight(node);
                } else {
                    rotateLeft(left);
                    rotateRight(node);
                }
                if (insert) {
                    return;
                }
            } else if (delta == 0) {
                node.height = leftHeight + 1;
                if (insert) {
                    return;
                }
            } else {
                node.height = Math.max(leftHeight, rightHeight) + 1;
                if (!insert) {
                    return;
                }
            }
        }
    }

    private void rotateLeft(Node<K, V> root) {
        Node<K, V> left = root.left;
        Node<K, V> pivot = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.right = pivotLeft;
        if (pivotLeft != null) {
            pivotLeft.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.left = root;
        root.parent = pivot;
        int i = 0;
        root.height = Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
        int i2 = root.height;
        if (pivotRight != null) {
            i = pivotRight.height;
        }
        pivot.height = Math.max(i2, i) + 1;
    }

    private void rotateRight(Node<K, V> root) {
        Node<K, V> pivot = root.left;
        Node<K, V> right = root.right;
        Node<K, V> pivotLeft = pivot.left;
        Node<K, V> pivotRight = pivot.right;
        root.left = pivotRight;
        if (pivotRight != null) {
            pivotRight.parent = root;
        }
        replaceInParent(root, pivot);
        pivot.right = root;
        root.parent = pivot;
        int i = 0;
        root.height = Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
        int i2 = root.height;
        if (pivotLeft != null) {
            i = pivotLeft.height;
        }
        pivot.height = Math.max(i2, i) + 1;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        LinkedHashTreeMap<K, V>.EntrySet result = this.entrySet;
        if (result != null) {
            return result;
        }
        LinkedHashTreeMap<K, V>.EntrySet entrySet2 = new EntrySet();
        this.entrySet = entrySet2;
        return entrySet2;
    }

    public Set<K> keySet() {
        LinkedHashTreeMap<K, V>.KeySet result = this.keySet;
        if (result != null) {
            return result;
        }
        LinkedHashTreeMap<K, V>.KeySet keySet2 = new KeySet();
        this.keySet = keySet2;
        return keySet2;
    }

    static final class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        int height;
        final K key;
        Node<K, V> left;
        Node<K, V> next;
        Node<K, V> parent;
        Node<K, V> prev;
        Node<K, V> right;
        V value;

        Node() {
            this.key = null;
            this.hash = -1;
            this.prev = this;
            this.next = this;
        }

        Node(Node<K, V> parent2, K key2, int hash2, Node<K, V> next2, Node<K, V> prev2) {
            this.parent = parent2;
            this.key = key2;
            this.hash = hash2;
            this.height = 1;
            this.next = next2;
            this.prev = prev2;
            prev2.next = this;
            next2.prev = this;
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(V value2) {
            V oldValue = this.value;
            this.value = value2;
            return oldValue;
        }

        /* JADX WARNING: Removed duplicated region for block: B:14:0x0032 A[ORIG_RETURN, RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(java.lang.Object r5) {
            /*
                r4 = this;
                boolean r0 = r5 instanceof java.util.Map.Entry
                r1 = 0
                if (r0 == 0) goto L_0x0036
                r0 = r5
                java.util.Map$Entry r0 = (java.util.Map.Entry) r0
                K r2 = r4.key
                if (r2 != 0) goto L_0x0013
                java.lang.Object r2 = r0.getKey()
                if (r2 != 0) goto L_0x0034
                goto L_0x001d
            L_0x0013:
                java.lang.Object r3 = r0.getKey()
                boolean r2 = r2.equals(r3)
                if (r2 == 0) goto L_0x0034
            L_0x001d:
                V r2 = r4.value
                if (r2 != 0) goto L_0x0028
                java.lang.Object r2 = r0.getValue()
                if (r2 != 0) goto L_0x0034
                goto L_0x0032
            L_0x0028:
                java.lang.Object r3 = r0.getValue()
                boolean r2 = r2.equals(r3)
                if (r2 == 0) goto L_0x0034
            L_0x0032:
                r1 = 1
                goto L_0x0035
            L_0x0034:
            L_0x0035:
                return r1
            L_0x0036:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.gson.internal.LinkedHashTreeMap.Node.equals(java.lang.Object):boolean");
        }

        public int hashCode() {
            K k = this.key;
            int i = 0;
            int hashCode = k == null ? 0 : k.hashCode();
            V v = this.value;
            if (v != null) {
                i = v.hashCode();
            }
            return hashCode ^ i;
        }

        public String toString() {
            return this.key + "=" + this.value;
        }

        public Node<K, V> first() {
            Node<K, V> node = this;
            Node<K, V> child = node.left;
            while (child != null) {
                node = child;
                child = node.left;
            }
            return node;
        }

        public Node<K, V> last() {
            Node<K, V> node = this;
            Node<K, V> child = node.right;
            while (child != null) {
                node = child;
                child = node.right;
            }
            return node;
        }
    }

    private void doubleCapacity() {
        this.table = doubleCapacity(this.table);
        Node<K, V>[] nodeArr = this.table;
        this.threshold = (nodeArr.length / 2) + (nodeArr.length / 4);
    }

    static <K, V> Node<K, V>[] doubleCapacity(Node<K, V>[] oldTable) {
        int oldCapacity = oldTable.length;
        Node<K, V>[] newTable = new Node[(oldCapacity * 2)];
        AvlIterator<K, V> iterator = new AvlIterator<>();
        AvlBuilder<K, V> leftBuilder = new AvlBuilder<>();
        AvlBuilder<K, V> rightBuilder = new AvlBuilder<>();
        for (int i = 0; i < oldCapacity; i++) {
            Node<K, V> root = oldTable[i];
            if (root != null) {
                iterator.reset(root);
                int leftSize = 0;
                int rightSize = 0;
                while (true) {
                    Node<K, V> next = iterator.next();
                    Node<K, V> node = next;
                    if (next == null) {
                        break;
                    } else if ((node.hash & oldCapacity) == 0) {
                        leftSize++;
                    } else {
                        rightSize++;
                    }
                }
                leftBuilder.reset(leftSize);
                rightBuilder.reset(rightSize);
                iterator.reset(root);
                while (true) {
                    Node<K, V> next2 = iterator.next();
                    Node<K, V> node2 = next2;
                    if (next2 == null) {
                        break;
                    } else if ((node2.hash & oldCapacity) == 0) {
                        leftBuilder.add(node2);
                    } else {
                        rightBuilder.add(node2);
                    }
                }
                Node<K, V> node3 = null;
                newTable[i] = leftSize > 0 ? leftBuilder.root() : null;
                int i2 = i + oldCapacity;
                if (rightSize > 0) {
                    node3 = rightBuilder.root();
                }
                newTable[i2] = node3;
            }
        }
        return newTable;
    }

    static class AvlIterator<K, V> {
        private Node<K, V> stackTop;

        AvlIterator() {
        }

        /* access modifiers changed from: package-private */
        public void reset(Node<K, V> root) {
            Node<K, V> stackTop2 = null;
            for (Node<K, V> n = root; n != null; n = n.left) {
                n.parent = stackTop2;
                stackTop2 = n;
            }
            this.stackTop = stackTop2;
        }

        public Node<K, V> next() {
            Node<K, V> stackTop2 = this.stackTop;
            if (stackTop2 == null) {
                return null;
            }
            Node<K, V> result = stackTop2;
            Node<K, V> stackTop3 = result.parent;
            result.parent = null;
            for (Node<K, V> n = result.right; n != null; n = n.left) {
                n.parent = stackTop3;
                stackTop3 = n;
            }
            this.stackTop = stackTop3;
            return result;
        }
    }

    static final class AvlBuilder<K, V> {
        private int leavesSkipped;
        private int leavesToSkip;
        private int size;
        private Node<K, V> stack;

        AvlBuilder() {
        }

        /* access modifiers changed from: package-private */
        public void reset(int targetSize) {
            this.leavesToSkip = ((Integer.highestOneBit(targetSize) * 2) - 1) - targetSize;
            this.size = 0;
            this.leavesSkipped = 0;
            this.stack = null;
        }

        /* access modifiers changed from: package-private */
        public void add(Node<K, V> node) {
            node.right = null;
            node.parent = null;
            node.left = null;
            node.height = 1;
            int i = this.leavesToSkip;
            if (i > 0) {
                int i2 = this.size;
                if ((i2 & 1) == 0) {
                    this.size = i2 + 1;
                    this.leavesToSkip = i - 1;
                    this.leavesSkipped++;
                }
            }
            node.parent = this.stack;
            this.stack = node;
            this.size++;
            int i3 = this.leavesToSkip;
            if (i3 > 0) {
                int i4 = this.size;
                if ((i4 & 1) == 0) {
                    this.size = i4 + 1;
                    this.leavesToSkip = i3 - 1;
                    this.leavesSkipped++;
                }
            }
            for (int scale = 4; (this.size & (scale - 1)) == scale - 1; scale *= 2) {
                int i5 = this.leavesSkipped;
                if (i5 == 0) {
                    Node<K, V> right = this.stack;
                    Node<K, V> center = right.parent;
                    Node<K, V> left = center.parent;
                    center.parent = left.parent;
                    this.stack = center;
                    center.left = left;
                    center.right = right;
                    center.height = right.height + 1;
                    left.parent = center;
                    right.parent = center;
                } else if (i5 == 1) {
                    Node<K, V> right2 = this.stack;
                    Node<K, V> center2 = right2.parent;
                    this.stack = center2;
                    center2.right = right2;
                    center2.height = right2.height + 1;
                    right2.parent = center2;
                    this.leavesSkipped = 0;
                } else if (i5 == 2) {
                    this.leavesSkipped = 0;
                }
            }
        }

        /* access modifiers changed from: package-private */
        public Node<K, V> root() {
            Node<K, V> stackTop = this.stack;
            if (stackTop.parent == null) {
                return stackTop;
            }
            throw new IllegalStateException();
        }
    }

    private abstract class LinkedTreeMapIterator<T> implements Iterator<T> {
        int expectedModCount = LinkedHashTreeMap.this.modCount;
        Node<K, V> lastReturned = null;
        Node<K, V> next = LinkedHashTreeMap.this.header.next;

        LinkedTreeMapIterator() {
        }

        public final boolean hasNext() {
            return this.next != LinkedHashTreeMap.this.header;
        }

        /* access modifiers changed from: package-private */
        public final Node<K, V> nextNode() {
            Node<K, V> e = this.next;
            if (e == LinkedHashTreeMap.this.header) {
                throw new NoSuchElementException();
            } else if (LinkedHashTreeMap.this.modCount == this.expectedModCount) {
                this.next = e.next;
                this.lastReturned = e;
                return e;
            } else {
                throw new ConcurrentModificationException();
            }
        }

        public final void remove() {
            Node<K, V> node = this.lastReturned;
            if (node != null) {
                LinkedHashTreeMap.this.removeInternal(node, true);
                this.lastReturned = null;
                this.expectedModCount = LinkedHashTreeMap.this.modCount;
                return;
            }
            throw new IllegalStateException();
        }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        public int size() {
            return LinkedHashTreeMap.this.size;
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            return new LinkedHashTreeMap<K, V>.LinkedTreeMapIterator<Map.Entry<K, V>>() {
                {
                    LinkedHashTreeMap linkedHashTreeMap = LinkedHashTreeMap.this;
                }

                public Map.Entry<K, V> next() {
                    return nextNode();
                }
            };
        }

        public boolean contains(Object o) {
            return (o instanceof Map.Entry) && LinkedHashTreeMap.this.findByEntry((Map.Entry) o) != null;
        }

        public boolean remove(Object o) {
            Node<K, V> node;
            if (!(o instanceof Map.Entry) || (node = LinkedHashTreeMap.this.findByEntry((Map.Entry) o)) == null) {
                return false;
            }
            LinkedHashTreeMap.this.removeInternal(node, true);
            return true;
        }

        public void clear() {
            LinkedHashTreeMap.this.clear();
        }
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        public int size() {
            return LinkedHashTreeMap.this.size;
        }

        public Iterator<K> iterator() {
            return new LinkedHashTreeMap<K, V>.LinkedTreeMapIterator<K>() {
                {
                    LinkedHashTreeMap linkedHashTreeMap = LinkedHashTreeMap.this;
                }

                public K next() {
                    return nextNode().key;
                }
            };
        }

        public boolean contains(Object o) {
            return LinkedHashTreeMap.this.containsKey(o);
        }

        public boolean remove(Object key) {
            return LinkedHashTreeMap.this.removeInternalByKey(key) != null;
        }

        public void clear() {
            LinkedHashTreeMap.this.clear();
        }
    }

    private Object writeReplace() throws ObjectStreamException {
        return new LinkedHashMap(this);
    }
}
