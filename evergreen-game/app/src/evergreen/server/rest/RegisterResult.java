package evergreen.server.rest;

public class RegisterResult {

    private final String name;
    public boolean success;

    public RegisterResult(String name, boolean success) {
        this.name = name;
        this.success = success;
    }

    public String getname() {
        return name;
    }
}