package com.optum.caa.fileprocessorservice.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.Gson;
import com.optum.caa.fileprocessorservice.beans.CESInputData;
import com.optum.caa.fileprocessorservice.constants.CAADataConstant;
import com.optum.caa.fileprocessorservice.entity.CAAMetaInfo;
import com.optum.caa.fileprocessorservice.entity.CAARecordInfo;
import com.optum.caa.fileprocessorservice.repository.CAADataRepository;
import com.optum.caa.fileprocessorservice.repository.CAAMetaInfoRepository;
import com.optum.dwmp.servicenow.beans.ServiceNowProperties;
import com.optum.dwmp.servicenow.service.ServiceNowServiceImp;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.util.CollectionUtils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


@Service
public class FileProcessorServiceImpl implements FileProcessorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileProcessorServiceImpl.class);
    private AmazonS3 s3Client;

    @Value("${bucketName}")
    private String bucketName;

    @Value("${spring.profiles.active}")
    private String envName;

    @Value("${backupfolder}")
    private String backupfolder;

    @Value("${folderName}")
    private String folderName;

    @Value("${recordInfoFile}")
    private String recordInfoFile;

    @Value("${spring.cloud.stream.bindings.supplierProcessor-out-0.destination}")
    private String supplierProcessor;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private CAADataRepository caaDataRepository;

    @Autowired
    private CAAMetaInfoRepository caaMetaInfoRepository;

    private final ServiceNowServiceImp serviceNowService;

    private final ServiceNowProperties serviceNowProperties;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${service-now.flow}")
    private String flow;

    @Value("${service-now.process}")
    private String process;

    @Value("${service-now.error}")
    private String error;

    @Value("${dividedby}")
    private int dividedby;


    private EmailService emailService;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssMS");

    public FileProcessorServiceImpl(AmazonS3 s3Client, ServiceNowProperties serviceNowProperties, ServiceNowServiceImp serviceNowService, EmailService emailService) {
        this.s3Client = s3Client;
        this.serviceNowProperties = serviceNowProperties;
        this.serviceNowService = serviceNowService;
        this.emailService = emailService;
    }

    /*
     * This method is used to read data file from Amazon S3
     * */
    public ResponseEntity<String> readFileFromOptumObjectStorage(String fileLocation, CAAMetaInfo metaInfo, Acknowledgment acknowledgment) throws IOException, ParseException {
        S3ObjectInputStream inputStream = null;
        BufferedReader bufferedReader = null;
        S3Object s3object = null;
        try {
            boolean exist = s3Client.doesObjectExist(bucketName, fileLocation + metaInfo.getInputFileName());
            String cdailyFileType = metaInfo.getInputFileName().contains("CES_CAA_INPUT") ? "CDAILY" : null;
            String fileType = metaInfo.getInputFileName().contains("CES_CAA_QINPUT") ? "QMDAILY" : cdailyFileType;
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String requestId = formatter.format(timestamp);
            metaInfo.setCreatedTimestamp(timestamp);
            metaInfo.setRequestNumber(Long.parseLong(requestId));
            metaInfo.setCreatedBy(CAADataConstant.FILE_PROCESSOR);
            metaInfo.setUpdatedBy(CAADataConstant.FILE_PROCESSOR);
            metaInfo.setFileType(fileType);
            if(fileType == null) {
                createServiceNowIncident(metaInfo.getRequestNumber(), fileType, CAADataConstant.INVALID_FILE_NAME_ERROR, "Incorrect file name");
                metaInfo.setStatus(CAADataConstant.INVALID_FILE_NAME);
                metaInfo.setUpdatedTimestamp(new Timestamp(System.currentTimeMillis()));
                caaMetaInfoRepository.saveAndFlush(metaInfo);
                LOGGER.error("Incorrect file name");
                return new ResponseEntity<>("Incorrect file name ", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (exist) {
                s3object = s3Client.getObject(bucketName, fileLocation + metaInfo.getInputFileName());
                inputStream = s3object.getObjectContent();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                String contents = bufferedReader.lines().collect(Collectors.joining("\n"));
                String[] records = contents.split("\n");
                if (records.length == metaInfo.getReceivedCount()) {
                    if (validateCount(metaInfo, fileType, requestId, records))
                        return new ResponseEntity<>("Data Not Processed ", HttpStatus.INTERNAL_SERVER_ERROR);
                } else {
                    createServiceNowIncident(metaInfo.getRequestNumber(), fileType, CAADataConstant.LENGTH_MISMATCH_ERROR, "MetaInfo records and actual records counts does not matched");
                    metaInfo.setStatus(CAADataConstant.ACTUALINFO_INVALID);
                    metaInfo.setUpdatedTimestamp(new Timestamp(System.currentTimeMillis()));
                    caaMetaInfoRepository.saveAndFlush(metaInfo);
                    LOGGER.error("MetaInfo records and CES Input record's counts does not matched");
                    acknowledgment.acknowledge();
                }
            } else {
                acknowledgment.acknowledge();
                createServiceNowIncident(metaInfo.getRequestNumber(), fileType, CAADataConstant.FILE_NOT_FOUND_ERROR, "File Not Found in Optum Object Storage");
                metaInfo.setStatus(CAADataConstant.FILE_NOT_FOUND);
                metaInfo.setUpdatedTimestamp(new Timestamp(System.currentTimeMillis()));
                caaMetaInfoRepository.saveAndFlush(metaInfo);
                LOGGER.error("File Not Found in Optum Object Storage");
                return new ResponseEntity<>("File Not Found in Optum Object Storage  ", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            emailNotification(fileType,metaInfo.getMetaInfoInputFileName(),metaInfo.getInputFileName(),metaInfo.getReceivedCount(),timestamp);
            LOGGER.info("Email Notification send successfully");
            return ResponseEntity.ok().body("ok");
        } catch (AmazonServiceException e) {
            LOGGER.error("Exception occurred while downloading file from Object Storage");
            throw new AmazonServiceException("Error occurred while downloading file from Object Storage");
        } catch (Exception e) {
            LOGGER.error("Exception occurred in FileProcessmyorServiceImpl::readFileFromAmazonS3Storage():{}", e.getMessage());
            throw new IOException("Exception occurred in FileProcessorServiceImpl::readFileFromAmazonS3Storage()");
        } finally {
            closeObjects(inputStream, bufferedReader, s3object);
        }
    }

    private boolean validateCount(CAAMetaInfo metaInfo, String fileType, String requestId, String[] records) {
        metaInfo.setStatus(CAADataConstant.ACTUALINFO_VALID);
        metaInfo.setProcessedCount(0);
        metaInfo.setFileType(fileType);
        metaInfo.setSuccessCount(0);
        metaInfo.setErrorCount(0);
        caaMetaInfoRepository.save(metaInfo);
        LOGGER.info("Records added in metainfo table FileProcessorServiceImpl:readFileFromOptumObjectStorage: :{}", metaInfo);
        int count = 1;
        int batchCount = 0;
        int recordsCout = 0;
        List<CAARecordInfo> caaRecordInfoList = new ArrayList<>();
        int dividedbyvalue = records.length / dividedby;
        for (String line : records) {
            String[] cesInput = line.split("\\|");
            CAARecordInfo data = new CAARecordInfo();
            CESInputData inputdata = new CESInputData();
            inputdata.setFileType(cesInput[0]);
            inputdata.setJobId(cesInput[1]);
            inputdata.setCustomerNumber(cesInput[2]);
            inputdata.setGroupNumber(cesInput[3]);
            inputdata.setBenefitSet(cesInput[4]);
            inputdata.setPv(cesInput[5]);
            inputdata.setRc(cesInput[6]);
            inputdata.setCoverageType(cesInput[7]);
            inputdata.setSystem(cesInput[8]);
            inputdata.setRequestDate(cesInput[9]);
            inputdata.setMemsCoverageCode(cesInput[10]);
            inputdata.setPolicyNumber(cesInput[11]);
            inputdata.setCardNumber(cesInput[12]);
            data.setRequestNumber(Long.parseLong(requestId));
            data.setCesInput(inputdata);
            Timestamp createdTimestamp = new Timestamp(System.currentTimeMillis());
            data.setCreatedTimestamp(createdTimestamp);
            data.setCreatedBy(CAADataConstant.FILE_PROCESSOR);
            data.setUpdatedBy(CAADataConstant.FILE_PROCESSOR);
            data.setLineNumber(count++);
            caaRecordInfoList.add(data);
            batchCount++;
            recordsCout++;
            if (batchCount == dividedbyvalue || records.length == recordsCout) {
                String saveData = saveDataIntoTableAndPublish(caaRecordInfoList);
                if (CAADataConstant.SUCCESS.equals(saveData)) {
                    batchCount = 0;
                    caaRecordInfoList.clear();
                } else {
                    return true;
                }

            }
        }
        createBackupFolder(metaInfo.getInputFileName());
        return false;
    }

    private void closeObjects(S3ObjectInputStream inputStream, BufferedReader bufferedReader, S3Object s3object) throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (bufferedReader != null) {
            bufferedReader.close();
        }
        if (s3object != null) {
            s3object.close();
        }
    }

    private void createServiceNowIncident(Long requestNo, String fileType, String error, String errorMsg) {
        serviceNowProperties.setDescription(createDescription(fileType, error, errorMsg));
        serviceNowProperties.setShortDescription(error);
        ResponseEntity<String> responseEntity = serviceNowService.generateIncidentTicket(serviceNowProperties);
        LOGGER.info("body {} ", responseEntity.getBody());
        HttpStatus status = responseEntity.getStatusCode();
        JSONObject serviceNowResponse = new JSONObject(responseEntity.getBody());
        JSONObject resultObj= (JSONObject)serviceNowResponse.getJSONArray("result").get(0);

        if (status != HttpStatus.CREATED) { // check status
            LOGGER.error("Issue creating incident {} - {} ", status, resultObj.getString("error_message") );
        } else {
            LOGGER.info("Incident ticket created {} ", responseEntity.getBody());
            Map<String, String> recordMap = new HashMap<String, String>();
            recordMap.put ("IncidentNo", resultObj.getString("display_value"));
            recordMap.put ("IncidentURL", resultObj.getString("record_link"));
            recordMap.put ("ErrorType", error);
            recordMap.put("Description", serviceNowProperties.getDescription());
            recordMap.put("ServiceNowType", serviceNowProperties.getUType());
            recordMap.put ("PriorityType", serviceNowProperties.getPriority());
            recordMap.put("AssignmentGroup", serviceNowProperties.getAssignmentGroup());
            recordMap.put("StateType", serviceNowProperties.getState());
            recordMap.put("RequestNo", requestNo.toString());
            try {
                emailService.sendEmail(recordMap);
            } catch (Exception e) {
                LOGGER.error("Exception occurred while sending email for servicenow");
            }
        }
    }

    /*
     * This method is used to create sub folder inside the backup folder  from Amazon S3
     * */
    public void createBackupFolder(String fileName) {
        try {
            String date = formatter.format(new Date());
            InputStream input = new ByteArrayInputStream(new byte[0]);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(0);
            s3Client.putObject(new PutObjectRequest(bucketName, backupfolder + recordInfoFile + date + "/", input, metadata));
            LOGGER.info("Created the backup folder in Optum Object Storage successfully");
            // copy backup file
            CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucketName,
                    folderName + fileName, bucketName,
                    backupfolder + recordInfoFile + date + "/" + fileName);
            s3Client.copyObject(copyObjRequest);
            s3Client.deleteObject(bucketName, folderName + fileName);
            LOGGER.info("Moved the file to the backup folder");
        } catch (AmazonServiceException e) {
            LOGGER.error("Exception occurred while creating the backup folder in object storage");
            throw new AmazonServiceException("Exception occurred while creating the backup folder in object storage");
        } catch (Exception e) {
            LOGGER.error("Exception occurred in FileProcessorServiceImpl::readFileFromAmazonS3Storage ");
        }
    }

    public String createDescription(String fileType, String error, String errorMsg) {
        String str = "FLOW: " + flow +
                "; PROCESS: " + process +
                "; SUBPROCESS: " + fileType +
                "; ERROR: " + error +
                "; DESCREPTION: " + errorMsg +
                "; DATE: " + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH).format(LocalDateTime.now()) +
                "; TIME: " + DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(LocalTime.now()) +
                "; ENVIRONMENT: " + activeProfile;
        return str;
    }

    public String saveDataIntoTableAndPublish(List<CAARecordInfo> infoList) {
        try {
            if (!CollectionUtils.isEmpty(infoList)) {
                caaDataRepository.saveAll(infoList);
                LOGGER.info("Saving the CES Input records in the recordinfo table ");
                for (CAARecordInfo caaRecordInfo : infoList) {
                    String actualrecords = new Gson().toJson(caaRecordInfo);
                    streamBridge.send(supplierProcessor, actualrecords);
                    LOGGER.info("Publishing the CES Input records to the RECORD EVENT TOPIC {}", actualrecords);
                }
                LOGGER.debug("record posted on kafka successfully");
            }
            return "Success";
        } catch (Exception e) {
            LOGGER.error("Error processing while posting data to kafka");
            return "Error";
        }

    }

    private void emailNotification(String fileType,String metaFile,String dataFfileName,int records,Timestamp timestamp){
        Map<String, String> emailMap = new HashMap<String, String>();
        emailMap.put ("envName", envName);
        emailMap.put ("fileType", fileType);
        emailMap.put ("metaFileName", metaFile);
        emailMap.put ("dataFileName", dataFfileName);
        emailMap.put ("timeStamp", String.valueOf(timestamp));
        emailMap.put ("receiveCount", String.valueOf(records));
        try {
            emailService.sendEmailNotification(emailMap);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while sending email");
        }

    }
}
