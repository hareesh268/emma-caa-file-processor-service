package com.optum.caa.fileprocessorservice.config;

import com.google.gson.Gson;
import com.optum.caa.fileprocessorservice.entity.CAAMetaInfo;
import com.optum.caa.fileprocessorservice.repository.CAAMetaInfoRepository;
import com.optum.caa.fileprocessorservice.service.FileProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import java.util.function.Consumer;

@Configuration

public class CAAConsumerDataConfig {
    Logger logger = LoggerFactory.getLogger(CAAConsumerDataConfig.class);


    private FileProcessorService fileProcessorService;

    @Autowired
    public CAAConsumerDataConfig(FileProcessorService fileProcessorService) {
        this.fileProcessorService = fileProcessorService;
    }

    @Value("${bucketName}")
    private String bucketName;

    @Value("${folderName}")
    private String folderName;

    @Autowired
    private CAAMetaInfoRepository caaMetaInfoRepository;

    /**
     * This method consumes the data from File Event Kafka topic and It will save the MetaInfo data in table
     */
    @Bean
    public Consumer<Message<?>> fileEventDataConsumer() {
        return message -> {
            CAAMetaInfo metaInfoDetails = new Gson().fromJson(message.getPayload().toString(), CAAMetaInfo.class);
            try {
                if (Optional.ofNullable(metaInfoDetails.getReceivedCount()).isPresent()) {
                    checkProcessorDetails(message, metaInfoDetails);
                }
            } catch (IOException e) {
                logger.error("Failed to create entry in the table {}", e.getMessage());
            } catch (KafkaException exception) {
                handleProcessingException(message.getPayload().toString(), exception);
            } catch (Exception exception) {
                logger.error("Failed to process record ", exception.getCause());
            }
        };
    }

    private void checkProcessorDetails(Message<?> message, CAAMetaInfo metaInfoDetails) throws IOException, ParseException {
        logger.info("Received a message :{}", metaInfoDetails);
        Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
        CAAMetaInfo metaInfo = caaMetaInfoRepository.findByMetaInfoInputFileName(metaInfoDetails.getMetaInfoInputFileName());
        if (Optional.ofNullable(metaInfo).isPresent()) {
            logger.info("MetaInfoInput file is already exist");
            if (Optional.ofNullable(acknowledgment).isPresent()) {
                acknowledgment.acknowledge();
                logger.info("Acknowledged successfully :{}", metaInfoDetails);
            }
        } else {
            ResponseEntity<String> response = fileProcessorService.readFileFromOptumObjectStorage(folderName, metaInfoDetails,acknowledgment);
            if (response.getStatusCodeValue() == 200 && Optional.ofNullable(acknowledgment).isPresent()) {
                acknowledgment.acknowledge();
                logger.info("Acknowledged successfully :CAAConsumerDataConfig:fileEventDataConsumer");
            }
        }
    }

    /*
     * This method is used handle exception while message processing
     * */
    public void handleProcessingException(String kafkaMessage, Exception e) {
        String msg;
        if (Optional.ofNullable(kafkaMessage).isPresent()) {
            msg = "Unexpected error occurred while processing record";
        } else {
            msg = "empty record found";
        }
        logger.error(msg, e.getCause());
    }
}
