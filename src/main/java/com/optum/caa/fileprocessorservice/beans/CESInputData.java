package com.optum.caa.fileprocessorservice.beans;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"fileType", "jobId", "customerNumber", "groupNumber", "benefitSet", "pv", "rc", "coverageType", "system", "requestDate", "memsCoverageCode", "policyNumber", "cardNumber"})
public class CESInputData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileType;
    private String jobId;
    private String customerNumber;
    private String benefitSet;
    private String groupNumber;
    private String pv;
    private String rc;
    private String coverageType;
    private String system;
    private String requestDate;
    private String memsCoverageCode;
    private String policyNumber;
    private String cardNumber;
}
