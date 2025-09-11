package com.pmh.disosang.map.store.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StoreBatchService {

    private final StoreRepository storeRepository;
    private static final String API_URL = "https://search.konacard.co.kr/api/v1/payable-merchants";

    @Transactional
    public void saveStoresFromCsv() throws IOException, CsvValidationException {
        String csvFilePath = "C:/Users/qkral/Desktop/2025_CHUNGNAM_TEAM_6_BE/천안 중앙시장.csv";
        try (CSVReader csvReader = new CSVReader(new FileReader(csvFilePath))) {
            String[] header = csvReader.readNext(); // 헤더 건너뜀

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 11) {
                    System.out.println("잘못된 줄 건너뜀: " + Arrays.toString(line));
                    continue;
                }

                Store store = Store.builder()
                        .placeName(line[2])                              // 상호
                        .phone(line[7].isBlank() ? null : line[7])      // 전화번호
                        .addressName(line[6])                            // 지번 주소
                        .roadAddressName(line[5])                        // 도로명 주소
                        .category(line[4])                               // 업종
                        .x(parseDouble(line[9]))                        // 경도
                        .y(parseDouble(line[8]))                         // 위도
                        .storeType("tm")                                 // 착한가격업소 구분자
                        .build();

                storeRepository.save(store);
            }
        }
    }


    @Transactional
    public void fetchAndSaveStores() {

        // 1. API 요청 바디 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("ptSignature", "vgQwdtNbFnHJyQ2SFTZuTyjueS6KMPlrmvGMfEyXV1MniOCSGqzGUiX4PcMly9sqyK3aqao/Zx8yjv28zU1Ia4CsjkvLEQyHEeIKquF9tr8wxIs5WpTiL+OM93J/fcmxIlyaC2FY1+TjyCDtp9K/nktJaYqTBEmnlFSvQpfGHTlOIRccmDjYXke9ABFEW6uJ");
        payload.put("id", "34");
        payload.put("bizType", "");
        payload.put("merchantType", "KB");
        payload.put("pageNum", "1");
        payload.put("pageSize", "25662");  // 테스트용으로 10개, 실제로는 전체 페이징 필요
        payload.put("affiliateName", "천안사랑카드");
        payload.put("searchKey", "");

        // 2. HTTP 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.set("Origin", "https://search.konacard.co.kr");
        headers.set("Referer", "https://search.konacard.co.kr/payable-merchants/cheonan");
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("X-Requested-With", "XMLHttpRequest");

        // 3. 요청
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, Map.class);

        // 4. 응답 처리
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("data")) {
            throw new RuntimeException("API 응답에 'data' 없음");
        }

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> merchants = (List<Map<String, Object>>) data.get("merchants");

        List<Store> stores = new ArrayList<>();
        for (Map<String, Object> merchant : merchants) {
            Store store = mapToStoreFromApi(merchant);
            stores.add(store);
        }

        // 5. 저장
        storeRepository.saveAll(stores);
    }




    // JSON -> Store 변환
    private Store mapToStoreFromApi(Map<String, Object> merchant) {
        return Store.builder()
                .placeName((String) merchant.get("simpleNm"))
                .category((String) merchant.get("bizTypeNm"))
                .addressName((String) merchant.get("addr"))
                .roadAddressName((String) merchant.get("addr"))  // 실제 도로명주소가 없으므로 addr로 대체
                .phone((String) merchant.get("telephone"))
                .x(parseDouble(merchant.get("longitude")))
                .y(parseDouble(merchant.get("latitude")))
                .build();
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }



}

