package com.example.hersafe.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;

        @SerializedName("legs")
        public List<Leg> legs;
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }

    public static class Leg {
        @SerializedName("distance")
        public Distance distance;

        @SerializedName("duration")
        public Duration duration;
    }

    public static class Distance {
        @SerializedName("value")
        public int value; // Distance in meters
        @SerializedName("text")
        public String text;
    }

    public static class Duration {
        @SerializedName("value")
        public int value; // Duration in seconds
        @SerializedName("text")
        public String text;
    }
}
