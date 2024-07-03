package com.featherservices.quill.config.serialization;

import com.featherservices.quill.config.serialization.collections.ArraySerializer;
import com.featherservices.quill.config.serialization.collections.CollectionSerializer;
import com.featherservices.quill.config.serialization.collections.MapSerializer;
import com.featherservices.quill.config.serialization.minecraft.ComponentSerializer;
import com.featherservices.quill.config.serialization.minecraft.ItemStackSerializer;
import com.featherservices.quill.config.serialization.minecraft.LocationSerializer;
import com.featherservices.quill.config.serialization.minecraft.WorldSerializer;
import com.featherservices.quill.config.serialization.other.EnumSerializer;
import com.featherservices.quill.config.serialization.other.ObjectSerializer;
import com.featherservices.quill.config.serialization.other.UUIDSerializer;
import com.featherservices.quill.config.serialization.primitives.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public final class SerializerFactory {

    public static StringSerializer STRING;
    public static IntegerSerializer INTEGER;
    public static CharacterSerializer CHARACTER;
    public static DoubleSerializer DOUBLE;
    public static BooleanSerializer BOOLEAN;
    public static EnumSerializer ENUM;
    public static WorldSerializer WORLD;
    public static ItemStackSerializer ITEMSTACK;
    public static LocationSerializer LOCATION;
    public static CollectionSerializer COLLECTION;
    public static ComponentSerializer COMPONENT;
    public static MapSerializer MAP;
    public static ArraySerializer ARRAY;
    public static ObjectSerializer OBJECT;
    public static UUIDSerializer UUID;

    @Getter
    private static SerializerFactory factory;

    static {
        factory = new SerializerFactory();

        /* Register build-in serializers */
        factory.register(STRING = new StringSerializer());
        factory.register(INTEGER = new IntegerSerializer());
        factory.register(CHARACTER = new CharacterSerializer());
        factory.register(DOUBLE = new DoubleSerializer());
        factory.register(BOOLEAN = new BooleanSerializer());
        factory.register(ENUM = new EnumSerializer());
        factory.register(WORLD = new WorldSerializer());
        factory.register(ITEMSTACK = new ItemStackSerializer());
        factory.register(LOCATION = new LocationSerializer());
        factory.register(COLLECTION = new CollectionSerializer());
        factory.register(COMPONENT = new ComponentSerializer());
        factory.register(MAP = new MapSerializer());
        factory.register(ARRAY = new ArraySerializer());
        factory.register(OBJECT = new ObjectSerializer());
        factory.register(UUID = new UUIDSerializer());
    }

    private final List<Serializer<?, ?>> serializers;

    private SerializerFactory() {
        serializers = new ArrayList<>();
    }

    public <O, I> void register(Serializer<O, I> serializer){
        this.serializers.add(serializer);
    }

    public Serializer<?, ?> get(Class<?> original){
        for (Serializer<?, ?> serializer : serializers) {
            if (serializer.isCompatibleWith(original)) {
                return serializer;
            }
        }
        return OBJECT;
    }

}
