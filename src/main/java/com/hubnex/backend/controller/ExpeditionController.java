package com.hubnex.backend.controller;

import com.hubnex.backend.dto.response.DocketRecordResponseDto;
import com.hubnex.backend.dto.response.ReceptionRecoveryResponseDto;
import com.hubnex.backend.model.TypeFlux;
import com.hubnex.backend.service.ConveyorExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expeditions")
@RequiredArgsConstructor
public class ExpeditionController {

    private final ConveyorExchangeService conveyorExchangeService;

    @Operation(summary = "List conveyor completed docket records ready for expedition")
    @GetMapping("/conveyor-ready")
    public List<DocketRecordResponseDto> getConveyorReady() {
        return conveyorExchangeService.getConveyorReadyRecords(TypeFlux.EXPEDITION);
    }

    @Operation(summary = "List expedition recovery operations")
    @GetMapping("/recoveries")
    public List<ReceptionRecoveryResponseDto> getRecoveries() {
        return conveyorExchangeService.getRecoveries(TypeFlux.EXPEDITION);
    }

    @Operation(summary = "List expedition docket records by recovery")
    @GetMapping("/recoveries/{recoveryId}/records")
    public List<DocketRecordResponseDto> getRecoveryRecords(@PathVariable String recoveryId) {
        return conveyorExchangeService.getRecoveryRecords(recoveryId, TypeFlux.EXPEDITION);
    }
}
