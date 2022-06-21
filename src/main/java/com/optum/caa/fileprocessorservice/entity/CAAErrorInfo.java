package com.optum.caa.fileprocessorservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Entity;
import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;

@Table(name = "caa_errorinfo")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CAAErrorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "errorid")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long errorId;

    @Column(name = "recordid")
    private Long recordId;

    @Column(name = "requestnumber")
    private Long requestNumber;

    @Column(name = "error_description", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private String errorDescription;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "create_dttm")
    private Timestamp createdTimestamp;
}
