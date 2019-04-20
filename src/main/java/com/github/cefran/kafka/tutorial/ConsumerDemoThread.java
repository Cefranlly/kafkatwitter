package com.github.cefran.kafka.tutorial;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoThread {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        new ConsumerDemoThread().run();
    }

    public ConsumerDemoThread(){

    }

    public void run(){
        Logger logger = LoggerFactory.getLogger(ConsumerDemoThread.class.getName());
        //latch for dealing with multiple threads
        CountDownLatch countDownLatch = new CountDownLatch(1);

        //create the consumer runnable
        Runnable myConsumerRunnable = new ConsumerRunnable(countDownLatch);

        //start the thread
        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutdown();
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("application has exited!");
        }
        ));

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        }finally {
            logger.info("Application is closing!");
        }
    }

    public class ConsumerRunnable implements Runnable{

        private CountDownLatch latch; // será capaz de cerrar nuestra aplicación de forma correcta
        private KafkaConsumer<String, String> consumer;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        public ConsumerRunnable(CountDownLatch latch){ // Para manejar la concurrencia
            this.latch = latch;
            Properties properties = new Properties();

            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
            // La razon de esto es que cuando kafka serializa un string a bytes, necesita el consumidor deserializar
            //para retornar la información
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "my-fifth-application");
            //Se pueden tener treas valores earliest/latest/none --> lee desde el inicio/lee desde lo más reciente o lo
            // nuevo/ va a tirar un error si no hay offset siendo guardados
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

            //create Consumer
            consumer = new KafkaConsumer<String, String>(properties);

            //subscribe consumer to our topic(s)
            //puedes tener un patron o colección de topicos; ej: una Array con varios topicos
            consumer.subscribe(Collections.singleton("first_topic"));
        }
        @Override
        public void run() {
            try{
                while(true) {
                    ConsumerRecords<String, String> records =  consumer.poll(Duration.ofMillis(100));

                    for(ConsumerRecord<String, String> record : records){

                        logger.info("Key: " + record.key());
                        logger.info("Value: " + record.value());
                        logger.info("Partition: " + record.partition());
                        logger.info("Offset: " + record.offset());
                    }
                }
        }catch (WakeupException e){
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                latch.countDown(); // permite al main code comprender que terminamos con el consumer
            }
        }

        public void shutdown(){
            // wakeup es un metodo especial para interrumpir consumer.poll()
            // Va a arrojar una excepción que se llama WakeupException
            consumer.wakeup();
        }
    }
}
