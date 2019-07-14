import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import org.apache.kafka.clients.producer.KafkaProducer;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.ArrayList;

public class SpeechToText extends DataSource
{
	private KafkaProducer<String, String> kafkaProducer;
	public static final String DATASOURCE_NAME = "speech_to_text";

	public SpeechToText(KafkaProducer<String, String> kafkaProducer)
	{
		this.kafkaProducer = kafkaProducer;
		this.dataSourceName = DATASOURCE_NAME;
	}
	public ResponseObserver<StreamingRecognizeResponse> createObserver()
	{
		return new ResponseObserver<StreamingRecognizeResponse>() {

					public void onStart(StreamController controller)
					{
						System.out.println("Request started");
					}

					public void onResponse(StreamingRecognizeResponse response)
					{
						try
						{
							JsonObject jsonObject = new JsonObject();

							if(response.getResultsList().size() != 0)
							{
								String message = response.getResultsList().get(0).getAlternativesList().get(0).getTranscript();
								jsonObject.addProperty("source", dataSourceName);
								jsonObject.addProperty("textContent", message);
								jsonObject.addProperty("textCreatedTimestamp", Long.toString(System.currentTimeMillis() / 1000L));

								ProducerRecord<String, String> record = new ProducerRecord<String, String>(ProducerFactory.TOPIC, jsonObject.toString());

								kafkaProducer.send(record);
								kafkaProducer.flush();

								System.out.println("[Google] Sent message " + message);
							}

						}
						catch (Exception e)
						{
						}

					}

					public void onComplete()
					{

					}

					public void onError(Throwable t)
					{
						System.out.println("CreateObserver");
					}
				};
	}

	/** Performs microphone streaming speech recognition with a duration of 1 minute. */
	public void streamingMicRecognize() throws Exception
	{
		ResponseObserver<StreamingRecognizeResponse> responseObserver = null;

		try
		{

			while(true)
			{
				if (!Producer.dataSourceHashMap.get(DATASOURCE_NAME).isRunning())
				{
					synchronized (Producer.dataSourceHashMap.get(DATASOURCE_NAME))
					{
						System.out.println(Producer.dataSourceHashMap.get(DATASOURCE_NAME) + " stopped");
						try
						{
							Producer.dataSourceHashMap.get(DATASOURCE_NAME).wait();

						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}

				SpeechClient client = SpeechClient.create();

				responseObserver = createObserver();

				ClientStream<StreamingRecognizeRequest> clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

				RecognitionConfig recognitionConfig =
						RecognitionConfig.newBuilder()
								.setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
								.setLanguageCode("en-US")
								.setSampleRateHertz(16000)
								.build();

				StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build();

				StreamingRecognizeRequest request =
						StreamingRecognizeRequest.newBuilder()
								.setStreamingConfig(streamingRecognitionConfig)
								.build(); // The first request in a streaming call has to be a config


				AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);

				DataLine.Info targetInfo =
						new DataLine.Info(
								TargetDataLine.class,
								audioFormat); // Set the system information to read from the microphone audio stream

				if (!AudioSystem.isLineSupported(targetInfo))
				{
					System.out.println("Microphone not supported");
					System.exit(0);
				}

				clientStream.send(request);

				// Target data line captures the audio stream the microphone produces.
				TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
				targetDataLine.open(audioFormat);
				targetDataLine.start();

				System.out.println("Google speech to text service is running. You can start speaking");

				// Audio Input Stream
				AudioInputStream audio = new AudioInputStream(targetDataLine);

				while (true)
				{
					 if (!Producer.dataSourceHashMap.get(DATASOURCE_NAME).isRunning())
					{
						break;
					}

					byte[] data = new byte[6400];
					audio.read(data);
//					if (estimatedTime > 60000) { // 60 seconds
//						System.out.println("Stop speaking.");
//						targetDataLine.stop();
//						targetDataLine.close();
//						break;
//					}
					request =
							StreamingRecognizeRequest.newBuilder()
									.setAudioContent(ByteString.copyFrom(data))
									.build();
					clientStream.send(request);

				}

			}
		}
		catch (Exception e)
		{
			System.out.println(e);
			System.out.println("Speech to text service stopped working. Please restart the producer.");
		}
		finally
		{
			kafkaProducer.close();
		}

	}


	@Override
	public void run()
	{
		try
		{
			this.streamingMicRecognize();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
