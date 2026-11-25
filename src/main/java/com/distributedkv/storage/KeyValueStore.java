
package com.distributedkv.storage;

import java.util.HashMap;
import java.util.Map;

public class KeyValueStore {

    private final Map<String, String> data;

    public KeyValueStore() {
        data = new HashMap<>();
    }

    // Insert or update a key-value pair
    public synchronized void put(String key, String value) {
        data.put(key, value);

        System.out.println(
                "Stored: " + key + " = " + value);
    }

    // Retrieve a value using its key
    public synchronized String get(String key) {
        return data.get(key);
    }

    // Delete a key-value pair
    public synchronized void delete(String key) {
        data.remove(key);

        System.out.println("Deleted key: " + key);
    }

    // Display all stored data
    public synchronized void displayAll() {
        System.out.println("\n===== KEY-VALUE STORE =====");

        if (data.isEmpty()) {
            System.out.println("Storage is empty.");
            return;
        }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            System.out.println(
                    entry.getKey() + " = " + entry.getValue());
        }
    }
}
