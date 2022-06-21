package com.optum.caa.fileprocessorservice.repository;

import com.optum.caa.fileprocessorservice.entity.CAAMetaInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CAAMetaInfoRepository extends JpaRepository<CAAMetaInfo, Long> {

   public CAAMetaInfo findByMetaInfoInputFileName(String metainfoinputfilename);
}
