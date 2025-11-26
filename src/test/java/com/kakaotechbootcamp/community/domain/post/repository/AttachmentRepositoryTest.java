package com.kakaotechbootcamp.community.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.config.annotation.RepositoryJpaTest;
import com.kakaotechbootcamp.community.domain.member.MemberFixture;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.PostFixture;
import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class AttachmentRepositoryTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

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
        attachmentRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("첨부파일을 저장하고 조회할 수 있다")
    void saveAndFind() {
        Attachment attachment = Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL);
        Attachment saved = attachmentRepository.save(attachment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAttachmentUrl()).isEqualTo(PostFixture.DEFAULT_IMAGE_URL);
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("게시글 ID로 첨부파일을 조회할 수 있다")
    void findByPostId() {
        Attachment attachment = attachmentRepository.save(Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL));

        Attachment found = attachmentRepository.findByPostId(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(attachment.getId());
        assertThat(found.getAttachmentUrl()).isEqualTo(PostFixture.DEFAULT_IMAGE_URL);
    }

    @Test
    @DisplayName("첨부파일이 없는 게시글 조회 시 빈 값을 반환한다")
    void findByPostId_notFound() {
        assertThat(attachmentRepository.findByPostId(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시글에 첨부파일이 하나만 존재하는 경우 조회된다")
    void findByPostId_singleAttachment() {
        Attachment attachment = attachmentRepository.save(Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL));

        Attachment found = attachmentRepository.findByPostId(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(attachment.getId());
    }

    @Test
    @DisplayName("첨부파일을 삭제 처리할 수 있다")
    void deleteAttachment() {
        Attachment attachment = attachmentRepository.save(Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL));
        attachment.delete();
        attachmentRepository.save(attachment);

        Attachment found = attachmentRepository.findById(attachment.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 첨부파일을 복구할 수 있다")
    void restoreAttachment() {
        Attachment attachment = attachmentRepository.save(Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL));
        attachment.delete();
        attachmentRepository.save(attachment);
        attachment.restore();
        attachmentRepository.save(attachment);

        Attachment found = attachmentRepository.findById(attachment.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }
}
