package com.optum.caa.fileprocessorservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.optum.dwmp.servicenow.beans.ServiceNowProperties;
import com.optum.dwmp.servicenow.service.ServiceNowServiceImp;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest

public class EmailServiceTest {

    @Autowired
    FileProcessorServiceImpl fileProcessorServiceImpl;
    @Autowired
    EmailService emailService;
    @Mock
    VelocityEngine velocityEngine;
    @Mock
    JavaMailSenderFactory mailSenderFactory;
    @Autowired
    private AmazonS3 s3Client;
    @Autowired
    private ServiceNowProperties serviceNowProperties;
    @Autowired
    private ServiceNowServiceImp serviceNowService;

    Map<String, String> notificationMap= new HashMap<>();
    Map<String, String> recordMap= new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceTest.class);

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    @Before
    public void setup() {

        emailService = new EmailService(this.velocityEngine, this.mailSenderFactory);
        ReflectionTestUtils.setField(emailService, "emailTo", "pritesh_raut@optum.com");
        ReflectionTestUtils.setField(emailService, "emailSubject", "CAA-DataFile");
        ReflectionTestUtils.setField(emailService, "template_path", "/templates/emailTemplate.vm");
        ReflectionTestUtils.setField(emailService, "emailNotificationTo", "pritesh_raut@optum.com");
        ReflectionTestUtils.setField(emailService, "emailNotificationSubject", "CAA-DataFile received");
        ReflectionTestUtils.setField(emailService, "emailNotificationTemplatePath", "/templates/emailNotificationTemplate.vm");
    }

    @Test
    public void validateSendEmail() throws UnsupportedEncodingException, MessagingException {
        /*
        FileProcessorServiceImpl fileProcessorServiceImpl = new FileProcessorServiceImpl(this.s3Client,this.serviceNowProperties,this.serviceNowService,this.emailService);
        Method method = FileProcessorServiceImpl.class.getDeclaredMethod("createServiceNowIncident");

        FileProcessorServiceImpl.class.getClassLoader().createServiceNowIncident();
        method.setAccessible(true);
        Object obj = null;
        method.invoke(obj);
         */
        emailService.sendEmail(recordMap);
        Assert.assertTrue(true);
    }


    @Test
    public void validateSendEmailNotificationwithValidEmailId() throws UnsupportedEncodingException, MessagingException {
        ReflectionTestUtils.setField(emailService, "emailNotificationTo", "pritesh_raut@optum.com");
        notificationMap.put("fileType","CDAILY");
        notificationMap.put("metaFileName","Test_metaInfoFile202142.txt");
        notificationMap.put("dataFileName","Test_CES_CAA_INPUT_202110080502.txt");
        notificationMap.put("receiveCount","12075");
        notificationMap.put("timeStamp",String.valueOf(timestamp));
        String result=emailService.sendEmailNotification(notificationMap);
        LOGGER.info("Email Notification has been sent sucessfully ");
        Assert.assertEquals("CAA-DataFile received",result);
    }

    @Test
    public void validateSendEmailNotificationwithInValidEmailId() {
        ReflectionTestUtils.setField(emailService, "emailNotificationTo", "");
        notificationMap.put("fileType","CDAILY");
        notificationMap.put("metaFileName","Test_metaInfoFile202142.txt");
        notificationMap.put("dataFileName","Test_CES_CAA_INPUT_202110080502.txt");
        notificationMap.put("receiveCount","12075");
        notificationMap.put("timeStamp",String.valueOf(timestamp));
        //String result=emailService.sendEmailNotification(notificationMap);
        //System.out.println("Send Email Notification Result : "+result);
        LOGGER.info("Invalid Email Id");
        Assert.assertThrows(Exception.class,()->emailService.sendEmailNotification(notificationMap));
    }

}
