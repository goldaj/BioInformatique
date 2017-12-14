package service.impl;

import com.google.api.client.http.*;
import com.google.api.client.util.ExponentialBackOff;

import java.io.IOException;

class HttpRequestInitializer implements com.google.api.client.http.HttpRequestInitializer {

    private ExponentialBackOff buildBackOff() {
        return new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(500)
                .setMaxElapsedTimeMillis(900000)
                .setMaxIntervalMillis(6000)
                .setMultiplier(1.5)
                .setRandomizationFactor(0.5)
                .build();
    }

    @Override
    public void initialize(HttpRequest httpRequest) throws IOException {
        httpRequest.setReadTimeout(60000);
    }
}
