package org.ligson.k8sterminalforward;

import org.apache.commons.codec.binary.Base64;

public class Test {
    public static void main(String[] args) {
        System.out.println(Base64.encodeBase64String(new byte[]{0x0a}));
    }
}
