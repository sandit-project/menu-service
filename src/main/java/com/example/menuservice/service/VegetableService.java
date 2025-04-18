package com.example.menuservice.service;

import com.example.menuservice.status.VegetableStatus;
import com.example.menuservice.domain.Vegetable;
import com.example.menuservice.dto.VegetableRequestDTO;
import com.example.menuservice.dto.VegetableResponseDTO;
import com.example.menuservice.exception.VegetableAlreadyExistsException;
import com.example.menuservice.exception.VegetableNotFoundException;
import com.example.menuservice.repository.VegetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VegetableService {
    private final VegetableRepository vegetableRepository;
    private final FileUploadService fileUploadService;

    // 채소 목록 조회
    public List<VegetableResponseDTO> viewVegetableList() {
        return vegetableRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 채소 이름으로 채소 조회
    public VegetableResponseDTO viewVegetable(String vegetableName) {
        Vegetable vegetable = vegetableRepository.findByVegetableName(vegetableName)
                .orElseThrow(() -> new VegetableNotFoundException(vegetableName));
        return toResponseDTO(vegetable);
    }

    // 채소 추가
    @Transactional
    public VegetableResponseDTO addVegetable(VegetableRequestDTO vegetableRequestDTO, MultipartFile file) throws IOException {
        String fileUrl = null;

        try {
            if (vegetableRepository.existsByVegetableName(vegetableRequestDTO.getVegetableName())) {
                throw new VegetableAlreadyExistsException(vegetableRequestDTO.getVegetableName());
            }

            VegetableStatus status = VegetableStatus.valueOf(vegetableRequestDTO.getStatus().toUpperCase());

            if (file != null && !file.isEmpty()) {
                fileUrl = fileUploadService.uploadFile(file);
            }

            Vegetable vegetable = Vegetable.builder()
                    .vegetableName(vegetableRequestDTO.getVegetableName())
                    .calorie(vegetableRequestDTO.getCalorie())
                    .price(vegetableRequestDTO.getPrice())
                    .status(status.name())
                    .img(fileUrl)
                    .build();

            return toResponseDTO(vegetableRepository.save(vegetable));
        } catch (Exception e) {
            if (fileUrl != null) {
                fileUploadService.deleteFile(fileUrl);
            }
            throw e;
        }
    }

    // 채소 삭제
    @Transactional
    public void removeVegetable(String vegetableName) {
        Vegetable vegetable = vegetableRepository.findByVegetableName(vegetableName)
                .orElseThrow(() -> new VegetableNotFoundException(vegetableName));
        vegetable.setStatus(VegetableStatus.DELETED.name());
        vegetableRepository.save(vegetable);
    }

    // 채소 수정
    @Transactional
    public VegetableResponseDTO editVegetableDetails(String vegetableName, VegetableRequestDTO vegetableRequestDTO, MultipartFile file) throws IOException {
        Vegetable vegetable = vegetableRepository.findByVegetableName(vegetableName)
                .orElseThrow(() -> new VegetableNotFoundException(vegetableName));

        String fileUrl = vegetable.getImg();
        try {
            if (file != null && !file.isEmpty()) {
                if (fileUrl != null) {
                    fileUploadService.deleteFile(fileUrl);
                }
                fileUrl = fileUploadService.uploadFile(file);
            }

            vegetable.updateVegetable(
                    vegetableRequestDTO.getVegetableName(),
                    vegetableRequestDTO.getCalorie(),
                    vegetableRequestDTO.getPrice(),
                    fileUrl,
                    vegetableRequestDTO.getStatus()
            );

            return toResponseDTO(vegetable);
        } catch (Exception e) {
            if (fileUrl != null) {
                fileUploadService.deleteFile(fileUrl);
            }
            throw e;
        }
    }

    // 채소 상태 업데이트
    @Transactional
    public void updateVegetableStatus(Long uid, String status) {
        Vegetable vegetable = vegetableRepository.findById(uid)
                .orElseThrow(() -> new VegetableNotFoundException("ID: " + uid));
        vegetable.setStatus(status);
    }

    // Vegetable -> VegetableResponseDTO 변환 메서드
    private VegetableResponseDTO toResponseDTO(Vegetable vegetable) {
        return new VegetableResponseDTO(
                vegetable.getUid(),
                vegetable.getVegetableName(),
                vegetable.getCalorie(),
                vegetable.getPrice(),
                vegetable.getImg(),
                vegetable.getStatus(),
                vegetable.getCreatedDate(),
                vegetable.getVersion()
        );
    }
}
