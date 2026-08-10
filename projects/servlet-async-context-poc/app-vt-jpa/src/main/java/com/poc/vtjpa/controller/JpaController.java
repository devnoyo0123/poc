package com.poc.vtjpa.controller;

import com.poc.vtjpa.dto.AggregatedResult;
import com.poc.vtjpa.dto.MemberDto;
import com.poc.vtjpa.dto.ProductDto;
import com.poc.vtjpa.repository.MemberRepository;
import com.poc.vtjpa.repository.ProductRepository;
import com.poc.vtjpa.service.JpaAggregationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/jpa")
public class JpaController {

    private final JpaAggregationService service;
    private final Executor jpaPlatformExecutor;
    private final Executor jpaVirtualExecutor;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public JpaController(JpaAggregationService service,
                         @Qualifier("jpaPlatformExecutor") Executor jpaPlatformExecutor,
                         @Qualifier("jpaVirtualExecutor") Executor jpaVirtualExecutor,
                         MemberRepository memberRepository,
                         ProductRepository productRepository) {
        this.service = service;
        this.jpaPlatformExecutor = jpaPlatformExecutor;
        this.jpaVirtualExecutor = jpaVirtualExecutor;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of(
                "memberCount", memberRepository.count(),
                "productCount", productRepository.count()
        );
    }

    @GetMapping("/sequential")
    public AggregatedResult sequential() {
        return service.sequential();
    }

    @GetMapping("/parallel-platform")
    public AggregatedResult parallelPlatform() {
        return service.parallel(jpaPlatformExecutor);
    }

    @GetMapping("/parallel-virtual")
    public AggregatedResult parallelVirtual() {
        return service.parallel(jpaVirtualExecutor);
    }

    @GetMapping("/async-fanout")
    public CompletableFuture<AggregatedResult> asyncFanout() {
        long start = System.currentTimeMillis();

        CompletableFuture<MemberDto> mf = CompletableFuture.supplyAsync(service::findMemberSlow, jpaPlatformExecutor);
        CompletableFuture<ProductDto> pf = CompletableFuture.supplyAsync(service::findProductSlow, jpaPlatformExecutor);

        return CompletableFuture.allOf(mf, pf)
                .thenApply(v -> {
                    long elapsed = System.currentTimeMillis() - start;
                    return new AggregatedResult(mf.join(), pf.join(), elapsed, "ASYNC_FANOUT");
                });
    }
}
