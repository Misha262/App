package com.app.service;

import com.app.model.Group;
import com.app.repository.GroupRepository;
import com.app.service.MembershipService;

import java.sql.SQLException;
import java.util.List;

public class GroupService {

    private final GroupRepository groupRepo = new GroupRepository();
    private final MembershipService membershipService = new MembershipService();

    /**
     * Список групп, в которых состоит пользователь.
     */
    public List<Group> getGroupsForUser(int userId) throws SQLException {
        return groupRepo.findByUserId(userId);
    }

    /**
     * Создать новую группу (always allowed if user is authenticated).
     */
    public Group createGroup(int ownerId, String name, String description) throws SQLException {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("Owner (userId) is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
        if (description == null) description = "";

        return groupRepo.createGroup(ownerId, name, description);
    }

    /**
     * Получить группу (но контроллер проверяет membership).
     */
    public Group getGroup(int groupId) throws SQLException {
        return groupRepo.findById(groupId);
    }

    /**
     * Обновить группу — проверка OWNER теперь в контроллере.
     */
    public void updateGroup(int groupId, String name, String description) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }
        if (description == null) description = "";

        groupRepo.updateGroup(groupId, name, description);
    }

    /**
     * Удалить группу — проверка OWNER также в контроллере.
     */
    public void deleteGroup(int groupId) throws SQLException {
        groupRepo.deleteGroup(groupId);
    }
}
