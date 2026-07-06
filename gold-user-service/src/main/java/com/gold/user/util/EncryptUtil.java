package com.gold.user.util;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
/**
 * 加密解密工具类
 * | 项目            | 建议                                                       |
 * | ------------- | -------------------------------------------------------- |
 * | **AES 密钥**    | 生产环境放 `application.yml` 或环境变量，不要硬编码                      |
 * | **密钥长度**      | AES 密钥必须是 16/24/32 字节（我写的 `"GoldSim2024!@#ABC"` 是 16 字节） |
 * | **BCrypt 强度** | `BCrypt.gensalt(12)` 中的 12 是强度（4-31），越大越慢越安全，默认 10       |
 * | **盐值**        | BCrypt 自动生成随机盐，每次加密结果不同，这是正常的                            |
 */
public class EncryptUtil {

    // ==================== BCrypt 密码加密（不可逆）====================

    /**
     * BCrypt 加密密码
     * @param password 明文密码
     * @return 加密后的密码
     */
    public static String bcryptEncrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * BCrypt 校验密码
     * @param password 明文密码
     * @param hashedPassword 数据库中的加密密码
     * @return 是否匹配
     */
    public static boolean bcryptCheck(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }


    // ==================== AES 对称加密（可逆）====================

    // AES 密钥（16/24/32字节），生产环境放配置文件或环境变量
    private static final String AES_KEY = "GoldSim2024!@#ABC"; // 16字节

    /**
     * AES 加密
     * @param content 明文
     * @return 密文（Base64）
     */
    public static String aesEncrypt(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
        return aes.encryptBase64(content);
    }

    /**
     * AES 解密
     * @param encryptStr 密文（Base64）
     * @return 明文
     */
    public static String aesDecrypt(String encryptStr) {
        if (encryptStr == null || encryptStr.isEmpty()) {
            return null;
        }
        AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
        return aes.decryptStr(encryptStr);
    }


    // ==================== MD5 摘要（不可逆）====================

    /**
     * MD5 加密（32位小写）
     * @param str 明文
     * @return MD5 摘要
     */
    public static String md5(String str) {
        return SecureUtil.md5(str);
    }

    /**
     * MD5 加密（带盐，防彩虹表）
     * @param str 明文
     * @param salt 盐值
     * @return MD5 摘要
     */
    public static String md5WithSalt(String str, String salt) {
        return SecureUtil.md5(str + salt);
    }


    // ==================== 测试 ====================

    public static void main(String[] args) {
        // BCrypt 测试
        String password = "123456";
        String hashed = bcryptEncrypt(password);
        System.out.println("BCrypt 加密: " + hashed);
        System.out.println("BCrypt 校验: " + bcryptCheck(password, hashed));
        System.out.println("BCrypt 校验错误: " + bcryptCheck("wrong", hashed));

        // AES 测试
        String idCard = "110101199001011234";
        String encrypted = aesEncrypt(idCard);
        System.out.println("\nAES 加密: " + encrypted);
        System.out.println("AES 解密: " + aesDecrypt(encrypted));

        // MD5 测试
        System.out.println("\nMD5: " + md5("123456"));
        System.out.println("MD5+Salt: " + md5WithSalt("123456", "GoldSim"));
    }
}