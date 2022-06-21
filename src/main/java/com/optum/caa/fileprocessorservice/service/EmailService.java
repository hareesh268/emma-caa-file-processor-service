package com.optum.caa.fileprocessorservice.service;

import com.optum.caa.fileprocessorservice.beans.SendEmailInfoPojo;
import org.apache.commons.collections.MapUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    private VelocityEngine velocityEngine;
    private JavaMailSenderFactory mailSenderFactory;
    private JavaMailSender mailSender;

    @Value("${dw.email.template_path}")
    private String emailTemplatePath;

    @Value("${dw.email.emailFrom}")
    private String emailFrom;

    @Value("${dw.email.emailTo}")
    private String emailTo;

    @Value("${dw.email.emailSubject}")
    private String emailSubject;

    @Value("${dw.email.emailFromName}")
    private String emailFromName;

    @Value("${dw.email.emailNotificationTo}")
    private String emailNotificationTo;

    @Value("${dw.email.emailNotificationSubject}")
    private String emailNotificationSubject;

    @Value("${dw.email.notificationTemplate_path}")
    private String emailNotificationTemplatePath;


    @Autowired
    public EmailService(VelocityEngine velocityEngine, JavaMailSenderFactory mailSenderFactory) {
        this.velocityEngine = velocityEngine;
        this.mailSenderFactory = mailSenderFactory;
    }

    @PostConstruct
    public void init() {
        this.mailSender = mailSenderFactory.getJavaMailSender();
    }

    public String sendEmail(Map<String, String> recordMap) throws UnsupportedEncodingException, MessagingException {

        // prepare and send the email
        SendEmailInfoPojo sendEmailInfoPojo = prepEmail(recordMap);
        return sendMail(sendEmailInfoPojo);
    }

    private SendEmailInfoPojo prepEmail(Map<String, String> recordMap) {
        SendEmailInfoPojo sendEmailInfoPojo = new SendEmailInfoPojo();
        sendEmailInfoPojo.setFromEmailAddress(emailFrom);
        sendEmailInfoPojo.setToEmailAddress(emailTo);
        sendEmailInfoPojo.setSubject(emailSubject + recordMap.get("IncidentNo"));
        sendEmailInfoPojo.setFromName(emailFromName);
        sendEmailInfoPojo.setTemplatePath(emailTemplatePath);
        sendEmailInfoPojo.setMimeBody(getEmailMimeBody(recordMap));

        return sendEmailInfoPojo;

    }

    // Create hash map to use for replacing text fields in email template
    private HashMap<String, String> getEmailMimeBody(Map<String, String> recordMap) {
        HashMap<String, String> mime = new HashMap<>();
        if (recordMap.size() > 0) {
            for (Map.Entry<String, String> RecordData : recordMap.entrySet()) {
                mime.put(RecordData.getKey(), RecordData.getValue());
            }
        }
        return mime;
    }

    /**
     * Sends the prepared email message
     *
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    private String sendMail(SendEmailInfoPojo sendEmailInfoPojo)
            throws UnsupportedEncodingException, MessagingException {

        Template providerMailTemplate;
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        mimeMessageHelper.setFrom(sendEmailInfoPojo.getFromEmailAddress(), sendEmailInfoPojo.getFromName());
        mimeMessageHelper.setSubject(sendEmailInfoPojo.getSubject());
        mimeMessageHelper.setTo(sendEmailInfoPojo.getToEmailAddress());

        VelocityContext velocityContext = new VelocityContext();

        if (!MapUtils.isEmpty(sendEmailInfoPojo.getMimeBody())) {
            Map<String, String> miimBodyMap = sendEmailInfoPojo.getMimeBody();

            for (Map.Entry<String, String> entry : miimBodyMap.entrySet()) {
                String velocityContextKey = entry.getKey();
                velocityContext.put(velocityContextKey, miimBodyMap.get(velocityContextKey));
            }
        }

        StringWriter writer = new StringWriter();
        providerMailTemplate = velocityEngine.getTemplate(sendEmailInfoPojo.getTemplatePath());

        providerMailTemplate.merge(velocityContext, writer);
        mimeMessage.setContent(writer.toString(), "text/html; charset=utf-8");
        mailSender.send(mimeMessage);

        return sendEmailInfoPojo.getSubject();
    }

    public String sendEmailNotification(Map<String, String> notificationMap) throws UnsupportedEncodingException, MessagingException {

        // prepare and send the email
        SendEmailInfoPojo sendEmailInfoPojo = prepEmailNotification(notificationMap);
        return sendMail(sendEmailInfoPojo);
    }

    private SendEmailInfoPojo prepEmailNotification(Map<String, String> notificationMap) {
        SendEmailInfoPojo sendEmailInfoPojo = new SendEmailInfoPojo();
        sendEmailInfoPojo.setFromEmailAddress(emailFrom);
        sendEmailInfoPojo.setToEmailAddress(emailNotificationTo);
        sendEmailInfoPojo.setSubject(emailNotificationSubject );
        sendEmailInfoPojo.setFromName(emailFromName);
        sendEmailInfoPojo.setTemplatePath(emailNotificationTemplatePath);
        sendEmailInfoPojo.setMimeBody(getEmailMimeBody(notificationMap));

        return sendEmailInfoPojo;

    }




}
