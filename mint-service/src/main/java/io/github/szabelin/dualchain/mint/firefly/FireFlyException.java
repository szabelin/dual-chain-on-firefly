package io.github.szabelin.dualchain.mint.firefly;

public class FireFlyException extends RuntimeException {

    private final int status;
    private final String body;

    public FireFlyException(int status, String body, String message) {
        super(message);
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
