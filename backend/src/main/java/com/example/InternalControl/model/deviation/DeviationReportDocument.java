package com.example.InternalControl.model.deviation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "deviation_report_document")
@IdClass(DeviationReportDocumentId.class)
public class DeviationReportDocument {

  @Id
  @Column(name = "report_id")
  private Long reportId;

  @Id
  @Column(name = "document_id")
  private Long documentId;
}

@Getter
@Setter
class DeviationReportDocumentId implements Serializable {
  private Long reportId;
  private Long documentId;
}
