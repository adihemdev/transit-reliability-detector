package com.transit.reliability.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class ServiceAlertHandler
        implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(
            Map<String, Object> input,
            Context context) {

        context.getLogger().log(
                "Received service alert: " + input
        );

        return input;
    }
}