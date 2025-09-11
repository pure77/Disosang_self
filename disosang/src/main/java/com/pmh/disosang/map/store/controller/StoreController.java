package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.map.store.dto.request.StoreSearchRequest;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Tag(name = "지도 검색 API", description = "지도 중심의 가맹점 검색")
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/map/search")
    @Operation(summary = "지도 기반 가맹점 검색", description = "지도 영역 내 키워드로 가맹점을 검색하여 중심 좌표 기준으로 정렬된 결과를 반환합니다. 착한가격업소만 필터링할시 키워드 에 cheap 을 입력,시장 필터링시 tm , 지도내 가게가 검색이 안될경우 키워드로만 검색해서 중심좌표에 가까운 가게들부터 반환함" )
    public ResponseEntity<List<StoreResponse>> searchStoresInMap(@Valid @ModelAttribute StoreSearchRequest request)  {



        List<StoreResponse> storeResponses = storeService.searchStoresInMap(
                request.getKeyword().trim(),
                request.getCenterY(),
                request.getCenterX(),
                request.getMinY(),
                request.getMaxY(),
                request.getMinX(),
                request.getMaxX()
        );
        return ResponseEntity.ok(storeResponses);
    }




}
