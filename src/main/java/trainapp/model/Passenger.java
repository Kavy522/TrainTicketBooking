package trainapp.model;

public class Passenger {
    private long passengerId;
    private long bookingId;
    private String name;
    private int age;
    private String gender;
    private String seatNumber;
    private String coachType;

    // Constructors
    public Passenger() {}

    public Passenger(long bookingId, String name, int age, String gender, String seatNumber, String coachType) {
        this.bookingId = bookingId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber;
        this.coachType = coachType;
    }

    // Getters and Setters
    public long getPassengerId() { return passengerId; }
    public void setPassengerId(long passengerId) { this.passengerId = passengerId; }

    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getCoachType() { return coachType; }
    public void setCoachType(String coachType) { this.coachType = coachType; }

    @Override
    public String toString() {
        return "Passenger{" +
                "passengerId=" + passengerId +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", coachType='" + coachType + '\'' +
                '}';
    }
}
