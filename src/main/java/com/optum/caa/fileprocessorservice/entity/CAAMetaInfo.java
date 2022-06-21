package com.optum.caa.fileprocessorservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * CAAMetaInfo this entity defines the META INFO records.
 */
@Table(name = "caa_metainfo")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CAAMetaInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "requestnumber")
    private Long requestNumber;

    @Column(name = "metainfoinput_filename")
    private String metaInfoInputFileName;

    @Column(name = "input_filename")
    private String inputFileName;

    @Column(name = "metainfooutput_filename")
    private String metaInfoOutputFileName;

    @Column(name = "output_filename")
    private String outputFileName;

    @Column(name = "status")
    private String status;

    @Column(name = "received_count")
    private Integer receivedCount;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "create_dttm")
    private Timestamp createdTimestamp;

    @Column(name = "update_dttm")
    private Timestamp updatedTimestamp;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "updatedby")
    private String updatedBy;

    @Column(name = "file_type")
    private String fileType;

}
