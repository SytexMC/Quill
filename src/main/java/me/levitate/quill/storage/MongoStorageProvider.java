package me.levitate.quill.storage;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class MongoStorageProvider<K, V> extends AbstractStorageProvider<K, V> {
    private final String mongoUri;
    private final String databaseName;
    private final String collectionName;
    private final ObjectMapper mapper;
    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    public MongoStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                                String mongoUri, String databaseName, String collectionName,
                                SerializationProvider serializationProvider) {
        super(plugin, keyClass, valueClass, serializationProvider);
        this.mongoUri = mongoUri;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.mapper = new ObjectMapper();
        serializationProvider.configureMapper(this.mapper);
    }

    @Override
    public void connect() {
        try {
            mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            collection = database.getCollection(collectionName);
            connected = true;
            load();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MongoDB", e);
        }
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            save();
            mongoClient.close();
            cache.clear();
            connected = false;
        }
    }

    @Override
    public synchronized void save() {
        ensureConnected();
        cache.forEach((key, value) -> {
            try {
                Document document = Document.parse(mapper.writeValueAsString(value));
                document.put("_id", key.toString());
                collection.replaceOne(
                    Filters.eq("_id", key.toString()),
                    document,
                    new ReplaceOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save document: " + key, e);
            }
        });
    }

    @Override
    public synchronized void load() {
        ensureConnected();
        cache.clear();
        
        try {
            FindIterable<Document> documents = collection.find();
            for (Document doc : documents) {
                String idStr = doc.getString("_id");
                doc.remove("_id");
                
                try {
                    K key = convertStringToKey(idStr);
                    V value = mapper.readValue(doc.toJson(), valueClass);
                    cache.put(key, value);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load document: " + idStr, e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load data from MongoDB", e);
        }
    }

    @Override
    public Optional<V> get(K key) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public synchronized void put(K key, V value) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        cache.put(key, value);
        try {
            Document document = Document.parse(mapper.writeValueAsString(value));
            document.put("_id", key.toString());
            collection.replaceOne(
                Filters.eq("_id", key.toString()),
                document,
                new ReplaceOptions().upsert(true)
            );
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save document: " + key, e);
        }
    }

    @Override
    public synchronized void update(K key, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");
        
        V value = cache.get(key);
        if (value != null) {
            updater.accept(value);
            put(key, value);
        }
    }

    @Override
    public synchronized boolean remove(K key) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        
        if (cache.remove(key) != null) {
            collection.deleteOne(Filters.eq("_id", key.toString()));
            return true;
        }
        return false;
    }

    @Override
    public synchronized void batchUpdate(Collection<K> keys, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(keys, "Keys collection cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");
        
        keys.forEach(key -> update(key, updater));
    }

    @SuppressWarnings("unchecked")
    private K convertStringToKey(String idStr) {
        if (keyClass == String.class) {
            return (K) idStr;
        } else if (keyClass == UUID.class) {
            return (K) UUID.fromString(idStr);
        } else if (keyClass == Integer.class) {
            return (K) Integer.valueOf(idStr);
        } else if (keyClass == Long.class) {
            return (K) Long.valueOf(idStr);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + keyClass);
        }
    }
}