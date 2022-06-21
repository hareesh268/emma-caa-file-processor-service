package com.optum.caa.fileprocessorservice.repository;

import com.optum.caa.fileprocessorservice.entity.CAARecordInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CAADataRepository extends  JpaRepository<CAARecordInfo, Long> {
}
