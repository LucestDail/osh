package com.project.osh.crypto;

import com.project.osh.enumeration.CryptEnumeration;
import com.project.osh.crypto.cryptUtil.*;

/**
 * @author Oh Seung Hyun
 * @version 1.0.0
 * @implNote Modulations for encrypting and decrypting
 *
 */
public class CryptCore {
	
	public String enc(String str, CryptEnumeration type) {
		String strValue = "";
		try {
			switch(type){
				case AES256 :
					AES256 aes256 = new AES256();
					strValue = aes256.encrypt(str);
					break;
				case SHA256 :
					SHA256 sha256 = new SHA256();
					strValue = sha256.sha256(str);
				default :
					break;
			}
		}catch(Exception e) {
			e.printStackTrace();
			strValue = "encrypt Error [" + type + "] : " + str;
		}
		
		return strValue;
	}
	
	public String dec(String str, CryptEnumeration type) {
		String strValue = "";
		try {
			switch(type){
				case AES256 :
					AES256 aes256 = new AES256();
					strValue = aes256.decrypt(str);
					break;
				default :
					break;
			}
		}catch(Exception e) {
			e.printStackTrace();
			strValue = "decrypt Error [" + type + "] : " + str;
		}
		return strValue;
	}
}
