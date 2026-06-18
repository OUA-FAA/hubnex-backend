package com.hubnex.backend.controller;

import com.hubnex.backend.dto.request.GroupRequestDto;
import com.hubnex.backend.dto.response.GroupResponseDto;
import com.hubnex.backend.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<GroupResponseDto> getAll() {
        return groupService.getAll();
    }

    @GetMapping("/{id}")
    public GroupResponseDto getById(@PathVariable Long id) {
        return groupService.getById(id);
    }

    @PostMapping
    public GroupResponseDto create(@Valid @RequestBody GroupRequestDto dto) {
        return groupService.create(dto);
    }

    @PutMapping("/{id}")
    public GroupResponseDto update(@PathVariable Long id, @Valid @RequestBody GroupRequestDto dto) {
        return groupService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public GroupResponseDto patch(@PathVariable Long id, @RequestBody GroupRequestDto dto) {
        return groupService.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        groupService.delete(id);
    }
}
