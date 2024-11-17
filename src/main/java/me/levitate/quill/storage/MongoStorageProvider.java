package me.levitate.quill.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.plugin.Plugin;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public class MongoStorageProvider<K, V> extends AbstractStorageProvider<K, V> {
    private final String mongoUri;
    private final String databaseName;
    private final String collectionName;
    private final ObjectMapper mapper;
    private MongoClient mongoClient;
    private MongoCollection<Document> collection;
    private final Object connectionLock = new Object();

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
        synchronized (connectionLock) {
            try {
                // Parse connection string and get settings
                ConnectionString connString = new ConnectionString(mongoUri);
                MongoClientSettings settings = createClientSettings(connString);

                // Create client with settings
                mongoClient = MongoClients.create(settings);
                MongoDatabase database = mongoClient.getDatabase(databaseName);
                collection = database.getCollection(collectionName);

                // Verify connection with simple operation
                collection.countDocuments();

                connected = true;
                load();

                plugin.getLogger().info("Successfully connected to MongoDB database");
            } catch (Exception e) {
                String errorMsg = "Failed to connect to MongoDB: " + e.getMessage();
                plugin.getLogger().log(Level.SEVERE, errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
        }
    }

    private MongoClientSettings createClientSettings(ConnectionString connString) {
        try {
            // Create SSL context with system trust store
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .applyToSslSettings(builder -> {
                        // Use SSL settings from connection string, or default to enabled for Atlas
                        boolean sslEnabled = connString.getSslEnabled() != null ?
                                connString.getSslEnabled() :
                                mongoUri.contains("mongodb+srv://");

                        builder.enabled(sslEnabled)
                                .context(sslContext)
                                .invalidHostNameAllowed(true);
                    })
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(20000, TimeUnit.MILLISECONDS)
                                    .readTimeout(20000, TimeUnit.MILLISECONDS))
                    .applyToServerSettings(builder ->
                            builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(20000, TimeUnit.MILLISECONDS))
                    .applyToConnectionPoolSettings(builder ->
                            builder.maxWaitTime(20000, TimeUnit.MILLISECONDS)
                                    .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MongoDB client settings", e);
        }
    }

    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            try {
                if (connected) {
                    save();
                }
            } finally {
                if (mongoClient != null) {
                    try {
                        mongoClient.close();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error closing MongoDB connection", e);
                    }
                    mongoClient = null;
                    collection = null;
                }
                cache.clear();
                connected = false;
            }
        }
    }

    @Override
    public synchronized void save() {
        ensureConnected();

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<K, V> entry : cache.entrySet()) {
            try {
                String json = mapper.writeValueAsString(entry.getValue());
                Document document = Document.parse(json);
                document.put("_id", entry.getKey().toString());

                collection.replaceOne(
                        Filters.eq("_id", entry.getKey().toString()),
                        document,
                        new ReplaceOptions().upsert(true)
                );
                successCount++;
            } catch (Exception e) {
                failCount++;
                plugin.getLogger().log(Level.WARNING,
                        "Failed to save document for key " + entry.getKey(), e);
            }
        }

        if (failCount > 0) {
            plugin.getLogger().warning(String.format(
                    "MongoDB save operation completed with %d successes and %d failures",
                    successCount, failCount
            ));
        }
    }

    @Override
    public synchronized void load() {
        ensureConnected();

        try {
            Map<K, V> tempCache = new HashMap<>();
            FindIterable<Document> documents = collection.find();

            for (Document doc : documents) {
                try {
                    String idStr = doc.getString("_id");
                    doc.remove("_id");

                    K key = convertStringToKey(idStr);
                    V value = mapper.readValue(doc.toJson(), valueClass);
                    tempCache.put(key, value);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to load document with ID " + doc.get("_id"), e);
                }
            }

            // Only update cache if all documents were processed
            cache.clear();
            cache.putAll(tempCache);

            plugin.getLogger().info(String.format(
                    "Successfully loaded %d documents from MongoDB",
                    cache.size()
            ));
        } catch (Exception e) {
            String errorMsg = "Failed to load data from MongoDB";
            plugin.getLogger().log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @SuppressWarnings("unchecked")
    private K convertStringToKey(String idStr) {
        try {
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
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert key '" + idStr +
                    "' to type " + keyClass.getSimpleName(), e);
        }
    }

    @Override
    public Optional<V> get(K key) {
        ensureConnected();
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public synchronized void put(K key, V value) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        cache.put(key, value);
    }

    @Override
    public synchronized void update(K key, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        V value = cache.get(key);
        if (value != null) {
            updater.accept(value);
        }
    }

    @Override
    public synchronized boolean remove(K key) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");

        try {
            collection.deleteOne(Filters.eq("_id", key.toString()));
            return cache.remove(key) != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove document for key " + key + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized void batchUpdate(Collection<K> keys, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(keys, "Keys collection cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        keys.forEach(key -> update(key, updater));
    }
}