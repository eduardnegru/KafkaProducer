import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterMessages extends DataSource {


	private KafkaProducer<String, String> kafkaProducer;
	public static List<String> keywords = new ArrayList<>();
	public static boolean RESET = false;

	public TwitterMessages(KafkaProducer<String, String> kafkaProducer)
	{
		this.kafkaProducer = kafkaProducer;
		this.dataSourceName = "twitter";
		keywords = Lists.newArrayList("romania");
	}

	public Client createTwitterClient(BlockingQueue<String> msgQueue)
	{

		/**Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

		List<String> searchKeywords = TwitterMessages.keywords;
		List<String> languages = Lists.newArrayList("en");

		//search for keywords in each tweet
		hosebirdEndpoint.trackTerms(searchKeywords);
		//filter only tweets in English
		hosebirdEndpoint.languages(languages);

		Authentication hosebirdAuth = new OAuth1(Config.TWITTER_CONSUMER_KEY, Config.TWITTER_CONSUMER_SECRED, Config.TWEETER_TOKEN, Config.TWITTER_SECRET);

		ClientBuilder builder = new ClientBuilder()
				.name("Hosebird-Client-01")
				.hosts(hosebirdHosts)
				.authentication(hosebirdAuth)
				.endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue));

		return builder.build();
	}

	@Override
	public void run()
	{
		System.out.println("Twitter client started running");
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(1000);
		Client client = this.createTwitterClient(msgQueue);
		client.connect();

		// on a different thread, or multiple different threads....
		while (!client.isDone()) {
			String msg = null;
			try
			{
				if(RESET)
				{
					System.out.println("resetting twitter client");
					System.out.println(TwitterMessages.keywords);
					client.stop();
					client = this.createTwitterClient(msgQueue);
					client.connect();
					RESET = false;
				}

				if (!Producer.dataSourceHashMap.get(this.dataSourceName).isRunning())
				{
					synchronized (Producer.dataSourceHashMap.get(this.dataSourceName))
					{
						System.out.println(Producer.dataSourceHashMap.get(this.dataSourceName) + " stopped");
						Producer.dataSourceHashMap.get(this.dataSourceName).wait();
						System.out.println("notified");
					}
				}


				msg = msgQueue.poll(5, TimeUnit.SECONDS);

				if(msg != null)
				{
					JsonParser parser = new JsonParser();
					JsonObject obj = parser.parse(msg).getAsJsonObject();
					String message = obj.get("text").getAsString();

					JsonObject jsonObject = new JsonObject();

					jsonObject.addProperty("source", this.dataSourceName);
					jsonObject.addProperty("textContent", message);
					jsonObject.addProperty("textCreatedTimestamp", Long.toString(System.currentTimeMillis() / 1000L));
					ProducerRecord<String, String> record = new ProducerRecord<String, String>(ProducerFactory.TOPIC, jsonObject.toString());
					System.out.println("[Twitter] Sent message " + message);
					kafkaProducer.send(record);
					kafkaProducer.flush();
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				client.stop();
			}
		}
	}
}
