package edu.ntnu.app;

import java.util.Objects;

public class Location {

    float latitude;
    float longitude;
    String name;
    String city;
    String country;

    public Location(float latitude, float longitude, String name, String city, String country) {
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
        return city.equals(location.city) && country.equals(location.country);
        //return country.equals(location.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(/*latitude, longitude, name, city, */country);
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return city + ", " + country;
        //return country;
    }
}
