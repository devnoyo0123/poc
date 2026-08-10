package com.career.pathenum

import com.career.pathenum.exception.InvalidDepthException
import com.career.pathenum.exception.ParentNotFoundException
import com.career.pathenum.exception.PostMismatchException
import com.career.pathenum.model.dto.CommentTreeNode
import com.career.pathenum.model.dto.CreateCommentRequest
import com.career.pathenum.repository.CommentRepository
import com.career.pathenum.service.CommentService
import com.career.pathenum.util.Base62Encoder
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Path Enumeration 댓글 시스템 통합 테스트.
 *
 * Testcontainers MySQL 8 + R2DBC + Flyway 구성에서:
 *   1. Base62Encoder 단위 검증
 *   2. Service - create / tree / descendants / ancestors / delete / move
 *   3. utf8mb4_bin collation 이 보장하는 path 대소문자 구분
 *   4. LIKE prefix 로 자손/삭제/이동이 정확히 동작하는지
 */
@SpringBootTest
@Testcontainers
class CommentIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val mysql: MySQLContainer<Nothing> = MySQLContainer<Nothing>(
            DockerImageName.parse("mysql:8.0").asCompatibleSubstituteFor("mysql:8.0")
        ).apply {
            withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            withUsername("root")
            withPassword("root")
            withDatabaseName("path_enum")
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProps(r: DynamicPropertyRegistry) {
            // R2DBC URL (asyncer r2dbc-mysql)
            r.add("spring.r2dbc.url") {
                "r2dbc:mysql://${mysql.host}:${mysql.firstMappedPort}/path_enum"
            }
            r.add("spring.r2dbc.username") { "root" }
            r.add("spring.r2dbc.password") { "root" }
            // Flyway (JDBC)
            r.add("spring.flyway.url") { mysql.jdbcUrl }
            r.add("spring.flyway.user") { "root" }
            r.add("spring.flyway.password") { "root" }
        }
    }

    @Autowired
    private lateinit var service: CommentService

    @Autowired
    private lateinit var repo: CommentRepository

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    /** 각 테스트 격리를 위해 comments 테이블을 비운다. Mono → awaitSingle 로 실제 실행을 보장한다. */
    @BeforeEach
    fun cleanUp() {
        runBlocking {
            databaseClient.sql("DELETE FROM comments").fetch().rowsUpdated().awaitSingle()
        }
        Unit
    }

    // =========================================================================
    // Base62Encoder
    // =========================================================================

    @Test
    fun `base62 encoding values`() {
        // 500 = 8*62 + 4 → "84" → "00084"
        assertThat(Base62Encoder.encode(0)).isEqualTo("00000")
        assertThat(Base62Encoder.encode(1)).isEqualTo("00001")
        assertThat(Base62Encoder.encode(9)).isEqualTo("00009")
        assertThat(Base62Encoder.encode(10)).isEqualTo("0000A")
        assertThat(Base62Encoder.encode(35)).isEqualTo("0000Z")
        assertThat(Base62Encoder.encode(36)).isEqualTo("0000a")
        assertThat(Base62Encoder.encode(61)).isEqualTo("0000z")
        assertThat(Base62Encoder.encode(62)).isEqualTo("00010")
        assertThat(Base62Encoder.encode(500)).isEqualTo("00084")
    }

    @Test
    fun `base62 round-trip decode`() {
        for (v in listOf(0L, 1L, 9L, 10L, 35L, 36L, 61L, 62L, 500L, 99999L)) {
            assertThat(Base62Encoder.decode(Base62Encoder.encode(v))).isEqualTo(v)
        }
    }

    @Test
    fun `depthOf returns path length divided by 5`() {
        assertThat(Base62Encoder.depthOf("00001")).isEqualTo(1)
        assertThat(Base62Encoder.depthOf("0000100002")).isEqualTo(2)
        assertThat(Base62Encoder.depthOf("000010000200003")).isEqualTo(3)
    }

    @Test
    fun `parentPath strips last 5 chars`() {
        assertThat(Base62Encoder.parentPath("00001")).isNull()
        assertThat(Base62Encoder.parentPath("0000100002")).isEqualTo("00001")
        assertThat(Base62Encoder.parentPath("000010000200003")).isEqualTo("0000100002")
    }

    // =========================================================================
    // Service: create
    // =========================================================================

    @Test
    fun `create root comment generates correct path`(): Unit = runBlocking {
        val c = service.createComment(CreateCommentRequest(postId = 1, parentId = null, content = "root"))
        assertThat(c.path).hasSize(5)
        assertThat(c.depth).isEqualTo(1)
        assertThat(c.parentId).isNull()
    }

    @Test
    fun `create reply appends base62 of id to parent path`(): Unit = runBlocking {
        val parent = service.createComment(CreateCommentRequest(1, null, "parent"))
        val child = service.createComment(CreateCommentRequest(1, parent.id, "child"))
        assertThat(child.path).hasSize(10)
        assertThat(child.path.take(5)).isEqualTo(parent.path)
        assertThat(Base62Encoder.decode(child.path.substring(5, 10))).isEqualTo(child.id)
        assertThat(child.depth).isEqualTo(2)
        assertThat(child.parentId).isEqualTo(parent.id)
    }

    @Test
    fun `create deeply nested comment chain depth 4`(): Unit = runBlocking {
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val c2 = service.createComment(CreateCommentRequest(1, c1.id, "c2"))
        val c3 = service.createComment(CreateCommentRequest(1, c2.id, "c3"))
        assertThat(c3.depth).isEqualTo(4)
        assertThat(c3.path).hasSize(20)
        assertThat(c3.path.take(5)).isEqualTo(r.path)
    }

    @Test
    fun `create reply with non-existent parent throws`() {
        assertThatThrownBy {
            runBlocking { service.createComment(CreateCommentRequest(1, 99999L, "x")) }
        }.isInstanceOf(ParentNotFoundException::class.java)
    }

    @Test
    fun `create reply with parent in different post throws`(): Unit = runBlocking {
        val p = service.createComment(CreateCommentRequest(1, null, "p"))
        assertThatThrownBy {
            runBlocking { service.createComment(CreateCommentRequest(2, p.id, "x")) }
        }.isInstanceOf(PostMismatchException::class.java)
    }

    // =========================================================================
    // Service: tree build
    // =========================================================================

    @Test
    fun `ORDER BY path returns hierarchical DFS order`(): Unit = runBlocking {
        //   r(1) ─ c1(2) ─ gc1(3)
        //       └ c2(4)
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))
        val c2 = service.createComment(CreateCommentRequest(1, r.id, "c2"))

        val tree = service.getCommentTree(1)
        val flatContents = flattenContents(tree)
        // path 순서 = DFS 순서: r → c1 → gc1 → c2
        assertThat(flatContents).containsExactly("r", "c1", "gc1", "c2")
    }

    @Test
    fun `tree structure has correct parent-child relationships`(): Unit = runBlocking {
        //   r ─ c1 ─ gc1
        //    └ c2
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val c2 = service.createComment(CreateCommentRequest(1, r.id, "c2"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))

        val tree = service.getCommentTree(1)
        assertThat(tree).hasSize(1)
        val root = tree.single()
        assertThat(root.comment.content).isEqualTo("r")
        assertThat(root.children).hasSize(2)
        val firstChild = root.children[0]
        val secondChild = root.children[1]
        assertThat(firstChild.comment.content).isEqualTo("c1")
        assertThat(secondChild.comment.content).isEqualTo("c2")
        assertThat(firstChild.children).hasSize(1)
        assertThat(firstChild.children.single().comment.content).isEqualTo("gc1")
        assertThat(secondChild.children).isEmpty()
    }

    // =========================================================================
    // Service: descendants / ancestors
    // =========================================================================

    @Test
    fun `getDescendants returns all subtree excluding self`(): Unit = runBlocking {
        //   r ─ c1 ─ gc1 ─ ggc
        //    └ c2
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val c2 = service.createComment(CreateCommentRequest(1, r.id, "c2"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))
        val ggc = service.createComment(CreateCommentRequest(1, gc1.id, "ggc"))

        val descs = service.getDescendants(r.id)
        // r 자신 제외 → c1, c2, gc1, ggc = 4개
        assertThat(descs).hasSize(4)
        assertThat(descs.map { it.content }).containsExactlyInAnyOrder("c1", "c2", "gc1", "ggc")
    }

    @Test
    fun `getAncestors returns path from root excluding self`(): Unit = runBlocking {
        // r ─ c1 ─ gc ─ ggc
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val gc = service.createComment(CreateCommentRequest(1, c1.id, "gc"))
        val ggc = service.createComment(CreateCommentRequest(1, gc.id, "ggc"))

        val ancestors = service.getAncestors(ggc.id)
        // ggc 자신 제외, 루트 방향 → [r, c1, gc]
        assertThat(ancestors).hasSize(3)
        assertThat(ancestors.map { it.content }).containsExactly("r", "c1", "gc")
    }

    // =========================================================================
    // Service: delete / move
    // =========================================================================

    @Test
    fun `delete root removes entire subtree`(): Unit = runBlocking {
        //   r ─ c1 ─ gc1
        //    └ c2 ─ gc2
        //  + c3 (직속)
        // 총 6개 노드 생성
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val c2 = service.createComment(CreateCommentRequest(1, r.id, "c2"))
        val c3 = service.createComment(CreateCommentRequest(1, r.id, "c3"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))
        val gc2 = service.createComment(CreateCommentRequest(1, c2.id, "gc2"))

        val deleted = service.deleteComment(r.id)

        // FK ON DELETE CASCADE 가 자손을 자동 삭제하므로
        // `DELETE WHERE path LIKE '00001%'` 의 rowsUpdated 는 직접 삭제한 행 수(=1) 만 반환한다.
        // 나머지 5행은 CASCADE 가 처리. 우리가 검증해야 할 것은 "모든 자손이 사라졌는가".
        assertThat(deleted).isGreaterThanOrEqualTo(1L)

        val remaining = repo.findByPostIdOrderByPath(1)
        assertThat(remaining).isEmpty()
    }

    @Test
    fun `delete leaf removes only itself`(): Unit = runBlocking {
        // r ─ c1 ─ gc1 (leaf)
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))

        val deleted = service.deleteComment(gc1.id)
        assertThat(deleted).isEqualTo(1L)

        val remaining = repo.findByPostIdOrderByPath(1).map { it.content }
        assertThat(remaining).containsExactlyInAnyOrder("r", "c1")
    }

    @Test
    fun `move comment with descendants updates all paths`(): Unit = runBlocking {
        // 트리 1: r1 ─ a ─ a1
        //              └ a2
        // 트리 2: r2
        // a 를 r2 아래로 이동 → a, a1, a2 의 path 가 r2 하위로 갱신되어야 한다.
        val r1 = service.createComment(CreateCommentRequest(1, null, "r1"))
        val a = service.createComment(CreateCommentRequest(1, r1.id, "a"))
        val a1 = service.createComment(CreateCommentRequest(1, a.id, "a1"))
        val a2 = service.createComment(CreateCommentRequest(1, a.id, "a2"))
        val r2 = service.createComment(CreateCommentRequest(1, null, "r2"))

        val oldAPath = a.path
        val affected = service.moveComment(a.id, r2.id)

        // a + a1 + a2 = 3행 갱신
        assertThat(affected).isEqualTo(3L)

        // 이동 후 상태 재조회
        val movedA = service.getComment(a.id)
        val movedA1 = service.getComment(a1.id)
        val movedA2 = service.getComment(a2.id)

        // 새 부모 r2 의 path 로 시작해야 함
        assertThat(movedA.path).startsWith(r2.path)
        assertThat(movedA.path).isNotEqualTo(oldAPath)
        assertThat(movedA.depth).isEqualTo(r2.depth + 1)
        assertThat(movedA1.path).startsWith(movedA.path)
        assertThat(movedA1.depth).isEqualTo(movedA.depth + 1)
        assertThat(movedA2.path).startsWith(movedA.path)
        assertThat(movedA2.depth).isEqualTo(movedA.depth + 1)
    }

    @Test
    fun `move to root sets depth to 1 and trims path`(): Unit = runBlocking {
        // r ─ c1 ─ gc1 을 루트로 이동
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))

        service.moveComment(c1.id, null)

        val moved = service.getComment(c1.id)
        val movedGc = service.getComment(gc1.id)
        assertThat(moved.depth).isEqualTo(1)
        assertThat(moved.path).hasSize(5)
        // 자손 depth 도 -1 씩
        assertThat(movedGc.depth).isEqualTo(2)
        assertThat(movedGc.path).startsWith(moved.path)
    }

    @Test
    fun `move to own descendant throws InvalidDepthException`(): Unit = runBlocking {
        // r ─ c1 ─ gc1
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))

        assertThatThrownBy {
            runBlocking { service.moveComment(r.id, gc1.id) }
        }.isInstanceOf(InvalidDepthException::class.java)
    }

    // =========================================================================
    // utf8mb4_bin collation 검증 (★ 핵심 학습 포인트)
    // =========================================================================

    @Test
    fun `utf8mb4_bin ensures A and a are distinct paths`(): Unit = runBlocking {
        // ID 10  → base62 "0000A"
        // ID 36  → base62 "0000a"
        // 서로 다른 path 를 가져야 LIKE prefix 가 겹치지 않는다.
        //
        // 시나리오:
        //   - post 1 에 댓글 10개 생성 (ID 1..10), 마지막 댓글이 ID=10 → path="0000A"
        //   - post 1 에 댓글 36개 있는 상황을 만들려면 너무 많으므로,
        //     직접 repository.save() 로 ID 10, 36 의 루트 댓글을 강제 주입한다.
        val comment10 = com.career.pathenum.model.Comment(
            id = 10,
            postId = 1,
            parentId = null,
            path = Base62Encoder.encode(10),   // "0000A"
            depth = 1,
            content = "uppercase-A",
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = null
        )
        val comment36 = com.career.pathenum.model.Comment(
            id = 36,
            postId = 1,
            parentId = null,
            path = Base62Encoder.encode(36),   // "0000a"
            depth = 1,
            content = "lowercase-a",
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = null
        )
        repo.save(comment10)
        repo.save(comment36)

        // path 가 실제로 다른 문자열
        assertThat(comment10.path).isEqualTo("0000A")
        assertThat(comment36.path).isEqualTo("0000a")
        assertThat(comment10.path).isNotEqualTo(comment36.path)

        // getDescendants(10) 은 자기 자신만 포함(comment10 에 자손이 없으므로 빈 리스트)
        val descsOf10 = service.getDescendants(10)
        assertThat(descsOf10).isEmpty()

        // getDescendants(36) 도 자기 자신만 → 빈 리스트
        val descsOf36 = service.getDescendants(36)
        assertThat(descsOf36).isEmpty()

        // 만약 collation 이 case-insensitive 였다면, LIKE '0000A%' 가 '0000a' 도 매칭해서
        // 10번의 자손으로 36번이 잘못 들어갔을 것이다. 빈 리스트 결과 = collation 이 binary 라는 증거.
    }

    @Test
    fun `LIKE prefix with depth+1 returns only direct children`(): Unit = runBlocking {
        // r ─ c1 ─ gc1 ─ ggc1
        //  └ c2 ─ gc2
        val r = service.createComment(CreateCommentRequest(1, null, "r"))
        val c1 = service.createComment(CreateCommentRequest(1, r.id, "c1"))
        val c2 = service.createComment(CreateCommentRequest(1, r.id, "c2"))
        val gc1 = service.createComment(CreateCommentRequest(1, c1.id, "gc1"))
        val gc2 = service.createComment(CreateCommentRequest(1, c2.id, "gc2"))
        val ggc1 = service.createComment(CreateCommentRequest(1, gc1.id, "ggc1"))

        // r 의 직속 자식 = [c1, c2]
        val directChildren = repo.findDirectChildren(1, r.path, r.depth)
        assertThat(directChildren).hasSize(2)
        assertThat(directChildren.map { it.content }).containsExactlyInAnyOrder("c1", "c2")

        // c1 의 직속 자식 = [gc1] 만 (ggc1 은 손자)
        val c1Children = repo.findDirectChildren(1, c1.path, c1.depth)
        assertThat(c1Children).hasSize(1)
        assertThat(c1Children.single().content).isEqualTo("gc1")
    }

    // =========================================================================
    // (Optional) Performance sanity
    // =========================================================================

    @Test
    fun `large tree query is fast enough`(): Unit = runBlocking {
        // 단일 post 에 1000개 댓글 생성 후 전체 path 정렬 조회 시간 로깅.
        // 엄격한 SLA 검증은 아니고, 극단적 지연이 없는지만 확인.
        val startCreate = System.currentTimeMillis()
        var prevRoot: com.career.pathenum.model.Comment? = null
        for (i in 1..1000) {
            if (i % 5 == 0 && prevRoot != null) {
                // 5번째마다 답글(루트 아래 한 단계 자식)을 섞어 깊이 변화 주기
                service.createComment(CreateCommentRequest(postId = 100, parentId = prevRoot!!.id, content = "reply-$i"))
            } else {
                prevRoot = service.createComment(CreateCommentRequest(postId = 100, parentId = null, content = "r-$i"))
            }
        }
        val created = System.currentTimeMillis() - startCreate

        val startQuery = System.currentTimeMillis()
        val tree = service.getCommentTree(100)
        val query = System.currentTimeMillis() - startQuery

        println("large_tree create_ms=$created query_ms=$query nodes=${tree.size}")
        assertThat(tree).isNotEmpty
        // 1000개 저장이 30초 안에 끝나야 함(PoC 기준)
        assertThat(created).isLessThan(30_000)
        // 트리 빌드는 5초 안에
        assertThat(query).isLessThan(5_000)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** 트리를 DFS 로 평탄화해 content 문자열 리스트로 반환. */
    private fun flattenContents(tree: List<CommentTreeNode>): List<String> {
        val out = mutableListOf<String>()
        fun walk(nodes: List<CommentTreeNode>) {
            for (n in nodes) {
                out.add(n.comment.content)
                walk(n.children)
            }
        }
        walk(tree)
        return out
    }
}
