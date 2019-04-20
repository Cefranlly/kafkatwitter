package com.github.cefran.kafka.twitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());
    // (API key)
    private final static String API_KEY = "MDlGtBsXTwbfT8AmrzKOF8Giw";
    // (API secret key)
    private  final static String API_SECRET_KEY = "msltKgyCatXaPDvuQSJ9uc3iGBrgfYyZdJLxLUzHh9feMNUV2t";
    // (Access token)
    private final static String ACCESS_TOKEN = "1099485563944603648-IsQJ4G6VfeUExlkXoemILtnITZ85xh";
    // (Access token key)
    private final static String ACCESS_TOKEN_KEY = "6Zs6FVLRyaMSUOBOevAHJgks3RX9jsCnUZGey4Np3YsnO";
    private List<String> terms = Lists.newArrayList("Macri");

    public TwitterProducer(){}

    public static void main(String[] args) {
        System.out.println("hello World!");
        new TwitterProducer().run();
    }

    public void run(){
        logger.info("Setup!");
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        // create a twitter client
        Client client = createTwitterClient(msgQueue);
        client.connect();

        // create a kafka producer
        KafkaProducer<String, String> producer = createKafkaProducer();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping application...");
            logger.info("shutting down client from twitter...");
            client.stop();
            logger.info("closing producer...");
            producer.close(); // esto es para que envie todos los mensajes que tiene en memoria antes de cerrar la app
            logger.info("done!");
        }));
        // on a different thread, or multiple different threads....
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }

            if (msg != null){
                logger.info(msg);
                producer.send(new ProducerRecord<>("twitter_tweets", null, msg), new Callback(){

                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if(e != null){
                            logger.error("Something bad happened", e);
                        }
                    }
                });
            }
        }
        logger.info("End of application!");
        // loop to send tweets to kafka
    }

    public Client createTwitterClient(BlockingQueue<String> msgQueue){
        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        //List<Long> followings = Lists.newArrayList(1234L, 566788L);
        hosebirdEndpoint.trackTerms(terms);

        //MDlGtBsXTwbfT8AmrzKOF8Giw (API key)
        //msltKgyCatXaPDvuQSJ9uc3iGBrgfYyZdJLxLUzHh9feMNUV2t (API secret key)
        //1099485563944603648-IsQJ4G6VfeUExlkXoemILtnITZ85xh (Access token)
        //6Zs6FVLRyaMSUOBOevAHJgks3RX9jsCnUZGey4Np3YsnO (Access token secret)
        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(API_KEY, API_SECRET_KEY, ACCESS_TOKEN, ACCESS_TOKEN_KEY);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01") // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));
                //.eventMessageQueue(eventQueue); // optional: use this if you want to process client events

        Client hosebirdClient = builder.build();
        // Attempts to establish a connection.
        return hosebirdClient; //.connect();

    }

    public KafkaProducer<String, String> createKafkaProducer(){
        Properties properties = new Properties();
        //properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        //Ayudan al producer a saber que dato se esta enviando a kafka y como debe ser serializado
        //properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //properties.setProperty("value.serializer",StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

        // 2.- Create safe Producer
        // KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); //because kafka 2.0 >= 1.1 so we can keep this as 5. Use 1 otherwise.

        return new KafkaProducer<String, String>(properties);

    }

}
