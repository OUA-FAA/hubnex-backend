package com.hubnex.backend.exception;

import com.hubnex.backend.dto.imports.DocketRecordImportResultDto;
import lombok.Getter;

@Getter
public class DocketRecordImportException extends RuntimeException {

    private final DocketRecordImportResultDto result;

    public DocketRecordImportException(String message, DocketRecordImportResultDto result) {
        super(message);
        this.result = result;
    }

    public DocketRecordImportException(String message, Throwable cause, DocketRecordImportResultDto result) {
        super(message, cause);
        this.result = result;
    }
}
