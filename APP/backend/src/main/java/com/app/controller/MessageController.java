package com.app.controller;

import com.app.model.Message;
import com.app.repository.MessageRepository;
import com.app.security.RoleGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository repo = new MessageRepository();

    @GetMapping
    public ResponseEntity<?> listByGroup(@RequestParam Integer groupId,
                                         @RequestAttribute("userId") Integer userId,
                                         @RequestParam(name = "limit", required = false, defaultValue = "200") Integer limit) throws Exception {
        RoleGuard.requireMember(userId, groupId);
        List<Message> list = repo.findRecentByGroup(groupId, limit);
        return ResponseEntity.ok(list);
    }
}
