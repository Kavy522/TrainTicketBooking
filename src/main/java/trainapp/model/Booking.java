package trainapp.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Booking {
    private LongProperty bookingId = new SimpleLongProperty();
    private IntegerProperty userId = new SimpleIntegerProperty();
    private LongProperty journeyId = new SimpleLongProperty();
    private IntegerProperty trainId = new SimpleIntegerProperty();
    private IntegerProperty sourceStationId = new SimpleIntegerProperty();
    private IntegerProperty destStationId = new SimpleIntegerProperty();
    private ObjectProperty<LocalDateTime> bookingTime = new SimpleObjectProperty<>();
    private DoubleProperty totalFare = new SimpleDoubleProperty();
    private StringProperty status = new SimpleStringProperty(); // waiting, confirmed, cancelled
    private StringProperty pnr = new SimpleStringProperty();
    private StringProperty userName = new SimpleStringProperty(); // New: For displaying username
    private StringProperty trainNumber = new SimpleStringProperty(); // New: For displaying train number

    // Constructors
    public Booking() {}

    public Booking(int userId, long journeyId, int trainId, int sourceStationId,
                   int destStationId, double totalFare, String status, String pnr) {
        this.userId.set(userId);
        this.journeyId.set(journeyId);
        this.trainId.set(trainId);
        this.sourceStationId.set(sourceStationId);
        this.destStationId.set(destStationId);
        this.totalFare.set(totalFare);
        this.status.set(status);
        this.pnr.set(pnr);
        this.bookingTime.set(LocalDateTime.now());
    }

    // Getters and Setters with Properties
    public long getBookingId() { return bookingId.get(); }
    public void setBookingId(long bookingId) { this.bookingId.set(bookingId); }
    public LongProperty bookingIdProperty() { return bookingId; }

    public int getUserId() { return userId.get(); }
    public void setUserId(int userId) { this.userId.set(userId); }
    public IntegerProperty userIdProperty() { return userId; }

    public long getJourneyId() { return journeyId.get(); }
    public void setJourneyId(long journeyId) { this.journeyId.set(journeyId); }
    public LongProperty journeyIdProperty() { return journeyId; }

    public int getTrainId() { return trainId.get(); }
    public void setTrainId(int trainId) { this.trainId.set(trainId); }
    public IntegerProperty trainIdProperty() { return trainId; }

    public int getSourceStationId() { return sourceStationId.get(); }
    public void setSourceStationId(int sourceStationId) { this.sourceStationId.set(sourceStationId); }
    public IntegerProperty sourceStationIdProperty() { return sourceStationId; }

    public int getDestStationId() { return destStationId.get(); }
    public void setDestStationId(int destStationId) { this.destStationId.set(destStationId); }
    public IntegerProperty destStationIdProperty() { return destStationId; }

    public LocalDateTime getBookingTime() { return bookingTime.get(); }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime.set(bookingTime); }
    public ObjectProperty<LocalDateTime> bookingTimeProperty() { return bookingTime; }

    public double getTotalFare() { return totalFare.get(); }
    public void setTotalFare(double totalFare) { this.totalFare.set(totalFare); }
    public DoubleProperty totalFareProperty() { return totalFare; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    public String getPnr() { return pnr.get(); }
    public void setPnr(String pnr) { this.pnr.set(pnr); }
    public StringProperty pnrProperty() { return pnr; }

    // New: UserName
    public String getUserName() { return userName.get(); }
    public void setUserName(String userName) { this.userName.set(userName); }
    public StringProperty userNameProperty() { return userName; }

    // New: TrainNumber
    public String getTrainNumber() { return trainNumber.get(); }
    public void setTrainNumber(String trainNumber) { this.trainNumber.set(trainNumber); }
    public StringProperty trainNumberProperty() { return trainNumber; }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId.get() +
                ", userId=" + userId.get() +
                ", trainId=" + trainId.get() +
                ", pnr='" + pnr.get() + '\'' +
                ", status='" + status.get() + '\'' +
                ", totalFare=" + totalFare.get() +
                '}';
    }
}