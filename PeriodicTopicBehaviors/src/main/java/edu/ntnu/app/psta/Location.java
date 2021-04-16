package edu.ntnu.app.psta;

import java.util.Objects;

public class Location {

    double latitude;
    double longitude;
    String name;
    String city;
    String country;

    public Location(double latitude, double longitude, String name, String city, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.city = city;
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return name.equals(location.name) && city.equals(location.city) && country.equals(location.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, name, city, country);
    }
}
