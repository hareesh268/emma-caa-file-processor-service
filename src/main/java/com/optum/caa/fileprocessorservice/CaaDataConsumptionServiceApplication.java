package com.optum.caa.fileprocessorservice;

import com.optum.dwmp.servicenow.beans.ServiceNowProperties;
import com.optum.dwmp.servicenow.service.ServiceNowServiceImp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import java.util.TimeZone;

@Import({ServiceNowProperties.class, ServiceNowServiceImp.class, RestTemplate.class})
@SpringBootApplication
public class CaaDataConsumptionServiceApplication {

    public static void main(String[] args) {
        //setting default time zone to CST (including daylight savings time)
        TimeZone.setDefault(TimeZone.getTimeZone("CST6CDT"));
        SpringApplication.run(CaaDataConsumptionServiceApplication.class, args);
    }

}
