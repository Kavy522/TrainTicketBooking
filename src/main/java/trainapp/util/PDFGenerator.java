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

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFGenerator {

    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    public byte[] generateTicketPDF(Booking booking) {
        try {
            System.out.println("Generating PDF ticket for booking: " + booking.getPnr());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add content to PDF
            addHeader(document);
            addBookingDetails(document, booking);
            addPassengerDetails(document, booking);
            addImportantInstructions(document);
            addFooter(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            System.out.println("PDF ticket generated successfully");

            return pdfBytes;

        } catch (Exception e) {
            System.err.println("Error generating PDF ticket: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public byte[] generateInvoicePDF(Booking booking) {
        try {
            System.out.println("Generating invoice PDF for booking: " + booking.getPnr());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add invoice content
            addInvoiceHeader(document, booking);
            addInvoiceDetails(document, booking);
            addPaymentSummary(document, booking);
            addInvoiceFooter(document);

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

    private void addHeader(Document document) throws DocumentException {
        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("🚂 TAILYATRI E-TICKET", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Subtitle
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
        Paragraph subtitle = new Paragraph("Electronic Reservation Slip (ERS)", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Line separator
        document.add(new Paragraph("_".repeat(80)));
        document.add(Chunk.NEWLINE);
    }

    private void addBookingDetails(Document document, Booking booking) throws DocumentException {
        try {
            // Get related data
            User user = userDAO.getUserById(booking.getUserId());
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());

            // Create booking details table
            PdfPTable bookingTable = new PdfPTable(4);
            bookingTable.setWidthPercentage(100);
            bookingTable.setSpacingAfter(20);

            // Headers
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            PdfPCell headerCell;

            headerCell = new PdfPCell(new Phrase("PNR", headerFont));
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(8);
            bookingTable.addCell(headerCell);

            headerCell = new PdfPCell(new Phrase("TRAIN", headerFont));
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(8);
            bookingTable.addCell(headerCell);

            headerCell = new PdfPCell(new Phrase("FROM - TO", headerFont));
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(8);
            bookingTable.addCell(headerCell);

            headerCell = new PdfPCell(new Phrase("STATUS", headerFont));
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(8);
            bookingTable.addCell(headerCell);

            // Data
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            bookingTable.addCell(new PdfPCell(new Phrase(booking.getPnr(), dataFont)));
            bookingTable.addCell(new PdfPCell(new Phrase(
                    (train != null ? train.getTrainNumber() + " - " + train.getName() : "N/A"), dataFont)));
            bookingTable.addCell(new PdfPCell(new Phrase(
                    (fromStation != null ? fromStation.getName() : "N/A") + " → " +
                            (toStation != null ? toStation.getName() : "N/A"), dataFont)));
            bookingTable.addCell(new PdfPCell(new Phrase(booking.getStatus().toUpperCase(), dataFont)));

            document.add(bookingTable);

            // Journey details
            PdfPTable journeyTable = new PdfPTable(3);
            journeyTable.setWidthPercentage(100);
            journeyTable.setSpacingAfter(20);

            addDetailRow(journeyTable, "Booking Date:",
                    booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            addDetailRow(journeyTable, "Total Fare:", "₹" + String.format("%.2f", booking.getTotalFare()));
            addDetailRow(journeyTable, "Passenger Name:", user != null ? user.getName() : "N/A");

            document.add(journeyTable);

        } catch (Exception e) {
            System.err.println("Error adding booking details to PDF: " + e.getMessage());
            // Add basic info even if detailed fetch fails
            document.add(new Paragraph("PNR: " + booking.getPnr()));
            document.add(new Paragraph("Amount: ₹" + booking.getTotalFare()));
        }
    }

    private void addPassengerDetails(Document document, Booking booking) throws DocumentException {
        try {
            List<Passenger> passengers = passengerDAO.getPassengersByBookingId(booking.getBookingId());

            if (!passengers.isEmpty()) {
                // Passenger details header
                Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
                Paragraph passengerHeader = new Paragraph("PASSENGER DETAILS", sectionFont);
                passengerHeader.setSpacingBefore(10);
                passengerHeader.setSpacingAfter(10);
                document.add(passengerHeader);

                // Passenger table
                PdfPTable passengerTable = new PdfPTable(5);
                passengerTable.setWidthPercentage(100);
                passengerTable.setSpacingAfter(20);

                // Headers
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
                String[] headers = {"S.No", "Name", "Age", "Gender", "Coach/Seat"};

                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(BaseColor.DARK_GRAY);
                    cell.setPadding(8);
                    passengerTable.addCell(cell);
                }

                // Passenger data
                Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);
                for (int i = 0; i < passengers.size(); i++) {
                    Passenger passenger = passengers.get(i);

                    passengerTable.addCell(new PdfPCell(new Phrase(String.valueOf(i + 1), dataFont)));
                    passengerTable.addCell(new PdfPCell(new Phrase(passenger.getName(), dataFont)));
                    passengerTable.addCell(new PdfPCell(new Phrase(String.valueOf(passenger.getAge()), dataFont)));
                    passengerTable.addCell(new PdfPCell(new Phrase(passenger.getGender(), dataFont)));
                    passengerTable.addCell(new PdfPCell(new Phrase(
                            passenger.getCoachType() + "/" + passenger.getSeatNumber(), dataFont)));
                }

                document.add(passengerTable);
            }
        } catch (Exception e) {
            System.err.println("Error adding passenger details to PDF: " + e.getMessage());
            document.add(new Paragraph("Passenger details unavailable"));
        }
    }

    private void addImportantInstructions(Document document) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Paragraph instructionHeader = new Paragraph("IMPORTANT INSTRUCTIONS", sectionFont);
        instructionHeader.setSpacingBefore(20);
        instructionHeader.setSpacingAfter(10);
        document.add(instructionHeader);

        Font instructionFont = new Font(Font.FontFamily.HELVETICA, 10);
        String[] instructions = {
                "• Please carry this e-ticket along with any one of the prescribed proofs of identity in original during the journey.",
                "• Passengers should report at the railway station at least 30 minutes before the scheduled departure of the train.",
                "• E-ticket is valid for travel only when the passenger's identity is verified by the authorized personnel.",
                "• This ticket is non-transferable and valid only for the person(s) named herein.",
                "• Cancellation and modification charges apply as per railway rules.",
                "• In case of any query, please contact customer support."
        };

        for (String instruction : instructions) {
            Paragraph p = new Paragraph(instruction, instructionFont);
            p.setSpacingAfter(5);
            document.add(p);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("_".repeat(80)));

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Generated by Tailyatri - Your Journey Partner | Customer Support: support@tailyatri.com", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);

        Paragraph generated = new Paragraph("Generated on: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), footerFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        document.add(generated);
    }

    private void addInvoiceHeader(Document document, Booking booking) throws DocumentException {
        // Invoice header
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph title = new Paragraph("🧾 PAYMENT INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Invoice number and date
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph invoiceInfo = new Paragraph("Invoice #: " + booking.getPnr() + " | Date: " +
                booking.getBookingTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), headerFont);
        invoiceInfo.setAlignment(Element.ALIGN_CENTER);
        invoiceInfo.setSpacingAfter(20);
        document.add(invoiceInfo);

        document.add(new Paragraph("_".repeat(80)));
        document.add(Chunk.NEWLINE);
    }

    private void addInvoiceDetails(Document document, Booking booking) throws DocumentException {
        // Customer details
        try {
            User user = userDAO.getUserById(booking.getUserId());
            if (user != null) {
                Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
                Paragraph customerHeader = new Paragraph("CUSTOMER DETAILS", sectionFont);
                customerHeader.setSpacingAfter(10);
                document.add(customerHeader);

                Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);
                document.add(new Paragraph("Name: " + user.getName(), dataFont));
                document.add(new Paragraph("Email: " + user.getEmail(), dataFont));
                document.add(new Paragraph("Phone: " + user.getPhone(), dataFont));
                document.add(Chunk.NEWLINE);
            }
        } catch (Exception e) {
            System.err.println("Error adding customer details: " + e.getMessage());
        }
    }

    private void addPaymentSummary(Document document, Booking booking) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Paragraph summaryHeader = new Paragraph("PAYMENT SUMMARY", sectionFont);
        summaryHeader.setSpacingAfter(10);
        document.add(summaryHeader);

        // Payment breakdown table
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        paymentTable.setSpacingAfter(20);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10);

        // Calculate breakdown (assuming 5% GST and ₹20 convenience fee)
        double baseFare = booking.getTotalFare() / 1.05 - 20; // Remove GST and convenience fee
        double convenienceFee = 20;
        double gst = baseFare * 0.05;

        paymentTable.addCell(new PdfPCell(new Phrase("Base Fare:", labelFont)));
        paymentTable.addCell(new PdfPCell(new Phrase("₹" + String.format("%.2f", baseFare), valueFont)));

        paymentTable.addCell(new PdfPCell(new Phrase("Convenience Fee:", labelFont)));
        paymentTable.addCell(new PdfPCell(new Phrase("₹" + String.format("%.2f", convenienceFee), valueFont)));

        paymentTable.addCell(new PdfPCell(new Phrase("GST (5%):", labelFont)));
        paymentTable.addCell(new PdfPCell(new Phrase("₹" + String.format("%.2f", gst), valueFont)));

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT:", labelFont));
        totalLabelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        paymentTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase("₹" + String.format("%.2f", booking.getTotalFare()), labelFont));
        totalValueCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        paymentTable.addCell(totalValueCell);

        document.add(paymentTable);
    }

    private void addInvoiceFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("_".repeat(80)));

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Thank you for choosing Tailyatri | This is a computer-generated invoice", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10);

        table.addCell(new PdfPCell(new Phrase(label, labelFont)));
        table.addCell(new PdfPCell(new Phrase(value, valueFont)));
        table.addCell(new PdfPCell(new Phrase(""))); // Empty cell for spacing
    }
}
