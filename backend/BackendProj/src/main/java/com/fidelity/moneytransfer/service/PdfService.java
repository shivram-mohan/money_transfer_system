package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    /**
     * Generate PDF transaction statement
     */
    public byte[] generateTransactionStatement(
            Account account,
            List<TransactionLog> transactions,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("Generating PDF statement for account: {}", account.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add header
            addHeader(document, account, startDate, endDate);

            // Add account summary
            addAccountSummary(document, account, transactions);

            // Add transactions table
            addTransactionsTable(document, transactions, account.getId());

            // Add footer
            addFooter(document);

            document.close();

            log.info("PDF generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, Account account,
                           LocalDate startDate, LocalDate endDate) {

        // Title
        Paragraph title = new Paragraph("TRANSACTION STATEMENT")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        // Subtitle
        Paragraph subtitle = new Paragraph("Money Transfer System")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);

        // Account details
        Table headerTable = new Table(2);
        headerTable.setWidth(UnitValue.createPercentValue(100));

        addHeaderRow(headerTable, "Account Holder:", account.getHolderName());
        addHeaderRow(headerTable, "Account Number:",
                String.valueOf(account.getId()));
        addHeaderRow(headerTable, "Statement Period:",
                startDate.format(DATE_FORMATTER) + " to " +
                        endDate.format(DATE_FORMATTER));
        addHeaderRow(headerTable, "Generated On:",
                LocalDate.now().format(DATE_FORMATTER));

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    private void addHeaderRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold())
                .setBorder(null)
                .setPadding(5));
        table.addCell(new Cell()
                .add(new Paragraph(value))
                .setBorder(null)
                .setPadding(5));
    }

    private void addAccountSummary(Document document, Account account,
                                   List<TransactionLog> transactions) {

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (TransactionLog txn : transactions) {
            if (txn.getFromAccountId() != null &&
                    txn.getFromAccountId().equals(account.getId())) {
                totalDebit = totalDebit.add(txn.getAmount());
            }
            if (txn.getToAccountId() != null &&
                    txn.getToAccountId().equals(account.getId())) {
                totalCredit = totalCredit.add(txn.getAmount());
            }
        }

        // Summary box
        Table summaryTable = new Table(3);
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        summaryTable.setBackgroundColor(new DeviceRgb(240, 240, 240));

        summaryTable.addCell(createSummaryCell("Current Balance",
                "₹" + account.getBalance()));
        summaryTable.addCell(createSummaryCell("Total Credits",
                "₹" + totalCredit));
        summaryTable.addCell(createSummaryCell("Total Debits",
                "₹" + totalDebit));

        document.add(summaryTable);
        document.add(new Paragraph("\n"));
    }

    private Cell createSummaryCell(String label, String value) {
        Paragraph labelPara = new Paragraph(label)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        Paragraph valuePara = new Paragraph(value)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);

        return new Cell()
                .add(labelPara)
                .add(valuePara)
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private void addTransactionsTable(Document document,
                                      List<TransactionLog> transactions,
                                      Long accountId) {

        // Table header
        Paragraph tableTitle = new Paragraph("Transaction Details")
                .setFontSize(16)
                .setBold()
                .setMarginBottom(10);
        document.add(tableTitle);

        if (transactions.isEmpty()) {
            document.add(new Paragraph("No transactions found for this period.")
                    .setItalic()
                    .setFontSize(12));
            return;
        }

        // Create table
        float[] columnWidths = {15, 20, 20, 15, 15, 15};
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        // Header row
        addTableHeader(table, "Date");
        addTableHeader(table, "Type");
        addTableHeader(table, "From/To");
        addTableHeader(table, "Amount");
        addTableHeader(table, "Status");
        addTableHeader(table, "Balance Effect");

        // Data rows
        for (TransactionLog txn : transactions) {
            boolean isDebit = txn.getFromAccountId() != null &&
                    txn.getFromAccountId().equals(accountId);

            String type = isDebit ? "DEBIT" : "CREDIT";
            String counterparty = isDebit ?
                    (txn.getToAccountHolderName() != null ?
                            txn.getToAccountHolderName() :
                            "Account #" + txn.getToAccountId()) :
                    (txn.getFromAccountHolderName() != null ?
                            txn.getFromAccountHolderName() :
                            "Account #" + txn.getFromAccountId());

            String balanceEffect = isDebit ?
                    "-₹" + txn.getAmount() :
                    "+₹" + txn.getAmount();

            DeviceRgb color = isDebit ?
                    new DeviceRgb(255, 200, 200) :
                    new DeviceRgb(200, 255, 200);

            table.addCell(new Cell()
                    .add(new Paragraph(txn.getCreatedOn()
                            .format(DATETIME_FORMATTER))
                            .setFontSize(9))
                    .setPadding(5));

            table.addCell(new Cell()
                    .add(new Paragraph(type).setFontSize(9).setBold())
                    .setPadding(5)
                    .setBackgroundColor(color));

            table.addCell(new Cell()
                    .add(new Paragraph(counterparty).setFontSize(9))
                    .setPadding(5));

            table.addCell(new Cell()
                    .add(new Paragraph("₹" + txn.getAmount())
                            .setFontSize(9))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT));

            table.addCell(new Cell()
                    .add(new Paragraph(txn.getStatus().toString())
                            .setFontSize(9))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER));

            table.addCell(new Cell()
                    .add(new Paragraph(balanceEffect)
                            .setFontSize(9)
                            .setBold())
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT));
        }

        document.add(table);
    }

    private void addTableHeader(Table table, String text) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(new DeviceRgb(63, 81, 181))
                .setFontColor(ColorConstants.WHITE)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n"));

        Paragraph footer = new Paragraph(
                "This is a computer-generated statement and does not require a signature.\n" +
                        "For any queries, please contact support@moneytransfer.com"
        )
                .setFontSize(8)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY);

        document.add(footer);
    }
}