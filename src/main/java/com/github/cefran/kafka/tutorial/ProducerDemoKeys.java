package com.github.cefran.kafka.tutorial;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    public static void main(String[] args) {
        System.out.println("Hello world!");

        final Logger logger = LoggerFactory.getLogger(ProducerDemoKeys.class);
        //Existen tres pasos necesarios
        // 1.- Crear Producer properties
        Properties properties = new Properties();
        //properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        //Ayudan al producer a saber que dato se esta enviando a kafka y como debe ser serializado
        //properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //properties.setProperty("value.serializer",StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        // 2.- Crear el Producer
        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        ProducerRecord<String, String> record =
                new ProducerRecord<String, String>("first_topic", "hello world two!");
        //También se pudiera hacer dando una key, y así garantizar que las keys que son iguales,
        // siempre van a la misma partición. 

        // 3.- Enviar data - Asynchronous
        producer.send(record, new Callback() {
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                //se ejecuta cada vez que el record es enviado satisfactoriamente o si una excepción es lanzada
                if(e == null){
                    // recordMetadata tiene un monton de metodos que retornan data
                    logger.info("Received new metadata: \n" +
                                "topic: " + recordMetadata.topic() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Parittion: " + recordMetadata.partition()
                    );
                }else{
                    logger.error("error while producing: ", e);
                }
            }
        });
        //como es asincrono, se producer los datos pero no se envian, se necesita un flush

        //flush data
        producer.flush();
        //flush y close producer
        producer.close();
    }
}
