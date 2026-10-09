package com.example.demo.complaint;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Complaint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String customerName;

  @Column(nullable = false)
  private String customerEmail;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private String status = "open";

  @Column(nullable = false)
  private String priority = "medium";

  @Column(columnDefinition = "text")
  private String resolutionNotes = "";

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;
}
