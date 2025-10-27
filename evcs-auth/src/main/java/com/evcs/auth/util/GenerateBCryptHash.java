package com.evcs.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        
        // 生成3个不同的哈希以便验证
        System.out.println("=== BCrypt Hashes for 'admin123' ===");
        for (int i = 1; i <= 3; i++) {
            String hash = encoder.encode(password);
            System.out.println("Hash " + i + ": " + hash);
            System.out.println("Length: " + hash.length());
            System.out.println("Verify: " + encoder.matches(password, hash));
            System.out.println();
        }
    }
}
