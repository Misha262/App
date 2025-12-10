package com.app.model;

/**
 * Членство пользователя в группе.
 * userName и userEmail используются для удобного вывода списка участников.
 */
public class Membership {

    private int membershipId;
    private int userId;
    private int groupId;
    private String role;      // OWNER, ADMIN, MEMBER
    private String joinedAt;

    // дополнительные поля из USERS
    private String userName;
    private String userEmail;

    public Membership(int membershipId,
                      int userId,
                      int groupId,
                      String role,
                      String joinedAt,
                      String userName,
                      String userEmail) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.groupId = groupId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    // Упрощённый конструктор, если имя/email не нужны
    public Membership(int membershipId,
                      int userId,
                      int groupId,
                      String role,
                      String joinedAt) {
        this(membershipId, userId, groupId, role, joinedAt, null, null);
    }

    public int getMembershipId() {
        return membershipId;
    }

    public int getUserId() {
        return userId;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getRole() {
        return role;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }
}
