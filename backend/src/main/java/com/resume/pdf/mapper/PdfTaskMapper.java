package com.resume.pdf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.pdf.entity.PdfTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * PDF 任务数据访问层。
 */
@Mapper
public interface PdfTaskMapper extends BaseMapper<PdfTask> {
}
