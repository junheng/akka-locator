package io.github.junheng.akka.locator;

public class ServiceLocation {
    public String type;
    public String url;
    public Double load;
    public String status;

    public ServiceLocation() {
        this.type = null;
        this.url = null;
        this.load = -0.0;
        this.status = null;
    }

    public ServiceLocation(String type, String url, Double load, String status) {
        this.type = type;
        this.url = url;
        this.load = load;
        this.status = status;
    }
}
