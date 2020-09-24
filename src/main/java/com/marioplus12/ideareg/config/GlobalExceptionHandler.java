package com.marioplus12.ideareg.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理
 *
 * @author 向磊
 * @since 2020-06-09 13:43:36
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrVO> exceptionHandler(HttpServletRequest request, Exception e) {
        ErrVO errVO = new ErrVO(request, e, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errVO);
    }

    @Data
    public static class ErrVO {

        private Integer code;

        private String path;

        private Long timestamp;

        private String message;

        private List<String> stackTrace;

        public ErrVO(HttpServletRequest request, Exception e, String message) {
            log.error(e.getMessage(), e);

            this.code = 500;
            this.message = StringUtils.isEmpty(message) ? e.getMessage() : message;
            this.timestamp = System.currentTimeMillis();
            this.path = request.getRequestURI();

            List<String> stackTraceList = new ArrayList<>();
            stackTraceList.add(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            for (StackTraceElement element : e.getStackTrace()) {
                stackTraceList.add("    at " + element);
            }
//            this.stackTrace = stackTraceList;

        }
    }
}
