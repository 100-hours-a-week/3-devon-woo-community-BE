package com.devon.techblog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class FileRepositoryTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.create());
        post = postRepository.save(PostFixture.create(member));
    }

    @AfterEach
    void tearDown() {
        fileRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("첨부파일을 저장하고 조회할 수 있다")
    void saveAndFind() {
        File file = File.create(post, PostFixture.DEFAULT_IMAGE_URL);
        File saved = fileRepository.save(file);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAttachmentUrl()).isEqualTo(PostFixture.DEFAULT_IMAGE_URL);
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("게시글 ID로 첨부파일을 조회할 수 있다")
    void findByPostId() {
        File file = fileRepository.save(File.create(post, PostFixture.DEFAULT_IMAGE_URL));

        File found = fileRepository.findByPostId(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(file.getId());
        assertThat(found.getAttachmentUrl()).isEqualTo(PostFixture.DEFAULT_IMAGE_URL);
    }

    @Test
    @DisplayName("첨부파일이 없는 게시글 조회 시 빈 값을 반환한다")
    void findByPostId_notFound() {
        assertThat(fileRepository.findByPostId(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시글에 첨부파일이 하나만 존재하는 경우 조회된다")
    void findByPostId_singleAttachment() {
        File file = fileRepository.save(File.create(post, PostFixture.DEFAULT_IMAGE_URL));

        File found = fileRepository.findByPostId(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(file.getId());
    }

    @Test
    @DisplayName("첨부파일을 삭제 처리할 수 있다")
    void deleteAttachment() {
        File file = fileRepository.save(File.create(post, PostFixture.DEFAULT_IMAGE_URL));
        file.delete();
        fileRepository.save(file);

        File found = fileRepository.findById(file.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 첨부파일을 복구할 수 있다")
    void restoreAttachment() {
        File file = fileRepository.save(File.create(post, PostFixture.DEFAULT_IMAGE_URL));
        file.delete();
        fileRepository.save(file);
        file.restore();
        fileRepository.save(file);

        File found = fileRepository.findById(file.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }
}
