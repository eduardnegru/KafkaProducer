import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.sound.sampled.*;
import javax.xml.crypto.Data;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Producer
{

    public static String[] dataSourceNames = new String[]{ProducerFactory.TRAINING_DATASET, ProducerFactory.GOOGLE_CLOUD_SPEECH_TO_TEXT, ProducerFactory.TWITTER_CLIENT};
//    public static String[] dataSourceNames = new String[]{ProducerFactory.TRAINING_DATASET};
    public static HashMap<String, DataSourceWrapper> dataSourceHashMap = new HashMap<>();
    public static ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void main(String[] args)
    {
        Server server = new Server(8888);
        server.start();

        //Setting kafka properties used for communication with Kafka brokers
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        int threadPoolSize = dataSourceNames.length;

        //Kafka producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(properties);

        ProducerFactory producerFactory = new ProducerFactory(kafkaProducer);

        for(int i = 0; i < dataSourceNames.length; i++)
        {
            DataSourceWrapper dataSourceWrapper = new DataSourceWrapper(producerFactory.make(dataSourceNames[i]),  true);
            dataSourceHashMap.put(dataSourceNames[i], dataSourceWrapper);
        }

        System.out.println(dataSourceHashMap);

        Iterator it = dataSourceHashMap.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<String, DataSourceWrapper> pair = (Map.Entry<String, DataSourceWrapper>)it.next();
            System.out.println("starting " + pair.getKey());
            executorService.submit(pair.getValue().getDataSource());
        }

        executorService.shutdown();
    }
}
