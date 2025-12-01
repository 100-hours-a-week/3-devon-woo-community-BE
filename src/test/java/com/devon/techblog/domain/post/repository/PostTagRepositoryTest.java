package com.devon.techblog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostTag;
import com.devon.techblog.domain.post.entity.Tag;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class PostTagRepositoryTest {

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Post post;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(MemberFixture.create());
        post = postRepository.save(PostFixture.create(member));
        tag1 = tagRepository.save(Tag.create("java"));
        tag2 = tagRepository.save(Tag.create("spring"));
    }

    @AfterEach
    void tearDown() {
        postTagRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("PostTag를 저장하고 조회할 수 있다")
    void saveAndFind() {
        PostTag postTag = PostTag.of(post, tag1);
        PostTag saved = postTagRepository.save(postTag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPost()).isEqualTo(post);
        assertThat(saved.getTag()).isEqualTo(tag1);
    }

    @Test
    @DisplayName("Post ID로 Tag와 함께 PostTag를 조회할 수 있다")
    void findByPostIdWithTag() {
        postTagRepository.save(PostTag.of(post, tag1));
        postTagRepository.save(PostTag.of(post, tag2));

        List<PostTag> postTags = postTagRepository.findByPostIdWithTag(post.getId());

        assertThat(postTags).hasSize(2);
        assertThat(postTags).extracting(pt -> pt.getTag().getName())
                .containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("Post ID로 PostTag를 삭제할 수 있다")
    void deleteByPostId() {
        postTagRepository.save(PostTag.of(post, tag1));
        postTagRepository.save(PostTag.of(post, tag2));

        postTagRepository.deleteByPostId(post.getId());
        postTagRepository.flush();

        List<PostTag> postTags = postTagRepository.findByPostIdWithTag(post.getId());
        assertThat(postTags).isEmpty();
    }

    @Test
    @DisplayName("Post를 삭제하면 PostTag도 함께 삭제된다 (cascade)")
    void cascadeDeleteWithPost() {
        postTagRepository.save(PostTag.of(post, tag1));
        postTagRepository.save(PostTag.of(post, tag2));
        postTagRepository.flush();

        Long postId = post.getId();
        postRepository.delete(post);
        postRepository.flush();

        List<PostTag> postTags = postTagRepository.findByPostIdWithTag(postId);
        assertThat(postTags).isEmpty();
        assertThat(tagRepository.findById(tag1.getId())).isPresent();
        assertThat(tagRepository.findById(tag2.getId())).isPresent();
    }
}
