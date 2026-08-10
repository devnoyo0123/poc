package com.poc.vtjpa.init;

import com.poc.vtjpa.entity.Member;
import com.poc.vtjpa.entity.Product;
import com.poc.vtjpa.repository.MemberRepository;
import com.poc.vtjpa.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final int BATCH_SIZE = 100;
    private static final int TOTAL_ROWS = 10_000;

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public DataInitializer(MemberRepository memberRepository,
                            ProductRepository productRepository) {
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedMembers();
        seedProducts();
    }

    private void seedMembers() {
        if (memberRepository.count() > 0) {
            log.info("members already seeded (count={}) — skip", memberRepository.count());
            return;
        }
        log.info("seeding {} members in batches of {}...", TOTAL_ROWS, BATCH_SIZE);
        long start = System.currentTimeMillis();
        for (int offset = 0; offset < TOTAL_ROWS; offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, TOTAL_ROWS);
            List<Member> batch = new ArrayList<>(end - offset);
            Instant now = Instant.now();
            for (int i = offset; i < end; i++) {
                batch.add(new Member("member-" + i, "m" + i + "@test.local", now));
            }
            memberRepository.saveAll(batch);
            memberRepository.flush();
        }
        log.info("members seeded in {} ms", System.currentTimeMillis() - start);
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            log.info("products already seeded (count={}) — skip", productRepository.count());
            return;
        }
        log.info("seeding {} products in batches of {}...", TOTAL_ROWS, BATCH_SIZE);
        long start = System.currentTimeMillis();
        for (int offset = 0; offset < TOTAL_ROWS; offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, TOTAL_ROWS);
            List<Product> batch = new ArrayList<>(end - offset);
            Instant now = Instant.now();
            for (int i = offset; i < end; i++) {
                batch.add(new Product("product-" + i, BigDecimal.valueOf(1000L + i), now));
            }
            productRepository.saveAll(batch);
            productRepository.flush();
        }
        log.info("products seeded in {} ms", System.currentTimeMillis() - start);
    }
}
