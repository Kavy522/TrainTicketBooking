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

/**
 * FIXED PDFGenerator - Ensures invoice amounts exactly match booking amounts.
 * Payment breakdown is calculated from booking.getTotalFare() to maintain consistency.
 */
public class PDFGenerator {

    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    // Modern color scheme inspired by Indian Railways
    private static final BaseColor INDIAN_ORANGE = new BaseColor(255, 153, 51);
    private static final BaseColor INDIAN_BLUE = new BaseColor(0, 123, 191);
    private static final BaseColor DARK_GREEN = new BaseColor(0, 100, 0);
    private static final BaseColor LIGHT_GRAY = new BaseColor(248, 248, 248);
    private static final BaseColor BORDER_GRAY = new BaseColor(220, 220, 220);

    public byte[] generateTicketPDF(Booking booking) {
        try {
            System.out.println("=== GENERATING TICKET PDF ===");
            System.out.println("Booking: " + booking.getPnr());
            System.out.println("Amount: ₹" + booking.getTotalFare());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add modern header with Indian Railways styling
            addModernTicketHeader(document);

            // Add booking overview card
            addBookingOverviewCard(document, booking);

            // Add journey details in modern layout
            addJourneyDetailsCard(document, booking);

            // Add passenger details in table format
            addPassengerDetailsCard(document, booking);

            // Add travel instructions
            addTravelInstructionsCard(document);

            // Add modern footer
            addModernTicketFooter(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            System.out.println("Ticket PDF generated successfully");
            return pdfBytes;

        } catch (Exception e) {
            System.err.println("Error generating ticket PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public byte[] generateInvoicePDF(Booking booking) {
        try {
            System.out.println("=== GENERATING INVOICE PDF ===");
            System.out.println("Booking: " + booking.getPnr());
            System.out.println("Amount: ₹" + booking.getTotalFare());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add modern invoice header
            addModernInvoiceHeader(document, booking);

            // Add company and customer details
            addInvoicePartyDetails(document, booking);

            // Add booking summary
            addInvoiceBookingSummary(document, booking);

            // FIXED: Add payment breakdown with consistent amounts
            addModernPaymentBreakdown(document, booking);

            // Add payment details
            addPaymentInformation(document, booking);

            // Add modern invoice footer
            addModernInvoiceFooter(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            System.out.println("Invoice PDF generated successfully");
            return pdfBytes;

        } catch (Exception e) {
            System.err.println("Error generating invoice PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addModernTicketHeader(Document document) throws DocumentException {
        // Top orange stripe (Indian Railways style)
        PdfPTable headerStripe = new PdfPTable(1);
        headerStripe.setWidthPercentage(100);
        headerStripe.setSpacingAfter(15);

        PdfPCell stripeCell = new PdfPCell();
        stripeCell.setBackgroundColor(INDIAN_ORANGE);
        stripeCell.setFixedHeight(8);
        stripeCell.setBorder(Rectangle.NO_BORDER);
        headerStripe.addCell(stripeCell);
        document.add(headerStripe);

        // Main header with logo and title
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3});
        headerTable.setSpacingAfter(20);

        // Logo section (left)
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        Font logoFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, INDIAN_BLUE);
        Paragraph logo = new Paragraph("🚂", logoFont);
        logo.setAlignment(Element.ALIGN_CENTER);
        logoCell.addElement(logo);
        headerTable.addCell(logoCell);

        // Title section (right)
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, INDIAN_BLUE);
        Paragraph title = new Paragraph("TAILYATRI", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        titleCell.addElement(title);

        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
        Paragraph subtitle = new Paragraph("Electronic Reservation Slip (ERS)", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_LEFT);
        titleCell.addElement(subtitle);

        headerTable.addCell(titleCell);
        document.add(headerTable);
    }

    private void addBookingOverviewCard(Document document, Booking booking) throws DocumentException {
        // Card container
        PdfPTable cardTable = new PdfPTable(1);
        cardTable.setWidthPercentage(100);
        cardTable.setSpacingAfter(15);

        PdfPCell cardCell = new PdfPCell();
        cardCell.setBackgroundColor(LIGHT_GRAY);
        cardCell.setBorderColor(BORDER_GRAY);
        cardCell.setBorderWidth(1);
        cardCell.setPadding(15);

        // PNR and Status row
        PdfPTable pnrTable = new PdfPTable(2);
        pnrTable.setWidthPercentage(100);

        Font pnrLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
        Font pnrValueFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, INDIAN_BLUE);
        Font statusFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, DARK_GREEN);

        PdfPCell pnrCell = new PdfPCell();
        pnrCell.setBorder(Rectangle.NO_BORDER);
        pnrCell.addElement(new Paragraph("PNR NUMBER", pnrLabelFont));
        pnrCell.addElement(new Paragraph(booking.getPnr(), pnrValueFont));
        pnrTable.addCell(pnrCell);

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        statusCell.addElement(new Paragraph("STATUS", pnrLabelFont));
        statusCell.addElement(new Paragraph("✓ " + booking.getStatus().toUpperCase(), statusFont));
        pnrTable.addCell(statusCell);

        cardCell.addElement(pnrTable);
        cardTable.addCell(cardCell);
        document.add(cardTable);
    }

    private void addJourneyDetailsCard(Document document, Booking booking) throws DocumentException {
        try {
            // Get related data
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());

            // Journey details card
            PdfPTable journeyCard = new PdfPTable(1);
            journeyCard.setWidthPercentage(100);
            journeyCard.setSpacingAfter(15);

            PdfPCell journeyCell = new PdfPCell();
            journeyCell.setBackgroundColor(BaseColor.WHITE);
            journeyCell.setBorderColor(BORDER_GRAY);
            journeyCell.setBorderWidth(1);
            journeyCell.setPadding(15);

            // Train details header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
            Paragraph trainHeader = new Paragraph("TRAIN DETAILS", headerFont);
            trainHeader.setSpacingAfter(10);
            journeyCell.addElement(trainHeader);

            // Train info table
            PdfPTable trainTable = new PdfPTable(4);
            trainTable.setWidthPercentage(100);
            trainTable.setWidths(new float[]{2, 2, 1.5f, 1.5f});

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.GRAY);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.BLACK);

            // Train details
            addInfoCell(trainTable, "TRAIN", train != null ? train.getTrainNumber() : "N/A", labelFont, valueFont);
            addInfoCell(trainTable, "NAME", train != null ? train.getName() : "N/A", labelFont, valueFont);
            addInfoCell(trainTable, "DATE", booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), labelFont, valueFont);
            addInfoCell(trainTable, "QUOTA", "GN", labelFont, valueFont);

            journeyCell.addElement(trainTable);

            // Route section with modern design
            journeyCell.addElement(new Paragraph(" "));
            Font routeHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
            Paragraph routeHeader = new Paragraph("JOURNEY ROUTE", routeHeaderFont);
            routeHeader.setSpacingAfter(10);
            journeyCell.addElement(routeHeader);

            // Route visualization
            PdfPTable routeTable = new PdfPTable(3);
            routeTable.setWidthPercentage(100);
            routeTable.setWidths(new float[]{2, 1, 2});

            Font stationFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE);
            Font arrowFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, INDIAN_ORANGE);

            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            fromCell.addElement(new Paragraph("FROM", labelFont));
            fromCell.addElement(new Paragraph(fromStation != null ? fromStation.getName() : "N/A", stationFont));
            routeTable.addCell(fromCell);

            PdfPCell arrowCell = new PdfPCell(new Phrase("→", arrowFont));
            arrowCell.setBorder(Rectangle.NO_BORDER);
            arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            arrowCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            routeTable.addCell(arrowCell);

            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            toCell.addElement(new Paragraph("TO", labelFont));
            toCell.addElement(new Paragraph(toStation != null ? toStation.getName() : "N/A", stationFont));
            routeTable.addCell(toCell);

            journeyCell.addElement(routeTable);
            journeyCard.addCell(journeyCell);
            document.add(journeyCard);

        } catch (Exception e) {
            System.err.println("Error adding journey details: " + e.getMessage());
            addFallbackJourneyDetails(document, booking);
        }
    }

    private void addPassengerDetailsCard(Document document, Booking booking) throws DocumentException {
        try {
            List<Passenger> passengers = passengerDAO.getPassengersByBookingId(booking.getBookingId());

            if (!passengers.isEmpty()) {
                // Passenger card
                PdfPTable passengerCard = new PdfPTable(1);
                passengerCard.setWidthPercentage(100);
                passengerCard.setSpacingAfter(15);

                PdfPCell passengerCell = new PdfPCell();
                passengerCell.setBackgroundColor(BaseColor.WHITE);
                passengerCell.setBorderColor(BORDER_GRAY);
                passengerCell.setBorderWidth(1);
                passengerCell.setPadding(15);

                // Header
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
                Paragraph passengerHeader = new Paragraph("PASSENGER DETAILS", headerFont);
                passengerHeader.setSpacingAfter(12);
                passengerCell.addElement(passengerHeader);

                // Passenger table with modern styling
                PdfPTable passengerTable = new PdfPTable(5);
                passengerTable.setWidthPercentage(100);
                passengerTable.setWidths(new float[]{0.5f, 2.5f, 0.8f, 1, 1.5f});

                // Table headers
                Font headerTableFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
                String[] headers = {"S.No", "Passenger Name", "Age", "Gender", "Coach/Seat"};

                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerTableFont));
                    cell.setBackgroundColor(INDIAN_BLUE);
                    cell.setPadding(8);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setBorderColor(BaseColor.WHITE);
                    passengerTable.addCell(cell);
                }

                // Passenger data with alternating row colors
                Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
                for (int i = 0; i < passengers.size(); i++) {
                    Passenger passenger = passengers.get(i);
                    BaseColor rowColor = (i % 2 == 0) ? BaseColor.WHITE : LIGHT_GRAY;

                    addPassengerCell(passengerTable, String.valueOf(i + 1), dataFont, rowColor, Element.ALIGN_CENTER);
                    addPassengerCell(passengerTable, passenger.getName(), dataFont, rowColor, Element.ALIGN_LEFT);
                    addPassengerCell(passengerTable, String.valueOf(passenger.getAge()), dataFont, rowColor, Element.ALIGN_CENTER);
                    addPassengerCell(passengerTable, passenger.getGender(), dataFont, rowColor, Element.ALIGN_CENTER);
                    addPassengerCell(passengerTable, passenger.getCoachType() + "/" + passenger.getSeatNumber(), dataFont, rowColor, Element.ALIGN_CENTER);
                }

                passengerCell.addElement(passengerTable);
                passengerCard.addCell(passengerCell);
                document.add(passengerCard);
            }
        } catch (Exception e) {
            System.err.println("Error adding passenger details: " + e.getMessage());
        }
    }

    private void addTravelInstructionsCard(Document document) throws DocumentException {
        // Instructions card
        PdfPTable instructionCard = new PdfPTable(1);
        instructionCard.setWidthPercentage(100);
        instructionCard.setSpacingAfter(15);

        PdfPCell instructionCell = new PdfPCell();
        instructionCell.setBackgroundColor(new BaseColor(252, 248, 227)); // Light yellow
        instructionCell.setBorderColor(new BaseColor(251, 188, 52)); // Yellow border
        instructionCell.setBorderWidth(1);
        instructionCell.setPadding(15);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(133, 77, 14));
        Paragraph instructionHeader = new Paragraph("⚠️ IMPORTANT TRAVEL INSTRUCTIONS", headerFont);
        instructionHeader.setSpacingAfter(8);
        instructionCell.addElement(instructionHeader);

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
            instructionCell.addElement(p);
        }

        instructionCard.addCell(instructionCell);
        document.add(instructionCard);
    }

    private void addModernTicketFooter(Document document) throws DocumentException {
        // Footer with modern styling
        document.add(new Paragraph(" "));

        LineSeparator ls = new LineSeparator();
        ls.setLineColor(BORDER_GRAY);
        document.add(new Chunk(ls));

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(10);

        PdfPCell leftFooter = new PdfPCell();
        leftFooter.setBorder(Rectangle.NO_BORDER);
        leftFooter.addElement(new Paragraph("Generated by Tailyatri", footerFont));
        leftFooter.addElement(new Paragraph("Customer Support: support@tailyatri.com", footerFont));
        footerTable.addCell(leftFooter);

        PdfPCell rightFooter = new PdfPCell();
        rightFooter.setBorder(Rectangle.NO_BORDER);
        rightFooter.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightFooter.addElement(new Paragraph("Generated on:", footerFont));
        rightFooter.addElement(new Paragraph(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), footerFont));
        footerTable.addCell(rightFooter);

        document.add(footerTable);
    }

    // ======================= INVOICE METHODS =======================

    private void addModernInvoiceHeader(Document document, Booking booking) throws DocumentException {
        // Top blue stripe
        PdfPTable headerStripe = new PdfPTable(1);
        headerStripe.setWidthPercentage(100);
        headerStripe.setSpacingAfter(20);

        PdfPCell stripeCell = new PdfPCell();
        stripeCell.setBackgroundColor(INDIAN_BLUE);
        stripeCell.setFixedHeight(8);
        stripeCell.setBorder(Rectangle.NO_BORDER);
        headerStripe.addCell(stripeCell);
        document.add(headerStripe);

        // Invoice header
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});
        headerTable.setSpacingAfter(25);

        // Company details (left)
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);

        Font companyFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, INDIAN_BLUE);
        Paragraph company = new Paragraph("🚂 TAILYATRI", companyFont);
        companyCell.addElement(company);

        Font addressFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
        companyCell.addElement(new Paragraph("Digital Train Booking Platform", addressFont));
        companyCell.addElement(new Paragraph("Email: support@tailyatri.com", addressFont));
        companyCell.addElement(new Paragraph("Phone: +91-9876543210", addressFont));
        headerTable.addCell(companyCell);

        // Invoice details (right)
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font invoiceFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, INDIAN_ORANGE);
        Paragraph invoiceTitle = new Paragraph("INVOICE", invoiceFont);
        invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
        invoiceCell.addElement(invoiceTitle);

        Font detailsFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        invoiceCell.addElement(new Paragraph("Invoice #: " + booking.getPnr(), detailsFont));
        invoiceCell.addElement(new Paragraph("Date: " + booking.getBookingTime()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), detailsFont));
        invoiceCell.addElement(new Paragraph("Payment: Completed", detailsFont));
        headerTable.addCell(invoiceCell);

        document.add(headerTable);
    }

    private void addInvoicePartyDetails(Document document, Booking booking) throws DocumentException {
        try {
            User user = userDAO.getUserById(booking.getUserId());

            PdfPTable partyTable = new PdfPTable(2);
            partyTable.setWidthPercentage(100);
            partyTable.setWidths(new float[]{1, 1});
            partyTable.setSpacingAfter(20);

            // Bill To section
            PdfPCell billToCell = new PdfPCell();
            billToCell.setBorder(Rectangle.BOX);
            billToCell.setBorderColor(BORDER_GRAY);
            billToCell.setPadding(12);
            billToCell.setBackgroundColor(LIGHT_GRAY);

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, INDIAN_BLUE);
            billToCell.addElement(new Paragraph("BILL TO", headerFont));

            if (user != null) {
                Font detailFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
                billToCell.addElement(new Paragraph(" "));
                billToCell.addElement(new Paragraph(user.getName(), detailFont));
                billToCell.addElement(new Paragraph(user.getEmail(), detailFont));
                billToCell.addElement(new Paragraph(user.getPhone(), detailFont));
            }
            partyTable.addCell(billToCell);

            // Payment details section
            PdfPCell paymentCell = new PdfPCell();
            paymentCell.setBorder(Rectangle.BOX);
            paymentCell.setBorderColor(BORDER_GRAY);
            paymentCell.setPadding(12);
            paymentCell.setBackgroundColor(LIGHT_GRAY);

            paymentCell.addElement(new Paragraph("PAYMENT DETAILS", headerFont));

            Font detailFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            paymentCell.addElement(new Paragraph(" "));
            paymentCell.addElement(new Paragraph("Payment Method: Online", detailFont));
            paymentCell.addElement(new Paragraph("Status: Completed", detailFont));
            paymentCell.addElement(new Paragraph("PNR: " + booking.getPnr(), detailFont));
            partyTable.addCell(paymentCell);

            document.add(partyTable);
        } catch (Exception e) {
            System.err.println("Error adding party details: " + e.getMessage());
        }
    }

    private void addInvoiceBookingSummary(Document document, Booking booking) throws DocumentException {
        try {
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
            Paragraph bookingHeader = new Paragraph("BOOKING SUMMARY", headerFont);
            bookingHeader.setSpacingAfter(12);
            document.add(bookingHeader);

            PdfPTable summaryTable = new PdfPTable(1);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);

            PdfPCell summaryCell = new PdfPCell();
            summaryCell.setBorder(Rectangle.BOX);
            summaryCell.setBorderColor(BORDER_GRAY);
            summaryCell.setPadding(15);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

            PdfPTable detailsTable = new PdfPTable(4);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1, 1, 1, 1});

            addInfoCell(detailsTable, "TRAIN", train != null ? train.getTrainNumber() : "N/A", labelFont, valueFont);
            addInfoCell(detailsTable, "ROUTE",
                    (fromStation != null ? fromStation.getName() : "N/A") + " → " +
                            (toStation != null ? toStation.getName() : "N/A"), labelFont, valueFont);
            addInfoCell(detailsTable, "DATE", booking.getBookingTime()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), labelFont, valueFont);
            addInfoCell(detailsTable, "PASSENGERS", String.valueOf(passengerDAO.getPassengersByBookingId(booking.getBookingId()).size()), labelFont, valueFont);

            summaryCell.addElement(detailsTable);
            summaryTable.addCell(summaryCell);
            document.add(summaryTable);

        } catch (Exception e) {
            System.err.println("Error adding booking summary: " + e.getMessage());
        }
    }

    /**
     * FIXED: Calculate payment breakdown from booking.getTotalFare() to ensure consistency.
     * This ensures invoice amount matches booking summary and payment amount.
     */
    private void addModernPaymentBreakdown(Document document, Booking booking) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
        Paragraph paymentHeader = new Paragraph("PAYMENT BREAKDOWN", headerFont);
        paymentHeader.setSpacingAfter(12);
        document.add(paymentHeader);

        PdfPTable paymentTable = new PdfPTable(3);
        paymentTable.setWidthPercentage(100);
        paymentTable.setWidths(new float[]{3, 1, 1});
        paymentTable.setSpacingAfter(20);

        // Header row
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
        addPaymentHeaderCell(paymentTable, "DESCRIPTION", tableHeaderFont);
        addPaymentHeaderCell(paymentTable, "QTY", tableHeaderFont);
        addPaymentHeaderCell(paymentTable, "AMOUNT", tableHeaderFont);

        // CRITICAL FIX: Calculate amounts from booking.getTotalFare() (from DB)
        double totalAmount = booking.getTotalFare();
        double convenienceFee = 20.0;
        double baseFare = totalAmount - convenienceFee;
        double gst = 0.0;

        // Ensure non-negative values
        if (baseFare < 0) {
            baseFare = 0;
            convenienceFee = totalAmount;
            System.out.println("Adjusted convenience fee due to low total amount");
        }

        // Round all amounts for display consistency
        baseFare = Math.round(baseFare * 100.0) / 100.0;
        convenienceFee = Math.round(convenienceFee * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        Font itemFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

        // Data rows with correct amounts
        addPaymentDataRow(paymentTable, "Train Booking Fare", "1", "₹" + String.format("%.2f", baseFare), itemFont, amountFont);
        addPaymentDataRow(paymentTable, "Convenience Fee", "1", "₹" + String.format("%.2f", convenienceFee), itemFont, amountFont);
        addPaymentDataRow(paymentTable, "GST (0%)", "1", "₹" + String.format("%.2f", gst), itemFont, amountFont);

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

        System.out.println("=== INVOICE PAYMENT BREAKDOWN ===");
        System.out.println("Base fare: ₹" + baseFare);
        System.out.println("Convenience fee: ₹" + convenienceFee);
        System.out.println("GST: ₹" + gst);
        System.out.println("Total: ₹" + totalAmount);
        System.out.println("This matches booking.getTotalFare(): ₹" + booking.getTotalFare());
    }

    private void addPaymentInformation(Document document, Booking booking) throws DocumentException {
        PdfPTable paymentInfoTable = new PdfPTable(1);
        paymentInfoTable.setWidthPercentage(100);
        paymentInfoTable.setSpacingAfter(20);

        PdfPCell paymentInfoCell = new PdfPCell();
        paymentInfoCell.setBackgroundColor(new BaseColor(232, 245, 233)); // Light green
        paymentInfoCell.setBorderColor(DARK_GREEN);
        paymentInfoCell.setBorderWidth(1);
        paymentInfoCell.setPadding(12);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, DARK_GREEN);
        Paragraph paymentHeader = new Paragraph("✓ PAYMENT CONFIRMED", headerFont);
        paymentHeader.setSpacingAfter(8);
        paymentInfoCell.addElement(paymentHeader);

        Font detailFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, DARK_GREEN);
        paymentInfoCell.addElement(new Paragraph("Payment has been successfully processed.", detailFont));
        paymentInfoCell.addElement(new Paragraph("Transaction completed on: " +
                booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), detailFont));

        paymentInfoTable.addCell(paymentInfoCell);
        document.add(paymentInfoTable);
    }

    private void addModernInvoiceFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        LineSeparator ls = new LineSeparator();
        ls.setLineColor(BORDER_GRAY);
        document.add(new Chunk(ls));

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Thank you for choosing Tailyatri! | This is a computer-generated invoice.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);

        Paragraph terms = new Paragraph("Terms & Conditions apply | For queries contact: support@tailyatri.com", footerFont);
        terms.setAlignment(Element.ALIGN_CENTER);
        document.add(terms);
    }

    // ======================= HELPER METHODS =======================

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addPassengerCell(PdfPTable table, String text, Font font, BaseColor backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BORDER_GRAY);
        table.addCell(cell);
    }

    private void addPaymentHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(INDIAN_BLUE);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addPaymentDataRow(PdfPTable table, String description, String qty, String amount, Font itemFont, Font amountFont) {
        BaseColor rowColor = BaseColor.WHITE;

        PdfPCell descCell = new PdfPCell(new Phrase(description, itemFont));
        descCell.setBackgroundColor(rowColor);
        descCell.setPadding(8);
        descCell.setBorderColor(BORDER_GRAY);
        table.addCell(descCell);

        PdfPCell qtyCell = new PdfPCell(new Phrase(qty, itemFont));
        qtyCell.setBackgroundColor(rowColor);
        qtyCell.setPadding(8);
        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qtyCell.setBorderColor(BORDER_GRAY);
        table.addCell(qtyCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, amountFont));
        amountCell.setBackgroundColor(rowColor);
        amountCell.setPadding(8);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setBorderColor(BORDER_GRAY);
        table.addCell(amountCell);
    }

    private void addFallbackJourneyDetails(Document document, Booking booking) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, INDIAN_BLUE);
        Paragraph journeyHeader = new Paragraph("JOURNEY DETAILS", headerFont);
        journeyHeader.setSpacingAfter(10);
        document.add(journeyHeader);

        Font detailFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        document.add(new Paragraph("PNR: " + booking.getPnr(), detailFont));
        document.add(new Paragraph("Amount: ₹" + String.format("%.2f", booking.getTotalFare()), detailFont));
        document.add(new Paragraph(" "));
    }
}