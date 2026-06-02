package com.example.jpa.service;

import com.example.jpa.dto.ExternalApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExternalApiServiceTest {

    @Autowired
    private ExternalApiService externalApiService;

    @Test
    @DisplayName("외부 API를 호출하여 게시글 목록을 가져온다")
    void fetchPosts_shouldReturnListOfPosts() {
        // when
        List<ExternalApiResponse> posts = externalApiService.fetchPosts();

        // then
        assertThat(posts).isNotEmpty();
        assertThat(posts.size()).isGreaterThan(0);
        assertThat(posts.get(0).getId()).isNotNull();
        assertThat(posts.get(0).getTitle()).isNotNull();
    }

    @Test
    @DisplayName("특정 ID의 게시글을 가져온다")
    void fetchPostById_shouldReturnSinglePost() {
        // given
        Long postId = 1L;

        // when
        ExternalApiResponse post = externalApiService.fetchPostById(postId);

        // then
        assertThat(post).isNotNull();
        assertThat(post.getId()).isEqualTo(postId);
        assertThat(post.getTitle()).isNotNull();
        assertThat(post.getBody()).isNotNull();
    }
}
