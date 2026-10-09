package com.example.demo.complaint;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ComplaintController {
  private static final Set<String> STATUSES = Set.of("open", "in_progress", "resolved", "closed");
  private static final Set<String> PRIORITIES = Set.of("low", "medium", "high");

  private final ComplaintRepository repo;

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record CreateRequest(
      String customerName,
      String customerEmail,
      String subject,
      String description,
      String priority) {}

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record UpdateRequest(String status, String priority, String resolutionNotes) {}

  @GetMapping("/complaints")
  public Map<String, Object> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    page = Math.max(1, page);
    pageSize = Math.min(100, Math.max(1, pageSize));
    var pageable =
        PageRequest.of(
            page - 1, pageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    var result = status == null ? repo.findAll(pageable) : repo.findByStatus(status, pageable);
    var body = new LinkedHashMap<String, Object>();
    body.put("items", result.getContent());
    body.put("page", page);
    body.put("page_size", pageSize);
    body.put("total", result.getTotalElements());
    body.put("total_pages", Math.max(1, result.getTotalPages()));
    return body;
  }

  @GetMapping("/stats")
  public Map<String, Long> stats() {
    var counts = new LinkedHashMap<String, Long>();
    STATUSES.forEach(s -> counts.put(s, 0L));
    repo.countByStatus().forEach(r -> counts.put((String) r[0], (Long) r[1]));
    counts.put("total", counts.values().stream().mapToLong(Long::longValue).sum());
    return counts;
  }

  @GetMapping("/complaints/{id}")
  public Complaint get(@PathVariable long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "complaint not found"));
  }

  @PostMapping("/complaints")
  @ResponseStatus(HttpStatus.CREATED)
  public Complaint create(@RequestBody CreateRequest r) {
    require("customer_name", r.customerName());
    require("customer_email", r.customerEmail());
    require("subject", r.subject());
    require("description", r.description());
    var priority = r.priority() == null ? "medium" : r.priority();
    if (!PRIORITIES.contains(priority)) {
      throw new ResponseStatusException(BAD_REQUEST, "invalid priority: " + priority);
    }
    var c = new Complaint();
    c.setCustomerName(r.customerName());
    c.setCustomerEmail(r.customerEmail());
    c.setSubject(r.subject());
    c.setDescription(r.description());
    c.setPriority(priority);
    c.setCreatedAt(Instant.now());
    c.setUpdatedAt(c.getCreatedAt());
    return repo.save(c);
  }

  @PatchMapping("/complaints/{id}")
  public Complaint update(@PathVariable long id, @RequestBody UpdateRequest r) {
    if (r.status() == null && r.priority() == null && r.resolutionNotes() == null) {
      throw new ResponseStatusException(BAD_REQUEST, "no valid fields to update");
    }
    if (r.status() != null && !STATUSES.contains(r.status())) {
      throw new ResponseStatusException(BAD_REQUEST, "invalid status: " + r.status());
    }
    if (r.priority() != null && !PRIORITIES.contains(r.priority())) {
      throw new ResponseStatusException(BAD_REQUEST, "invalid priority: " + r.priority());
    }
    var c = get(id);
    if (r.status() != null) c.setStatus(r.status());
    if (r.priority() != null) c.setPriority(r.priority());
    if (r.resolutionNotes() != null) c.setResolutionNotes(r.resolutionNotes());
    c.setUpdatedAt(Instant.now());
    return repo.save(c);
  }

  @DeleteMapping("/complaints/{id}")
  public Map<String, Long> delete(@PathVariable long id) {
    repo.delete(get(id));
    return Map.of("deleted", id);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
  }

  private static void require(String field, String value) {
    if (value == null || value.isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "missing field: " + field);
    }
  }
}
