package com.marioplus12.ideareg.util.archiver;

import java.io.IOException;

/**
 * 解压异常
 *
 * @author marioplus12
 */
public class DecompressionException extends IOException {
    public DecompressionException(String message) {
        super(message);
    }

    public DecompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
