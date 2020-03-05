package ca.carleton.gcrc.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Geometry utility class.
 */
public class GeometryUtils {
    private static final Pattern GEOM_REGEX = Pattern.compile("^(MULTIPOINT|POINT)\\(?\\(([-+]*\\d+\\.\\d+|[-+]*\\d+) ([-+]*\\d+\\.\\d+|[-+]*\\d+)\\)\\)?$");
    private static final Matcher MATCHER = GEOM_REGEX.matcher("");

    /**
     * Pulls out the latitude and longitude values from a simplified nunaliit_geom "wkt" field. Possible formats stored
     * as (lon, lat) are:
     * - "MULTIPOINT((-122.084 37.421998))"
     * - "POINT(-75.7 45.4)"
     *
     * @param nunaliitGeomWkt The value stored in the "wkt" field a a nunaliit_geom.
     * @return Map of strings storing coordinates using keys "lon" and "lat".
     */
    public static Map<String, String> extractLatLon(String nunaliitGeomWkt) {
        Map<String, String> coords = null;
        String lon = "";
        String lat = "";

        if (nunaliitGeomWkt != null && !nunaliitGeomWkt.isEmpty()) {
            MATCHER.reset(nunaliitGeomWkt);
            if (MATCHER.matches()) {
                lon = MATCHER.group(2);
                lat = MATCHER.group(3);

                coords = new HashMap<>(2);
                coords.put("lon", lon);
                coords.put("lat", lat);
            }
        }

        return coords;
    }
}
