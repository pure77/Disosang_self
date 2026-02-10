package com.pmh.disosang.map.store.controller;

import com.opencsv.exceptions.CsvValidationException;
import com.pmh.disosang.map.store.service.StoreBatchService;
import com.pmh.disosang.map.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Profile("dev")
@RequestMapping("/dev")
@Tag(name = "가게 정보 저장 API", description = "가게 정보를 저장할때쓰는 API 입니다 한번실행후 실행X")
public class StoreBatchController {
/*
    private final StoreService storeService;

    private final StoreBatchService storeBatchService;

    @Operation(summary = "가맹점 정보 DB 저장", description = "가맹점 정보를 DB에 저장하기위한 코드 실행X (중복실행시 DB 정보가 겹처서 저장됩니다)")
    public ResponseEntity<String> getAllSaveStores() {
        storeBatchService.fetchAndSaveStores();
        return ResponseEntity.ok("DB save finish");
    }

    @GetMapping("/csvdb")
    public ResponseEntity<String> CsvToDB() {
        try {
            storeBatchService.saveStoresFromCsv();
            return ResponseEntity.ok("CSV 데이터 저장 완료!");
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("CSV 저장 중 오류 발생: " + e.getMessage());
        }

    }*/

}
