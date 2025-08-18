package trainapp.model;

public enum TrainClass {
    SL("Sleeper"),
    _3A("AC 3 Tier"),  // Note: Use _3A to avoid starting with number
    _2A("AC 2 Tier"),
    _1A("AC First Class");

    private final String displayName;

    TrainClass(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TrainClass fromString(String classCode) {
        switch (classCode) {
            case "SL": return SL;
            case "3A": return _3A;
            case "2A": return _2A;
            case "1A": return _1A;
            default: throw new IllegalArgumentException("Invalid class: " + classCode);
        }
    }
}
