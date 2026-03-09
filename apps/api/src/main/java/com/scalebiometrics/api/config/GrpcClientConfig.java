package com.scalebiometrics.api.config;

import com.scalebiometrics.proto.matcher.MatcherServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${grpc.client.master.address}")
    private String masterAddress;

    @Bean
    public ManagedChannel masterChannel() {
        // masterAddress format expected: "host:port" or "static://host:port"
        // For simplicity, we assume "host:port" or handle the URI parsing if needed.
        // Here we strip "static://" if present for ManagedChannelBuilder.forTarget
        String target = masterAddress.replace("static://", "");
        return ManagedChannelBuilder.forTarget(target)
                .usePlaintext() // Disable TLS for internal communication
                .build();
    }

    @Bean
    public MatcherServiceGrpc.MatcherServiceBlockingStub matcherServiceStub(ManagedChannel masterChannel) {
        return MatcherServiceGrpc.newBlockingStub(masterChannel);
    }
}
