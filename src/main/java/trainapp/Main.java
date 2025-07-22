package trainapp;


import trainapp.model.Passenger;
import trainapp.util.PDFGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        /*String pnr, String trainName, String trainNo, String date,
                String source, String destination,
                List< Passenger > passengers, double fare, String filePath*/

        ArrayList<Passenger> passangers = new ArrayList<>();
        passangers.add(new Passenger("kavy" , 18 , "male" , "5" , "Window" , "A3"));

        File file = new File("src/main/resources/tickets/1_ticket.pdf");
        file.createNewFile();

            // Create test passenger list
            List<PDFGenerator.PassengerInfo> passengers = List.of(
                    new PDFGenerator.PassengerInfo("John Doe", 35, "M", "CNF/A1/25", "CNF/A1/25"),
                    new PDFGenerator.PassengerInfo("Jane Smith", 28, "F", "CNF/A1/26", "CNF/A1/26"),
                    new PDFGenerator.PassengerInfo("Robert Johnson", 45, "M", "RAC/12", "RAC/12"),
                    new PDFGenerator.PassengerInfo("Emily Davis", 22, "F", "WL/15", "WL/10")
            );

            // Generate current timestamp for unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputPath = "src/main/resources/tickets/ticket_" + timestamp + ".pdf";

            // Call the method with test data
            PDFGenerator.generateBookingPDF(
                    outputPath,
                    "1234567890",  // PNR
                    "12345",       // Train No
                    "RAJDHANI EXP", // Train Name
                    "3A",          // Class
                    "GENERAL",      // Quota
                    "MUMBAI CENTRAL", // From
                    "NEW DELHI",    // To
                    "15-Jul-2023",  // Date
                    "16:35",        // Departure
                    "08:15",        // Arrival
                    "10-Jul-2023 14:22:33", // Booking DateTime
                    "TEST123456789", // Transaction ID
                    passengers,
                    1850.00,       // Base Fare
                    275.00,        // Catering
                    45.50,         // Convenience Fee
                    2170.50        // Total Fare
            );

            // Verify the file was created
            Path path = Paths.get(outputPath);

            System.out.println("Test PDF generated at: " + path.toAbsolutePath());
        }
    }
