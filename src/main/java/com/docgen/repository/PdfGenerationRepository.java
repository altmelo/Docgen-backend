package com.docgen.repository;

import com.docgen.entity.PdfGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PdfGenerationRepository extends JpaRepository<PdfGeneration, UUID> {

    List<PdfGeneration> findByClientId(UUID clientId);
}
