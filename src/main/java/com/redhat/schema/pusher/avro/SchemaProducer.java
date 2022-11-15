package com.redhat.schema.pusher.avro;

import java.io.IOException;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class SchemaProducer<K,V> implements Producer<K, V>  {

    private ProducerConfig producerConfig;
    private String clientId;
    private Serializer<K> keySerializer;
    private Serializer<V> valueSerializer;

    @Override
    public void close() throws IOException {
    }
    
    public SchemaProducer(final Map<String, Object> configs) {
        this(configs, null, null);
    }


    public SchemaProducer(Map<String, Object> configs, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this(new ProducerConfig(appendSerializerToConfig(configs, keySerializer, valueSerializer)),
                keySerializer, valueSerializer);
    }


    public SchemaProducer(Properties properties) {
        this(properties, null, null);
    }


    public SchemaProducer(Properties properties, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this(Utils.propsToMap(properties), keySerializer, valueSerializer);
    }

    @SuppressWarnings("unchecked")
    SchemaProducer(ProducerConfig config,
                  Serializer<K> keySerializer,
                  Serializer<V> valueSerializer) {

        this.producerConfig = config;

        String transactionalId = config.getString(ProducerConfig.TRANSACTIONAL_ID_CONFIG);

        this.clientId = config.getString(ProducerConfig.CLIENT_ID_CONFIG);

        LogContext logContext;
        if (transactionalId == null)
            logContext = new LogContext(String.format("[Producer clientId=%s] ", clientId));
        else
            logContext = new LogContext(String.format("[Producer clientId=%s, transactionalId=%s] ", clientId, transactionalId));
        Logger log = logContext.logger(SchemaProducer.class);
        log.trace("Starting the Kafka producer");

        if (keySerializer == null) {
            this.keySerializer = config.getConfiguredInstance(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                                                                     Serializer.class);
            this.keySerializer.configure(config.originals(Collections.singletonMap(ProducerConfig.CLIENT_ID_CONFIG, clientId)), true);
        } else {
            config.ignore(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
            this.keySerializer = keySerializer;
        }
        if (valueSerializer == null) {
            this.valueSerializer = config.getConfiguredInstance(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                                                                       Serializer.class);
            this.valueSerializer.configure(config.originals(Collections.singletonMap(ProducerConfig.CLIENT_ID_CONFIG, clientId)), false);
        } else {
            config.ignore(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
            this.valueSerializer = valueSerializer;
        }

    }

    static Map<String, Object> appendSerializerToConfig(Map<String, Object> configs,
            Serializer<?> keySerializer,
            Serializer<?> valueSerializer) {
        // validate serializer configuration, if the passed serializer instance is null, the user must explicitly set a valid serializer configuration value
        Map<String, Object> newConfigs = new HashMap<>(configs);
        if (keySerializer != null)
            newConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer.getClass());
        else if (newConfigs.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG) == null)
            throw new ConfigException(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, null, "must be non-null.");
        if (valueSerializer != null)
            newConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer.getClass());
        else if (newConfigs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG) == null)
            throw new ConfigException(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, null, "must be non-null.");
        return newConfigs;
    }

    public void send(ProducerRecord<K, V> record) {
        try {
            keySerializer.serialize(record.topic(), record.headers(), record.key());
        } catch (ClassCastException cce) {
            throw new SerializationException("Can't convert key of class " + record.key().getClass().getName() +
                    " to class " + producerConfig.getClass(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG).getName() +
                    " specified in key.serializer", cce);
        }
        try {
            valueSerializer.serialize(record.topic(), record.headers(), record.value());
        } catch (ClassCastException cce) {
            throw new SerializationException("Can't convert value of class " + record.value().getClass().getName() +
                    " to class " + producerConfig.getClass(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG).getName() +
                    " specified in value.serializer", cce);
        }
    }
}
