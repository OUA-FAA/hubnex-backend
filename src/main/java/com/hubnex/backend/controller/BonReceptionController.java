package com.hubnex.backend.controller;

import com.hubnex.backend.dto.imports.DocketRecordUpdateResultDto;
import com.hubnex.backend.dto.response.DocketRecordResponseDto;
import com.hubnex.backend.dto.response.ReceptionRecoveryResponseDto;
import com.hubnex.backend.service.ConveyorExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bon-receptions")
@RequiredArgsConstructor
public class BonReceptionController {

    private final ConveyorExchangeService conveyorExchangeService;

    @Operation(summary = "List conveyor completed docket records ready for reception")
    @GetMapping("/conveyor-ready")
    public List<DocketRecordResponseDto> getConveyorReady() {
        return conveyorExchangeService.getConveyorReadyRecords();
    }

    @Operation(summary = "List conveyor recovery operations")
    @GetMapping("/recoveries")
    public List<ReceptionRecoveryResponseDto> getRecoveries() {
        return conveyorExchangeService.getRecoveries();
    }

    @Operation(summary = "List docket records by conveyor recovery")
    @GetMapping("/recoveries/{receptionRecoveryId}/records")
    public List<DocketRecordResponseDto> getRecoveryRecords(@PathVariable String receptionRecoveryId) {
        return conveyorExchangeService.getRecoveryRecords(receptionRecoveryId);
    }

    @Operation(summary = "List incomplete docket records")
    @GetMapping("/incomplete-docket-records")
    public List<DocketRecordResponseDto> getIncompleteDocketRecords() {
        return conveyorExchangeService.getIncompleteRecords();
    }

    @Operation(summary = "Export incomplete docket records to Excel")
    @GetMapping("/incomplete-docket-records/export")
    public ResponseEntity<byte[]> exportIncompleteDocketRecords() {
        byte[] content = conveyorExchangeService.exportIncompleteRecords();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomplete-docket-records.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "Import corrected incomplete docket records")
    @PostMapping(value = "/incomplete-docket-records/import-corrected", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocketRecordUpdateResultDto importCorrectedIncompleteDocketRecords(@RequestParam("file") MultipartFile file) {
        return conveyorExchangeService.importCorrectedIncompleteRecords(file);
    }
}
