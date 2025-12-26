package com.dbmonitor.controller;

import com.dbmonitor.service.ReportExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Slf4j
public class ReportApiController {

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping("/metrics/pdf")
    public ResponseEntity<byte[]> exportMetricsToPdf(@RequestParam Long connectionId) {
        try {
            byte[] pdfBytes = reportExportService.exportMetricsToPdf(connectionId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "metrics-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error exporting metrics to PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/metrics/excel")
    public ResponseEntity<byte[]> exportMetricsToExcel(@RequestParam Long connectionId) {
        try {
            byte[] excelBytes = reportExportService.exportMetricsToExcel(connectionId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "metrics-report.xlsx");
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
        } catch (Exception e) {
            log.error("Error exporting metrics to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queries/excel")
    public ResponseEntity<byte[]> exportQueriesToExcel() {
        try {
            byte[] excelBytes = reportExportService.exportQueriesToExcel();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "queries-report.xlsx");
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
        } catch (Exception e) {
            log.error("Error exporting queries to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/alerts/pdf")
    public ResponseEntity<byte[]> exportAlertsToPdf() {
        try {
            byte[] pdfBytes = reportExportService.exportAlertsToPdf();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "alerts-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error exporting alerts to PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
