import com.google.gson.JsonObject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;


public class TrainingDataset extends DataSource
{
//	public static final String INPUT_FILE_CSV = "/home/licenta/train/ml/train.csv";
	public static final String INPUT_FILE_CSV = "/home/RealTimeToxicityDetection/perspective_api/original_test.csv";
	public static final File csvData = new File(INPUT_FILE_CSV);
	private KafkaProducer<String, String> kafkaProducer;
	public static final int MESSAGE_SEND_INTERVAL_MILLISECONDS = 10000;
	public static final String DATASOURCE_NAME = "training_dataset";

	public TrainingDataset(KafkaProducer<String, String> kafkaProducer)
	{
		this.kafkaProducer = kafkaProducer;
		this.dataSourceName = DATASOURCE_NAME;
	}

	@Override
	public void run()
	{
		try
		{
			int i = 0;

			CSVParser csvParser = CSVParser.parse(csvData, Charset.defaultCharset(), CSVFormat.DEFAULT);

			for (CSVRecord csvRecord : csvParser)
			{
				while(true)
				{
					if (!Producer.dataSourceHashMap.get(this.dataSourceName).isRunning())
					{

						synchronized (Producer.dataSourceHashMap.get(this.dataSourceName))
						{
							System.out.println(Producer.dataSourceHashMap.get(this.dataSourceName) + " stopped");
							Producer.dataSourceHashMap.get(this.dataSourceName).wait();
						}
					}
					else
					{
						break;
					}
				}

				if(i % 1000 == 0)
				{
					System.out.println(i);
				}

				Iterator iterator = csvRecord.iterator();
				JsonObject jsonObject = new JsonObject();

				jsonObject.addProperty("source", this.dataSourceName);
//				iterator.next();
				jsonObject.addProperty("textContent", (String)iterator.next());
//				iterator.next();
				jsonObject.addProperty("textCreatedTimestamp", Long.toString(System.currentTimeMillis() / 1000L));
				ProducerRecord<String, String> record = new ProducerRecord<String, String>(ProducerFactory.TOPIC, jsonObject.toString());
				kafkaProducer.send(record);
				kafkaProducer.flush();
				System.out.println("[Training Dataset] Sent message");
				Thread.sleep(MESSAGE_SEND_INTERVAL_MILLISECONDS);
				i += 1;
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			kafkaProducer.close();
		}

		System.out.println("finished");
	}
}
