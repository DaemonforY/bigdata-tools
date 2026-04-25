package com.example.demo.projects.realrank;

import lombok.Data;

import java.io.Serializable;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 08:08:45
 * @Version 1.0
 */
@Data
public class UserScoreDTO implements Serializable {
    private String userId;
    private long score;

    public UserScoreDTO() {}

    public UserScoreDTO(String userId, long score) {
        this.userId = userId;
        this.score = score;
    }

    // 显式 getter/setter，避免 Lombok 注解处理器在 mvn spring-boot:run 时未启用
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }
}
