package com.optum.caa.fileprocessorservice.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.optum.caa.fileprocessorservice.entity.CAAMetaInfo;
import com.optum.caa.fileprocessorservice.repository.CAADataRepository;
import com.optum.caa.fileprocessorservice.repository.CAAMetaInfoRepository;
import com.optum.dwmp.servicenow.beans.ServiceNowProperties;
import com.optum.dwmp.servicenow.service.ServiceNowServiceImp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest

public class FileProcessorServiceImplTest {

    @Autowired
    StreamBridge streamBridge;

    @Autowired
    FileProcessorServiceImpl fileProcessorServiceImpl;

    @Autowired
    EmailService emailService;

    @Autowired
    AmazonS3 s3Client;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

    @Mock
    CAADataRepository caaDataRepository;

    @Mock
    CAAMetaInfoRepository caaMetaInfoRepository;

    @Mock
    ServiceNowServiceImp serviceNowService;

    @Mock
    ServiceNowProperties serviceNowProperties;

    @Mock
    AmazonS3 s3;

    @Mock
    Acknowledgment acknowledgment;

    @Before
    public void setup() {

        fileProcessorServiceImpl = new FileProcessorServiceImpl(this.s3Client, this.serviceNowProperties,
                this.serviceNowService, this.emailService);
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "bucketName", "emmacaadev");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "backupfolder", "CAA_backup/");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "folderName", "CAA/");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "recordInfoFile", "caa_recordinfofile/");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "supplierProcessor", "kaas.emmadev.caa.recordevent");
    }

    /* test case for file not exist in optum object storage */
    @Ignore
    @Test
    public void testFileNotExistInOptumObjectStorage() throws IOException, ParseException {
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        caaMetaInfo.setInputFileName("test.txt");
        Assert.assertThrows(IOException.class,()->fileProcessorServiceImpl.readFileFromOptumObjectStorage("CAA/",caaMetaInfo,acknowledgment));
    }

    @Test
    public void testReadFileFromOptumObjectStorageWithQInputFile() throws IOException,ParseException {
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "bucketName", "emmacaastage");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "caaMetaInfoRepository", caaMetaInfoRepository);
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "caaDataRepository", caaDataRepository);
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "streamBridge", streamBridge);
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        s3Client.putObject("emmacaastage", "CAA" + "/" + "CES_CAA_QINPUT_20210831075712_Test.txt", new File(FileProcessorServiceImpl.class.getClassLoader().getResource("CES_CAA_QINPUT_20210831075712_Test.txt").getFile()));
        s3Client.putObject("emmacaastage", "CAA" + "/" + "metaInfoFile20210831075712.txt", new File(FileProcessorServiceImpl.class.getClassLoader().getResource("metaInfoFile20210831075712.txt").getFile()));
        caaMetaInfo.setInputFileName("CES_CAA_QINPUT_20210831075712_Test.txt");
        caaMetaInfo.setReceivedCount(1);
        ResponseEntity<String> responseEntity = fileProcessorServiceImpl.readFileFromOptumObjectStorage("CAA/", caaMetaInfo, acknowledgment);
        System.out.println(responseEntity.getStatusCodeValue());
        s3Client.deleteObject("emmacaastage", "CAA" + "/" + "metaInfoFile20210831075712.txt");
        Assert.assertEquals(200, responseEntity.getStatusCodeValue());
    }


    @Test
    public void testReadFileFromOptumObjectStorageWithInputFile() throws IOException,ParseException {
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "bucketName", "emmacaastage");
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "caaMetaInfoRepository", caaMetaInfoRepository);
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "caaDataRepository", caaDataRepository);
        ReflectionTestUtils.setField(fileProcessorServiceImpl, "streamBridge", streamBridge);
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        s3Client.putObject("emmacaastage", "CAA" + "/" + "CES_CAA_INPUT_20210831075713_Test.txt", new File(FileProcessorServiceImpl.class.getClassLoader().getResource("CES_CAA_INPUT_20210831075713_Test.txt").getFile()));
        s3Client.putObject("emmacaastage", "CAA" + "/" + "metaInfoFile20210831075713.txt", new File(FileProcessorServiceImpl.class.getClassLoader().getResource("metaInfoFile20210831075713.txt").getFile()));
        caaMetaInfo.setInputFileName("CES_CAA_INPUT_20210831075713_Test.txt");
        caaMetaInfo.setReceivedCount(1);
        ResponseEntity<String> responseEntity = fileProcessorServiceImpl.readFileFromOptumObjectStorage("CAA/", caaMetaInfo,acknowledgment);
        System.out.println(responseEntity.getStatusCodeValue());
        s3Client.deleteObject("emmacaastage", "CAA" + "/" + "metaInfoFile20210831075713.txt");
        Assert.assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Ignore
    @Test
    public void testRecordCountMissMatch() throws Exception {
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        caaMetaInfo.setInputFileName("CES_CAA_QINPUT_20210831075712_Test.txt");
        caaMetaInfo.setReceivedCount(3);
        Assert.assertThrows(IOException.class,()->fileProcessorServiceImpl.readFileFromOptumObjectStorage("CAA/",caaMetaInfo,acknowledgment));
    }

    @Test
    public void testAmazonServiceException() {
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        fileProcessorServiceImpl = new FileProcessorServiceImpl(s3,serviceNowProperties,serviceNowService,emailService);
        when(s3.doesObjectExist(any(),any())).thenThrow(new AmazonServiceException("Error"));
        Assert.assertThrows(AmazonServiceException.class,()->fileProcessorServiceImpl.readFileFromOptumObjectStorage("CAA/",caaMetaInfo,acknowledgment));
    }

    @Test
    public void testExceptionInFileProcessorServiceImpl() {
        CAAMetaInfo caaMetaInfo = new CAAMetaInfo();
        FileProcessorServiceImpl fileProcessorServiceImpl1 = new FileProcessorServiceImpl(s3Client,serviceNowProperties,serviceNowService,emailService);
        Assert.assertThrows(Exception.class,()->fileProcessorServiceImpl1.readFileFromOptumObjectStorage("CAA/",caaMetaInfo,acknowledgment));
    }

    @Test
    public void testExceptionInFileProcessorServiceImplCreateBackup(){
        FileProcessorServiceImpl fileProcessorServiceImpl1 = new FileProcessorServiceImpl(s3Client,serviceNowProperties,serviceNowService,emailService);
        fileProcessorServiceImpl1.createBackupFolder("test");
        Assert.assertTrue(true);
    }

    @Test
    public void testAmazonServiceExceptionCreateBackup(){
        fileProcessorServiceImpl = new FileProcessorServiceImpl(s3,serviceNowProperties,serviceNowService,emailService);
        when(s3.putObject(any(PutObjectRequest.class))).thenThrow(new AmazonServiceException("Error"));
        Assert.assertThrows(AmazonServiceException.class,()->fileProcessorServiceImpl.createBackupFolder("test"));
    }
}