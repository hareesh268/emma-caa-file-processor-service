package com.optum.caa.fileprocessorservice.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailInfoPojo {
    private String templatePath ;
    private String fromEmailAddress;
    private String toEmailAddress;
    private String messageBody;
    private String fromName;
    private String subject;
    private Map<String, String> mimeBody;
}
