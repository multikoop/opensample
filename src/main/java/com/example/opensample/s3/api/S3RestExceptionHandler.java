package com.example.opensample.s3.api;

import com.example.opensample.s3.service.S3SampleObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = S3SampleDataController.class)
public class S3RestExceptionHandler {

    @ExceptionHandler(S3SampleObjectNotFoundException.class)
    public ProblemDetail handleS3ObjectNotFound(S3SampleObjectNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("S3 object not found");
        return detail;
    }
}
