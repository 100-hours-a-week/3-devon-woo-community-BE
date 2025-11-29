package com.devon.techblog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.entity.Series;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class SeriesRepositoryTest {

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.create());
    }

    @AfterEach
    void tearDown() {
        seriesRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("시리즈를 저장하고 조회할 수 있다")
    void saveAndFind() {
        Series series = Series.create(member, "시리즈명", "시리즈 설명");
        Series saved = seriesRepository.save(series);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("시리즈명");
        assertThat(saved.getDescription()).isEqualTo("시리즈 설명");
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("회원 ID로 시리즈 목록을 조회할 수 있다")
    void findByMemberId() {
        seriesRepository.save(Series.create(member, "시리즈1", "설명1"));
        seriesRepository.save(Series.create(member, "시리즈2", "설명2"));

        List<Series> found = seriesRepository.findByMemberId(member.getId());

        assertThat(found).hasSize(2);
        assertThat(found).extracting("name").containsExactlyInAnyOrder("시리즈1", "시리즈2");
    }

    @Test
    @DisplayName("삭제된 시리즈는 회원 ID로 조회되지 않는다")
    void findByMemberId_excludesDeletedSeries() {
        seriesRepository.save(Series.create(member, "시리즈1", "설명1"));
        Series series2 = seriesRepository.save(Series.create(member, "시리즈2", "설명2"));

        series2.delete();
        seriesRepository.save(series2);

        List<Series> found = seriesRepository.findByMemberId(member.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("시리즈1");
    }

    @Test
    @DisplayName("ID로 삭제되지 않은 시리즈를 조회할 수 있다")
    void findByIdAndNotDeleted() {
        Series series = seriesRepository.save(Series.create(member, "시리즈명", "설명"));

        Optional<Series> found = seriesRepository.findByIdAndNotDeleted(series.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("시리즈명");
    }

    @Test
    @DisplayName("삭제된 시리즈는 ID로 조회되지 않는다")
    void findByIdAndNotDeleted_excludesDeletedSeries() {
        Series series = seriesRepository.save(Series.create(member, "시리즈명", "설명"));
        series.delete();
        seriesRepository.save(series);

        Optional<Series> found = seriesRepository.findByIdAndNotDeleted(series.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("다른 회원의 시리즈는 조회되지 않는다")
    void findByMemberId_filtersByMemberId() {
        Member otherMember = memberRepository.save(MemberFixture.create("other@test.com", "password", "other"));
        seriesRepository.save(Series.create(member, "내 시리즈", "설명"));
        seriesRepository.save(Series.create(otherMember, "다른 사람 시리즈", "설명"));

        List<Series> found = seriesRepository.findByMemberId(member.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("내 시리즈");
    }

    @Test
    @DisplayName("시리즈가 없는 회원의 경우 빈 목록이 반환된다")
    void findByMemberId_returnsEmptyListWhenNoSeries() {
        List<Series> found = seriesRepository.findByMemberId(member.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("삭제 후 복구한 시리즈는 다시 조회된다")
    void findByIdAndNotDeleted_includesRestoredSeries() {
        Series series = seriesRepository.save(Series.create(member, "시리즈명", "설명"));
        series.delete();
        seriesRepository.save(series);

        Optional<Series> deletedCheck = seriesRepository.findByIdAndNotDeleted(series.getId());
        assertThat(deletedCheck).isEmpty();

        series.restore();
        seriesRepository.save(series);

        Optional<Series> restoredCheck = seriesRepository.findByIdAndNotDeleted(series.getId());
        assertThat(restoredCheck).isPresent();
        assertThat(restoredCheck.get().getName()).isEqualTo("시리즈명");
    }
}
