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
 * Loads the NTA 2020 code-to-name lookup table from a CSV resource at startup
 * and provides name resolution for NTA codes returned by Geosupport.
 *
 * <p>The CSV must be located at {@code classpath:lookup/nta2020.csv} and have
 * the format: {@code "NTA code","NTA name",} (one entry per line, header on
 * line 1).
 */
public class NtaLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NtaLookupService.class);
    private static final String RESOURCE_PATH = "/lookup/nta2020.csv";

    private final Map<String, String> ntaNameByCode;

    public NtaLookupService() {
        this.ntaNameByCode = loadCsv();
    }

    /**
     * Returns the NTA name for the given 2020 NTA code, or an empty string if
     * the code is not found.
     *
     * @param nta2020Code the 6-character NTA 2020 code (e.g. {@code "BK0104"})
     * @return the NTA name, or {@code ""} if not found
     */
    public String lookupName(String nta2020Code) {
        if (nta2020Code == null) {
            return "";
        }
        return ntaNameByCode.getOrDefault(nta2020Code.trim(), "");
    }

    private Map<String, String> loadCsv() {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = NtaLookupService.class.getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                LOGGER.error("NTA lookup CSV not found at classpath:{}", RESOURCE_PATH);
                return Collections.emptyMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // skip header
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // Line format: "BK0104","East Williamsburg",
                String stripped = line.replace("\"", "");
                String[] parts = stripped.split(",");
                if (parts.length >= 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
            LOGGER.info("Loaded {} NTA 2020 name entries from {}", map.size(), RESOURCE_PATH);
        }
        catch (IOException e) {
            LOGGER.error("Failed to load NTA lookup CSV from classpath:{}", RESOURCE_PATH, e);
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }
}
