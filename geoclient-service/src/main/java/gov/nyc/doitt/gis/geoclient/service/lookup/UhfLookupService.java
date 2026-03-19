/*
 * Copyright 2013-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.nyc.doitt.gis.geoclient.service.lookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the UHF 42 zip-to-name lookup tables from CSV resources at startup
 * and provides UHF neighborhood name resolution for zip codes returned by
 * Geosupport.
 *
 * <p>Two files are required on the classpath:
 * <ul>
 *   <li>{@code lookup/uhf_zip_to_code.csv} — maps ZIP → UHFCODE
 *       (columns: ZIP, ZCTA, MODZCTA, UHFCODE)</li>
 *   <li>{@code lookup/uhf_names.csv} — maps UHFCODE → UHFNAME
 *       (columns: UHFCODE, UHFNAME, BOROUGH, ALTCHPUHF, CHSUHF)</li>
 * </ul>
 */
public class UhfLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UhfLookupService.class);
    private static final String ZIP_CSV_PATH = "/lookup/uhf_zip_to_code.csv";
    private static final String NAMES_CSV_PATH = "/lookup/uhf_names.csv";

    private final Map<String, String> uhfNameByZip;

    public UhfLookupService() {
        Map<String, String> nameByCode = loadNamesByCode();
        this.uhfNameByZip = buildZipToNameMap(nameByCode);
    }

    /**
     * Returns the UHF 42 neighborhood name for the given zip code, or an empty
     * string if the zip code is not found.
     *
     * @param zipCode the 5-digit zip code string (e.g. {@code "10001"})
     * @return the UHF name, or {@code ""} if not found
     */
    public String lookupName(String zipCode) {
        if (zipCode == null) {
            return "";
        }
        return uhfNameByZip.getOrDefault(zipCode.trim(), "");
    }

    private Map<String, String> loadNamesByCode() {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = UhfLookupService.class.getResourceAsStream(NAMES_CSV_PATH)) {
            if (is == null) {
                LOGGER.error("UHF names CSV not found at classpath:{}", NAMES_CSV_PATH);
                return Collections.emptyMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                // Line format: "101","Kingsbridge - Riverdale","Bronx",...
                String stripped = line.replace("\"", "");
                String[] parts = stripped.split(",", -1);
                if (parts.length >= 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
            LOGGER.info("Loaded {} UHF code-to-name entries from {}", map.size(), NAMES_CSV_PATH);
        }
        catch (IOException e) {
            LOGGER.error("Failed to load UHF names CSV from classpath:{}", NAMES_CSV_PATH, e);
            return Collections.emptyMap();
        }
        return map;
    }

    private Map<String, String> buildZipToNameMap(Map<String, String> nameByCode) {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = UhfLookupService.class.getResourceAsStream(ZIP_CSV_PATH)) {
            if (is == null) {
                LOGGER.error("UHF zip CSV not found at classpath:{}", ZIP_CSV_PATH);
                return Collections.emptyMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                // Line format: "10001",10001,"10001",306
                String stripped = line.replace("\"", "");
                String[] parts = stripped.split(",", -1);
                if (parts.length >= 4) {
                    String zip = parts[0].trim();
                    String uhfCode = parts[3].trim();
                    String name = nameByCode.get(uhfCode);
                    if (name != null) {
                        map.put(zip, name);
                    }
                }
            }
            LOGGER.info("Loaded {} zip-to-UHF-name entries from {}", map.size(), ZIP_CSV_PATH);
        }
        catch (IOException e) {
            LOGGER.error("Failed to load UHF zip CSV from classpath:{}", ZIP_CSV_PATH, e);
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }
}
