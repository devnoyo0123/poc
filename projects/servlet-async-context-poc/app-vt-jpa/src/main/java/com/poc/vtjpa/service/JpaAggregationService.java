package com.poc.vtjpa.service;

import com.poc.vtjpa.dto.AggregatedResult;
import com.poc.vtjpa.dto.MemberDto;
import com.poc.vtjpa.dto.ProductDto;
import com.poc.vtjpa.entity.Member;
import com.poc.vtjpa.entity.Product;
import com.poc.vtjpa.repository.MemberRepository;
import com.poc.vtjpa.repository.ProductRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class JpaAggregationService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final JpaAggregationService self;

    public JpaAggregationService(MemberRepository memberRepository,
                                  ProductRepository productRepository,
                                  @Lazy JpaAggregationService self) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.self = self;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public MemberDto findMemberSlow() {
        Member m = memberRepository.findFirstSlow();
        return MemberDto.from(m);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public ProductDto findProductSlow() {
        Product p = productRepository.findFirstSlow();
        return ProductDto.from(p);
    }

    public AggregatedResult sequential() {
        long start = System.currentTimeMillis();
        MemberDto m = self.findMemberSlow();
        ProductDto p = self.findProductSlow();
        long elapsed = System.currentTimeMillis() - start;
        return new AggregatedResult(m, p, elapsed, "SEQUENTIAL");
    }

    public AggregatedResult parallel(Executor executor) {
        long start = System.currentTimeMillis();

        CompletableFuture<MemberDto> mf = CompletableFuture.supplyAsync(self::findMemberSlow, executor);
        CompletableFuture<ProductDto> pf = CompletableFuture.supplyAsync(self::findProductSlow, executor);

        CompletableFuture.allOf(mf, pf).join();

        long elapsed = System.currentTimeMillis() - start;
        return new AggregatedResult(mf.join(), pf.join(), elapsed, "PARALLEL");
    }
}
