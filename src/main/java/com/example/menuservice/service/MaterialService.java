package com.example.menuservice.service;

import com.example.menuservice.domain.Material;
import com.example.menuservice.dto.MaterialResponseDTO;
import com.example.menuservice.event.IngredientEventDTO;
import com.example.menuservice.exception.MaterialAlreadyExistsException;
import com.example.menuservice.exception.MaterialNotFoundException;
import com.example.menuservice.repository.MaterialRepository;
import com.example.menuservice.sqs.SqsService;
import com.example.menuservice.status.MaterialStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final FileUploadService fileUploadService;
    private final SqsService sqsService;
    private final ObjectMapper objectMapper;

    // 재료 목록 조회
    public List<MaterialResponseDTO> viewMaterialList() {
        return materialRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 재료 이름으로 재료 조회
    public MaterialResponseDTO viewMaterial(String materialName) {
        Material material = materialRepository.findByMaterialName(materialName)
                .orElseThrow(() -> new MaterialNotFoundException(materialName));
        return toResponseDTO(material);
    }

    // 재료 추가
    @Transactional
    public MaterialResponseDTO addMaterial(String materialRequestJson, MultipartFile file) throws IOException {
        String fileUrl = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(materialRequestJson);

            String materialName = jsonNode.get("materialName").asText();
            Double calorie = jsonNode.get("calorie").asDouble();
            int price = jsonNode.get("price").asInt();
            String statusStr = jsonNode.get("status").asText();

            if (materialRepository.existsByMaterialName(materialName)) {
                throw new MaterialAlreadyExistsException(materialName);
            }

            if (file != null && !file.isEmpty()) {
                fileUrl = fileUploadService.uploadFile(file);
            }

            Material material = Material.builder()
                    .materialName(materialName)
                    .calorie(calorie)
                    .price(price)
                    .img(fileUrl)
                    .status(statusStr.toUpperCase())
                    .build();


            // SQS 메시지 전송 (추가 이벤트)
            IngredientEventDTO event = IngredientEventDTO.builder()
                    .type("material")
                    .name(material.getMaterialName())
                    .calorie(material.getCalorie())
                    .price(material.getPrice())
                    .status(material.getStatus())
                    .img(material.getImg())
                    .build();

            sqsService.sendMessageToSqs(objectMapper.writeValueAsString(event), "ingredient-add-menu-service");

            return toResponseDTO(material);

        } catch (Exception e) {
            if (fileUrl != null) {
                fileUploadService.deleteFile(fileUrl);
            }
            throw e;
        }
    }

    // 재료 수정
    @Transactional
    public MaterialResponseDTO editMaterialDetails(String materialName, String materialRequestJson, MultipartFile file) throws IOException {
        String fileUrl = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(materialRequestJson);

            String materialNameFromJson = jsonNode.get("materialName").asText();
            Double calorie = jsonNode.get("calorie").asDouble();
            int price = jsonNode.get("price").asInt();
            String statusStr = jsonNode.get("status").asText();

            Material material = materialRepository.findByMaterialName(materialName)
                    .orElseThrow(() -> new MaterialNotFoundException(materialName));

            fileUrl = material.getImg();

            if (file != null && !file.isEmpty()) {
                if (fileUrl != null) {
                    fileUploadService.deleteFile(fileUrl);
                }
                fileUrl = fileUploadService.uploadFile(file);
            }

            material.updateMaterial(
                    materialNameFromJson,
                    calorie,
                    price,
                    fileUrl,
                    statusStr.toUpperCase()
            );



            // SQS 메시지 전송 (수정 이벤트)
            IngredientEventDTO event = IngredientEventDTO.builder()
                    .type("material")
                    .id(material.getUid())
                    .name(material.getMaterialName())
                    .price(material.getPrice())
                    .calorie(material.getCalorie())
                    .status(material.getStatus())
                    .img(material.getImg())
                    .build();

            sqsService.sendMessageToSqs(objectMapper.writeValueAsString(event), "ingredient-update-menu-service");

            return toResponseDTO(material);

        } catch (Exception e) {
            if (fileUrl != null) {
                fileUploadService.deleteFile(fileUrl);
            }
            throw e;
        }
    }

    // 재료 삭제 (상태만 DELETED로 변경)
    @Transactional
    public void removeMaterial(String materialName) {
        Material material = materialRepository.findByMaterialName(materialName)
                .orElseThrow(() -> new MaterialNotFoundException(materialName));
        material.setStatus(MaterialStatus.DELETED.name());


        // SQS 메시지 전송 (삭제 이벤트)
        try {
            IngredientEventDTO event = IngredientEventDTO.builder()
                    .type("material")
                    .id(material.getUid())
                    .name(material.getMaterialName())
                    .price(material.getPrice())
                    .calorie(material.getCalorie())
                    .status(material.getStatus())
                    .img(material.getImg())
                    .build();

            sqsService.sendMessageToSqs(objectMapper.writeValueAsString(event), "ingredient-delete-menu-service");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to send delete message to SQS", e);
        }
    }

    // Material -> MaterialResponseDTO 변환 메서드
    private MaterialResponseDTO toResponseDTO(Material material) {
        return new MaterialResponseDTO(
                material.getUid(),
                material.getMaterialName(),
                material.getCalorie(),
                material.getPrice(),
                material.getImg(),
                material.getStatus(),
                material.getCreatedDate(),
                material.getVersion()
        );
    }
}
