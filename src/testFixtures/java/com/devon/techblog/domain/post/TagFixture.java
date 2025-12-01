package com.devon.techblog.domain.post;

import com.devon.techblog.domain.post.entity.Tag;
import org.springframework.test.util.ReflectionTestUtils;

public class TagFixture {
    public static String DEFAULT_POST_NAME = "Java";

    public static Tag create(){
        return Tag.create(DEFAULT_POST_NAME);
    }

    public static Tag create(String name){
        return Tag.create(name);
    }

    public static Tag createWitId(Long id){
        Tag tag = Tag.create(DEFAULT_POST_NAME);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    public static Tag createWitId(Long id, String name){
        Tag tag = Tag.create(name);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }


}
