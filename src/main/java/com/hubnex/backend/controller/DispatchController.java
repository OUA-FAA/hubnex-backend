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
@RequestMapping({"/api/dispatch", "/api/bon-dispatch"})
@RequiredArgsConstructor
public class DispatchController {

    private final ConveyorExchangeService conveyorExchangeService;

    @Operation(summary = "List dispatch recovery operations")
    @GetMapping("/recoveries")
    public List<ReceptionRecoveryResponseDto> getRecoveries() {
        return conveyorExchangeService.getRecoveries(TypeFlux.DISPATCH);
    }

    @Operation(summary = "List dispatch docket records by recovery")
    @GetMapping("/recoveries/{recoveryId}/records")
    public List<DocketRecordResponseDto> getRecoveryRecords(@PathVariable String recoveryId) {
        return conveyorExchangeService.getRecoveryRecords(recoveryId, TypeFlux.DISPATCH);
    }
}
