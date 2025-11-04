package com.evcs.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码哈希生成工具
 *
 * 使用说明：
 * 1. 运行此类生成 'password' 的BCrypt哈希
 * 2. 将生成的哈希更新到数据库初始化脚本中
 * 3. 更新所有相关文档，确保密码统一为 'password'
 */
public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password";

        System.out.println("=== EVCS 系统密码哈希生成工具 ===");
        System.out.println("明文密码: " + password);
        System.out.println();

        // 生成3个不同的哈希以便验证
        System.out.println("=== BCrypt Hashes for '" + password + "' ===");
        for (int i = 1; i <= 3; i++) {
            String hash = encoder.encode(password);
            System.out.println("Hash " + i + ": " + hash);
            System.out.println("Length: " + hash.length());
            System.out.println("Verify: " + encoder.matches(password, hash));
            System.out.println();
        }

        // 生成一个用于数据库的推荐哈希
        String recommendedHash = encoder.encode(password);
        System.out.println("=== 推荐使用 ===");
        System.out.println("数据库哈希: " + recommendedHash);
        System.out.println("验证结果: " + encoder.matches(password, recommendedHash));
    }
}
