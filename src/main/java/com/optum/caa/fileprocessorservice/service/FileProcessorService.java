package com.optum.caa.fileprocessorservice.service;

import com.optum.caa.fileprocessorservice.entity.CAAMetaInfo;
import com.optum.caa.fileprocessorservice.entity.CAARecordInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * This interface defines the readFileFromAmazonS3Storage method.
 */
public interface FileProcessorService {
    public ResponseEntity<String> readFileFromOptumObjectStorage(String fileLocation, CAAMetaInfo metaInfoDetails, Acknowledgment acknowledgment) throws IOException, ParseException;
    public String saveDataIntoTableAndPublish(List<CAARecordInfo> infoList);

}

