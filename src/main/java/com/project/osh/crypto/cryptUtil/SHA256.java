package com.project.osh.crypto.cryptUtil;

import java.security.MessageDigest;

public class SHA256 {
	/**
     * SHA-256 Hasing Method
     * 
     * @param bytes
     * @return
     * @throws Exception
     */
    public String sha256(String msg) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(msg.getBytes());
        return bytesToHex(md.digest());
    }
   
    /**
     * Convert Byte to Hex
     * 
     * @param bytes
     * @return
     */
    public String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b: bytes) {
          builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
