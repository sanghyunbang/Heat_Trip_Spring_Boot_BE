package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.repository.PlaceTraitRepository;
import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "한글 라벨 → CAT3" 사전.
 * - DB(place_traits) 우선 로드
 * - CSV로 보강 (classpath: curation/cat3_traits.csv)
 * - 메모리 캐시로 빠른 해석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cat3DictionaryService {

    private final PlaceTraitRepository traitRepo;

    private final Map<String, Set<String>> labelToCat3 = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> normLabelToCat3 = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        int fromDb = loadFromDb();
        int fromCsv = loadFromCsvIfExists("curation/cat3_traits.csv");
        log.info("Cat3Dictionary loaded: {} pairs(DB) + {}(CSV)", fromDb, fromCsv);
    }

    private int loadFromDb() {
        int c = 0;
        for (Object[] row : traitRepo.findAllDistinctPairs()) {
            String label = String.valueOf(row[0]).trim();
            String cat3 = String.valueOf(row[1]).trim();
            if (label.isEmpty() || cat3.isEmpty()) continue;
            put(label, cat3);
            c++;
        }
        return c;
    }

    private int loadFromCsvIfExists(String classpathCsv) {
        var res = new ClassPathResource(classpathCsv);
        if (!res.exists()) return 0;
        int added = 0;
        try (var reader = new CSVReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) return 0;
            int cat3Idx = -1, nameIdx = -1;
            for (int i = 0; i < header.length; i++) {
                String h = header[i].trim();
                if (h.equalsIgnoreCase("CAT3")) cat3Idx = i;
                if (h.equalsIgnoreCase("이름")) nameIdx = i;
            }
            if (cat3Idx < 0 || nameIdx < 0) return 0;

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length <= Math.max(cat3Idx, nameIdx)) continue;
                String cat3 = row[cat3Idx].trim();
                String name = row[nameIdx].trim();
                if (cat3.isEmpty() || name.isEmpty()) continue;

                boolean exists = labelToCat3.getOrDefault(name, Set.of()).contains(cat3);
                if (!exists) {
                    put(name, cat3);
                    added++;
                }
            }
        } catch (Exception e) {
            log.warn("CSV load failed: {}", e.toString());
        }
        return added;
    }

    private void put(String label, String cat3) {
        labelToCat3.computeIfAbsent(label, k -> new HashSet<>()).add(cat3);
        normLabelToCat3.computeIfAbsent(normalize(label), k -> new HashSet<>()).add(cat3);
    }

    private static String normalize(String s) { return s.replace(" ", ""); }

    public Set<String> resolveCat3Codes(Collection<String> labels) {
        if (labels == null || labels.isEmpty()) return Set.of();
        Set<String> out = new HashSet<>();
        for (String raw : labels) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            var exact = labelToCat3.get(trimmed);
            if (exact != null) out.addAll(exact);
            var norm = normLabelToCat3.get(normalize(trimmed));
            if (norm != null) out.addAll(norm);
        }
        return out;
    }
}
