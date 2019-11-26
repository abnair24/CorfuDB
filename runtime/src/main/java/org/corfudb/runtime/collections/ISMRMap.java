package org.corfudb.runtime.collections;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.corfudb.annotations.Accessor;
import org.corfudb.annotations.ConflictParameter;
import org.corfudb.annotations.Mutator;
import org.corfudb.annotations.MutatorAccessor;

/**
 * Created by mwei on 1/9/16.
 */
@SuppressWarnings("checkstyle:abbreviation")
public interface ISMRMap<K, V> extends Map<K, V> {

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with any modification to
     * the map, since the size of the map could be potentially changed.
     */
    @Accessor
    @Override
    int size();

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with any modification to
     * the map, since the size of the map could be potentially changed.
     */
    @Accessor
    @Override
    boolean isEmpty();

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with any operation on the
     * given key.
     */
    @Accessor
    @Override
    boolean containsKey(@ConflictParameter Object key);

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with any modification to
     * the map, since the presence of values could be potentially changed.
     */
    @Accessor
    @Override
    boolean containsValue(Object value);

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with any operation on the
     * given key.
     */
    @Accessor
    @Override
    V get(@ConflictParameter Object key);

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation produces a conflict with any other
     * operation on the given key.
     */
    @MutatorAccessor(name = "put", undoFunction = "undoPut", undoRecordFunction = "undoPutRecord")
    @Override
    V put(@ConflictParameter K key, V value);


    /**
     * This operation behaves like a put operation, but does not
     * return the previous value, and does not result in a read
     * of the map.
     *
     * <p>Calling this operation produces the same put record as calling
     * "put" directly. However, the runtime will not try to sync
     * the object to obtain an upcall.
     *
     * <p>Conflicts: this operation produces a conflict with any other
     * operation on the given key.
     */
    @Mutator(name = "put", noUpcall = true)
    default void blindPut(@ConflictParameter K key, V value) {
        // This is just a stub, the annotation processor will generate an update with
        // put(key, value), since this method doesn't require an upcall therefore no
        // operations are needed to be executed on the internal data structure
    }

    /** Generate an undo record for a put, given the previous state of the map
     * and the parameters to the put call.
     *
     * @param previousState     The previous state of the map
     * @param key               The key from the put call
     * @param value             The value from the put call. This is not
     *                          needed to generate an undo record.
     * @return                  An undo record, which for a put is the
     *                          previous value in the map.
     */
    default V undoPutRecord(ISMRMap<K,V> previousState, K key, V value) {
        return previousState.get(key);
    }

    /** Undo a put, given the current state of the map, an undo record
     * and the arguments to the put command to undo.
     *
     * @param map           The state of the map after the put to undo
     * @param undoRecord    The undo record generated by undoPutRecord
     * @param key           The key of the put to undo
     * @param value         The value of the put to undo, which is not
     *                      needed.
     */
    default void undoPut(ISMRMap<K,V> map, V undoRecord, K key, V value) {
        if (undoRecord == null) {
            map.remove(key);
        } else {
            map.put(key, undoRecord);
        }
    }

    /**
     * {@jnheritDoc}
     *
     * <p>Conflicts: this operation produces a conflict with any other
     * operation on the given key.
     */
    @MutatorAccessor(name = "remove", undoFunction = "undoRemove",
            undoRecordFunction = "undoRemoveRecord")
    @Override
    V remove(@ConflictParameter Object key);

    /** Generate an undo record for a remove, given the previous state of the map
     * and the parameters to the remove call.
     *
     * @param previousState     The previous state of the map
     * @param key               The key from the remove call
     * @return                  An undo record, which for a remove is the
     *                          previous value in the map.
     */
    default V undoRemoveRecord(ISMRMap<K,V> previousState, K key) {
        return previousState.get(key);
    }

    /** Undo a remove, given the current state of the map, an undo record
     * and the arguments to the remove command to undo.
     *
     * @param map           The state of the map after the put to undo
     * @param undoRecord    The undo record generated by undoRemoveRecord
     */
    default void undoRemove(ISMRMap<K,V> map, V undoRecord, K key) {
        if (undoRecord == null) {
            map.remove(key);
        } else {
            map.put(key, undoRecord);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts on any keys that are in the map given.
     */
    @Mutator(name = "putAll", undoFunction = "undoPutAll",
            undoRecordFunction = "undoPutAllRecord",
            conflictParameterFunction = "putAllConflictFunction")
    @Override
    void putAll(Map<? extends K, ? extends V> m);


    /** Generate the conflict parameters for putAll, given the arguments to the
     * putAll operation.
     * @param m                 The map for the putAll operation.
     * @return                  An array of conflict parameters, which are the
     *                          hash codes of the keys given.
     */
    default Object[] putAllConflictFunction(Map<? extends K, ? extends V> m) {
        return m.keySet().stream()
                .map(Object::hashCode)
                .toArray(Object[]::new);
    }

    enum UndoNullable {
        NULL;
    }

    /** Generate an undo record for putAll, given the previous state of the map
     * and the parameters to the putAll call.
     *
     * @param previousState     The previous state of the map
     * @param m                 The map from the putAll call
     * @return                  An undo record, which for a putAll is all the
     *                          previous entries in the map.
     */
    default Map<K,V> undoPutAllRecord(ISMRMap<K,V> previousState, Map<? extends K, ? extends V> m) {
        ImmutableMap.Builder<K,V> builder = ImmutableMap.builder();
        m.keySet().forEach(k -> builder.put(k,
                (previousState.get(k) == null ? (V) UndoNullable.NULL : previousState.get(k))));
        return builder.build();
    }

    /** Undo a remove, given the current state of the map, an undo record
     * and the arguments to the remove command to undo.
     *
     * @param map           The state of the map after the put to undo
     * @param undoRecord    The undo record generated by undoRemoveRecord
     */
    default void undoPutAll(ISMRMap<K,V> map, Map<K,V> undoRecord,
                            Map<? extends K, ? extends V> m) {
        undoRecord.entrySet().forEach(e -> {
            if (e.getValue() == UndoNullable.NULL) {
                map.remove(e.getKey());
            } else {
                map.put(e.getKey(), e.getValue());
            }
        }
        );
    }


    /**
     * {@inheritDoc}
     *
     * <p>Conflicts: this operation conflicts with the entire map, since it drops
     * all mappings which are present.
     */
    @Mutator(name = "clear", reset = true)
    @Override
    void clear();

    /**
     * {@inheritDoc}
     *
     * <p>This function currently does not return a view like the java.util implementation,
     * and changes to the keySet will *not* be reflected in the map.
     *
     * <p>Conflicts: This operation currently conflicts with any modification
     * to the map.
     */
    @Accessor
    @Override
    Set<K> keySet();

    /**
     * {@inheritDoc}
     *
     * <p>This function currently does not return a view like the java.util implementation,
     * and changes to the values will *not* be reflected in the map.
     *
     * <p>Conflicts: This operation currently conflicts with any modification
     * to the map.
     */
    @Accessor
    @Override
    Collection<V> values();

    /**
     * {@inheritDoc}
     *
     * <p>This function currently does not return a view like the java.util implementation,
     * and changes to the entrySet will *not* be reflected in the map.
     *
     * <p>Conflicts: This operation currently conflicts with any modification
     * to the map.
     */
    @Accessor
    @Override
    Set<Entry<K, V>> entrySet();

}