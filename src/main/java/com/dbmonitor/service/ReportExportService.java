package com.dbmonitor.service;

import com.dbmonitor.model.Alert;
import com.dbmonitor.model.DatabaseMetrics;
import com.dbmonitor.model.QueryMetrics;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ReportExportService {

    @Autowired
    private MultiDatabaseMonitoringService multiDatabaseMonitoringService;

    @Autowired
    private QueryMonitoringService queryMonitoringService;

    @Autowired
    private AlertService alertService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportMetricsToPdf(Long connectionId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("Database Metrics Report")
                .setFontSize(20)
                .setBold());

            // Current Metrics
            DatabaseMetrics current = multiDatabaseMonitoringService.getCurrentMetrics(connectionId);
            document.add(new Paragraph("Current Metrics").setFontSize(16).setBold());
            document.add(new Paragraph("Connection: " + current.getConnectionName()));
            document.add(new Paragraph("Database Type: " + current.getDatabaseType()));
            document.add(new Paragraph("Database Version: " + current.getDatabaseVersion()));
            document.add(new Paragraph("Status: " + current.getDatabaseStatus()));
            document.add(new Paragraph("CPU Usage: " + String.format("%.2f%%", current.getCpuUsage())));
            document.add(new Paragraph("Memory Usage: " + String.format("%.2f%%", current.getMemoryUsage())));
            document.add(new Paragraph("Active Connections: " + current.getActiveConnections()));
            document.add(new Paragraph("Idle Connections: " + current.getIdleConnections()));
            document.add(new Paragraph("Connection Pool Usage: " + String.format("%.2f%%", current.getConnectionUsagePercent())));

            // Historical Metrics
            List<DatabaseMetrics> history = multiDatabaseMonitoringService.getMetricsHistory(connectionId, 50);
            if (!history.isEmpty()) {
                document.add(new Paragraph("\nHistorical Metrics (Last 50 Records)").setFontSize(16).setBold());
                Table table = new Table(new float[]{2, 2, 2, 2, 2});
                table.addHeaderCell("Timestamp");
                table.addHeaderCell("CPU %");
                table.addHeaderCell("Memory %");
                table.addHeaderCell("Active Conn");
                table.addHeaderCell("Pool Usage %");

                for (DatabaseMetrics metric : history) {
                    table.addCell(metric.getTimestamp().format(DATE_FORMATTER));
                    table.addCell(String.format("%.2f", metric.getCpuUsage()));
                    table.addCell(String.format("%.2f", metric.getMemoryUsage()));
                    table.addCell(String.valueOf(metric.getActiveConnections()));
                    table.addCell(String.format("%.2f", metric.getConnectionUsagePercent()));
                }
                document.add(table);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    public byte[] exportMetricsToExcel(Long connectionId) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Current Metrics Sheet
            Sheet currentSheet = workbook.createSheet("Current Metrics");
            DatabaseMetrics current = multiDatabaseMonitoringService.getCurrentMetrics(connectionId);
            
            int rowNum = 0;
            Row row = currentSheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Metric");
            row.createCell(1).setCellValue("Value");

            addExcelRow(currentSheet, rowNum++, "Connection Name", current.getConnectionName());
            addExcelRow(currentSheet, rowNum++, "Database Type", current.getDatabaseType());
            addExcelRow(currentSheet, rowNum++, "Database Version", current.getDatabaseVersion());
            addExcelRow(currentSheet, rowNum++, "Status", current.getDatabaseStatus());
            addExcelRow(currentSheet, rowNum++, "CPU Usage %", String.format("%.2f", current.getCpuUsage()));
            addExcelRow(currentSheet, rowNum++, "Memory Usage %", String.format("%.2f", current.getMemoryUsage()));
            addExcelRow(currentSheet, rowNum++, "Active Connections", String.valueOf(current.getActiveConnections()));
            addExcelRow(currentSheet, rowNum++, "Idle Connections", String.valueOf(current.getIdleConnections()));
            addExcelRow(currentSheet, rowNum++, "Total Connections", String.valueOf(current.getTotalConnections()));
            addExcelRow(currentSheet, rowNum++, "Max Connections", String.valueOf(current.getMaxConnections()));

            // Historical Metrics Sheet
            Sheet historySheet = workbook.createSheet("Historical Metrics");
            List<DatabaseMetrics> history = multiDatabaseMonitoringService.getMetricsHistory(connectionId, 100);
            
            rowNum = 0;
            row = historySheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Timestamp");
            row.createCell(1).setCellValue("CPU %");
            row.createCell(2).setCellValue("Memory %");
            row.createCell(3).setCellValue("Active Connections");
            row.createCell(4).setCellValue("Idle Connections");
            row.createCell(5).setCellValue("Pool Usage %");

            for (DatabaseMetrics metric : history) {
                row = historySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(metric.getTimestamp().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(metric.getCpuUsage());
                row.createCell(2).setCellValue(metric.getMemoryUsage());
                row.createCell(3).setCellValue(metric.getActiveConnections());
                row.createCell(4).setCellValue(metric.getIdleConnections());
                row.createCell(5).setCellValue(metric.getConnectionUsagePercent());
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public byte[] exportQueriesToExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Queries");
            List<QueryMetrics> queries = queryMonitoringService.getAllQueries();
            
            int rowNum = 0;
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Query Type");
            row.createCell(1).setCellValue("Execution Time (ms)");
            row.createCell(2).setCellValue("Status");
            row.createCell(3).setCellValue("Executed At");
            row.createCell(4).setCellValue("Query");

            for (QueryMetrics query : queries) {
                row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(query.getQueryType());
                row.createCell(1).setCellValue(query.getExecutionDurationMs());
                row.createCell(2).setCellValue(query.getStatus());
                row.createCell(3).setCellValue(query.getExecutionTime().format(DATE_FORMATTER));
                row.createCell(4).setCellValue(query.getQueryText());
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating queries Excel report", e);
            throw new RuntimeException("Failed to generate queries Excel report", e);
        }
    }

    public byte[] exportAlertsToPdf() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("Database Alerts Report")
                .setFontSize(20)
                .setBold());

            List<Alert> alerts = alertService.getAllAlerts();
            
            Table table = new Table(new float[]{2, 2, 3, 2, 2});
            table.addHeaderCell("Type");
            table.addHeaderCell("Severity");
            table.addHeaderCell("Message");
            table.addHeaderCell("Created At");
            table.addHeaderCell("Acknowledged");

            for (Alert alert : alerts) {
                table.addCell(alert.getAlertType());
                table.addCell(alert.getSeverity());
                table.addCell(alert.getMessage());
                table.addCell(alert.getCreatedAt().format(DATE_FORMATTER));
                table.addCell(alert.getAcknowledged() ? "Yes" : "No");
            }
            
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating alerts PDF report", e);
            throw new RuntimeException("Failed to generate alerts PDF report", e);
        }
    }

    private void addExcelRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
