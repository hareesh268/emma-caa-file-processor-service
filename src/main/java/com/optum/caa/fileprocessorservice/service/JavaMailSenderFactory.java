package com.optum.caa.fileprocessorservice.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
public class JavaMailSenderFactory {

    @Value("${spring.mail.host}")
    private String mailHost;
    @Value("${spring.mail.port}")
    private Integer mailPort;
    @Value("${spring.mail.props.mail.debug}")
    private String mailDebug;

    @Bean
    public VelocityEngine getVelocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        return velocityEngine;
    }

    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.debug", mailDebug);
        mailSender.setJavaMailProperties(props);

        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        return mailSender;
    }

}
