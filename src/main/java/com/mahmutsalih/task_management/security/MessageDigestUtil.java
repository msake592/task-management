package com.mahmutsalih.task_management.security;

import java.security.MessageDigest;

final class MessageDigestUtil {

    private MessageDigestUtil() {
    }

    static boolean isEqual(byte[] left, byte[] right) {
        return MessageDigest.isEqual(left, right);
    }
}
