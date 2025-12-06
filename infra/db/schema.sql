CREATE DATABASE IF NOT EXISTS `mydb`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `mydb`;

-- MEMBER
CREATE TABLE IF NOT EXISTS `member` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `email`            VARCHAR(255) NOT NULL,
    `password`         VARCHAR(255) NOT NULL,
    `nickname`         VARCHAR(10)  NOT NULL,
    `profile_image_url` VARCHAR(500),
    `status`           VARCHAR(255) NOT NULL,
    `role`             VARCHAR(255) NOT NULL,
    `last_login_at`    DATETIME(6),
    `handle`           VARCHAR(50),
    `bio`              TEXT,
    `company`          VARCHAR(100),
    `location`         VARCHAR(100),
    `primary_stack`    TEXT,
    `interests`        TEXT,
    `social_links`     TEXT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SERIES
CREATE TABLE IF NOT EXISTS `series` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `member_id`   BIGINT       NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `description` TEXT,
    `thumbnail`   VARCHAR(500),
    `is_deleted`  TINYINT(1)   NOT NULL,
    `created_at`  DATETIME(6)  NOT NULL,
    `updated_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_series_member_id` (`member_id`),
    CONSTRAINT `fk_series_member`
      FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
      ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- POST
CREATE TABLE IF NOT EXISTS `post` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `member_id`        BIGINT       NOT NULL,
    `title`            VARCHAR(200) NOT NULL,
    `content`          TEXT         NOT NULL,
    `views_count`      BIGINT       NOT NULL,
    `like_count`       BIGINT       NOT NULL,
    `comment_count`    BIGINT       NOT NULL,
    `is_deleted`       TINYINT(1)   NOT NULL,
    `summary`          VARCHAR(500),
    `series_id`        BIGINT,
    `visibility`       VARCHAR(20),
    `is_draft`         TINYINT(1)   NOT NULL,
    `comments_allowed` TINYINT(1)   NOT NULL,
    `thumbnail`        VARCHAR(500),
    `image_url`        VARCHAR(500),
    `created_at`       DATETIME(6)  NOT NULL,
    `updated_at`       DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_post_member_id` (`member_id`),
    KEY `idx_post_series_id` (`series_id`),
    CONSTRAINT `fk_post_member`
      FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
      ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_post_series`
      FOREIGN KEY (`series_id`) REFERENCES `series` (`id`)
      ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TAG
CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50)  NOT NULL,
    `usage_count` BIGINT       NOT NULL,
    `created_at`  DATETIME(6)  NOT NULL,
    `updated_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- COMMENT
CREATE TABLE IF NOT EXISTS `comment` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `post_id`    BIGINT      NOT NULL,
    `member_id`  BIGINT      NOT NULL,
    `content`    TEXT        NOT NULL,
    `is_deleted` TINYINT(1)  NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_comment_post_id` (`post_id`),
    KEY `idx_comment_member_id` (`member_id`),
    CONSTRAINT `fk_comment_post`
      FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
      ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comment_member`
      FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
      ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- POST_TAG (composite key)
CREATE TABLE IF NOT EXISTS `post_tag` (
    `post_id` BIGINT NOT NULL,
    `tag_id`  BIGINT NOT NULL,
    PRIMARY KEY (`post_id`, `tag_id`),
    KEY `idx_post_tag_tag_id` (`tag_id`),
    CONSTRAINT `fk_post_tag_post`
      FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
      ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_post_tag_tag`
      FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`)
      ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- POST_LIKE (composite key, CreatedOnlyEntity)
CREATE TABLE IF NOT EXISTS `post_like` (
    `post_id`   BIGINT      NOT NULL,
    `member_id` BIGINT      NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`post_id`, `member_id`),
    KEY `idx_post_like_member_id` (`member_id`),
    CONSTRAINT `fk_post_like_post`
      FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
      ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_post_like_member`
      FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
      ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FILE
CREATE TABLE IF NOT EXISTS `file` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `file_type`     VARCHAR(20)  NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `storage_key`   VARCHAR(500) NOT NULL,
    `url`           VARCHAR(500) NOT NULL,
    `size`          BIGINT       NOT NULL,
    `mime_type`     VARCHAR(100) NOT NULL,
    `status`        VARCHAR(20)  NOT NULL,
    `is_deleted`    TINYINT(1)   NOT NULL,
    `created_at`    DATETIME(6)  NOT NULL,
    `updated_at`    DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- OAUTH_MEMBER
CREATE TABLE IF NOT EXISTS `oauth_member` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `provider`    VARCHAR(255) NOT NULL,
    `provider_id` VARCHAR(255) NOT NULL,
    `member_id`   BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_oauth_member_member_id` (`member_id`),
    CONSTRAINT `fk_oauth_member_member`
      FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
      ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY `uk_oauth_provider_pid` (`provider`, `provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
