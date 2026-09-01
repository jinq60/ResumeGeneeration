package com.resume.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.template.entity.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 模板数据访问层。
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {

    /**
     * 按 code 查询模板（包含已逻辑删除的行）。
     * <p>
     * 自定义 {@code @Select} SQL 不会被 MyBatis-Plus 追加 {@code deleted = 0} 条件，
     * 可查到已删除行。uk_template_code 唯一索引不区分 deleted，
     * 创建模板前必须用它查重（{@code selectOne} 永远看不到已删行）。
     * </p>
     */
    @Select("SELECT * FROM template WHERE code = #{code} LIMIT 1")
    Template selectByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 按 id 查询模板（包含已逻辑删除/未上架的行），仅供渲染历史简历使用。
     * <p>
     * {@code selectById} 会自动追加 {@code deleted = 0}，永远取不到已删除模板；
     * 渲染场景（分享页/预览/PDF 导出）需要容忍模板被删除，保证历史简历仍可渲染。
     * 编辑/选择模板场景仍应走严格校验的 {@link #selectById}。
     * </p>
     */
    @Select("SELECT * FROM template WHERE id = #{id} LIMIT 1")
    Template selectByIdIncludingDeleted(@Param("id") String id);

    /**
     * 统计引用指定模板且未删除的简历数量（用于删除模板前的引用检查）。
     * <p>
     * template 模块不允许依赖 resume 模块（依赖方向：resume → template），
     * 因此用原生 SQL 统计 resume 表，避免引入对 resume 模块 Java 类的编译期依赖。
     * </p>
     */
    @Select("SELECT COUNT(*) FROM resume WHERE template_id = #{templateId} AND deleted = 0")
    long countResumeReferences(@Param("templateId") String templateId);

    /**
     * 复活已逻辑删除的模板行：绕过 {@code @TableLogic} 的 {@code deleted=0} 追加条件。
     * <p>
     * 普通 {@code updateById} 在 {@code deleted=1} 行上会因 WHERE 追加 {@code deleted=0} 导致影响行数为 0 而静默失败。
     * 此方法使用原生 {@code @Update} 直写，包含 {@code deleted} 列与全部可变字段。
     * </p>
     */
    @Update("UPDATE template SET code = #{code}, name = #{name}, category = #{category}, "
            + "thumbnail_url = #{thumbnailUrl}, description = #{description}, "
            + "config = #{config,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}, "
            + "html_template = #{htmlTemplate}, render_engine = #{renderEngine}, "
            + "is_builtin = #{isBuiltin}, is_premium = #{isPremium}, is_recommended = #{isRecommended}, "
            + "sort_order = #{sortOrder}, status = #{status}, version = #{version}, deleted = #{deleted}, "
            + "created_by = #{createdBy}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateIncludingDeleted(Template template);
}
