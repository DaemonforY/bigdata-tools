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

    public UserScoreDTO(String userId, long score) {
        this.userId = userId;
        this.score = score;
    }
}
