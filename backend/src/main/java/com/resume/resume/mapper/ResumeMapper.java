package com.resume.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 简历数据访问层。
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    /**
     * 统计已逻辑删除的简历数。
     * <p>
     * 自定义 {@code @Select} SQL 不会被 MyBatis-Plus 追加逻辑删除条件，
     * 因此可真实统计 deleted=1 的行（与 {@code selectCount} 自动追加 deleted=0 不同）。
     * </p>
     */
    @Select("SELECT COUNT(*) FROM resume WHERE deleted = 1")
    long countDeletedResumes();
}
