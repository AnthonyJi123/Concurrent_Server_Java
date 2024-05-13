package cpen221.mp3.server;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecentKeyTracker<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<K> queue = new ConcurrentLinkedQueue<>();

    public void put(K key, V value) {
        if (map.containsKey(key)) {
            queue.remove(key);
        }
        map.put(key, value);
        queue.offer(key);
    }

    public V get(K key) {
        return map.get(key);
    }

    public K getMostRecentKey() {
        return queue.peek();
    }

    public int size() {
        return map.size();
    }
}