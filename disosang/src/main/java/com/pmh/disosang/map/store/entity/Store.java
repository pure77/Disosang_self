package com.pmh.disosang.map.store.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(length = 100)
    private String category;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "road_address_name")
    private  String roadAddressName;
    @Column(length = 20)
    private String phone;

    @Column(name = "x")
    private Double x;  // 경도 (longitude)

    @Column(name = "y")
    private Double y;  // 위도 (latitude)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "store_type")
    @NotNull
    private  String storeType;



    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
