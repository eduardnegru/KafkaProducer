import org.apache.kafka.clients.producer.KafkaProducer;

public class ProducerFactory {

    public static final String TRAINING_DATASET = "training_dataset";
    public static final String GOOGLE_CLOUD_SPEECH_TO_TEXT = "speech_to_text";
    public static final String TWITTER_CLIENT = "twitter";
    public static final String TOPIC = "first_topic"; // this must be created from cli. TODO change it

    private KafkaProducer<String, String> kafkaProducer;

    public ProducerFactory(KafkaProducer<String, String> kafkaProducer)
    {
        this.kafkaProducer = kafkaProducer;
    }

    public DataSource make(String dataSource)
    {
        switch (dataSource)
        {
            case TRAINING_DATASET:
                return new TrainingDataset(this.kafkaProducer);
            case GOOGLE_CLOUD_SPEECH_TO_TEXT:
                return new SpeechToText(this.kafkaProducer);
            case TWITTER_CLIENT:
                return new TwitterMessages(this.kafkaProducer);

            default:
                return null;
        }
    }

}
