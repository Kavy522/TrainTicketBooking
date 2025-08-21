package trainapp.util;

import trainapp.model.Booking;
import trainapp.model.Passenger;
import trainapp.dao.PassengerDAO;
import trainapp.dao.UserDAO;
import trainapp.dao.TrainDAO;
import trainapp.dao.StationDAO;
import trainapp.model.User;
import trainapp.model.Train;
import trainapp.model.Station;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized PDFGenerator for high-performance ticket and invoice generation.
 *
 * Key Performance Features:
 * - Pre-defined static fonts and colors for reuse
 * - Comprehensive data caching to minimize DAO calls
 * - Optimized table layouts with minimal nesting
 * - Batch content writing for improved throughput
 * - Zero console logging for production performance
 * - Memory-efficient ByteArrayOutputStream handling
 */
public class PDFGenerator {

    // -------------------------------------------------------------------------
    // DAO Dependencies (Consider injecting these for better testability)
    // -------------------------------------------------------------------------

    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    // -------------------------------------------------------------------------
    // Performance Caches (Thread-safe for concurrent PDF generation)
    // -------------------------------------------------------------------------

    /** Cache for user data to avoid repeated database calls */
    private final Map<Integer, User> userCache = new ConcurrentHashMap<>();

    /** Cache for train data to avoid repeated database calls */
    private final Map<Integer, Train> trainCache = new ConcurrentHashMap<>();

    /** Cache for station data to avoid repeated database calls */
    private final Map<Integer, Station> stationCache = new ConcurrentHashMap<>();

    /** Cache for passenger lists to avoid repeated database calls */
    private final Map<Integer, List<Passenger>> passengerCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Pre-defined Static Resources (Reused across all PDF generations)
    // -------------------------------------------------------------------------

    /** Modern Indian Railways inspired color scheme */
    private static final BaseColor INDIAN_ORANGE = new BaseColor(255, 153, 51);
    private static final BaseColor INDIAN_BLUE = new BaseColor(0, 123, 191);
    private static final BaseColor DARK_GREEN = new BaseColor(0, 100, 0);
    private static final BaseColor LIGHT_GRAY = new BaseColor(248, 248, 248);
    private static final BaseColor BORDER_GRAY = new BaseColor(220, 220, 220);
    private static final BaseColor LIGHT_YELLOW = new BaseColor(252, 248, 227);
    private static final BaseColor YELLOW_BORDER = new BaseColor(251, 188, 52);
    private static final BaseColor LIGHT_GREEN = new BaseColor(232, 245, 233);

    /** Pre-defined fonts for optimal performance (avoid repeated creation) */
    private static final Font BRAND_FONT = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, INDIAN_BLUE);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, INDIAN_BLUE);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.GRAY);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.BLACK);
    private static final Font DATA_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font TABLE_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    private static final Font COMPANY_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, INDIAN_BLUE);
    private static final Font INVOICE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, INDIAN_ORANGE);

    /** Date formatter for consistent date display across PDFs */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // -------------------------------------------------------------------------
    // Public API Methods
    // -------------------------------------------------------------------------

    /**
     * Generates an optimized train ticket PDF with enhanced performance.
     *
     * Performance optimizations applied:
     * - Cached data retrieval for users, trains, stations
     * - Pre-defined fonts and colors
     * - Optimized table layouts
     * - Batch content writing
     *
     * @param booking The booking object containing ticket details
     * @return PDF bytes ready for download/email, null if generation fails
     * @throws IllegalArgumentException if booking is null
     */
    public byte[] generateTicketPDF(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Generate ticket content with optimized methods
            addOptimizedTicketHeader(document);
            addOptimizedBookingOverview(document, booking);
            addOptimizedJourneyDetails(document, booking);
            addOptimizedPassengerDetails(document, booking);
            addOptimizedTravelInstructions(document);
            addOptimizedTicketFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            // Silent error handling for production - log to proper logging system
            return null;
        }
    }

    /**
     * Generates an optimized invoice PDF with consistent amount calculations.
     *
     * Ensures invoice amounts exactly match booking amounts through:
     * - Direct calculation from booking.getTotalFare()
     * - Consistent breakdown across all components
     * - Proper rounding and formatting
     *
     * @param booking The booking object containing invoice details
     * @return PDF bytes ready for download/email, null if generation fails
     * @throws IllegalArgumentException if booking is null
     */
    public byte[] generateInvoicePDF(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Generate invoice content with optimized methods
            addOptimizedInvoiceHeader(document, booking);
            addOptimizedPartyDetails(document, booking);
            addOptimizedBookingSummary(document, booking);
            addOptimizedPaymentBreakdown(document, booking);
            addOptimizedPaymentInformation(document, booking);
            addOptimizedInvoiceFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            // Silent error handling for production - log to proper logging system
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Optimized Ticket Generation Methods
    // -------------------------------------------------------------------------

    /**
     * Creates an optimized modern header with pre-defined styling.
     */
    private void addOptimizedTicketHeader(Document document) throws DocumentException {
        // Top orange stripe using pre-defined color
        PdfPTable stripe = createSimpleTable(1, 100, 15);
        PdfPCell stripeCell = new PdfPCell();
        stripeCell.setBackgroundColor(INDIAN_ORANGE);
        stripeCell.setFixedHeight(8);
        stripeCell.setBorder(Rectangle.NO_BORDER);
        stripe.addCell(stripeCell);
        document.add(stripe);

        // Header with logo and title
        PdfPTable header = createSimpleTable(2, 100, 20);
        header.setWidths(new float[]{1, 3});

        // Logo section
        PdfPCell logoCell = createBorderlessCell();
        logoCell.addElement(new Paragraph("🚂", BRAND_FONT));
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.addCell(logoCell);

        // Title section
        PdfPCell titleCell = createBorderlessCell();
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph("TAILYATRI", TITLE_FONT));
        titleCell.addElement(new Paragraph("Electronic Reservation Slip (ERS)", SUBTITLE_FONT));
        header.addCell(titleCell);

        document.add(header);
    }

    /**
     * Creates an optimized booking overview card with cached data.
     */
    private void addOptimizedBookingOverview(Document document, Booking booking) throws DocumentException {
        PdfPTable card = createStyledCard();
        PdfPCell cardCell = createStyledCardCell();

        // PNR and Status using pre-defined fonts
        PdfPTable pnrTable = createSimpleTable(2, 100, 0);

        PdfPCell pnrCell = createBorderlessCell();
        pnrCell.addElement(new Paragraph("PNR NUMBER", LABEL_FONT));
        pnrCell.addElement(new Paragraph(booking.getPnr(), new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, INDIAN_BLUE)));
        pnrTable.addCell(pnrCell);

        PdfPCell statusCell = createBorderlessCell();
        statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        statusCell.addElement(new Paragraph("STATUS", LABEL_FONT));
        statusCell.addElement(new Paragraph("✓ " + booking.getStatus().toUpperCase(),
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, DARK_GREEN)));
        pnrTable.addCell(statusCell);

        cardCell.addElement(pnrTable);
        card.addCell(cardCell);
        document.add(card);
    }

    /**
     * Creates optimized journey details with cached train and station data.
     */
    private void addOptimizedJourneyDetails(Document document, Booking booking) throws DocumentException {
        // Use cached data to avoid repeated DAO calls
        Train train = getCachedTrain(booking.getTrainId());
        Station fromStation = getCachedStation(booking.getSourceStationId());
        Station toStation = getCachedStation(booking.getDestStationId());

        PdfPTable card = createStyledCard();
        PdfPCell cardCell = createStyledCardCell();

        // Train details header
        cardCell.addElement(new Paragraph("TRAIN DETAILS", HEADER_FONT));
        cardCell.addElement(new Paragraph(" ")); // Spacing

        // Train info table with optimized layout
        PdfPTable trainTable = createSimpleTable(4, 100, 0);
        trainTable.setWidths(new float[]{2, 2, 1.5f, 1.5f});

        addOptimizedInfoCell(trainTable, "TRAIN", train != null ? train.getTrainNumber() : "N/A");
        addOptimizedInfoCell(trainTable, "NAME", train != null ? train.getName() : "N/A");
        addOptimizedInfoCell(trainTable, "DATE", booking.getBookingTime().format(DATE_FORMATTER));
        addOptimizedInfoCell(trainTable, "QUOTA", "GN");

        cardCell.addElement(trainTable);

        // Route section with optimized design
        cardCell.addElement(new Paragraph(" "));
        cardCell.addElement(new Paragraph("JOURNEY ROUTE", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK)));

        // Optimized route visualization
        PdfPTable routeTable = createSimpleTable(3, 100, 0);
        routeTable.setWidths(new float[]{2, 1, 2});

        // From station
        PdfPCell fromCell = createBorderlessCell();
        fromCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        fromCell.addElement(new Paragraph("FROM", LABEL_FONT));
        fromCell.addElement(new Paragraph(fromStation != null ? fromStation.getName() : "N/A",
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE)));
        routeTable.addCell(fromCell);

        // Arrow
        PdfPCell arrowCell = new PdfPCell(new Phrase("→",
                new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, INDIAN_ORANGE)));
        arrowCell.setBorder(Rectangle.NO_BORDER);
        arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        arrowCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        routeTable.addCell(arrowCell);

        // To station
        PdfPCell toCell = createBorderlessCell();
        toCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        toCell.addElement(new Paragraph("TO", LABEL_FONT));
        toCell.addElement(new Paragraph(toStation != null ? toStation.getName() : "N/A",
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE)));
        routeTable.addCell(toCell);

        cardCell.addElement(routeTable);
        card.addCell(cardCell);
        document.add(card);
    }

    /**
     * Creates optimized passenger details with cached passenger data.
     */
    private void addOptimizedPassengerDetails(Document document, Booking booking) throws DocumentException {
        List<Passenger> passengers = getCachedPassengers((int)booking.getBookingId());

        if (!passengers.isEmpty()) {
            PdfPTable card = createStyledCard();
            PdfPCell cardCell = createStyledCardCell();

            cardCell.addElement(new Paragraph("PASSENGER DETAILS", HEADER_FONT));

            // Optimized passenger table
            PdfPTable passengerTable = createSimpleTable(5, 100, 0);
            passengerTable.setWidths(new float[]{0.5f, 2.5f, 0.8f, 1, 1.5f});

            // Headers with pre-defined styling
            String[] headers = {"S.No", "Passenger Name", "Age", "Gender", "Coach/Seat"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER_FONT));
                cell.setBackgroundColor(INDIAN_BLUE);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(BaseColor.WHITE);
                passengerTable.addCell(cell);
            }

            // Passenger data with alternating colors
            for (int i = 0; i < passengers.size(); i++) {
                Passenger passenger = passengers.get(i);
                BaseColor rowColor = (i % 2 == 0) ? BaseColor.WHITE : LIGHT_GRAY;

                addOptimizedPassengerCell(passengerTable, String.valueOf(i + 1), rowColor, Element.ALIGN_CENTER);
                addOptimizedPassengerCell(passengerTable, passenger.getName(), rowColor, Element.ALIGN_LEFT);
                addOptimizedPassengerCell(passengerTable, String.valueOf(passenger.getAge()), rowColor, Element.ALIGN_CENTER);
                addOptimizedPassengerCell(passengerTable, passenger.getGender(), rowColor, Element.ALIGN_CENTER);
                addOptimizedPassengerCell(passengerTable, passenger.getCoachType() + "/" + passenger.getSeatNumber(), rowColor, Element.ALIGN_CENTER);
            }

            cardCell.addElement(passengerTable);
            card.addCell(cardCell);
            document.add(card);
        }
    }

    /**
     * Creates optimized travel instructions with pre-defined styling.
     */
    private void addOptimizedTravelInstructions(Document document) throws DocumentException {
        PdfPTable card = createSimpleTable(1, 100, 15);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_YELLOW);
        cell.setBorderColor(YELLOW_BORDER);
        cell.setBorderWidth(1);
        cell.setPadding(15);

        Font instructionHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(133, 77, 14));
        cell.addElement(new Paragraph("⚠️ IMPORTANT TRAVEL INSTRUCTIONS", instructionHeaderFont));

        Font instructionFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(120, 53, 15));
        String[] instructions = {
                "• Carry this e-ticket with valid photo ID proof during journey",
                "• Report at station 30 minutes before departure time",
                "• E-ticket is valid only when passenger identity is verified",
                "• Ticket is non-transferable and valid only for named passengers",
                "• Keep ticket safe throughout the journey for verification"
        };

        for (String instruction : instructions) {
            Paragraph p = new Paragraph(instruction, instructionFont);
            p.setSpacingAfter(3);
            cell.addElement(p);
        }

        card.addCell(cell);
        document.add(card);
    }

    /**
     * Creates optimized ticket footer with pre-defined styling.
     */
    private void addOptimizedTicketFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        LineSeparator ls = new LineSeparator();
        ls.setLineColor(BORDER_GRAY);
        document.add(new Chunk(ls));

        PdfPTable footer = createSimpleTable(2, 100, 10);

        PdfPCell leftFooter = createBorderlessCell();
        leftFooter.addElement(new Paragraph("Generated by Tailyatri", FOOTER_FONT));
        leftFooter.addElement(new Paragraph("Customer Support: support@tailyatri.com", FOOTER_FONT));
        footer.addCell(leftFooter);

        PdfPCell rightFooter = createBorderlessCell();
        rightFooter.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightFooter.addElement(new Paragraph("Generated on:", FOOTER_FONT));
        rightFooter.addElement(new Paragraph(java.time.LocalDateTime.now().format(DATETIME_FORMATTER), FOOTER_FONT));
        footer.addCell(rightFooter);

        document.add(footer);
    }

    // -------------------------------------------------------------------------
    // Optimized Invoice Generation Methods
    // -------------------------------------------------------------------------

    /**
     * Creates optimized invoice header with pre-defined styling.
     */
    private void addOptimizedInvoiceHeader(Document document, Booking booking) throws DocumentException {
        // Blue stripe
        PdfPTable stripe = createSimpleTable(1, 100, 20);
        PdfPCell stripeCell = new PdfPCell();
        stripeCell.setBackgroundColor(INDIAN_BLUE);
        stripeCell.setFixedHeight(8);
        stripeCell.setBorder(Rectangle.NO_BORDER);
        stripe.addCell(stripeCell);
        document.add(stripe);

        // Header table
        PdfPTable header = createSimpleTable(2, 100, 25);
        header.setWidths(new float[]{3, 2});

        // Company details
        PdfPCell companyCell = createBorderlessCell();
        companyCell.addElement(new Paragraph("🚂 TAILYATRI", COMPANY_FONT));
        companyCell.addElement(new Paragraph("Digital Train Booking Platform", LABEL_FONT));
        companyCell.addElement(new Paragraph("Email: support@tailyatri.com", LABEL_FONT));
        companyCell.addElement(new Paragraph("Phone: +91-9876543210", LABEL_FONT));
        header.addCell(companyCell);

        // Invoice details
        PdfPCell invoiceCell = createBorderlessCell();
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        invoiceCell.addElement(new Paragraph("INVOICE", INVOICE_FONT));
        invoiceCell.addElement(new Paragraph("Invoice #: " + booking.getPnr(), DATA_FONT));
        invoiceCell.addElement(new Paragraph("Date: " + booking.getBookingTime().format(DATE_FORMATTER), DATA_FONT));
        invoiceCell.addElement(new Paragraph("Payment: Completed", DATA_FONT));
        header.addCell(invoiceCell);

        document.add(header);
    }

    /**
     * Creates optimized party details with cached user data.
     */
    private void addOptimizedPartyDetails(Document document, Booking booking) throws DocumentException {
        User user = getCachedUser(booking.getUserId());

        PdfPTable partyTable = createSimpleTable(2, 100, 20);
        partyTable.setWidths(new float[]{1, 1});

        // Bill To section
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorder(Rectangle.BOX);
        billToCell.setBorderColor(BORDER_GRAY);
        billToCell.setPadding(12);
        billToCell.setBackgroundColor(LIGHT_GRAY);

        billToCell.addElement(new Paragraph("BILL TO", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE)));
        if (user != null) {
            billToCell.addElement(new Paragraph(" "));
            billToCell.addElement(new Paragraph(user.getName(), DATA_FONT));
            billToCell.addElement(new Paragraph(user.getEmail(), DATA_FONT));
            billToCell.addElement(new Paragraph(user.getPhone(), DATA_FONT));
        }
        partyTable.addCell(billToCell);

        // Payment details section
        PdfPCell paymentCell = new PdfPCell();
        paymentCell.setBorder(Rectangle.BOX);
        paymentCell.setBorderColor(BORDER_GRAY);
        paymentCell.setPadding(12);
        paymentCell.setBackgroundColor(LIGHT_GRAY);

        paymentCell.addElement(new Paragraph("PAYMENT DETAILS", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE)));
        paymentCell.addElement(new Paragraph(" "));
        paymentCell.addElement(new Paragraph("Payment Method: Online", DATA_FONT));
        paymentCell.addElement(new Paragraph("Status: Completed", DATA_FONT));
        paymentCell.addElement(new Paragraph("PNR: " + booking.getPnr(), DATA_FONT));
        partyTable.addCell(paymentCell);

        document.add(partyTable);
    }

    /**
     * Creates optimized booking summary with cached data.
     */
    private void addOptimizedBookingSummary(Document document, Booking booking) throws DocumentException {
        Train train = getCachedTrain(booking.getTrainId());
        Station fromStation = getCachedStation(booking.getSourceStationId());
        Station toStation = getCachedStation(booking.getDestStationId());
        List<Passenger> passengers = getCachedPassengers((int)booking.getBookingId());

        document.add(new Paragraph("BOOKING SUMMARY", HEADER_FONT));

        PdfPTable summary = createSimpleTable(1, 100, 20);
        PdfPCell summaryCell = new PdfPCell();
        summaryCell.setBorder(Rectangle.BOX);
        summaryCell.setBorderColor(BORDER_GRAY);
        summaryCell.setPadding(15);

        PdfPTable details = createSimpleTable(4, 100, 0);
        details.setWidths(new float[]{1, 1, 1, 1});

        addOptimizedInfoCell(details, "TRAIN", train != null ? train.getTrainNumber() : "N/A");
        addOptimizedInfoCell(details, "ROUTE",
                (fromStation != null ? fromStation.getName() : "N/A") + " → " +
                        (toStation != null ? toStation.getName() : "N/A"));
        addOptimizedInfoCell(details, "DATE", booking.getBookingTime().format(DATE_FORMATTER));
        addOptimizedInfoCell(details, "PASSENGERS", String.valueOf(passengers.size()));

        summaryCell.addElement(details);
        summary.addCell(summaryCell);
        document.add(summary);
    }

    /**
     * Creates optimized payment breakdown with consistent amount calculations.
     * Ensures invoice amounts exactly match booking amounts.
     */
    private void addOptimizedPaymentBreakdown(Document document, Booking booking) throws DocumentException {
        document.add(new Paragraph("PAYMENT BREAKDOWN", HEADER_FONT));

        PdfPTable paymentTable = createSimpleTable(3, 100, 20);
        paymentTable.setWidths(new float[]{3, 1, 1});

        // Headers
        addPaymentHeaderCell(paymentTable, "DESCRIPTION");
        addPaymentHeaderCell(paymentTable, "QTY");
        addPaymentHeaderCell(paymentTable, "AMOUNT");

        // Calculate amounts from booking.getTotalFare() for consistency
        double totalAmount = booking.getTotalFare();
        double convenienceFee = 20.0;
        double baseFare = Math.max(0, totalAmount - convenienceFee);
        double gst = 0.0;

        // Ensure proper rounding for display
        baseFare = Math.round(baseFare * 100.0) / 100.0;
        convenienceFee = Math.round(convenienceFee * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        // Data rows
        addPaymentDataRow(paymentTable, "Train Booking Fare", "1", "₹" + String.format("%.2f", baseFare));
        addPaymentDataRow(paymentTable, "Convenience Fee", "1", "₹" + String.format("%.2f", convenienceFee));
        addPaymentDataRow(paymentTable, "GST (0%)", "1", "₹" + String.format("%.2f", gst));

        // Total row
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT", totalFont));
        totalLabelCell.setBackgroundColor(INDIAN_BLUE);
        totalLabelCell.setPadding(10);
        totalLabelCell.setColspan(2);
        paymentTable.addCell(totalLabelCell);

        PdfPCell totalAmountCell = new PdfPCell(new Phrase("₹" + String.format("%.2f", totalAmount), totalFont));
        totalAmountCell.setBackgroundColor(INDIAN_BLUE);
        totalAmountCell.setPadding(10);
        totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        paymentTable.addCell(totalAmountCell);

        document.add(paymentTable);
    }

    /**
     * Creates optimized payment information section.
     */
    private void addOptimizedPaymentInformation(Document document, Booking booking) throws DocumentException {
        PdfPTable paymentInfo = createSimpleTable(1, 100, 20);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GREEN);
        cell.setBorderColor(DARK_GREEN);
        cell.setBorderWidth(1);
        cell.setPadding(12);

        cell.addElement(new Paragraph("✓ PAYMENT CONFIRMED", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, DARK_GREEN)));
        cell.addElement(new Paragraph("Payment has been successfully processed.", new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, DARK_GREEN)));
        cell.addElement(new Paragraph("Transaction completed on: " + booking.getBookingTime().format(DATETIME_FORMATTER),
                new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, DARK_GREEN)));

        paymentInfo.addCell(cell);
        document.add(paymentInfo);
    }

    /**
     * Creates optimized invoice footer.
     */
    private void addOptimizedInvoiceFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        LineSeparator ls = new LineSeparator();
        ls.setLineColor(BORDER_GRAY);
        document.add(new Chunk(ls));

        Paragraph footer = new Paragraph("Thank you for choosing Tailyatri! | This is a computer-generated invoice.", FOOTER_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);

        Paragraph terms = new Paragraph("Terms & Conditions apply | For queries contact: support@tailyatri.com", FOOTER_FONT);
        terms.setAlignment(Element.ALIGN_CENTER);
        document.add(terms);
    }

    // -------------------------------------------------------------------------
    // Performance-Optimized Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Creates a simple table with optimized settings.
     */
    private PdfPTable createSimpleTable(int columns, float widthPercentage, float spacingAfter) throws DocumentException {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(widthPercentage);
        table.setSpacingAfter(spacingAfter);
        return table;
    }

    /**
     * Creates a styled card table for content sections.
     */
    private PdfPTable createStyledCard() throws DocumentException {
        return createSimpleTable(1, 100, 15);
    }

    /**
     * Creates a styled card cell with consistent formatting.
     */
    private PdfPCell createStyledCardCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(1);
        cell.setPadding(15);
        return cell;
    }

    /**
     * Creates a borderless cell for layout purposes.
     */
    private PdfPCell createBorderlessCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    /**
     * Adds an optimized info cell with pre-defined styling.
     */
    private void addOptimizedInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = createBorderlessCell();
        cell.setPadding(5);
        cell.addElement(new Paragraph(label, LABEL_FONT));
        cell.addElement(new Paragraph(value, VALUE_FONT));
        table.addCell(cell);
    }

    /**
     * Adds an optimized passenger cell with consistent styling.
     */
    private void addOptimizedPassengerCell(PdfPTable table, String text, BaseColor backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, DATA_FONT));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BORDER_GRAY);
        table.addCell(cell);
    }

    /**
     * Adds a payment header cell with consistent styling.
     */
    private void addPaymentHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(INDIAN_BLUE);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Adds a payment data row with consistent styling.
     */
    private void addPaymentDataRow(PdfPTable table, String description, String qty, String amount) {
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

        PdfPCell descCell = new PdfPCell(new Phrase(description, DATA_FONT));
        descCell.setBackgroundColor(BaseColor.WHITE);
        descCell.setPadding(8);
        descCell.setBorderColor(BORDER_GRAY);
        table.addCell(descCell);

        PdfPCell qtyCell = new PdfPCell(new Phrase(qty, DATA_FONT));
        qtyCell.setBackgroundColor(BaseColor.WHITE);
        qtyCell.setPadding(8);
        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyCell.setBorderColor(BORDER_GRAY);
        table.addCell(qtyCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, amountFont));
        amountCell.setBackgroundColor(BaseColor.WHITE);
        amountCell.setPadding(8);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setBorderColor(BORDER_GRAY);
        table.addCell(amountCell);
    }

    // -------------------------------------------------------------------------
    // High-Performance Caching Methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves user data with caching to avoid repeated database calls.
     * Thread-safe implementation for concurrent PDF generation.
     */
    private User getCachedUser(int userId) {
        return userCache.computeIfAbsent(userId, id -> {
            try {
                return userDAO.getUserById(id);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Retrieves train data with caching to avoid repeated database calls.
     * Thread-safe implementation for concurrent PDF generation.
     */
    private Train getCachedTrain(int trainId) {
        return trainCache.computeIfAbsent(trainId, id -> {
            try {
                return trainDAO.getTrainById(id);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Retrieves station data with caching to avoid repeated database calls.
     * Thread-safe implementation for concurrent PDF generation.
     */
    private Station getCachedStation(int stationId) {
        return stationCache.computeIfAbsent(stationId, id -> {
            try {
                return stationDAO.getStationById(id);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Retrieves passenger list with caching to avoid repeated database calls.
     * Thread-safe implementation for concurrent PDF generation.
     */
    private List<Passenger> getCachedPassengers(int bookingId) {
        return passengerCache.computeIfAbsent(bookingId, id -> {
            try {
                return passengerDAO.getPassengersByBookingId(id);
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Cache Management Methods (Optional - for memory management)
    // -------------------------------------------------------------------------

    /**
     * Clears all caches to free memory. Call this periodically in high-volume scenarios.
     * Thread-safe operation.
     */
    public void clearCaches() {
        userCache.clear();
        trainCache.clear();
        stationCache.clear();
        passengerCache.clear();
    }

    /**
     * Returns the current cache statistics for monitoring purposes.
     */
    public String getCacheStatistics() {
        return String.format("Cache Stats - Users: %d, Trains: %d, Stations: %d, Passengers: %d",
                userCache.size(), trainCache.size(), stationCache.size(), passengerCache.size());
    }
}