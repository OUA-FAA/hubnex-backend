package com.hubnex.backend.controller;

import com.hubnex.backend.dto.conveyor.ConveyorExchangeResultDto;
import com.hubnex.backend.dto.imports.DocketRecordImportFailedRowDto;
import com.hubnex.backend.dto.imports.DocketRecordImportResultDto;
import com.hubnex.backend.dto.request.DocketRecordRequestDto;
import com.hubnex.backend.dto.request.SendToConveyorRequestDto;
import com.hubnex.backend.dto.response.DocketRecordManifestResponseDto;
import com.hubnex.backend.dto.response.DocketRecordResponseDto;
import com.hubnex.backend.dto.response.ReceptionRecoveryResponseDto;
import com.hubnex.backend.model.TypeFlux;
import com.hubnex.backend.service.ConveyorExchangeService;
import com.hubnex.backend.service.DocketRecordImportService;
import com.hubnex.backend.service.DocketRecordService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/docket-records")
@RequiredArgsConstructor
@Slf4j
public class DocketRecordController {

    private final DocketRecordService docketRecordService;
    private final DocketRecordImportService docketRecordImportService;
    private final ConveyorExchangeService conveyorExchangeService;

    @Operation(summary = "List docket records")
    @GetMapping
    public List<DocketRecordResponseDto> getAll() {
        return docketRecordService.getAll();
    }

    @Operation(summary = "List imported docket record manifests")
    @GetMapping("/manifests")
    public List<DocketRecordManifestResponseDto> getManifests() {
        return docketRecordService.getManifests();
    }

    @Operation(summary = "List docket records by manifest import batch")
    @GetMapping("/manifests/{importBatchId}/records")
    public List<DocketRecordResponseDto> getManifestRecords(@PathVariable String importBatchId) {
        return docketRecordService.getByImportBatchId(importBatchId);
    }

    @Operation(summary = "Delete an imported manifest batch")
    @DeleteMapping("/manifests/{importBatchId}")
    public ResponseEntity<Void> deleteManifest(@PathVariable String importBatchId) {
        docketRecordService.deleteManifest(importBatchId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List conveyor recoveries by flow type")
    @GetMapping("/recoveries")
    public List<ReceptionRecoveryResponseDto> getRecoveries(@RequestParam TypeFlux typeFlux) {
        return conveyorExchangeService.getRecoveries(typeFlux);
    }

    @Operation(summary = "List docket records by recovery and flow type")
    @GetMapping("/recoveries/{recoveryId}/records")
    public List<DocketRecordResponseDto> getRecoveryRecords(@PathVariable String recoveryId,
                                                            @RequestParam TypeFlux typeFlux) {
        return conveyorExchangeService.getRecoveryRecords(recoveryId, typeFlux);
    }

    @Operation(summary = "Delete a conveyor recovery by flow type")
    @DeleteMapping("/recoveries/{recoveryId}")
    public ResponseEntity<Void> deleteRecovery(@PathVariable String recoveryId,
                                               @RequestParam TypeFlux typeFlux) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Delete recovery endpoint reached recoveryId={} typeFlux={} user={} authorities={}",
                recoveryId,
                typeFlux,
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : Collections.emptyList());
        conveyorExchangeService.deleteRecovery(recoveryId, typeFlux);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get docket record by id")
    @GetMapping("/{id}")
    public DocketRecordResponseDto getById(@PathVariable Long id) {
        return docketRecordService.getById(id);
    }

    @Operation(summary = "Get docket record by connote")
    @GetMapping("/by-connote/{connote}")
    public DocketRecordResponseDto getByConnote(@PathVariable String connote) {
        return docketRecordService.getByConnote(connote);
    }

    @Operation(summary = "Create docket record")
    @PostMapping
    public DocketRecordResponseDto create(@Valid @RequestBody DocketRecordRequestDto dto) {
        return docketRecordService.create(dto);
    }

    @Operation(summary = "Update docket record")
    @PutMapping("/{id}")
    public DocketRecordResponseDto update(@PathVariable Long id, @Valid @RequestBody DocketRecordRequestDto dto) {
        return docketRecordService.update(id, dto);
    }

    @Operation(summary = "Patch docket record")
    @PatchMapping("/{id}")
    public DocketRecordResponseDto patch(@PathVariable Long id, @RequestBody DocketRecordRequestDto dto) {
        return docketRecordService.patch(id, dto);
    }

    @Operation(summary = "Delete docket record")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        docketRecordService.delete(id);
    }

    @Operation(summary = "Import docket records from Excel",
            description = "Required headers: Batch Number, LTA, Connote, Hub, Line, weight (Kg), Width, Length, Height, volume (L*L*H/5000), Date réception")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocketRecordImportResultDto importExcel(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(name = "typeFlux", required = false) String typeFlux) {
        return docketRecordImportService.importFromExcel(file, typeFlux);
    }

    @Operation(summary = "Preview docket record Excel import",
            description = "Validates the Manifeste Excel file and returns import statistics without saving any record")
    @PostMapping(value = {"/import-excel/preview", "/import-excel/preview/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocketRecordImportResultDto previewExcel(@RequestParam("file") MultipartFile file,
                                                     HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Preview endpoint reached");
        log.info("Docket record import preview path={} method={} user={} authorities={}",
                request.getRequestURI(),
                request.getMethod(),
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : Collections.emptyList());
        return docketRecordImportService.previewFromExcel(file);
    }

    @Operation(summary = "Export failed docket record import rows")
    @PostMapping("/import-excel/failed-rows/export")
    public ResponseEntity<byte[]> exportFailedRows(@RequestBody List<DocketRecordImportFailedRowDto> failedRows,
                                                   HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Failed rows export request path={} method={} user={} authorities={}",
                request.getRequestURI(),
                request.getMethod(),
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : Collections.emptyList());

        byte[] content = docketRecordImportService.exportFailedRows(failedRows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=docket-record-failed-rows.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "Send docket records to external conveyor")
    @PostMapping("/send-to-conveyor")
    public ConveyorExchangeResultDto sendToConveyor(
            @RequestBody(required = false) SendToConveyorRequestDto dto,
            @RequestParam(name = "typeFlux", required = false) String typeFlux,
            @RequestParam(name = "importBatchId", required = false) String importBatchId) {
        if (dto != null) {
            return conveyorExchangeService.sendToConveyor(dto.getTypeFlux(), dto.getImportBatchIds());
        }
        return conveyorExchangeService.sendToConveyor(typeFlux, importBatchId);
    }

    @Operation(summary = "Fetch external conveyor response and update docket records")
    @GetMapping("/conveyor-response")
    public ConveyorExchangeResultDto fetchConveyorResponse(
            @RequestParam(name = "typeFlux", required = false) String typeFlux,
            @RequestParam(name = "importBatchIds", required = false) java.util.Set<String> importBatchIds) {
        return conveyorExchangeService.fetchAndApplyConveyorResponse(typeFlux, importBatchIds);
    }
}
