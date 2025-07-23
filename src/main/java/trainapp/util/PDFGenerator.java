//package trainapp.util;
//
//import com.itextpdf.kernel.colors.Color;
//import com.itextpdf.kernel.colors.ColorConstants;
//import com.itextpdf.kernel.colors.DeviceRgb;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.geom.PageSize;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.borders.SolidBorder;
//import com.itextpdf.layout.element.*;
//import com.itextpdf.layout.property.TextAlignment;
//import com.itextpdf.layout.property.UnitValue;
//
//import java.io.File;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//public class PDFGenerator {
//
//    // Color scheme
//    private static final Color ACCENT_COLOR = new DeviceRgb(0, 82, 147); // Railway blue
//    private static final Color LIGHT_BG = new DeviceRgb(240, 240, 240);
//    private static final Color DARK_TEXT = new DeviceRgb(51, 51, 51);
//
//    // Spacing
//    private static final float SECTION_SPACING = 15f;
//
//    // Font sizes
//    private static final float TITLE_SIZE = 18f;
//    private static final float HEADER_SIZE = 14f;
//    private static final float BODY_SIZE = 12f;
//    private static final float FOOTER_SIZE = 9f;
//
//    public static void generateBookingPDF(
//            String outputPath,
//            String pnr,
//            String trainNo,
//            String trainName,
//            String travelClass,
//            String quota,
//            String bookingFrom,
//            String bookingTo,
//            String startDate,
//            String departureTime,
//            String arrivalTime,
//            String bookingDateTime,
//            String transactionId,
//            List<PassengerInfo> passengers,
//            double fare,
//            double catering,
//            double convenience,
//            double totalFare
//    ) throws IOException {
//        // Validate output path
//        if (outputPath == null || outputPath.trim().isEmpty()) {
//            throw new IllegalArgumentException("Output path cannot be null or empty");
//        }
//
//        File file = new File(outputPath);
//        File parentDir = file.getParentFile();
//        if (parentDir != null && !parentDir.exists()) {
//            if (!parentDir.mkdirs()) {
//                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
//            }
//        }
//
//        PdfWriter writer = new PdfWriter(file);
//        PdfDocument pdf = new PdfDocument(writer);
//        Document doc = new Document(pdf, PageSize.A4);
//        doc.setMargins(30, 30, 30, 30);
//
//        try {
//            PdfFont font = PdfFontFactory.createFont();
//
//            // Add header with railway branding
//            addRailwayHeader(doc, font);
//
//            // Add journey details section
//            addJourneyDetails(doc, font, pnr, trainNo, trainName, travelClass, quota,
//                    bookingFrom, bookingTo, startDate, departureTime, arrivalTime,
//                    bookingDateTime, transactionId);
//
//            // Add passenger table
//            addPassengerTable(doc, font, passengers);
//
//            // Add fare breakdown
//            addFareDetails(doc, font, fare, catering, convenience, totalFare);
//
//            // Add footer with disclaimers
//            addFooter(doc, font);
//
//        } finally {
//            doc.close();
//        }
//    }
//
//    private static void addRailwayHeader(Document doc, PdfFont font) {
//        Div header = new Div()
//                .setBackgroundColor(ACCENT_COLOR)
//                .setPadding(15)
//                .setMarginBottom(SECTION_SPACING);
//
//        // Main title
//        Paragraph title = new Paragraph("INDIAN RAILWAYS")
//                .setFontColor(ColorConstants.WHITE)
//                .setTextAlignment(TextAlignment.CENTER)
//                .setFontSize(TITLE_SIZE)
//                .setBold()
//                .setFont(font);
//
//        // Subtitle
//        Paragraph subtitle = new Paragraph("E-TICKET / JOURNEY DETAILS")
//                .setFontColor(ColorConstants.WHITE)
//                .setTextAlignment(TextAlignment.CENTER)
//                .setFontSize(HEADER_SIZE)
//                .setFont(font);
//
//        header.add(title);
//        header.add(subtitle);
//        doc.add(header);
//    }
//
//    private static void addJourneyDetails(Document doc, PdfFont font, String pnr,
//                                          String trainNo, String trainName, String travelClass,
//                                          String quota, String bookingFrom, String bookingTo,
//                                          String startDate, String departureTime,
//                                          String arrivalTime, String bookingDateTime,
//                                          String transactionId) {
//        // Create a bordered container
//        Div detailsContainer = new Div()
//                .setBorder(new SolidBorder(ACCENT_COLOR, 1))
//                .setPadding(15)
//                .setMarginBottom(SECTION_SPACING);
//
//        // Title
//        detailsContainer.add(
//                new Paragraph("JOURNEY DETAILS")
//                        .setFontColor(ACCENT_COLOR)
//                        .setFontSize(HEADER_SIZE)
//                        .setBold()
//                        .setFont(font)
//                        .setMarginBottom(10)
//        );
//
//        // Two-column layout for details
//        float[] columnWidths = {1, 1};
//        Table detailsTable = new Table(columnWidths)
//                .setFont(font)
//                .setFontSize(BODY_SIZE);
//
//        // Left column
//        detailsTable.addCell(createDetailCell("PNR Number:", pnr));
//        detailsTable.addCell(createDetailCell("Train Number/Name:", trainNo + " / " + trainName));
//        detailsTable.addCell(createDetailCell("Class/Quota:", travelClass + " / " + quota));
//        detailsTable.addCell(createDetailCell("From Station:", bookingFrom));
//
//        // Right column
//        detailsTable.addCell(createDetailCell("Booking Date:", bookingDateTime));
//        detailsTable.addCell(createDetailCell("Transaction ID:", transactionId));
//        detailsTable.addCell(createDetailCell("Departure/Arrival:",
//                departureTime + " / " + arrivalTime));
//        detailsTable.addCell(createDetailCell("To Station:", bookingTo));
//
//        detailsContainer.add(detailsTable);
//        doc.add(detailsContainer);
//    }
//
//    private static Cell createDetailCell(String label, String value) {
//        return new Cell()
//                .setBorder(null)
//                .add(new Paragraph(label).setBold())
//                .add(new Paragraph(value).setMarginTop(3));
//    }
//
//    private static void addPassengerTable(Document doc, PdfFont font, List<PassengerInfo> passengers) {
//        if (passengers == null || passengers.isEmpty()) {
//            doc.add(new Paragraph("No passenger information available").setFont(font));
//            return;
//        }
//
//        // Table with proper styling
//        float[] columnWidths = {5, 25, 8, 10, 20, 20};
//        Table table = new Table(columnWidths)
//                .setMarginTop(SECTION_SPACING)
//                .setMarginBottom(SECTION_SPACING)
//                .setFont(font)
//                .setFontSize(BODY_SIZE);
//
//        // Header row with accent color
//        String[] headers = {"No.", "Passenger Name", "Age", "Gender", "Booking Status", "Current Status"};
//        for (String header : headers) {
//            table.addHeaderCell(
//                    new Cell()
//                            .setBackgroundColor(ACCENT_COLOR)
//                            .setFontColor(ColorConstants.WHITE)
//                            .add(new Paragraph(header).setBold())
//            );
//        }
//
//        // Alternate row coloring
//        boolean alternate = false;
//        for (int i = 0; i < passengers.size(); i++) {
//            PassengerInfo p = passengers.get(i);
//            Cell[] cells = {
//                    new Cell().add(new Paragraph(String.valueOf(i+1))),
//                    new Cell().add(new Paragraph(p.getName())),
//                    new Cell().add(new Paragraph(String.valueOf(p.getAge()))),
//                    new Cell().add(new Paragraph(p.getGender())),
//                    new Cell().add(new Paragraph(p.getBookingStatus())),
//                    new Cell().add(new Paragraph(p.getCurrentStatus()))
//            };
//
//            if (alternate) {
//                for (Cell cell : cells) {
//                    cell.setBackgroundColor(LIGHT_BG);
//                }
//            }
//
//            for (Cell cell : cells) {
//                table.addCell(cell);
//            }
//
//            alternate = !alternate;
//        }
//
//        doc.add(table);
//    }
//
//    private static void addFareDetails(Document doc, PdfFont font,
//                                       double fare, double catering,
//                                       double convenience, double totalFare) {
//        // Fare summary with clean design
//        Div fareContainer = new Div()
//                .setBorder(new SolidBorder(ACCENT_COLOR, 1))
//                .setPadding(15)
//                .setMarginBottom(SECTION_SPACING);
//
//        // Title
//        fareContainer.add(
//                new Paragraph("FARE SUMMARY")
//                        .setFontColor(ACCENT_COLOR)
//                        .setFontSize(HEADER_SIZE)
//                        .setBold()
//                        .setFont(font)
//                        .setMarginBottom(10)
//        );
//
//        // Fare breakdown table
//        float[] columnWidths = {3, 1};
//        Table fareTable = new Table(columnWidths)
//                .setFont(font)
//                .setFontSize(BODY_SIZE);
//
//        // Add fare rows
//        addFareRow(fareTable, "Base Fare:", fare);
//        addFareRow(fareTable, "Catering Charges:", catering);
//        addFareRow(fareTable, "Convenience Fee:", convenience);
//
//        // Total row
//        fareTable.addCell(
//                new Cell(1, 1)
//                        .setBorderTop(new SolidBorder(DARK_TEXT, 0.5f))
//                        .add(new Paragraph("TOTAL:").setBold())
//        );
//        fareTable.addCell(
//                new Cell(1, 1)
//                        .setBorderTop(new SolidBorder(DARK_TEXT, 0.5f))
//                        .setTextAlignment(TextAlignment.RIGHT)
//                        .add(new Paragraph("₹" + String.format("%.2f", totalFare)).setBold())
//        );
//
//        fareContainer.add(fareTable);
//        doc.add(fareContainer);
//    }
//
//    private static void addFareRow(Table table, String label, double amount) {
//        table.addCell(new Cell().add(new Paragraph(label)));
//        table.addCell(
//                new Cell()
//                        .setTextAlignment(TextAlignment.RIGHT)
//                        .add(new Paragraph("₹" + String.format("%.2f", amount)))
//        );
//    }
//
//    private static void addFooter(Document doc, PdfFont font) {
//        Div footer = new Div()
//                .setMarginTop(SECTION_SPACING)
//                .setPaddingTop(10)
//                .setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
//                .setFont(font)
//                .setFontSize(FOOTER_SIZE);
//
//        // Add disclaimer items
//        String[] disclaimers = {
//                "• This is a computer generated ticket and does not require signature",
//                "• Please carry valid ID proof matching passenger names",
//                "• Check PNR status 4 hours before departure for updates",
//                "• E-ticket is valid on mobile, no printout required",
//                "• For any queries, contact care@irctc.co.in or call 139"
//        };
//
//        for (String text : disclaimers) {
//            footer.add(
//                    new Paragraph(text)
//                            .setFontColor(DARK_TEXT)
//                            .setMarginBottom(3)
//            );
//        }
//
//        // Add generated timestamp
//        footer.add(
//                new Paragraph("Generated on: " +
//                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")))
//                        .setTextAlignment(TextAlignment.RIGHT)
//                        .setItalic()
//                        .setMarginTop(10)
//        );
//
//        doc.add(footer);
//    }
//
//    public static class PassengerInfo {
//        private final String name;
//        private final int age;
//        private final String gender;
//        private final String bookingStatus;
//        private final String currentStatus;
//
//        public PassengerInfo(String name, int age, String gender,
//                             String bookingStatus, String currentStatus) {
//            this.name = validateField(name, "Name");
//            this.age = validateAge(age);
//            this.gender = validateField(gender, "Gender");
//            this.bookingStatus = validateField(bookingStatus, "Booking Status");
//            this.currentStatus = validateField(currentStatus, "Current Status");
//        }
//
//        private String validateField(String value, String fieldName) {
//            if (value == null || value.trim().isEmpty()) {
//                throw new IllegalArgumentException(fieldName + " cannot be null or empty");
//            }
//            return value.trim();
//        }
//
//        private int validateAge(int age) {
//            if (age <= 0 || age > 120) {
//                throw new IllegalArgumentException("Age must be between 1 and 120");
//            }
//            return age;
//        }
//
//        // Getters
//        public String getName() { return name; }
//        public int getAge() { return age; }
//        public String getGender() { return gender; }
//        public String getBookingStatus() { return bookingStatus; }
//        public String getCurrentStatus() { return currentStatus; }
//    }
//}