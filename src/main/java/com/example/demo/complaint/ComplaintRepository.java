package com.example.demo.complaint;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
  Page<Complaint> findByStatus(String status, Pageable pageable);

  @Query("select c.status, count(c) from Complaint c group by c.status")
  List<Object[]> countByStatus();
}
