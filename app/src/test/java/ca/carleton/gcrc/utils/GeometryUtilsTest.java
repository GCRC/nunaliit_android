package ca.carleton.gcrc.utils;

import junit.framework.TestCase;

import java.util.Map;

public class GeometryUtilsTest extends TestCase {
    public void testPoint() {
        String wkt = "POINT(-75.7 45.4)";

        Map<String, String> coords = GeometryUtils.extractLatLon(wkt);
        assertEquals("-75.7", coords.get("lon"));
        assertEquals("45.4", coords.get("lat"));
    }

    public void testMultipoint() {
        String wkt = "MULTIPOINT((-122.084 37.421998))";

        Map<String, String> coords = GeometryUtils.extractLatLon(wkt);
        assertEquals("-122.084", coords.get("lon"));
        assertEquals("37.421998", coords.get("lat"));
    }
}