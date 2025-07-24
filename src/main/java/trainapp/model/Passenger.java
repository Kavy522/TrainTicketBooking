package trainapp.model;
public class Passenger {

    private int passenger_id;
    private long booking_id;
    private String name;
    private int age;
    private String gender;
    private String seatNumber;
    private String coach_type;

    public Passenger() {}

    public Passenger(int passenger_id, long booking_id, String name, int age, String gender, String seatNumber, String coach_type) {
        this.passenger_id = passenger_id;
        this.booking_id = booking_id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber;
        this.coach_type = coach_type;
    }

    // Getters and Setters
    public int getPassenger_id() {
        return passenger_id;
    }

    public void setPassenger_id(int passenger_id) {
        this.passenger_id=passenger_id;
    }

    public long getBooking_id() {
        return booking_id;
    }

    public void setBooking_id (long booking_Id) {
        this.booking_id=booking_Id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getCoach_type() {
        return coach_type;
    }

    public void setCoach_type(String coach_type) {
        this.coach_type = coach_type;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", coach_type='" + coach_type + '\'' +
                '}';
    }
}

