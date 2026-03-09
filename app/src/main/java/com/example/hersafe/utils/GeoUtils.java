package com.example.hersafe.utils;

import java.util.HashMap;
import java.util.Map;

public class GeoUtils {

    private static final Map<String, double[]> AREA_CENTERS = new HashMap<>();

    static {
        // Damascus & Rif Dimashq
        AREA_CENTERS.put("دمشق (المركز)", new double[]{33.5138, 36.2765});
        AREA_CENTERS.put("ريف دمشق - داريا", new double[]{33.4583, 36.2393});
        AREA_CENTERS.put("ريف دمشق - دوما", new double[]{33.5719, 36.4025});
        AREA_CENTERS.put("ريف دمشق - الزبداني", new double[]{33.7258, 36.0964});
        AREA_CENTERS.put("ريف دمشق - السيدة زينب", new double[]{33.4461, 36.3353});

        // Aleppo
        AREA_CENTERS.put("حلب (المدينة)", new double[]{36.2021, 37.1343});
        AREA_CENTERS.put("حلب - عفرين", new double[]{36.5097, 36.8689});
        AREA_CENTERS.put("حلب - الباب", new double[]{36.3697, 37.5147});
        AREA_CENTERS.put("حلب - منبج", new double[]{36.5281, 37.9549});

        // Hama
        AREA_CENTERS.put("حماه (المدينة)", new double[]{35.1318, 36.7578});
        AREA_CENTERS.put("حماه - مصياف", new double[]{35.0653, 36.3421});
        AREA_CENTERS.put("حماه - سلمية", new double[]{35.0113, 37.0532});
        AREA_CENTERS.put("حماه - السقيلبية", new double[]{35.3853, 36.4497});
        AREA_CENTERS.put("حماه - محردة", new double[]{35.2514, 36.5744});
        AREA_CENTERS.put("حماه - حلفايا", new double[]{35.2650, 36.6083});

        // Homs
        AREA_CENTERS.put("حمص (المدينة)", new double[]{34.7324, 36.7137});
        AREA_CENTERS.put("حمص - تدمر", new double[]{34.5601, 38.2672});
        AREA_CENTERS.put("حمص - القصير", new double[]{34.5095, 36.5796});
        AREA_CENTERS.put("حمص - تلكلخ", new double[]{34.6644, 36.2575});

        // Latakia
        AREA_CENTERS.put("اللاذقية (المدينة)", new double[]{35.5317, 35.7901});
        AREA_CENTERS.put("اللاذقية - جبلة", new double[]{35.3615, 35.9256});
        AREA_CENTERS.put("اللاذقية - القرداحة", new double[]{35.4578, 36.0617});
        AREA_CENTERS.put("اللاذقية - الحفة", new double[]{35.6044, 36.0528});

        // Tartus
        AREA_CENTERS.put("طرطوس (المدينة)", new double[]{34.8890, 35.8866});
        AREA_CENTERS.put("طرطوس - بانياس", new double[]{35.1834, 35.9458});
        AREA_CENTERS.put("طرطوس - صافيتا", new double[]{34.8219, 36.1172});
        AREA_CENTERS.put("طرطوس - الدريكيش", new double[]{34.8969, 36.1361});

        // Idlib
        AREA_CENTERS.put("إدلب (المدينة)", new double[]{35.9306, 36.6339});
        AREA_CENTERS.put("إدلب - جسر الشغور", new double[]{35.8156, 36.3217});
        AREA_CENTERS.put("إدلب - معرة النعمان", new double[]{35.6447, 36.6711});

        // Daraa
        AREA_CENTERS.put("درعا (المدينة)", new double[]{32.6184, 36.1014});
        AREA_CENTERS.put("درعا - الصنمين", new double[]{33.0678, 36.1786});
        AREA_CENTERS.put("درعا - إزرع", new double[]{32.8683, 36.2508});

        // Suwayda
        AREA_CENTERS.put("السويداء (المدينة)", new double[]{32.7090, 36.5658});
        AREA_CENTERS.put("السويداء - شهبا", new double[]{32.8550, 36.6264});
        AREA_CENTERS.put("السويداء - صلخد", new double[]{32.4939, 36.7083});

        // Deir ez-Zor
        AREA_CENTERS.put("دير الزور (المدينة)", new double[]{35.3364, 40.1458});
        AREA_CENTERS.put("دير الزور - البوكمال", new double[]{34.4533, 40.9114});
        AREA_CENTERS.put("دير الزور - الميادين", new double[]{35.0200, 40.4539});

        // Hasakah
        AREA_CENTERS.put("الحسكة (المدينة)", new double[]{36.5074, 40.7380});
        AREA_CENTERS.put("الحسكة - القامشلي", new double[]{37.0503, 41.2291});

        // Raqqa
        AREA_CENTERS.put("الرقة (المدينة)", new double[]{35.9525, 39.0089});
        AREA_CENTERS.put("الرقة - الثورة (الطبقة)", new double[]{35.8392, 38.5447});

        // Quneitra
        AREA_CENTERS.put("القنيطرة", new double[]{33.1264, 35.8239});
    }

    public static String getClosestArea(double lat, double lng) {
        double minDistance = Double.MAX_VALUE;
        String closest = "غير محدد";

        for (Map.Entry<String, double[]> entry : AREA_CENTERS.entrySet()) {
            double[] pos = entry.getValue();
            // Simple Euclidean distance for performance (good enough for this scale)
            double dist = Math.pow(lat - pos[0], 2) + Math.pow(lng - pos[1], 2);
            if (dist < minDistance) {
                minDistance = dist;
                closest = entry.getKey();
            }
        }
        return closest;
    }
    
    public static String getGovernorateFromArea(String area) {
        if (area.contains("دمشق")) return "دمشق وريفها";
        if (area.contains("حلب")) return "حلب";
        if (area.contains("حماه")) return "حماه";
        if (area.contains("حمص")) return "حمص";
        if (area.contains("اللاذقية")) return "اللاذقية";
        if (area.contains("طرطوس")) return "طرطوس";
        if (area.contains("إدلب")) return "إدلب";
        if (area.contains("درعا")) return "درعا";
        if (area.contains("السويداء")) return "السويداء";
        if (area.contains("دير الزور")) return "دير الزور";
        if (area.contains("الحسكة")) return "الحسكة";
        if (area.contains("الرقة")) return "الرقة";
        if (area.contains("القنيطرة")) return "القنيطرة";
        return "غير محدد";
    }
}
