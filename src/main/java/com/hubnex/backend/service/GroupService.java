package com.hubnex.backend.service;

import com.hubnex.backend.dto.request.GroupRequestDto;
import com.hubnex.backend.dto.response.GroupResponseDto;
import com.hubnex.backend.model.Group;
import com.hubnex.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public List<GroupResponseDto> getAll() {
        return groupRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public GroupResponseDto getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    public GroupResponseDto create(GroupRequestDto dto) {
        Group group = Group.builder()
                .name(dto.getName())
                .build();

        return mapToResponse(groupRepository.save(group));
    }

    public GroupResponseDto update(Long id, GroupRequestDto dto) {
        Group group = getEntityById(id);
        group.setName(dto.getName());
        return mapToResponse(groupRepository.save(group));
    }

    public void delete(Long id) {
        groupRepository.deleteById(id);
    }

    private Group getEntityById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private GroupResponseDto mapToResponse(Group group) {
        return GroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .build();
    }
}
