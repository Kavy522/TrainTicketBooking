package trainapp.model;
public class Passenger {

    private String name;
    private int age;
    private String gender;
    private String seatNumber;
    private String berthPreference;
    private String coach;

    public Passenger() {}

    public Passenger(String name, int age, String gender, String seatNumber, String berthPreference, String coach) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber;
        this.berthPreference = berthPreference;
        this.coach = coach;
    }

    // Getters and Setters
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

    public String getBerthPreference() {
        return berthPreference;
    }

    public void setBerthPreference(String berthPreference) {
        this.berthPreference = berthPreference;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", berthPreference='" + berthPreference + '\'' +
                ", coach='" + coach + '\'' +
                '}';
    }
}

