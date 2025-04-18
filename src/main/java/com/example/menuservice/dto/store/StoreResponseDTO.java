package com.example.menuservice.dto.store;

import com.example.menuservice.domain.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
public class StoreResponseDTO {
    private Long    uid;
    private String  storeName;
    private String  storeAddress;
    private String  storePostcode;
    private Double  storeLatitude;
    private Double  storeLongitude;
    private String  storeStatus;
    private Instant storeCreatedDate;
}