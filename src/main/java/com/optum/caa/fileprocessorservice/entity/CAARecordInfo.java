package com.optum.caa.fileprocessorservice.entity;

import com.optum.caa.fileprocessorservice.beans.B360OutputData;
import com.optum.caa.fileprocessorservice.beans.CESInputData;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * CAARecordInfo this entity defines the CES Input records.
 */
@Table(name = "caa_recordinfo")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CAARecordInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "recordid", nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Column(name = "requestnumber")
    private Long requestNumber;

    @Column(name = "ces_input", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private CESInputData cesInput;

    @Column(name = "b360_output", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private B360OutputData b360Output;

    @Column(name = "linenumber")
    private Integer lineNumber;

    @Column(name = "create_dttm")
    private Timestamp createdTimestamp;

    @Column(name = "update_dttm")
    private Timestamp updatedTimestamp;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "updatedby")
    private String updatedBy;

}
